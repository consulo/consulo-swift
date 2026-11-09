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

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.importing.AbstractExternalModuleImportProvider;
import consulo.externalSystem.importing.ExternalModuleImportContext;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataManager;
import consulo.project.Project;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.ui.image.Image;
import jakarta.inject.Inject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens/imports a directory holding {@code Package.swift} (or the manifest itself). The package directory is the linked
 * project path.
 */
@ExtensionImpl
public class SwiftPackageModuleImportProvider extends AbstractExternalModuleImportProvider {
    @Inject
    public SwiftPackageModuleImportProvider(ProjectDataManager dataManager) {
        super(dataManager, SwiftPackageManagerConstants.SYSTEM_ID);
    }

    @Override
    public Image getIcon() {
        return SwiftIconGroup.swift();
    }

    @Override
    public boolean canImport(Path fileOrDirectory) {
        if (Files.isDirectory(fileOrDirectory)) {
            return Files.exists((fileOrDirectory.resolve(SwiftPackageManagerConstants.MANIFEST)));
        }
        return SwiftPackageManagerConstants.MANIFEST.equals(fileOrDirectory.getFileName().toString());
    }

    @Override
    protected File getExternalProjectConfigToUse(File file) {
        return file.isDirectory() ? file : file.getParentFile();
    }

    @Override
    protected void doPrepare(ExternalModuleImportContext context) {
    }

    @Override
    protected void beforeCommit(DataNode<ProjectData> dataNode, Project project) {
    }

    @Override
    protected void applyExtraSettings(ExternalModuleImportContext context) {
    }
}
