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

import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.project.Project;
import consulo.swift.packageManager.setting.SwiftPackageProjectSettings;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * A package is out of date when its {@code Package.swift} or {@code Package.resolved} changes.
 */
public class SwiftPackageAutoImportAware implements ExternalSystemAutoImportAware {
    @Override
    public @Nullable String getAffectedExternalProjectPath(String changedFileOrDirPath, Project project) {
        Path changed = Path.of(changedFileOrDirPath);
        Path fileName = changed.getFileName();
        if (fileName == null) {
            return null;
        }
        String name = fileName.toString();
        if (!SwiftPackageManagerConstants.MANIFEST.equals(name) && !SwiftPackageManagerConstants.RESOLVED.equals(name)) {
            return null;
        }

        Path directory = changed.getParent();
        if (directory == null) {
            return null;
        }
        SwiftPackageProjectSettings settings = SwiftPackageManagerUtil.findProjectSettings(project, directory.toString());
        return settings == null ? null : settings.getExternalProjectPath();
    }

    @Override
    public List<Path> getAffectedExternalProjectFilePaths(String projectPath, Project project) {
        Path directory = SwiftPackageManagerUtil.toPackageDirectory(Path.of(projectPath));
        return List.of(directory.resolve(SwiftPackageManagerConstants.MANIFEST), directory.resolve(SwiftPackageManagerConstants.RESOLVED));
    }
}
