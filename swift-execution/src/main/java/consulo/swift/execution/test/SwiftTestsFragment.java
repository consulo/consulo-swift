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

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.ui.ComboBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;

public class SwiftTestsFragment implements RunConfigurationFragment<SwiftTestRunConfiguration> {
    @Override
    public String getId() {
        return "swiftTests";
    }

    @Override
    public LocalizeValue getName() {
        return SwiftExecutionLocalize.testConfigurationFragmentTests();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.FIRST;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<SwiftTestRunConfiguration> createEditor(Project project, Disposable parent) {
        ComboBox<SwiftTestFramework> framework = ComboBox.create(SwiftTestFramework.values());
        framework.setTextRenderer(value -> value == null ? LocalizeValue.empty() : value.getDisplayName());
        TextBox filter = TextBox.create();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(SwiftExecutionLocalize.testConfigurationFrameworkLabel(), framework);
                layout.addLabeled(SwiftExecutionLocalize.testConfigurationFilterLabel(), filter);
            }

            @RequiredUIAccess
            @Override
            public void reset(SwiftTestRunConfiguration target) {
                framework.setValue(target.getFramework(), false);
                filter.setValue(target.getFilter(), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(SwiftTestRunConfiguration target) {
                target.setFramework(framework.getValue());
                target.setFilter(filter.getValue());
            }
        };
    }
}
