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

import consulo.externalSystem.setting.ExternalProjectSettings;
import org.jspecify.annotations.Nullable;

/**
 * Settings of one linked package - the directory holding {@code Package.swift}.
 */
public class SwiftPackageProjectSettings extends ExternalProjectSettings {
    private @Nullable String myToolchainName;
    private String myBuildArguments = "";

    /**
     * @return name of the Swift bundle to build with, {@code null} for the most recent one
     */
    public @Nullable String getToolchainName() {
        return myToolchainName;
    }

    public void setToolchainName(@Nullable String toolchainName) {
        myToolchainName = toolchainName;
    }

    /**
     * @return arguments added to every {@code swift build}/{@code swift test} of the package, e.g. {@code --build-system native}
     */
    public String getBuildArguments() {
        return myBuildArguments;
    }

    public void setBuildArguments(@Nullable String buildArguments) {
        myBuildArguments = buildArguments == null ? "" : buildArguments;
    }

    @Override
    public SwiftPackageProjectSettings clone() {
        SwiftPackageProjectSettings clone = new SwiftPackageProjectSettings();
        copyTo(clone);
        clone.myToolchainName = myToolchainName;
        clone.myBuildArguments = myBuildArguments;
        return clone;
    }
}
