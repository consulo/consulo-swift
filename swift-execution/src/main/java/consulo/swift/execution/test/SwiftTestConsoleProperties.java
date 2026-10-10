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

import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.SMCustomMessagesParsing;
import consulo.execution.test.sm.runner.OutputToGeneralTestEventsConverter;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.sm.runner.SMTestLocator;

public class SwiftTestConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {
    public static final String TEST_FRAMEWORK_NAME = "Swift Test";

    public SwiftTestConsoleProperties(RunConfiguration configuration, Executor executor) {
        super(configuration, TEST_FRAMEWORK_NAME, executor);
        setIdBasedTestTree(true);
    }

    @Override
    public SMTestLocator getTestLocator() {
        return SwiftTestLocator.INSTANCE;
    }

    @Override
    public OutputToGeneralTestEventsConverter createTestEventsConverter(String testFrameworkName, TestConsoleProperties consoleProperties) {
        return new SwiftTestEventsConverter(testFrameworkName, consoleProperties);
    }
}
