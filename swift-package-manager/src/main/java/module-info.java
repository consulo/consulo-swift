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
 * Swift Package Manager as an external system: importing {@code Package.swift}, auto-reload, package tasks.
 */
@NullMarked
module consulo.swift.packageManager {
    requires transitive consulo.swift.toolchain;
    requires transitive consulo.external.system.api;
    requires consulo.module.creation.api;
    requires com.google.gson;

    exports consulo.swift.packageManager;
    exports consulo.swift.packageManager.execution;
    exports consulo.swift.packageManager.importing;
    exports consulo.swift.packageManager.localize;
    exports consulo.swift.packageManager.module.extension;
    exports consulo.swift.packageManager.project;
    exports consulo.swift.packageManager.setting;
    exports consulo.swift.packageManager.task;

    opens consulo.swift.packageManager.setting to consulo.util.xml.serializer;
}
