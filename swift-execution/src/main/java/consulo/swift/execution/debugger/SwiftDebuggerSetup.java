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
package consulo.swift.execution.debugger;

import consulo.nativeDev.debugger.NativeDebuggerInstallation;
import consulo.nativeDev.debugger.dap.LldbDapNativeDebuggerProvider;
import consulo.nativeDev.debugger.driver.NativeDebuggerSetup;
import consulo.swift.toolchain.SwiftToolchain;

import java.nio.file.Path;
import java.util.List;

/**
 * Prefers the LLDB of the toolchain the program was built with: a stock LLDB stops at breakpoints and steps fine, but
 * has no Swift type system - Swift variables, {@code po} and expression evaluation need the Swift toolchain's LLDB.
 */
public class SwiftDebuggerSetup implements NativeDebuggerSetup {
    private final SwiftToolchain myToolchain;

    public SwiftDebuggerSetup(SwiftToolchain toolchain) {
        myToolchain = toolchain;
    }

    @Override
    public List<String> getSourceLanguages() {
        return List.of("swift");
    }

    @Override
    public List<NativeDebuggerInstallation> getToolchainDebuggers() {
        Path debugAdapter = myToolchain.findDebugAdapter();
        if (debugAdapter == null) {
            return List.of();
        }
        return List.of(new NativeDebuggerInstallation(LldbDapNativeDebuggerProvider.ID, debugAdapter));
    }
}
