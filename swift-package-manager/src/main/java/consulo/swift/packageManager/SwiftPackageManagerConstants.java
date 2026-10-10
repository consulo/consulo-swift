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
package consulo.swift.packageManager;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;

public interface SwiftPackageManagerConstants {
    ProjectSystemId SYSTEM_ID = new ProjectSystemId("SWIFT_PM", SwiftPackageManagerLocalize.systemName(), SwiftIconGroup.swift());

    String MANIFEST = "Package.swift";
    String RESOLVED = "Package.resolved";
    String BUILD_DIRECTORY = ".build";
    String SWIFTPM_DIRECTORY = ".swiftpm";

    String TASK_BUILD = "build";
    String TASK_TEST = "test";
    String TASK_CLEAN = "clean";
    String TASK_RESOLVE = "resolve";
    String TASK_UPDATE = "update";
}
