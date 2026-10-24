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

import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfigurationModule;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunProfileWithCompileBeforeLaunchOption;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.nativeDev.debugger.run.NativeProgramRunConfiguration;
import consulo.nativeDev.profiler.NativeProfilableRunProfile;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import consulo.swift.execution.compiler.SwiftBuildCommand;
import consulo.swift.execution.compiler.SwiftBuildableConfiguration;
import consulo.swift.execution.compiler.SwiftCompilerRunner;
import consulo.swift.execution.debugger.SwiftDebuggerSetup;
import consulo.swift.execution.localize.SwiftExecutionLocalize;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.toolchain.SwiftToolchain;
import consulo.swift.toolchain.module.extension.SwiftModuleExtension;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs/debugs an executable product of a package. The product is built before launch by {@link SwiftCompilerRunner},
 * debugging goes through nativeDev ({@link NativeProgramRunConfiguration}) with the LLDB of the module's toolchain.
 */
public class SwiftRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
    implements NativeProgramRunConfiguration, SwiftBuildableConfiguration, NativeProfilableRunProfile,
    RunProfileWithCompileBeforeLaunchOption {
    public SwiftRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, new RunConfigurationModule(project), factory);
    }

    @Override
    protected Class<SwiftRunConfigurationOptions> getOptionsClass() {
        return SwiftRunConfigurationOptions.class;
    }

    @Override
    public SwiftRunConfigurationOptions getOptions() {
        return (SwiftRunConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        SwiftRunConfigurationOptions options = new SwiftRunConfigurationOptions();
        options.setProduct(converter.getElement().getAttributeValue("product"));
        String buildConfiguration = converter.getElement().getAttributeValue("build-configuration");
        options.setBuildConfiguration(SwiftBuildCommand.RELEASE.equals(buildConfiguration) ? SwiftBuildCommand.RELEASE : null);
        converter.setOptions(options);

        NativeProgramRunConfiguration.convertLegacyProgramState(converter);
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(new SwiftProductFragment());
    }

    public String getProduct() {
        return StringUtil.notNullize(getOptions().getProduct());
    }

    public void setProduct(@Nullable String product) {
        getOptions().setProduct(StringUtil.notNullize(product).trim());
    }

    public String getBuildConfiguration() {
        return SwiftBuildCommand.RELEASE.equals(getOptions().getBuildConfiguration()) ? SwiftBuildCommand.RELEASE : SwiftBuildCommand.DEBUG;
    }

    public void setBuildConfiguration(@Nullable String buildConfiguration) {
        getOptions().setBuildConfiguration(SwiftBuildCommand.RELEASE.equals(buildConfiguration) ? SwiftBuildCommand.RELEASE : null);
    }


    @Override
    public @Nullable Module getBuildModule() {
        return getConfigurationModule().getModule();
    }

    @Override
    public List<String> getBuildArguments(Path packageDirectory) {
        return SwiftBuildCommand.build(getProject(), packageDirectory, getProduct(), getBuildConfiguration());
    }

    @Override
    public Collection<Module> getValidModules() {
        List<Module> modules = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(getProject()).getModules()) {
            if (module.getExtension(SwiftModuleExtension.class) != null) {
                modules.add(module);
            }
        }
        return modules;
    }


    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new RuntimeConfigurationError(SwiftExecutionLocalize.runConfigurationNoModule());
        }
        if (getProduct().isEmpty()) {
            throw new RuntimeConfigurationError(SwiftExecutionLocalize.runConfigurationNoProduct());
        }
        if (SwiftPackageManagerUtil.findPackageDirectory(module) == null) {
            throw new RuntimeConfigurationError(SwiftExecutionLocalize.runConfigurationNoPackage(module.getName()));
        }
        if (SwiftPackageManagerUtil.findToolchain(module) == null) {
            throw new RuntimeConfigurationError(SwiftExecutionLocalize.runConfigurationNoToolchain(module.getName()));
        }
    }

    @Override
    public @Nullable RunProfileState getState(Executor executor, ExecutionEnvironment environment) {
        return new SwiftRunProfileState(environment, this);
    }

    /**
     * The built product with the program parameters, environment and working directory of this configuration.
     */
    public GeneralCommandLine createCommandLine(ExecutionEnvironment environment) throws ExecutionException {
        SwiftLaunch launch = resolveLaunch(environment);
        GeneralCommandLine commandLine = createCommandLine(launch.executable().toString(), launch.workingDirectory());
        launch.toolchain().configureProgramEnvironment(commandLine.getEnvironment());
        return commandLine;
    }

    @Override
    public NativeDebugTarget createDebugTarget(ExecutionEnvironment environment) throws ExecutionException {
        SwiftLaunch launch = resolveLaunch(environment);
        Map<String, String> variables = new LinkedHashMap<>(getEnvs());
        launch.toolchain().configureProgramEnvironment(variables);
        return NativeDebugTarget.launch(
                launch.executable(),
                getProgramArguments(),
                launch.workingDirectory(),
                variables,
                null
            )
            .withSetup(SwiftDebuggerSetup.create(getProject(), launch.toolchain()));
    }

    /**
     * Where the built product is - known from the build before launch, otherwise asked from SwiftPM.
     */
    public SwiftLaunch resolveLaunch(ExecutionEnvironment environment) throws ExecutionException {
        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoModule().get());
        }
        if (getProduct().isEmpty()) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoProduct().get());
        }
        Path packageDirectory = SwiftPackageManagerUtil.findPackageDirectory(module);
        if (packageDirectory == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoPackage(module.getName()).get());
        }
        SwiftToolchain toolchain = SwiftPackageManagerUtil.findToolchain(module);
        if (toolchain == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoToolchain(module.getName()).get());
        }

        Path binPath = environment.getUserData(SwiftCompilerRunner.BIN_PATH);
        if (binPath == null) {
            binPath = SwiftBuildCommand.queryBinPath(toolchain, packageDirectory, getBuildArguments(packageDirectory));
        }
        if (binPath == null) {
            throw new ExecutionException(SwiftExecutionLocalize.compilerBinPathFailed().get());
        }

        Path workingDirectory = resolveWorkingDirectory(packageDirectory);
        return new SwiftLaunch(
            toolchain,
            SwiftBuildCommand.executable(toolchain, binPath, getProduct()),
            workingDirectory != null ? workingDirectory : packageDirectory
        );
    }



    /**
     * @param executable       the built product
     * @param workingDirectory the configured one, or the package directory
     */
    public record SwiftLaunch(SwiftToolchain toolchain, Path executable, Path workingDirectory) {
    }
}
