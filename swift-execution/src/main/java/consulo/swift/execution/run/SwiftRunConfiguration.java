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
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationModule;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunProfileWithCompileBeforeLaunchOption;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.nativeDev.debugger.run.NativeProgramRunConfiguration;
import consulo.nativeDev.debugger.run.NativeProgramSettings;
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
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
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
    private static final String ATTR_PRODUCT = "product";
    private static final String ATTR_BUILD_CONFIGURATION = "build-configuration";

    private String myProduct = "";
    private String myBuildConfiguration = SwiftBuildCommand.DEBUG;
    private final NativeProgramSettings myProgramSettings = new NativeProgramSettings();

    public SwiftRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, new RunConfigurationModule(project), factory);
    }

    public String getProduct() {
        return myProduct;
    }

    public void setProduct(@Nullable String product) {
        myProduct = StringUtil.notNullize(product).trim();
    }

    public String getBuildConfiguration() {
        return myBuildConfiguration;
    }

    public void setBuildConfiguration(@Nullable String buildConfiguration) {
        myBuildConfiguration = SwiftBuildCommand.RELEASE.equals(buildConfiguration) ? SwiftBuildCommand.RELEASE : SwiftBuildCommand.DEBUG;
    }

    @Override
    public NativeProgramSettings getProgramSettings() {
        return myProgramSettings;
    }

    @Override
    public @Nullable Module getBuildModule() {
        return getConfigurationModule().getModule();
    }

    @Override
    public List<String> getBuildArguments(Path packageDirectory) {
        return SwiftBuildCommand.build(getProject(), packageDirectory, myProduct, myBuildConfiguration);
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
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SwiftRunConfigurationEditor(getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new RuntimeConfigurationError(SwiftExecutionLocalize.runConfigurationNoModule());
        }
        if (myProduct.isEmpty()) {
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
        GeneralCommandLine commandLine = myProgramSettings.createCommandLine(launch.executable().toString(), launch.workingDirectory());
        launch.toolchain().configureProgramEnvironment(commandLine.getEnvironment());
        return commandLine;
    }

    @Override
    public NativeDebugTarget createDebugTarget(ExecutionEnvironment environment) throws ExecutionException {
        SwiftLaunch launch = resolveLaunch(environment);
        Map<String, String> variables = new LinkedHashMap<>(myProgramSettings.getEnvs());
        launch.toolchain().configureProgramEnvironment(variables);
        return NativeDebugTarget.launch(
                launch.executable(),
                myProgramSettings.getProgramArguments(),
                launch.workingDirectory(),
                variables,
                null
            )
            .withSetup(new SwiftDebuggerSetup(launch.toolchain()));
    }

    /**
     * Where the built product is - known from the build before launch, otherwise asked from SwiftPM.
     */
    public SwiftLaunch resolveLaunch(ExecutionEnvironment environment) throws ExecutionException {
        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoModule().get());
        }
        if (myProduct.isEmpty()) {
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

        Path workingDirectory = myProgramSettings.getWorkingDirectoryPath();
        return new SwiftLaunch(
            toolchain,
            SwiftBuildCommand.executable(toolchain, binPath, myProduct),
            workingDirectory != null ? workingDirectory : packageDirectory
        );
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        setProduct(element.getAttributeValue(ATTR_PRODUCT));
        setBuildConfiguration(element.getAttributeValue(ATTR_BUILD_CONFIGURATION));
        myProgramSettings.readExternal(element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.setAttribute(ATTR_PRODUCT, myProduct);
        element.setAttribute(ATTR_BUILD_CONFIGURATION, myBuildConfiguration);
        myProgramSettings.writeExternal(element);
    }

    /**
     * @param executable       the built product
     * @param workingDirectory the configured one, or the package directory
     */
    public record SwiftLaunch(SwiftToolchain toolchain, Path executable, Path workingDirectory) {
    }
}
