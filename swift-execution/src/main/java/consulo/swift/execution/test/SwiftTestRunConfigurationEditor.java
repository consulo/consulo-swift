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
package consulo.swift.execution.test;

import consulo.execution.configuration.ui.SettingsEditor;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.nativeDev.debugger.run.NativeProgramParametersLayout;
import consulo.project.Project;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

public class SwiftTestRunConfigurationEditor extends SettingsEditor<SwiftTestRunConfiguration> {
    private final Project myProject;
    private @Nullable Layout myLayout;

    public SwiftTestRunConfigurationEditor(Project project) {
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
    protected void resetEditorFrom(SwiftTestRunConfiguration configuration) {
        Layout layout = myLayout;
        if (layout != null) {
            layout.reset(configuration);
        }
    }

    @RequiredUIAccess
    @Override
    protected void applyEditorTo(SwiftTestRunConfiguration configuration) {
        Layout layout = myLayout;
        if (layout != null) {
            layout.apply(configuration);
        }
    }

    private static class Layout extends NativeProgramParametersLayout<SwiftTestRunConfiguration> {
        private final ComboBox<String> myModule;
        private final ComboBox<SwiftTestFramework> myFramework;
        private final TextBox myFilter;

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

            ComboBox.Builder<SwiftTestFramework> frameworks = ComboBox.builder();
            for (SwiftTestFramework framework : SwiftTestFramework.values()) {
                frameworks.add(framework, framework.getDisplayName());
            }
            myFramework = frameworks.build();

            myFilter = TextBox.create();
        }

        @RequiredUIAccess
        @Override
        protected void addBefore(FormBuilder builder) {
            builder.addLabeled(SwiftExecutionLocalize.runConfigurationModuleLabel(), myModule);
            builder.addLabeled(SwiftExecutionLocalize.testConfigurationFrameworkLabel(), myFramework);
            builder.addLabeled(SwiftExecutionLocalize.testConfigurationFilterLabel(), myFilter);
        }

        @RequiredUIAccess
        @Override
        public void reset(SwiftTestRunConfiguration configuration) {
            super.reset(configuration);
            Module module = configuration.getConfigurationModule().getModule();
            if (module != null) {
                myModule.setValue(module.getName());
            }
            myFramework.setValue(configuration.getFramework());
            myFilter.setValue(configuration.getFilter());
        }

        @RequiredUIAccess
        @Override
        public void apply(SwiftTestRunConfiguration configuration) {
            super.apply(configuration);
            configuration.setModuleName(myModule.getValue());
            configuration.setFramework(myFramework.getValue());
            configuration.setFilter(myFilter.getValue());
        }
    }
}
