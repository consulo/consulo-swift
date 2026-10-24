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

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.swift.execution.compiler.SwiftBuildCommand;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.ui.ComboBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;

public class SwiftProductFragment implements RunConfigurationFragment<SwiftRunConfiguration> {
    @Override
    public String getId() {
        return "swiftProduct";
    }

    @Override
    public LocalizeValue getName() {
        return SwiftExecutionLocalize.runConfigurationFragmentProduct();
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
    public RunConfigurationFragmentEditor<SwiftRunConfiguration> createEditor(Project project, Disposable parent) {
        TextBox product = TextBox.create();
        ComboBox<String> buildConfiguration = ComboBox.create(SwiftBuildCommand.DEBUG, SwiftBuildCommand.RELEASE);

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(SwiftExecutionLocalize.runConfigurationProductLabel(), product);
                layout.addLabeled(SwiftExecutionLocalize.runConfigurationBuildConfigurationLabel(), buildConfiguration);
            }

            @RequiredUIAccess
            @Override
            public void reset(SwiftRunConfiguration target) {
                product.setValue(target.getProduct(), false);
                buildConfiguration.setValue(target.getBuildConfiguration(), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(SwiftRunConfiguration target) {
                target.setProduct(product.getValue());
                target.setBuildConfiguration(buildConfiguration.getValue());
            }
        };
    }
}
