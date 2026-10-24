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

import org.jspecify.annotations.NullMarked;

/**
 * Building ({@code swift build}), running, testing, native debugging (nativeDev + the toolchain's lldb-dap) and profiling of
 * Swift packages.
 */
@NullMarked
module consulo.swift.execution {
    requires consulo.swift.packageManager;
    requires consulo.compiler.api;
    requires consulo.build.ui.api;
    requires consulo.execution.api;
    requires consulo.execution.debug.api;
    requires consulo.execution.test.sm.api;
    requires consulo.project.ui.api;
    requires consulo.execution.profiler.api;
    requires consulo.nativeDev.debugger.api;
    requires consulo.nativeDev.debugger.dap;
    requires consulo.nativeDev.profiler.api;
    requires build.serviceMessages;
    requires com.google.gson;

    exports consulo.swift.execution.compiler;
    exports consulo.swift.execution.debugger;
    exports consulo.swift.execution.localize;
    exports consulo.swift.execution.profiler;
    exports consulo.swift.execution.run;
    exports consulo.swift.execution.test;

    opens consulo.swift.execution.run to consulo.util.xml.serializer, consulo.execution.api;
    opens consulo.swift.execution.test to consulo.util.xml.serializer, consulo.execution.api;
}
