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
package consulo.swift.packageManager.newProject;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.Task;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.content.bundle.Sdk;
import consulo.externalSystem.importing.ImportSpecBuilder;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher;
import consulo.fileEditor.FileEditorManager;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.logging.Logger;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.scratch.NewModuleBuilder;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.module.creation.scratch.NewModuleContext;
import consulo.module.creation.scratch.NewModuleContextGroup;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.packageManager.setting.SwiftPackageProjectSettings;
import consulo.swift.packageManager.setting.SwiftPackageSettings;
import consulo.swift.toolchain.SwiftToolchain;
import consulo.swift.toolchain.module.extension.SwiftMutableModuleExtension;
import consulo.ui.ex.wizard.WizardStep;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * New project/module "Swift Package": once the project is initialized, {@code swift package init} into the content root
 * (unless there is a package already), then the package is linked to Swift Package Manager and imported - the import
 * takes over the module (named like the package) and creates its run configurations.
 */
@ExtensionImpl(id = "swift")
public class SwiftNewModuleBuilder implements NewModuleBuilder {
    private static final Logger LOG = Logger.getInstance(SwiftNewModuleBuilder.class);
    private static final int INIT_TIMEOUT_MS = 60_000;

    @Override
    public void setupContext(NewModuleContext context) {
        NewModuleContextGroup group = context.addGroup("swift", SwiftPackageManagerLocalize.newProjectGroup(), SwiftIconGroup.swift());
        NewModuleBuilderProcessor<SwiftNewModuleWizardContext> processor = new NewModuleBuilderProcessor<>() {
            @Override
            public SwiftNewModuleWizardContext createContext(boolean isNewProject) {
                return new SwiftNewModuleWizardContext(isNewProject);
            }

            @Override
            public void buildSteps(Consumer<WizardStep<SwiftNewModuleWizardContext>> consumer, SwiftNewModuleWizardContext context) {
                consumer.accept(new SwiftNewModuleSetupStep(context));
            }

            @RequiredReadAction
            @Override
            public void process(SwiftNewModuleWizardContext context, ContentEntry contentEntry, ModifiableRootModel modifiableRootModel) {
                setupModule(context, contentEntry, modifiableRootModel);
            }
        };
        group.add(SwiftPackageManagerLocalize.newProjectPackage(), SwiftIconGroup.swift(), processor);
    }

    @RequiredReadAction
    private static void setupModule(SwiftNewModuleWizardContext context, ContentEntry contentEntry, ModifiableRootModel rootModel) {
        Sdk bundle = context.getToolchainBundle();
        SwiftMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SwiftMutableModuleExtension.class);
        if (extension != null) {
            extension.setEnabled(true);
            if (bundle != null) {
                extension.getInheritableSdk().set(null, bundle);
                rootModel.addModuleExtensionSdkEntry(extension);
            }
        }

        String contentUrl = contentEntry.getUrl();
        contentEntry.addFolder(contentUrl + "/Sources", ProductionContentFolderTypeProvider.getInstance());
        contentEntry.addFolder(contentUrl + "/Tests", TestContentFolderTypeProvider.getInstance());
        String buildUrl = contentUrl + "/" + SwiftPackageManagerConstants.BUILD_DIRECTORY;
        contentEntry.addFolder(buildUrl, ExcludedContentFolderTypeProvider.getInstance());

        VirtualFile contentRoot = contentEntry.getFile();
        if (contentRoot == null) {
            return;
        }
        Project project = rootModel.getProject();
        // as the import of an existing package does: the tool window of Swift Package Manager is decided on when the
        // project opens, and the package is only linked once it exists
        project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);

        String name = context.getName();
        SwiftPackageType type = context.getPackageType();
        // this runs on the UI thread before the root model is committed (and for a new project, before it is open)
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> createPackage(project, bundle, contentRoot, name, type));
    }

    private static void createPackage(Project project, @Nullable Sdk bundle, VirtualFile contentRoot, String name, SwiftPackageType type) {
        String path = contentRoot.getPath();
        Sdk toolchainBundle = bundle != null ? bundle : SwiftPackageManagerUtil.findToolchainSdk(project, path);
        SwiftToolchain toolchain = SwiftToolchain.fromSdk(toolchainBundle);
        if (toolchainBundle == null || toolchain == null) {
            notifyFailure(project, SwiftPackageManagerLocalize.errorNoToolchain().get());
            return;
        }
        String bundleName = toolchainBundle.getName();

        Task.Backgroundable.queue(project, SwiftPackageManagerLocalize.newProjectCreating(), indicator -> {
            // an existing package in the chosen directory is imported as it is
            boolean hasPackage = Files.isRegularFile(contentRoot.toNioPath().resolve(SwiftPackageManagerConstants.MANIFEST));
            if (!hasPackage && !initPackage(project, toolchain, contentRoot, name, type)) {
                return;
            }
            contentRoot.refresh(false, true);
            project.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                linkAndImport(project, path, bundleName);
                openEntryFile(project, contentRoot);
            });
        });
    }

    private static boolean initPackage(Project project,
                                       SwiftToolchain toolchain,
                                       VirtualFile contentRoot,
                                       String name,
                                       SwiftPackageType type) {
        Path directory = contentRoot.toNioPath();
        List<String> arguments = List.of("package", "init", "--type", type.getId(), "--name", name);
        try {
            GeneralCommandLine commandLine = toolchain.createSwiftCommandLine(directory, arguments);
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(commandLine, INIT_TIMEOUT_MS);
            if (output.isTimeout() || output.getExitCode() != 0) {
                String reason = output.getStderr().isBlank() ? output.getStdout() : output.getStderr();
                notifyFailure(project, reason.strip());
                return false;
            }
            return true;
        }
        catch (ExecutionException e) {
            LOG.warn("swift package init failed in " + directory, e);
            notifyFailure(project, String.valueOf(e.getMessage()));
            return false;
        }
    }

    /**
     * Links the package to Swift Package Manager and imports it - as linking a {@code Package.swift} in its tool window does.
     */
    private static void linkAndImport(Project project, String path, String bundleName) {
        SwiftPackageSettings settings = SwiftPackageSettings.getInstance(project);
        if (SwiftPackageManagerUtil.findProjectSettings(project, path) == null) {
            SwiftPackageProjectSettings projectSettings = new SwiftPackageProjectSettings();
            projectSettings.setExternalProjectPath(path);
            projectSettings.setToolchainName(bundleName);
            settings.linkProject(projectSettings);
        }
        ExternalSystemProjectRefresher.getInstance().refreshProjects(
            new ImportSpecBuilder(project, SwiftPackageManagerConstants.SYSTEM_ID)
                .forceWhenUptodate()
                .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
        );
    }

    /**
     * The first source of the package - {@code Sources/<Name>/<Name>.swift} (or {@code main.swift}) of the templates.
     */
    private static void openEntryFile(Project project, VirtualFile contentRoot) {
        VirtualFile sources = contentRoot.findChild("Sources");
        if (sources == null) {
            return;
        }
        VirtualFile[] entry = new VirtualFile[1];
        VirtualFileUtil.visitChildrenRecursively(sources, new VirtualFileVisitor<Object>() {
            @Override
            public boolean visitFile(VirtualFile file) {
                if (entry[0] == null && !file.isDirectory() && "swift".equals(file.getExtension())) {
                    entry[0] = file;
                }
                return entry[0] == null;
            }
        });
        if (entry[0] != null) {
            FileEditorManager.getInstance(project).openFile(entry[0], true);
        }
    }

    private static void notifyFailure(Project project, String reason) {
        Notification notification = new Notification(
            SwiftPackageNotificationGroupContributor.GROUP,
            SwiftPackageManagerLocalize.systemName().get(),
            SwiftPackageManagerLocalize.newProjectFailed(reason).get(),
            NotificationType.ERROR
        );
        Notifications.Bus.notify(notification, project);
    }
}
