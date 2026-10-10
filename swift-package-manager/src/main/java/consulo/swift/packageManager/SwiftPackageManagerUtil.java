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

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.externalSystem.service.module.extension.ExternalSystemModuleExtension;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.platform.Platform;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.packageManager.setting.SwiftPackageExecutionSettings;
import consulo.swift.packageManager.setting.SwiftPackageProjectSettings;
import consulo.swift.packageManager.setting.SwiftPackageSettings;
import consulo.swift.toolchain.SwiftSdkType;
import consulo.swift.toolchain.SwiftToolchain;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SwiftPackageManagerUtil {
    private SwiftPackageManagerUtil() {
    }

    /**
     * The bundle chosen for the linked package, or the most recent Swift bundle.
     */
    public static @Nullable Sdk findToolchainSdk(Project project, @Nullable String linkedProjectPath) {
        SwiftPackageProjectSettings settings = linkedProjectPath == null ? null : findProjectSettings(project, linkedProjectPath);
        String toolchainName = settings == null ? null : settings.getToolchainName();
        SdkTable sdkTable = SdkTable.getInstance();
        if (toolchainName != null) {
            Sdk sdk = sdkTable.findSdk(toolchainName);
            if (sdk != null && sdk.getSdkType() instanceof SwiftSdkType) {
                return sdk;
            }
        }
        return sdkTable.findMostRecentSdkOfType(SwiftSdkType.getInstance());
    }

    public static SwiftPackageExecutionSettings createExecutionSettings(Project project, String linkedProjectPath) {
        Sdk sdk = findToolchainSdk(project, linkedProjectPath);
        SwiftToolchain toolchain = SwiftToolchain.fromSdk(sdk);
        return new SwiftPackageExecutionSettings(
            sdk == null ? null : sdk.getName(),
            toolchain == null ? null : toolchain.getHome().toString(),
            getBuildArguments(project, linkedProjectPath)
        );
    }

    public static List<String> getBuildArguments(Project project, String linkedProjectPath) {
        SwiftPackageProjectSettings settings = findProjectSettings(project, linkedProjectPath);
        return settings == null ? List.of() : ParametersListUtil.parse(settings.getBuildArguments());
    }

    /**
     * A package may be linked by its directory or by its {@code Package.swift} - both name the same settings.
     */
    public static @Nullable SwiftPackageProjectSettings findProjectSettings(Project project, String packagePath) {
        Path packageDirectory = toPackageDirectory(Path.of(packagePath));
        for (SwiftPackageProjectSettings settings : SwiftPackageSettings.getInstance(project).getLinkedProjectsSettings()) {
            String linkedProjectPath = settings.getExternalProjectPath();
            if (linkedProjectPath != null && toPackageDirectory(Path.of(linkedProjectPath)).equals(packageDirectory)) {
                return settings;
            }
        }
        return null;
    }

    /**
     * The toolchain resolved on the IDE side for a resolve/task run.
     */
    public static SwiftToolchain toToolchain(@Nullable SwiftPackageExecutionSettings settings) throws ExternalSystemException {
        String home = settings == null ? null : settings.getToolchainHome();
        if (home == null) {
            throw new ExternalSystemException(SwiftPackageManagerLocalize.errorNoToolchain().get());
        }
        return new SwiftToolchain(Platform.current(), Path.of(home));
    }

    /**
     * The toolchain of the module's Swift extension, or the one the package of the module is set up with.
     */
    public static @Nullable SwiftToolchain findToolchain(Module module) {
        SwiftToolchain toolchain = SwiftModuleExtension.findToolchain(module);
        if (toolchain != null) {
            return toolchain;
        }
        Path packageDirectory = findPackageDirectory(module);
        String packagePath = packageDirectory == null ? null : packageDirectory.toString();
        return SwiftToolchain.fromSdk(findToolchainSdk(module.getProject(), packagePath));
    }

    /**
     * The package a module was imported from, or the content root of the module holding {@code Package.swift}.
     */
    public static @Nullable Path findPackageDirectory(Module module) {
        ExternalSystemModuleExtension<?> extension = module.getExtension(ExternalSystemModuleExtension.class);
        String systemId = extension == null ? null : extension.getOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
        if (extension != null && SwiftPackageManagerConstants.SYSTEM_ID.getId().equals(systemId)) {
            String linkedProjectPath = extension.getOption(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
            if (linkedProjectPath != null) {
                return toPackageDirectory(Path.of(linkedProjectPath));
            }
        }

        for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
            if (contentRoot.findChild(SwiftPackageManagerConstants.MANIFEST) != null) {
                return contentRoot.toNioPath();
            }
        }
        return null;
    }

    /**
     * @param fileOrDirectory {@code Package.swift} or the directory holding it
     */
    public static Path toPackageDirectory(Path fileOrDirectory) {
        if (Files.isDirectory(fileOrDirectory)) {
            return fileOrDirectory;
        }
        Path parent = fileOrDirectory.getParent();
        return parent == null ? fileOrDirectory : parent;
    }
}
