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
package consulo.swift.execution.run;

import consulo.execution.configuration.ui.SettingsEditor;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.nativeDev.debugger.run.NativeProgramParametersLayout;
import consulo.project.Project;
import consulo.swift.execution.compiler.SwiftBuildCommand;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

public class SwiftRunConfigurationEditor extends SettingsEditor<SwiftRunConfiguration> {
    private final Project myProject;
    private @Nullable Layout myLayout;

    public SwiftRunConfigurationEditor(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        Layout layout = new Layout(myProject);
        layout.build();
        myLayout = layout;
        return layout.getComponent();
    }

    @RequiredUIAccess
    @Override
    protected void resetEditorFrom(SwiftRunConfiguration configuration) {
        Layout layout = myLayout;
        if (layout != null) {
            layout.reset(configuration);
        }
    }

    @RequiredUIAccess
    @Override
    protected void applyEditorTo(SwiftRunConfiguration configuration) {
        Layout layout = myLayout;
        if (layout != null) {
            layout.apply(configuration);
        }
    }

    private static class Layout extends NativeProgramParametersLayout<SwiftRunConfiguration> {
        private final ComboBox<String> myModule;
        private final TextBox myProduct;
        private final ComboBox<String> myBuildConfiguration;

        @RequiredUIAccess
        private Layout(Project project) {
            super(project);

            ComboBox.Builder<String> modules = ComboBox.builder();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                if (module.getExtension(SwiftModuleExtension.class) != null) {
                    modules.add(module.getName(), LocalizeValue.of(module.getName()));
                }
            }
            myModule = modules.build();
            myProduct = TextBox.create();
            myBuildConfiguration = ComboBox.create(SwiftBuildCommand.DEBUG, SwiftBuildCommand.RELEASE);
        }

        @RequiredUIAccess
        @Override
        protected void addBefore(FormBuilder builder) {
            builder.addLabeled(SwiftExecutionLocalize.runConfigurationModuleLabel(), myModule);
            builder.addLabeled(SwiftExecutionLocalize.runConfigurationProductLabel(), myProduct);
            builder.addLabeled(SwiftExecutionLocalize.runConfigurationBuildConfigurationLabel(), myBuildConfiguration);
        }

        @RequiredUIAccess
        @Override
        public void reset(SwiftRunConfiguration configuration) {
            super.reset(configuration);
            Module module = configuration.getConfigurationModule().getModule();
            if (module != null) {
                myModule.setValue(module.getName());
            }
            myProduct.setValue(configuration.getProduct());
            myBuildConfiguration.setValue(configuration.getBuildConfiguration());
        }

        @RequiredUIAccess
        @Override
        public void apply(SwiftRunConfiguration configuration) {
            super.apply(configuration);
            configuration.setModuleName(myModule.getValue());
            configuration.setProduct(myProduct.getValue());
            configuration.setBuildConfiguration(myBuildConfiguration.getValue());
        }
    }
}
