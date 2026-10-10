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
package consulo.swift.packageManager.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.externalSystem.setting.ExternalSystemSettingsListener;

import java.util.Collection;
import java.util.Set;

@TopicAPI(ComponentScope.PROJECT)
public interface SwiftPackageSettingsListener extends ExternalSystemSettingsListener<SwiftPackageProjectSettings> {
    Class<SwiftPackageSettingsListener> TOPIC = SwiftPackageSettingsListener.class;

    @Override
    default void onProjectRenamed(String oldName, String newName) {
    }

    @Override
    default void onProjectsLinked(Collection<SwiftPackageProjectSettings> settings) {
    }

    @Override
    default void onProjectsUnlinked(Set<String> linkedProjectPaths) {
    }

    @Override
    default void onBulkChangeStart() {
    }

    @Override
    default void onBulkChangeEnd() {
    }
}
