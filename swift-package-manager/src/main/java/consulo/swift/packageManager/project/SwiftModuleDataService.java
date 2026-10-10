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
package consulo.swift.packageManager.project;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.swift.toolchain.module.extension.SwiftMutableModuleExtension;

import java.util.Collection;

/**
 * Turns the Swift extension of an imported module on and binds it to the toolchain the package was imported with.
 */
@ExtensionImpl
public class SwiftModuleDataService implements ProjectDataService<SwiftModuleData, Module> {
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
                ModuleManager moduleManager = ModuleManager.getInstance(project);
                for (DataNode<SwiftModuleData> node : toImport) {
                    DataNode<ModuleData> moduleNode = ExternalSystemApiUtil.findParent(node, ProjectKeys.MODULE);
                    Module module = moduleNode == null ? null : moduleManager.findModuleByName(moduleNode.getData().getInternalName());
                    if (module != null) {
                        enableSwift(module, node.getData());
                    }
                }
            }
        });
    }

    @Override
    public void removeData(Collection<? extends Module> toRemove, Project project, boolean synchronous) {
    }

    private static void enableSwift(Module module, SwiftModuleData data) {
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        SwiftMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SwiftMutableModuleExtension.class);
        if (extension == null) {
            rootModel.dispose();
            return;
        }

        extension.setEnabled(true);
        String toolchainName = data.getToolchainName();
        if (toolchainName != null) {
            extension.getInheritableSdk().set(null, toolchainName);
        }
        if (rootModel.findModuleExtensionSdkEntry(extension) == null) {
            rootModel.addModuleExtensionSdkEntry(extension);
        }
        rootModel.commit();
    }
}
