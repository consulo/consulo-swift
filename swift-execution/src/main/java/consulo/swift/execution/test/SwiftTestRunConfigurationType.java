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

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;

@ExtensionImpl
public class SwiftTestRunConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "SwiftTestRunConfiguration";

    public static SwiftTestRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(SwiftTestRunConfigurationType.class);
    }

    public SwiftTestRunConfigurationType() {
        super(
            ID,
            SwiftExecutionLocalize.testConfigurationName(),
            SwiftExecutionLocalize.testConfigurationDescription(),
            SwiftIconGroup.swift()
        );
        addFactory(new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new SwiftTestRunConfiguration(project, this, "");
            }

            @Override
            public boolean isApplicable(Project project) {
                return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SwiftModuleExtension.class);
            }
        });
    }

    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }
}
