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
package consulo.swift.packageManager.importing;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.setting.AbstractExternalProjectSettingsConfigurable;
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurableFactory;
import consulo.externalSystem.service.setting.ExternalSystemSettingsPlace;
import consulo.project.ProjectManager;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.swift.packageManager.setting.SwiftPackageProjectSettings;
import consulo.swift.packageManager.setting.SwiftPackageSettings;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionImpl
public class SwiftPackageSettingsConfigurableFactory
    implements ExternalSystemSettingsConfigurableFactory<SwiftPackageProjectSettings, SwiftPackageSettings> {
    @Override
    public ProjectSystemId getSystemId() {
        return SwiftPackageManagerConstants.SYSTEM_ID;
    }

    @Override
    public SwiftPackageProjectSettings createProjectSettings() {
        return new SwiftPackageProjectSettings();
    }

    @Override
    public SwiftPackageSettings createSystemSettings() {
        return new SwiftPackageSettings(ProjectManager.getInstance().getDefaultProject());
    }

    @Override
    @RequiredUIAccess
    public AbstractExternalProjectSettingsConfigurable<SwiftPackageProjectSettings> createProjectSettingsConfigurable(
        SwiftPackageProjectSettings settings,
        ExternalSystemSettingsPlace place
    ) {
        return new SwiftPackageProjectSettingsConfigurable(settings, place);
    }
}
