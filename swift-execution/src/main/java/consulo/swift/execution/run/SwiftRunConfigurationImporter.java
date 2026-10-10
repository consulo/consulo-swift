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

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.execution.test.SwiftTestRunConfiguration;
import consulo.swift.execution.test.SwiftTestRunConfigurationType;
import consulo.swift.packageManager.project.SwiftModuleData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Gives every executable product of an imported package a run configuration, and a package with tests a test
 * configuration of all its tests - unless they have them already.
 */
@ExtensionImpl
public class SwiftRunConfigurationImporter implements ProjectDataService<SwiftModuleData, Module> {
    @Override
    public Key<SwiftModuleData> getTargetDataKey() {
        return SwiftModuleData.KEY;
    }

    @Override
    public void importData(Collection<DataNode<SwiftModuleData>> toImport, Project project, boolean synchronous) {
        if (toImport.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
            @Override
            public void execute() {
                createConfigurations(toImport, project);
            }
        });
    }

    @Override
    public void removeData(Collection<? extends Module> toRemove, Project project, boolean synchronous) {
    }

    private static void createConfigurations(Collection<DataNode<SwiftModuleData>> toImport, Project project) {
        RunManager runManager = RunManager.getInstance(project);
        SwiftRunConfigurationType type = SwiftRunConfigurationType.getInstance();
        SwiftTestRunConfigurationType testType = SwiftTestRunConfigurationType.getInstance();

        Set<String> existing = new HashSet<>();
        for (RunConfiguration configuration : runManager.getConfigurationsList(type)) {
            if (configuration instanceof SwiftRunConfiguration swift) {
                existing.add(key(swift.getConfigurationModule().getModuleName(), swift.getProduct()));
            }
        }
        Set<String> testedModules = new HashSet<>();
        for (RunConfiguration configuration : runManager.getConfigurationsList(testType)) {
            if (configuration instanceof SwiftTestRunConfiguration test && test.getFilter().isEmpty()) {
                testedModules.add(test.getConfigurationModule().getModuleName());
            }
        }

        RunnerAndConfigurationSettings first = null;
        for (DataNode<SwiftModuleData> node : toImport) {
            DataNode<ModuleData> moduleNode = ExternalSystemApiUtil.findParent(node, ProjectKeys.MODULE);
            if (moduleNode == null) {
                continue;
            }
            String moduleName = moduleNode.getData().getInternalName();
            for (String product : node.getData().getExecutableProducts()) {
                if (!existing.add(key(moduleName, product))) {
                    continue;
                }
                SwiftRunConfiguration configuration = new SwiftRunConfiguration(project, type.getFactory(), product);
                configuration.setModuleName(moduleName);
                configuration.setProduct(product);
                RunnerAndConfigurationSettings settings = runManager.createConfiguration(configuration, type.getFactory());
                runManager.addConfiguration(settings);
                if (first == null) {
                    first = settings;
                }
            }

            if (node.getData().hasTests() && testedModules.add(moduleName)) {
                String name = SwiftExecutionLocalize.testProducerAllTests(moduleName).get();
                SwiftTestRunConfiguration configuration = new SwiftTestRunConfiguration(project, testType.getFactory(), name);
                configuration.setModuleName(moduleName);
                runManager.addConfiguration(runManager.createConfiguration(configuration, testType.getFactory()));
            }
        }

        if (first != null && runManager.getSelectedConfiguration() == null) {
            runManager.setSelectedConfiguration(first);
        }
    }

    private static String key(String moduleName, String product) {
        return moduleName + "/" + product;
    }
}
