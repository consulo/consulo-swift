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

import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * What the resolver and the task manager need to run {@code swift} for a package - resolved on the IDE side, because the
 * toolchain is a bundle of the IDE.
 */
public class SwiftPackageExecutionSettings extends ExternalSystemExecutionSettings {
    private final @Nullable String myToolchainName;
    private final @Nullable String myToolchainHome;
    private final List<String> myBuildArguments;

    public SwiftPackageExecutionSettings(@Nullable String toolchainName, @Nullable String toolchainHome, List<String> buildArguments) {
        myToolchainName = toolchainName;
        myToolchainHome = toolchainHome;
        myBuildArguments = List.copyOf(buildArguments);
    }

    public @Nullable String getToolchainName() {
        return myToolchainName;
    }

    public @Nullable String getToolchainHome() {
        return myToolchainHome;
    }

    public List<String> getBuildArguments() {
        return myBuildArguments;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(myToolchainName);
        result = 31 * result + Objects.hashCode(myToolchainHome);
        result = 31 * result + myBuildArguments.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof SwiftPackageExecutionSettings that) || !super.equals(o)) {
            return false;
        }
        return Objects.equals(myToolchainName, that.myToolchainName)
            && Objects.equals(myToolchainHome, that.myToolchainHome)
            && myBuildArguments.equals(that.myBuildArguments);
    }
}
