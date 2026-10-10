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

import consulo.disposer.Disposable;
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep;
import consulo.module.ui.BundleBox;
import consulo.module.ui.BundleBoxBuilder;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.toolchain.SwiftSdkType;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Name and location, the toolchain to create the package with, and its template.
 */
public class SwiftNewModuleSetupStep extends UnifiedProjectOrModuleNameStep<SwiftNewModuleWizardContext> {
    private @Nullable BundleBox myBundleBox;
    private @Nullable ComboBox<SwiftPackageType> myPackageTypeBox;

    public SwiftNewModuleSetupStep(SwiftNewModuleWizardContext context) {
        super(context);
    }

    @RequiredUIAccess
    @Override
    protected void extend(FormBuilder builder, Disposable uiDisposable) {
        super.extend(builder, uiDisposable);

        BundleBoxBuilder bundleBoxBuilder = BundleBoxBuilder.create(uiDisposable);
        bundleBoxBuilder.withSdkTypeFilterByClass(SwiftSdkType.class);
        BundleBox bundleBox = bundleBoxBuilder.build();
        bundleBox.getComponent().selectFirst();
        builder.addLabeled(SwiftPackageManagerLocalize.settingsToolchainLabel(), bundleBox.getComponent());
        myBundleBox = bundleBox;

        ComboBox.Builder<SwiftPackageType> types = ComboBox.builder();
        for (SwiftPackageType type : SwiftPackageType.values()) {
            types.add(type, type.getDisplayName());
        }
        ComboBox<SwiftPackageType> packageTypeBox = types.build();
        packageTypeBox.setValue(SwiftPackageType.EXECUTABLE);
        builder.addLabeled(SwiftPackageManagerLocalize.newProjectTypeLabel(), packageTypeBox);
        myPackageTypeBox = packageTypeBox;
    }

    @Override
    public void onStepLeave(SwiftNewModuleWizardContext context) {
        super.onStepLeave(context);
        BundleBox.BundleBoxItem item = myBundleBox == null ? null : myBundleBox.getComponent().getValue();
        context.setToolchainBundle(item == null ? null : item.getBundle());
        context.setPackageType(myPackageTypeBox == null ? null : myPackageTypeBox.getValue());
    }
}
