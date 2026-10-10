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

import com.google.gson.JsonParseException;
import consulo.content.base.SourcesOrderRootType;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ContentRootData;
import consulo.externalSystem.model.project.LibraryData;
import consulo.externalSystem.model.project.LibraryDependencyData;
import consulo.externalSystem.model.project.LibraryLevel;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.externalSystem.rt.model.ExternalSystemSourceType;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.service.project.ProjectData;
import consulo.localize.LocalizeValue;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.packageManager.setting.SwiftPackageExecutionSettings;
import consulo.swift.packageManager.task.SwiftPackageCommand;
import consulo.swift.toolchain.SwiftToolchain;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static consulo.swift.packageManager.SwiftPackageManagerConstants.SYSTEM_ID;

/**
 * Turns a package into one module: content root = package directory, a source root per target ({@code Tests/*} ones as
 * test roots), {@code .build} excluded, and a project library per package it depends on (rooted at its checkout).
 */
public class SwiftPackageProjectResolver implements ExternalSystemProjectResolver<SwiftPackageExecutionSettings> {
    private static final Map<String, LocalizeValue> TASKS = Map.of(
        SwiftPackageManagerConstants.TASK_BUILD, SwiftPackageManagerLocalize.taskBuildDescription(),
        SwiftPackageManagerConstants.TASK_TEST, SwiftPackageManagerLocalize.taskTestDescription(),
        SwiftPackageManagerConstants.TASK_CLEAN, SwiftPackageManagerLocalize.taskCleanDescription(),
        SwiftPackageManagerConstants.TASK_RESOLVE, SwiftPackageManagerLocalize.taskResolveDescription(),
        SwiftPackageManagerConstants.TASK_UPDATE, SwiftPackageManagerLocalize.taskUpdateDescription()
    );

    private final SwiftPackageCommand myCommand = new SwiftPackageCommand();

    @Override
    public @Nullable DataNode<ProjectData> resolveProjectInfo(ExternalSystemTaskId id,
                                                              String projectPath,
                                                              boolean isPreviewMode,
                                                              @Nullable SwiftPackageExecutionSettings settings,
                                                              ExternalSystemTaskNotificationListener listener)
        throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
        Path packageDirectory = SwiftPackageManagerUtil.toPackageDirectory(Path.of(projectPath));
        if (!Files.isRegularFile(packageDirectory.resolve(SwiftPackageManagerConstants.MANIFEST))) {
            throw new IllegalArgumentException(SwiftPackageManagerLocalize.errorNoManifest(packageDirectory).get());
        }

        SwiftToolchain toolchain = SwiftPackageManagerUtil.toToolchain(settings);

        if (!isPreviewMode) {
            listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, SwiftPackageManagerLocalize.statusResolving().get()));
            myCommand.run(id, toolchain, packageDirectory, List.of("package", "resolve"), true, listener);
        }

        listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, SwiftPackageManagerLocalize.statusReading().get()));
        List<String> describe = List.of("package", "describe", "--type", "json");
        SwiftPackageDescription.Package swiftPackage = parse(describe, () -> SwiftPackageDescription.parsePackage(
            myCommand.run(id, toolchain, packageDirectory, describe, false, listener)));

        List<SwiftPackageDescription.Dependency> dependencies = List.of();
        if (!isPreviewMode) {
            List<String> showDependencies = List.of("package", "show-dependencies", "--format", "json");
            dependencies = parse(showDependencies, () -> SwiftPackageDescription.parseDependencies(
                myCommand.run(id, toolchain, packageDirectory, showDependencies, false, listener)));
        }

        return createProjectNode(packageDirectory, projectPath, swiftPackage, dependencies, settings);
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId taskId, ExternalSystemTaskNotificationListener listener) {
        return myCommand.cancel();
    }

    static DataNode<ProjectData> createProjectNode(Path packageDirectory,
                                                   String linkedProjectPath,
                                                   SwiftPackageDescription.Package swiftPackage,
                                                   List<SwiftPackageDescription.Dependency> dependencies,
                                                   @Nullable SwiftPackageExecutionSettings settings) {
        String directory = packageDirectory.toString();
        String name = swiftPackage.name().isEmpty() ? String.valueOf(packageDirectory.getFileName()) : swiftPackage.name();

        ProjectData projectData = new ProjectData(SYSTEM_ID, name, directory, linkedProjectPath);
        DataNode<ProjectData> projectNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);

        for (Map.Entry<String, LocalizeValue> task : TASKS.entrySet()) {
            projectNode.createChild(ProjectKeys.TASK, new TaskData(SYSTEM_ID, task.getKey(), linkedProjectPath, task.getValue().get()));
        }

        ModuleData moduleData = new ModuleData(name, SYSTEM_ID, name, directory, linkedProjectPath);
        DataNode<ModuleData> moduleNode = projectNode.createChild(ProjectKeys.MODULE, moduleData);

        ContentRootData contentRoot = new ContentRootData(SYSTEM_ID, directory);
        for (SwiftPackageDescription.Target target : swiftPackage.targets()) {
            if (!target.hasSources()) {
                continue;
            }
            ExternalSystemSourceType type = target.isTest() ? ExternalSystemSourceType.TEST : ExternalSystemSourceType.SOURCE;
            contentRoot.storePath(type, packageDirectory.resolve(target.path()).normalize().toString());
        }
        for (String excluded : List.of(SwiftPackageManagerConstants.BUILD_DIRECTORY, SwiftPackageManagerConstants.SWIFTPM_DIRECTORY)) {
            contentRoot.storePath(ExternalSystemSourceType.EXCLUDED, packageDirectory.resolve(excluded).toString());
        }
        moduleNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);

        for (SwiftPackageDescription.Dependency dependency : dependencies) {
            String path = dependency.path();
            boolean resolved = path != null && Files.isDirectory(Path.of(path));
            LibraryData library = new LibraryData(SYSTEM_ID, "SwiftPM: " + dependency.identity(), !resolved);
            if (resolved) {
                library.addPath(SourcesOrderRootType.ID, path);
            }
            projectNode.createChild(ProjectKeys.LIBRARY, library);
            moduleNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, new LibraryDependencyData(moduleData, library, LibraryLevel.PROJECT));
        }

        List<String> executableProducts = swiftPackage.executableProducts().stream().map(SwiftPackageDescription.Product::name).toList();
        String toolchainName = settings == null ? null : settings.getToolchainName();
        boolean hasTests = swiftPackage.targets().stream().anyMatch(SwiftPackageDescription.Target::isTest);
        moduleNode.createChild(SwiftModuleData.KEY, new SwiftModuleData(toolchainName, executableProducts, hasTests));
        return projectNode;
    }

    private static <T> T parse(List<String> arguments, JsonReader<T> reader) {
        try {
            return reader.read();
        }
        catch (JsonParseException | IllegalStateException e) {
            String command = "swift " + String.join(" ", arguments);
            throw new ExternalSystemException(SwiftPackageManagerLocalize.errorBadOutput(command, String.valueOf(e.getMessage())).get());
        }
    }

    @FunctionalInterface
    private interface JsonReader<T> {
        T read() throws JsonParseException;
    }
}
