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

import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.runner.OutputToGeneralTestEventsConverter;
import consulo.util.dataholder.Key;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor;

import java.text.ParseException;

/**
 * Feeds {@code swift test} output - and the swift-testing events {@link SwiftTestEventStreamTailer} puts into it, marked
 * with {@link #EVENT_PREFIX} - through {@link SwiftTestOutputParser}.
 */
public class SwiftTestEventsConverter extends OutputToGeneralTestEventsConverter {
    public static final String EVENT_PREFIX = "\u0001swift-testing-event ";

    private final SwiftTestOutputParser myParser = new SwiftTestOutputParser();

    public SwiftTestEventsConverter(String testFrameworkName, TestConsoleProperties consoleProperties) {
        super(testFrameworkName, consoleProperties);
    }

    /**
     * Called from the stdout reader and from the event stream tailer.
     */
    @Override
    protected synchronized boolean processServiceMessages(String text, Key outputType, ServiceMessageVisitor visitor)
        throws ParseException {
        if (super.processServiceMessages(text, outputType, visitor)) {
            return true;
        }

        SwiftTestOutputParser.Result result = text.startsWith(EVENT_PREFIX)
            ? myParser.parseSwiftTestingEvent(text.substring(EVENT_PREFIX.length()).strip())
            : myParser.parseXCTestLine(text);
        for (String message : result.messages()) {
            super.processServiceMessages(message, outputType, visitor);
        }
        return result.consumed();
    }
}
