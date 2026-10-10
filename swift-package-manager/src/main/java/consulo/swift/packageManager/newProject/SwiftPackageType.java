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
package consulo.swift.packageManager.newProject;

import consulo.localize.LocalizeValue;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;

/**
 * Templates of {@code swift package init --type}.
 */
public enum SwiftPackageType {
    EXECUTABLE("executable"),
    LIBRARY("library"),
    TOOL("tool"),
    EMPTY("empty");

    private final String myId;

    SwiftPackageType(String id) {
        myId = id;
    }

    public String getId() {
        return myId;
    }

    public LocalizeValue getDisplayName() {
        return switch (this) {
            case EXECUTABLE -> SwiftPackageManagerLocalize.packageTypeExecutable();
            case LIBRARY -> SwiftPackageManagerLocalize.packageTypeLibrary();
            case TOOL -> SwiftPackageManagerLocalize.packageTypeTool();
            case EMPTY -> SwiftPackageManagerLocalize.packageTypeEmpty();
        };
    }
}
