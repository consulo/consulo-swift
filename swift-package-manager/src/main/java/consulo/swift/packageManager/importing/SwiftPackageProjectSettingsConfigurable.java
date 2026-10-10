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

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.disposer.Disposable;
import consulo.externalSystem.service.setting.AbstractExternalProjectSettingsConfigurable;
import consulo.externalSystem.service.setting.ExternalSystemSettingsPlace;
import consulo.localize.LocalizeValue;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.packageManager.setting.SwiftPackageProjectSettings;
import consulo.swift.toolchain.SwiftSdkType;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class SwiftPackageProjectSettingsConfigurable extends AbstractExternalProjectSettingsConfigurable<SwiftPackageProjectSettings> {
    private static final String MOST_RECENT = "";

    private @Nullable ComboBox<String> myToolchainBox;
    private @Nullable TextBox myBuildArgumentsBox;

    public SwiftPackageProjectSettingsConfigurable(SwiftPackageProjectSettings settings, ExternalSystemSettingsPlace place) {
        super(settings, place);
    }

    @Override
    @RequiredUIAccess
    protected Component createExtraUIComponent(Disposable uiDisposable) {
        ComboBox.Builder<String> toolchains = ComboBox.builder();
        toolchains.add(MOST_RECENT, SwiftPackageManagerLocalize.settingsToolchainMostRecent());
        for (Sdk sdk : SdkTable.getInstance().getSdksOfType(SwiftSdkType.getInstance())) {
            toolchains.add(sdk.getName(), LocalizeValue.of(sdk.getName()));
        }
        ComboBox<String> toolchainBox = toolchains.build();
        String toolchainName = getSettings().getToolchainName();
        toolchainBox.setValue(toolchainName == null ? MOST_RECENT : toolchainName);
        myToolchainBox = toolchainBox;

        TextBox buildArgumentsBox = TextBox.create(getSettings().getBuildArguments());
        myBuildArgumentsBox = buildArgumentsBox;

        return FormBuilder.create()
            .addLabeled(SwiftPackageManagerLocalize.settingsToolchainLabel(), toolchainBox)
            .addLabeled(SwiftPackageManagerLocalize.settingsBuildArgumentsLabel(), buildArgumentsBox)
            .build();
    }

    @Override
    @RequiredUIAccess
    protected boolean isExtraModified() {
        return !Objects.equals(getToolchainName(), getSettings().getToolchainName())
            || !getBuildArguments().equals(getSettings().getBuildArguments());
    }

    @Override
    @RequiredUIAccess
    protected void applyExtra() {
        if (myToolchainBox == null) {
            return;
        }
        getSettings().setToolchainName(getToolchainName());
        getSettings().setBuildArguments(getBuildArguments());
    }

    @RequiredUIAccess
    private @Nullable String getToolchainName() {
        String value = myToolchainBox == null ? null : myToolchainBox.getValue();
        return value == null || MOST_RECENT.equals(value) ? null : value;
    }

    @RequiredUIAccess
    private String getBuildArguments() {
        String value = myBuildArgumentsBox == null ? null : myBuildArgumentsBox.getValue();
        return value == null ? "" : value.trim();
    }
}
