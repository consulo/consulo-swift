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
package consulo.swift.packageManager;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.service.project.autoimport.CachingExternalSystemAutoImportAware;
import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.swift.packageManager.project.SwiftPackageProjectResolver;
import consulo.swift.packageManager.setting.SwiftPackageExecutionSettings;
import consulo.swift.packageManager.setting.SwiftPackageLocalSettings;
import consulo.swift.packageManager.setting.SwiftPackageProjectSettings;
import consulo.swift.packageManager.setting.SwiftPackageSettings;
import consulo.swift.packageManager.setting.SwiftPackageSettingsListener;
import consulo.swift.packageManager.task.SwiftPackageTaskManager;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@ExtensionImpl
public class SwiftPackageManager implements ExternalSystemAutoImportAware,
    ExternalSystemManager<SwiftPackageProjectSettings, SwiftPackageSettingsListener, SwiftPackageSettings, SwiftPackageLocalSettings,
        SwiftPackageExecutionSettings> {

    private final SwiftPackageAutoImportAware myAutoImportAware = new SwiftPackageAutoImportAware();
    private final ExternalSystemAutoImportAware myAutoImportDelegate = new CachingExternalSystemAutoImportAware(myAutoImportAware);

    @Override
    public ProjectSystemId getSystemId() {
        return SwiftPackageManagerConstants.SYSTEM_ID;
    }

    @Override
    public Function<Project, SwiftPackageSettings> getSettingsProvider() {
        return SwiftPackageSettings::getInstance;
    }

    @Override
    public Function<Project, SwiftPackageLocalSettings> getLocalSettingsProvider() {
        return SwiftPackageLocalSettings::getInstance;
    }

    @Override
    public Function<Pair<Project, String>, SwiftPackageExecutionSettings> getExecutionSettingsProvider() {
        return pair -> SwiftPackageManagerUtil.createExecutionSettings(pair.getFirst(), pair.getSecond());
    }

    @Override
    public Supplier<? extends ExternalSystemProjectResolver<SwiftPackageExecutionSettings>> getProjectResolverFactory() {
        return SwiftPackageProjectResolver::new;
    }

    @Override
    public Supplier<? extends ExternalSystemTaskManager<SwiftPackageExecutionSettings>> getTaskManagerFactory() {
        return SwiftPackageTaskManager::new;
    }

    @Override
    public FileChooserDescriptor getExternalProjectDescriptor() {
        return new FileChooserDescriptor(true, true, false, false, false, false) {
            @Override
            public boolean isFileSelectable(VirtualFile file) {
                return file.isDirectory() || SwiftPackageManagerConstants.MANIFEST.equals(file.getName());
            }
        };
    }

    @Override
    public @Nullable String getAffectedExternalProjectPath(String changedFileOrDirPath, Project project) {
        return myAutoImportDelegate.getAffectedExternalProjectPath(changedFileOrDirPath, project);
    }

    @Override
    public List<Path> getAffectedExternalProjectFilePaths(String projectPath, Project project) {
        return myAutoImportAware.getAffectedExternalProjectFilePaths(projectPath, project);
    }
}
