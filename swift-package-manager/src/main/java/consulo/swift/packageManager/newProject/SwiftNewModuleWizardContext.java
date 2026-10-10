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

import consulo.content.bundle.Sdk;
import consulo.module.creation.NewModuleWizardContextBase;
import org.jspecify.annotations.Nullable;

public class SwiftNewModuleWizardContext extends NewModuleWizardContextBase {
    private @Nullable Sdk myToolchainBundle;
    private SwiftPackageType myPackageType = SwiftPackageType.EXECUTABLE;

    public SwiftNewModuleWizardContext(boolean isNewProject) {
        super(isNewProject);
    }

    public @Nullable Sdk getToolchainBundle() {
        return myToolchainBundle;
    }

    public void setToolchainBundle(@Nullable Sdk toolchainBundle) {
        myToolchainBundle = toolchainBundle;
    }

    public SwiftPackageType getPackageType() {
        return myPackageType;
    }

    public void setPackageType(@Nullable SwiftPackageType packageType) {
        myPackageType = packageType == null ? SwiftPackageType.EXECUTABLE : packageType;
    }
}
