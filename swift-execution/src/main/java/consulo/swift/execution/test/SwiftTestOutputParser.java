/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.swift.execution.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import consulo.execution.test.sm.ServiceMessageBuilder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns {@code swift test} output into events of an id-based test tree (as TeamCity service messages):
 * <ul>
 *     <li>XCTest - its console lines, the format of swift-corelibs-xctest (Linux, Windows) and of macOS XCTest</li>
 *     <li>swift-testing - lines of its JSON event stream ({@code --event-stream-output-path}, version 0), which unlike its
 *     console output names every test by a stable id and gives its source location</li>
 * </ul>
 * Not thread-safe.
 */
public class SwiftTestOutputParser {
    public static final String ROOT_ID = "0";

    private static final String XCTEST_PREFIX = "xctest:";
    private static final String SWIFT_TESTING_PREFIX = "swift-testing:";

    private static final Pattern XCTEST_SUITE = Pattern.compile("^Test Suite '(.+)' (started|passed|failed) at .*$");
    private static final Pattern XCTEST_CASE_STARTED = Pattern.compile("^Test Case '(.+)' started(?: at .*|\\.)$");
    private static final Pattern XCTEST_CASE_FINISHED =
        Pattern.compile("^Test Case '(.+)' (passed|failed|skipped) \\((\\d+(?:\\.\\d+)?) seconds\\)\\.?$");
    /**
     * {@code /path/T.swift:10: error: GreetTests.testFails : XCTAssertEqual failed: ...}; macOS names the case
     * {@code -[Module.Class method]}.
     */
    private static final Pattern XCTEST_FAILURE = Pattern.compile("^(.+?):(\\d+): error: (.+?) : (.*)$");
    private static final Pattern MACOS_CASE = Pattern.compile("^-\\[(?:[^ .]+\\.)?([^ ]+) ([^ \\]]+)]$");

    /**
     * Result of one line: the service messages it makes, and whether the line itself is to be hidden from the console.
     */
    public record Result(List<String> messages, boolean consumed) {
        static final Result NONE = new Result(List.of(), false);
    }

    private record XCTestCase(String className, String method) {
        String id() {
            return XCTEST_PREFIX + className + "/" + method;
        }
    }

    private record Failure(String message, String details) {
    }

    private record SwiftTestingRecord(String id, String name, boolean suite, @Nullable String locationHint) {
    }

    private final Map<String, Failure> myXCTestFailures = new HashMap<>();

    private final Map<String, SwiftTestingRecord> mySwiftTestingRecords = new HashMap<>();
    private final Map<String, Double> mySwiftTestingStarts = new HashMap<>();
    private final Map<String, List<Failure>> mySwiftTestingFailures = new HashMap<>();
    private final Set<String> mySyntheticSuites = new LinkedHashSet<>();

    // region XCTest

    public Result parseXCTestLine(String rawLine) {
        String line = rawLine.strip();

        Matcher suite = XCTEST_SUITE.matcher(line);
        if (suite.matches()) {
            String name = suite.group(1);
            if (isBundleSuite(name)) {
                return new Result(List.of(), true);
            }
            String id = XCTEST_PREFIX + name;
            ServiceMessageBuilder message = "started".equals(suite.group(2))
                ? ServiceMessageBuilder.testSuiteStarted(name)
                .addAttribute("nodeId", id)
                .addAttribute("parentNodeId", ROOT_ID)
                .addAttribute("locationHint", SwiftTestLocator.XCTEST_PROTOCOL + "://" + name)
                : ServiceMessageBuilder.testSuiteFinished(name).addAttribute("nodeId", id);
            return new Result(List.of(message.toString()), true);
        }

        Matcher started = XCTEST_CASE_STARTED.matcher(line);
        if (started.matches()) {
            XCTestCase testCase = parseXCTestCase(started.group(1));
            if (testCase == null) {
                return Result.NONE;
            }
            String message = ServiceMessageBuilder.testStarted(testCase.method())
                .addAttribute("nodeId", testCase.id())
                .addAttribute("parentNodeId", XCTEST_PREFIX + testCase.className())
                .addAttribute("locationHint", SwiftTestLocator.XCTEST_PROTOCOL + "://" + testCase.className() + "/" + testCase.method())
                .toString();
            return new Result(List.of(message), true);
        }

        Matcher failure = XCTEST_FAILURE.matcher(line);
        if (failure.matches()) {
            XCTestCase testCase = parseXCTestCase(failure.group(3));
            if (testCase != null) {
                String location = failure.group(1) + ":" + failure.group(2);
                Failure previous = myXCTestFailures.get(testCase.id());
                String details = (previous == null ? "" : previous.details() + "\n") + location;
                myXCTestFailures.put(testCase.id(), new Failure(previous == null ? failure.group(4) : previous.message(), details));
            }
            // shown as output of the running test
            return Result.NONE;
        }

        Matcher finished = XCTEST_CASE_FINISHED.matcher(line);
        if (finished.matches()) {
            XCTestCase testCase = parseXCTestCase(finished.group(1));
            if (testCase == null) {
                return Result.NONE;
            }
            List<String> messages = new ArrayList<>();
            String status = finished.group(2);
            Failure testFailure = myXCTestFailures.remove(testCase.id());
            if ("failed".equals(status)) {
                messages.add(ServiceMessageBuilder.testFailed(testCase.method())
                    .addAttribute("nodeId", testCase.id())
                    .addAttribute("message", testFailure == null ? "" : testFailure.message())
                    .addAttribute("details", testFailure == null ? "" : testFailure.details())
                    .toString());
            }
            else if ("skipped".equals(status)) {
                messages.add(ServiceMessageBuilder.testIgnored(testCase.method()).addAttribute("nodeId", testCase.id()).toString());
            }
            long duration = Math.round(Double.parseDouble(finished.group(3)) * 1000);
            messages.add(ServiceMessageBuilder.testFinished(testCase.method())
                .addAttribute("nodeId", testCase.id())
                .addAttribute("duration", String.valueOf(duration))
                .toString());
            return new Result(messages, true);
        }

        return Result.NONE;
    }

    /**
     * "All tests", "Selected tests" and the bundle ("debug.xctest", "DemoPackageTests.xctest") only wrap the test classes.
     */
    private static boolean isBundleSuite(String name) {
        return "All tests".equals(name) || "Selected tests".equals(name) || name.endsWith(".xctest");
    }

    /**
     * {@code GreetTests.testGreet} (corelibs) or {@code -[DemoTests.GreetTests testGreet]} (macOS).
     */
    private static @Nullable XCTestCase parseXCTestCase(String name) {
        Matcher macOS = MACOS_CASE.matcher(name);
        if (macOS.matches()) {
            return new XCTestCase(macOS.group(1), macOS.group(2));
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return null;
        }
        String className = name.substring(0, dot);
        int moduleDot = className.lastIndexOf('.');
        return new XCTestCase(moduleDot < 0 ? className : className.substring(moduleDot + 1), name.substring(dot + 1));
    }

    // endregion

    // region swift-testing

    public Result parseSwiftTestingEvent(String line) {
        JsonObject root;
        try {
            root = JsonParser.parseString(line).getAsJsonObject();
        }
        catch (JsonParseException | IllegalStateException e) {
            return Result.NONE;
        }
        JsonObject payload = object(root, "payload");
        if (payload == null) {
            return Result.NONE;
        }

        if ("test".equals(string(root, "kind"))) {
            recordTest(payload);
            return new Result(List.of(), true);
        }

        List<String> messages = new ArrayList<>();
        String kind = string(payload, "kind");
        String testId = string(payload, "testID");
        SwiftTestingRecord record = testId == null ? null : mySwiftTestingRecords.get(testId);
        if ("runEnded".equals(kind)) {
            for (String suite : mySyntheticSuites) {
                String nodeId = SWIFT_TESTING_PREFIX + suite;
                messages.add(ServiceMessageBuilder.testSuiteFinished(suite).addAttribute("nodeId", nodeId).toString());
            }
            mySyntheticSuites.clear();
        }
        else if (record != null && kind != null) {
            onEvent(kind, record, payload, messages);
        }
        return new Result(messages, true);
    }

    private void recordTest(JsonObject payload) {
        String id = string(payload, "id");
        String name = string(payload, "name");
        if (id == null || name == null) {
            return;
        }
        String displayName = string(payload, "displayName");
        String locationHint = null;
        JsonObject location = object(payload, "sourceLocation");
        if (location != null) {
            String path = string(location, "_filePath");
            JsonElement line = location.get("line");
            if (path != null && line != null && line.isJsonPrimitive()) {
                locationHint = "file://" + path + ":" + line.getAsInt();
            }
        }
        boolean suite = "suite".equals(string(payload, "kind"));
        mySwiftTestingRecords.put(id, new SwiftTestingRecord(id, displayName != null ? displayName : name, suite, locationHint));
    }

    private void onEvent(String kind, SwiftTestingRecord record, JsonObject payload, List<String> messages) {
        String nodeId = SWIFT_TESTING_PREFIX + record.id();
        switch (kind) {
            case "testStarted" -> {
                mySwiftTestingStarts.put(record.id(), instant(payload));
                ServiceMessageBuilder message = record.suite()
                    ? ServiceMessageBuilder.testSuiteStarted(record.name())
                    : ServiceMessageBuilder.testStarted(record.name());
                message.addAttribute("nodeId", nodeId).addAttribute("parentNodeId", parentNodeId(record, messages));
                if (record.locationHint() != null) {
                    message.addAttribute("locationHint", record.locationHint());
                }
                messages.add(message.toString());
            }
            case "issueRecorded" -> {
                JsonObject issue = object(payload, "issue");
                JsonElement known = issue == null ? null : issue.get("isKnown");
                if (known != null && known.isJsonPrimitive() && known.getAsBoolean()) {
                    return;
                }
                List<String> texts = messageTexts(payload);
                String message = texts.isEmpty() ? "" : texts.get(0);
                mySwiftTestingFailures.computeIfAbsent(record.id(), id -> new ArrayList<>())
                    .add(new Failure(message, String.join("\n", texts)));
            }
            case "testSkipped" -> {
                List<String> texts = messageTexts(payload);
                messages.add(ServiceMessageBuilder.testStarted(record.name())
                    .addAttribute("nodeId", nodeId)
                    .addAttribute("parentNodeId", parentNodeId(record, messages))
                    .toString());
                messages.add(ServiceMessageBuilder.testIgnored(record.name())
                    .addAttribute("nodeId", nodeId)
                    .addAttribute("message", texts.isEmpty() ? "" : texts.get(0))
                    .toString());
                messages.add(ServiceMessageBuilder.testFinished(record.name()).addAttribute("nodeId", nodeId).toString());
            }
            case "testEnded" -> {
                if (record.suite()) {
                    messages.add(ServiceMessageBuilder.testSuiteFinished(record.name()).addAttribute("nodeId", nodeId).toString());
                    return;
                }
                List<Failure> failures = mySwiftTestingFailures.remove(record.id());
                if (failures != null && !failures.isEmpty()) {
                    List<String> details = new ArrayList<>();
                    for (Failure failure : failures) {
                        details.add(failure.details());
                    }
                    messages.add(ServiceMessageBuilder.testFailed(record.name())
                        .addAttribute("nodeId", nodeId)
                        .addAttribute("message", failures.get(0).message())
                        .addAttribute("details", String.join("\n\n", details))
                        .toString());
                }
                ServiceMessageBuilder finished = ServiceMessageBuilder.testFinished(record.name()).addAttribute("nodeId", nodeId);
                Double start = mySwiftTestingStarts.remove(record.id());
                double end = instant(payload);
                if (start != null && end >= start) {
                    finished.addAttribute("duration", String.valueOf(Math.round((end - start) * 1000)));
                }
                messages.add(finished.toString());
            }
            default -> {
            }
        }
    }

    /**
     * Ids nest: {@code Module.Suite/Inner/test()/File.swift:9:6}. A parent that is not a reported suite - the module of
     * top-level tests and suites - becomes a node of its own.
     */
    private String parentNodeId(SwiftTestingRecord record, List<String> messages) {
        String id = record.id();
        String parentId;
        if (record.suite()) {
            int slash = id.lastIndexOf('/');
            parentId = slash > 0 ? id.substring(0, slash) : moduleOf(id);
        }
        else {
            // drop the name and the source location
            int location = id.lastIndexOf('/');
            int name = location > 0 ? id.lastIndexOf('/', location - 1) : -1;
            parentId = name > 0 ? id.substring(0, name) : moduleOf(id);
        }

        SwiftTestingRecord parent = mySwiftTestingRecords.get(parentId);
        if (parent != null && parent.suite()) {
            return SWIFT_TESTING_PREFIX + parentId;
        }
        if (mySyntheticSuites.add(parentId)) {
            messages.add(ServiceMessageBuilder.testSuiteStarted(parentId)
                .addAttribute("nodeId", SWIFT_TESTING_PREFIX + parentId)
                .addAttribute("parentNodeId", ROOT_ID)
                .toString());
        }
        return SWIFT_TESTING_PREFIX + parentId;
    }

    private static String moduleOf(String id) {
        int slash = id.indexOf('/');
        String first = slash < 0 ? id : id.substring(0, slash);
        int dot = first.indexOf('.');
        return dot < 0 ? first : first.substring(0, dot);
    }

    private static double instant(JsonObject payload) {
        JsonObject instant = object(payload, "instant");
        JsonElement absolute = instant == null ? null : instant.get("absolute");
        return absolute != null && absolute.isJsonPrimitive() ? absolute.getAsDouble() : -1;
    }

    private static List<String> messageTexts(JsonObject payload) {
        List<String> texts = new ArrayList<>();
        JsonElement messages = payload.get("messages");
        if (messages instanceof JsonArray array) {
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    String text = string(element.getAsJsonObject(), "text");
                    if (text != null) {
                        texts.add(text);
                    }
                }
            }
        }
        return texts;
    }

    // endregion

    private static @Nullable JsonObject object(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
