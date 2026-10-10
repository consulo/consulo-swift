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

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixtures are what Swift 6.4 printed on Linux for an XCTest class (one passing, one failing test) and a swift-testing
 * suite (the same), and the swift-testing event stream of that run.
 */
public class SwiftTestOutputParserTest {
    @Test
    public void testXCTestOutput() throws Exception {
        SwiftTestOutputParser parser = new SwiftTestOutputParser();
        List<ServiceMessage> messages = new ArrayList<>();
        List<String> shown = new ArrayList<>();
        for (String line : fixture("swift-test-linux.out").split("\n")) {
            SwiftTestOutputParser.Result result = parser.parseXCTestLine(line + "\n");
            messages.addAll(parse(result.messages()));
            if (!result.consumed()) {
                shown.add(line);
            }
        }

        assertEquals(List.of(
            "testSuiteStarted GreetTests",
            "testStarted testFails",
            "testFailed testFails",
            "testFinished testFails",
            "testStarted testGreet",
            "testFinished testGreet",
            "testSuiteFinished GreetTests"
        ), names(messages));

        Map<String, String> suite = messages.get(0).getAttributes();
        assertEquals("xctest:GreetTests", suite.get("nodeId"));
        assertEquals(SwiftTestOutputParser.ROOT_ID, suite.get("parentNodeId"));

        Map<String, String> started = messages.get(1).getAttributes();
        assertEquals("xctest:GreetTests/testFails", started.get("nodeId"));
        assertEquals("xctest:GreetTests", started.get("parentNodeId"));
        assertEquals("swift-xctest://GreetTests/testFails", started.get("locationHint"));

        Map<String, String> failed = messages.get(2).getAttributes();
        assertEquals("XCTAssertEqual failed: (\"hi\") is not equal to (\"bye\") -", String.valueOf(failed.get("message")).strip());
        assertEquals("/work/demo/Tests/DemoTests/T.swift:10", failed.get("details"));

        assertEquals("1", messages.get(3).getAttributes().get("duration"));

        // the failure itself stays in the output of the test, swift-testing's console output is left alone
        assertTrue(shown.stream().anyMatch(line -> line.contains("error: GreetTests.testFails")));
        assertTrue(shown.stream().anyMatch(line -> line.startsWith("✔ Test greets() passed")));
        assertFalse(shown.stream().anyMatch(line -> line.startsWith("Test Case")));
    }

    @Test
    public void testMacOSXCTestNames() throws Exception {
        SwiftTestOutputParser parser = new SwiftTestOutputParser();
        List<ServiceMessage> messages = new ArrayList<>();
        messages.addAll(parse(parser.parseXCTestLine("Test Case '-[DemoTests.GreetTests testGreet]' started.").messages()));
        messages.addAll(parse(parser.parseXCTestLine("Test Case '-[DemoTests.GreetTests testGreet]' passed (0.002 seconds).").messages()));

        assertEquals(List.of("testStarted testGreet", "testFinished testGreet"), names(messages));
        assertEquals("xctest:GreetTests/testGreet", messages.get(0).getAttributes().get("nodeId"));
        assertEquals("2", messages.get(1).getAttributes().get("duration"));
    }

    @Test
    public void testSwiftTestingEvents() throws Exception {
        SwiftTestOutputParser parser = new SwiftTestOutputParser();
        List<ServiceMessage> messages = new ArrayList<>();
        for (String line : fixture("swift-testing-events.jsonl").split("\n")) {
            SwiftTestOutputParser.Result result = parser.parseSwiftTestingEvent(line);
            assertTrue(result.consumed());
            messages.addAll(parse(result.messages()));
        }

        assertEquals(List.of(
            "testSuiteStarted DemoTests",
            "testSuiteStarted GreetingSuite",
            "testStarted greets()",
            "testStarted Greeting is long",
            "testFinished greets()",
            "testFailed Greeting is long",
            "testFinished Greeting is long",
            "testSuiteFinished GreetingSuite",
            "testSuiteFinished DemoTests"
        ), names(messages));

        Map<String, String> module = messages.get(0).getAttributes();
        assertEquals("swift-testing:DemoTests", module.get("nodeId"));
        assertEquals(SwiftTestOutputParser.ROOT_ID, module.get("parentNodeId"));

        Map<String, String> suite = messages.get(1).getAttributes();
        assertEquals("swift-testing:DemoTests.GreetingSuite", suite.get("nodeId"));
        assertEquals("swift-testing:DemoTests", suite.get("parentNodeId"));
        assertEquals("file:///work/demo/Tests/DemoTests/SwiftTestingTests.swift:4", suite.get("locationHint"));

        Map<String, String> test = messages.get(3).getAttributes();
        assertEquals("swift-testing:DemoTests.GreetingSuite/failing()/SwiftTestingTests.swift:9:6", test.get("nodeId"));
        assertEquals("swift-testing:DemoTests.GreetingSuite", test.get("parentNodeId"));
        assertEquals("file:///work/demo/Tests/DemoTests/SwiftTestingTests.swift:9", test.get("locationHint"));

        Map<String, String> failed = messages.get(5).getAttributes();
        assertEquals("Expectation failed: greet().count > 5", failed.get("message"));
        assertNotNull(messages.get(4).getAttributes().get("duration"));
    }

    @Test
    public void testNotJson() {
        SwiftTestOutputParser.Result result = new SwiftTestOutputParser().parseSwiftTestingEvent("not json");
        assertTrue(result.messages().isEmpty());
    }

    private static List<ServiceMessage> parse(List<String> messages) throws ParseException {
        List<ServiceMessage> result = new ArrayList<>();
        for (String message : messages) {
            ServiceMessage parsed = ServiceMessage.parse(message);
            assertNotNull(parsed, message);
            result.add(parsed);
        }
        return result;
    }

    private static List<String> names(List<ServiceMessage> messages) {
        return messages.stream().map(message -> message.getMessageName() + " " + message.getAttributes().get("name")).toList();
    }

    private static String fixture(String name) throws IOException {
        try (InputStream stream = SwiftTestOutputParserTest.class.getResourceAsStream("/swifttest/" + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
