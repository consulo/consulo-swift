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

import consulo.localize.LocalizeValue;
import consulo.swift.execution.localize.SwiftExecutionLocalize;

import java.util.List;

/**
 * Which tests {@code swift test} runs. One debug session runs one framework - the test binary runs XCTest unless told
 * {@code --testing-library swift-testing} - so debugging {@link #ALL} debugs the XCTest tests.
 */
public enum SwiftTestFramework {
    ALL(List.of()),
    XCTEST(List.of("--disable-swift-testing")),
    SWIFT_TESTING(List.of("--disable-xctest"));

    private final List<String> mySwiftTestArguments;

    SwiftTestFramework(List<String> swiftTestArguments) {
        mySwiftTestArguments = swiftTestArguments;
    }

    public List<String> getSwiftTestArguments() {
        return mySwiftTestArguments;
    }

    public LocalizeValue getDisplayName() {
        return switch (this) {
            case ALL -> SwiftExecutionLocalize.testFrameworkAll();
            case XCTEST -> SwiftExecutionLocalize.testFrameworkXctest();
            case SWIFT_TESTING -> SwiftExecutionLocalize.testFrameworkSwiftTesting();
        };
    }
}
