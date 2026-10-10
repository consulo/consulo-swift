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
package consulo.swift.execution.compiler;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.compiler.CompileContextEx;
import consulo.compiler.CompileDriver;
import consulo.compiler.CompilerRunner;
import consulo.compiler.scope.CompileScope;
import consulo.dataContext.DataContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.toolchain.SwiftToolchain;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds Swift packages with {@code swift build} - for "Build Project" and for "build before launch" of a
 * {@link SwiftBuildableConfiguration}, which then gets the directory of its binaries through {@link #BIN_PATH}.
 */
@ExtensionImpl
public class SwiftCompilerRunner implements CompilerRunner {
    /**
     * Published on the {@link ExecutionEnvironment} of a run configuration that was built before launch.
     */
    public static final Key<Path> BIN_PATH = Key.create("swift.bin.path");

    /**
     * {@code /path/File.swift:12:5: error: message} - the format of swiftc and clang; the path may hold {@code :} on Windows.
     */
    private static final Pattern DIAGNOSTIC = Pattern.compile("^(.+?):(\\d+):(\\d+): (error|warning|note): (.*)$");
    private static final String ERROR_PREFIX = "error: ";
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[0-9;]*[A-Za-z]");
    /**
     * What swift-build - the default build system - reports when it does not understand the layout of the toolchain
     * (e.g. distribution packages that put the toolchain into {@code /usr/lib/swift}).
     */
    private static final String SWIFT_BUILD_TOOLCHAIN_NOT_FOUND = "in any registered toolchain";
    private static final String SWIFT_BUILD_TOOL_NOT_FOUND = "error: unable to spawn process";

    private final Project myProject;
    private final ModuleExtensionHelper myModuleExtensionHelper;

    @Inject
    public SwiftCompilerRunner(Project project, ModuleExtensionHelper moduleExtensionHelper) {
        myProject = project;
        myModuleExtensionHelper = moduleExtensionHelper;
    }

    @Override
    public LocalizeValue getName() {
        return SwiftExecutionLocalize.compilerName();
    }

    @RequiredReadAction
    @Override
    public Result checkAvailable(DataContext dataContext) {
        if (!myModuleExtensionHelper.hasModuleExtension(SwiftModuleExtension.class)) {
            return NO;
        }
        RunConfiguration runConfiguration = dataContext.getData(RunConfiguration.KEY);
        if (runConfiguration != null && !(runConfiguration instanceof SwiftBuildableConfiguration)) {
            return NO;
        }
        return new YesResult(PlatformIconGroup.actionsCompile());
    }

    @Override
    public boolean build(CompileDriver compileDriver,
                         CompileContextEx context,
                         BuildProgress<BuildProgressDescriptor> buildProgress,
                         boolean isRebuild,
                         boolean forceCompile,
                         boolean onlyCheckStatus) {
        if (onlyCheckStatus) {
            // SwiftPM decides itself what is out of date
            return false;
        }

        CompileScope compileScope = context.getCompileScope();
        if (compileScope.getUserData(RunConfiguration.KEY) instanceof SwiftBuildableConfiguration configuration) {
            return buildForLaunch(configuration, compileScope.getUserData(ExecutionEnvironment.KEY), context, buildProgress, isRebuild);
        }

        boolean success = true;
        Set<Path> builtPackages = new HashSet<>();
        for (Module module : compileScope.getAffectedModules()) {
            if (module.getExtension(SwiftModuleExtension.class) == null) {
                continue;
            }
            Path packageDirectory = SwiftPackageManagerUtil.findPackageDirectory(module);
            if (packageDirectory == null || !builtPackages.add(packageDirectory)) {
                continue;
            }
            SwiftToolchain toolchain = SwiftPackageManagerUtil.findToolchain(module);
            if (toolchain == null) {
                context.newError(SwiftExecutionLocalize.compilerNoToolchain(module.getName())).add();
                success = false;
                continue;
            }
            if (isRebuild && !run(toolchain, packageDirectory, SwiftBuildCommand.clean(), context, buildProgress)) {
                success = false;
                continue;
            }
            List<String> arguments = SwiftBuildCommand.build(myProject, packageDirectory, null, SwiftBuildCommand.DEBUG);
            success &= run(toolchain, packageDirectory, arguments, context, buildProgress);
        }
        return success;
    }

    private boolean buildForLaunch(SwiftBuildableConfiguration configuration,
                                   @Nullable ExecutionEnvironment environment,
                                   CompileContextEx context,
                                   BuildProgress<BuildProgressDescriptor> buildProgress,
                                   boolean isRebuild) {
        Module module = configuration.getBuildModule();
        if (module == null) {
            context.newError(SwiftExecutionLocalize.runConfigurationNoModule()).add();
            return false;
        }
        Path packageDirectory = SwiftPackageManagerUtil.findPackageDirectory(module);
        if (packageDirectory == null) {
            context.newError(SwiftExecutionLocalize.compilerNoPackage(module.getName())).add();
            return false;
        }
        SwiftToolchain toolchain = SwiftPackageManagerUtil.findToolchain(module);
        if (toolchain == null) {
            context.newError(SwiftExecutionLocalize.compilerNoToolchain(module.getName())).add();
            return false;
        }

        if (isRebuild && !run(toolchain, packageDirectory, SwiftBuildCommand.clean(), context, buildProgress)) {
            return false;
        }

        List<String> arguments = configuration.getBuildArguments(packageDirectory);
        if (!run(toolchain, packageDirectory, arguments, context, buildProgress)) {
            return false;
        }

        if (environment != null) {
            Path binPath = SwiftBuildCommand.queryBinPath(toolchain, packageDirectory, arguments);
            if (binPath == null) {
                context.newError(SwiftExecutionLocalize.compilerBinPathFailed()).add();
                return false;
            }
            environment.putUserData(BIN_PATH, binPath);
        }
        return true;
    }

    private static boolean run(SwiftToolchain toolchain,
                               Path packageDirectory,
                               List<String> arguments,
                               CompileContextEx context,
                               BuildProgress<BuildProgressDescriptor> buildProgress) {
        String presentation = "swift " + String.join(" ", arguments);
        context.getProgressIndicator().setText(LocalizeValue.of(presentation));
        buildProgress.output("> " + presentation + "\n", true);

        ProcessHandler processHandler;
        try {
            processHandler = ProcessHandlerBuilder.create(toolchain.createSwiftCommandLine(packageDirectory, arguments)).killable().build();
        }
        catch (ExecutionException e) {
            context.newError(SwiftExecutionLocalize.compilerCannotRun(presentation, String.valueOf(e.getMessage()))).add();
            return false;
        }

        AtomicInteger exitCode = new AtomicInteger(-1);
        AtomicBoolean toolchainNotFound = new AtomicBoolean();
        // swift build repeats every diagnostic of a compile job in its summary
        Set<String> reported = ConcurrentHashMap.newKeySet();
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (outputType != ProcessOutputTypes.STDOUT && outputType != ProcessOutputTypes.STDERR) {
                    return;
                }
                String text = event.getText();
                buildProgress.output(text, outputType == ProcessOutputTypes.STDOUT);
                report(text, reported, buildProgress);
                if (text.contains(SWIFT_BUILD_TOOLCHAIN_NOT_FOUND) || text.contains(SWIFT_BUILD_TOOL_NOT_FOUND)) {
                    toolchainNotFound.set(true);
                }
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                exitCode.set(event.getExitCode());
            }
        });

        processHandler.startNotify();
        while (!processHandler.waitFor(100)) {
            if (context.getProgressIndicator().isCanceled()) {
                processHandler.destroyProcess();
                return false;
            }
        }

        int code = exitCode.get();
        if (code == 0) {
            return true;
        }
        // the compile driver goes by the errors of the context - without one the launch would go on
        context.newError(SwiftExecutionLocalize.compilerFailed(presentation, code)).add();
        if (toolchainNotFound.get()) {
            context.newInfo(SwiftExecutionLocalize.compilerSwiftBuildToolchainHint()).add();
        }
        return false;
    }

    private static void report(String text, Set<String> reported, BuildProgress<BuildProgressDescriptor> buildProgress) {
        for (String coloredLine : text.split("\\R")) {
            // NO_COLOR is set, but a build system may still color its diagnostics
            String line = ANSI_ESCAPE.matcher(coloredLine).replaceAll("");
            if (line.startsWith(ERROR_PREFIX) && reported.add(line)) {
                // SwiftPM's own errors - of the build, not of a source file
                LocalizeValue message = LocalizeValue.of(line.substring(ERROR_PREFIX.length()));
                buildProgress.message(message, message, MessageEvent.Kind.ERROR, null);
                continue;
            }
            Matcher matcher = DIAGNOSTIC.matcher(line);
            if (!matcher.matches() || !reported.add(line)) {
                continue;
            }
            File file = new File(matcher.group(1));
            if (!file.isFile()) {
                continue;
            }
            MessageEvent.Kind kind = switch (matcher.group(4)) {
                case "error" -> MessageEvent.Kind.ERROR;
                case "warning" -> MessageEvent.Kind.WARNING;
                default -> MessageEvent.Kind.INFO;
            };
            int lineNumber = Integer.parseInt(matcher.group(2)) - 1;
            int column = Integer.parseInt(matcher.group(3)) - 1;
            LocalizeValue message = LocalizeValue.of(matcher.group(5));
            buildProgress.fileMessage(message, message, kind, new FilePosition(file, lineNumber, column));
        }
    }
}
