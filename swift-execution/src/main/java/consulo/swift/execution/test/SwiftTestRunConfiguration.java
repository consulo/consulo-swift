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
package consulo.swift.execution.test;

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
import consulo.execution.test.sm.runner.SMRunnerConsolePropertiesProvider;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.nativeDev.debugger.run.NativeProgramRunConfiguration;
import consulo.process.ExecutionException;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Runs the tests of a package with {@code swift test} into a test tree; debugging launches the test binary itself under
 * the native debugger.
 */
public class SwiftTestRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
    implements NativeProgramRunConfiguration, SwiftBuildableConfiguration, RunProfileWithCompileBeforeLaunchOption,
    SMRunnerConsolePropertiesProvider {
    private static final String TEST_BINARY_SUFFIX = "PackageTests.xctest";


    public SwiftTestRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, new RunConfigurationModule(project), factory);
    }

    @Override
    protected Class<SwiftTestRunConfigurationOptions> getOptionsClass() {
        return SwiftTestRunConfigurationOptions.class;
    }

    @Override
    public SwiftTestRunConfigurationOptions getOptions() {
        return (SwiftTestRunConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        SwiftTestRunConfigurationOptions options = new SwiftTestRunConfigurationOptions();
        options.setFilter(converter.getElement().getAttributeValue("filter"));
        SwiftTestFramework framework = parseFramework(converter.getElement().getAttributeValue("framework"));
        options.setFramework(framework == null || framework == SwiftTestFramework.ALL ? null : framework.name());
        converter.setOptions(options);

        NativeProgramRunConfiguration.convertLegacyProgramState(converter);
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(new SwiftTestsFragment());
    }

    /**
     * @return regular expression of {@code swift test --filter} ({@code Target.Class/method}, {@code Target.Suite/test()}),
     * empty for all tests
     */
    public String getFilter() {
        return StringUtil.notNullize(getOptions().getFilter());
    }

    public void setFilter(@Nullable String filter) {
        getOptions().setFilter(StringUtil.notNullize(filter).trim());
    }

    public SwiftTestFramework getFramework() {
        SwiftTestFramework framework = parseFramework(getOptions().getFramework());
        return framework == null ? SwiftTestFramework.ALL : framework;
    }

    public void setFramework(@Nullable SwiftTestFramework framework) {
        getOptions().setFramework(framework == null || framework == SwiftTestFramework.ALL ? null : framework.name());
    }


    /**
     * {@code swift build --build-tests} with the arguments of the package.
     */
    @Override
    public @Nullable Module getBuildModule() {
        return getConfigurationModule().getModule();
    }

    @Override
    public List<String> getBuildArguments(Path packageDirectory) {
        List<String> arguments = new ArrayList<>(SwiftBuildCommand.build(getProject(), packageDirectory, null, SwiftBuildCommand.DEBUG));
        arguments.add(1, "--build-tests");
        return arguments;
    }

    /**
     * {@code swift test} for the run - without building when the tests were built before launch.
     */
    public List<String> getTestArguments(Path packageDirectory, boolean built, @Nullable Path eventStream) {
        List<String> arguments = new ArrayList<>();
        arguments.add("test");
        // the working directory of the tests may be another one
        arguments.add("--package-path");
        arguments.add(packageDirectory.toString());
        if (built) {
            arguments.add("--skip-build");
        }
        List<String> buildArguments = SwiftBuildCommand.build(getProject(), packageDirectory, null, SwiftBuildCommand.DEBUG);
        arguments.addAll(buildArguments.subList(1, buildArguments.size()));
        arguments.addAll(getFramework().getSwiftTestArguments());
        String filter = getFilter();
        if (!filter.isEmpty()) {
            arguments.add("--filter");
            arguments.add(filter);
        }
        if (eventStream != null) {
            arguments.add("--event-stream-output-path");
            arguments.add(eventStream.toString());
            arguments.add("--event-stream-version");
            arguments.add("0");
        }
        arguments.addAll(getProgramArguments());
        return arguments;
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
    public SMTRunnerConsoleProperties createTestConsoleProperties(Executor executor) {
        return new SwiftTestConsoleProperties(this, executor);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new RuntimeConfigurationError(SwiftExecutionLocalize.runConfigurationNoModule());
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
        return new SwiftTestRunProfileState(environment, this);
    }

    public SwiftTestLaunch resolveLaunch() throws ExecutionException {
        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoModule().get());
        }
        Path packageDirectory = SwiftPackageManagerUtil.findPackageDirectory(module);
        if (packageDirectory == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoPackage(module.getName()).get());
        }
        SwiftToolchain toolchain = SwiftPackageManagerUtil.findToolchain(module);
        if (toolchain == null) {
            throw new ExecutionException(SwiftExecutionLocalize.runConfigurationNoToolchain(module.getName()).get());
        }
        Path workingDirectory = resolveWorkingDirectory(packageDirectory);
        return new SwiftTestLaunch(toolchain, packageDirectory, workingDirectory != null ? workingDirectory : packageDirectory);
    }

    /**
     * Debugging runs the test binary ({@code <Package>PackageTests.xctest}) directly: XCTest takes the filter as a test
     * specifier, swift-testing is selected with {@code --testing-library swift-testing} and takes it as {@code --filter}.
     */
    @Override
    public NativeDebugTarget createDebugTarget(ExecutionEnvironment environment) throws ExecutionException {
        SwiftTestLaunch launch = resolveLaunch();
        Path binPath = environment.getUserData(SwiftCompilerRunner.BIN_PATH);
        if (binPath == null) {
            List<String> buildArguments = getBuildArguments(launch.packageDirectory());
            binPath = SwiftBuildCommand.queryBinPath(launch.toolchain(), launch.packageDirectory(), buildArguments);
        }
        if (binPath == null) {
            throw new ExecutionException(SwiftExecutionLocalize.compilerBinPathFailed().get());
        }
        Path testBinary = findTestBinary(binPath);
        if (testBinary == null || Files.isDirectory(testBinary)) {
            // a directory is a macOS bundle, which only xctest/swiftpm-testing-helper can load
            throw new ExecutionException(SwiftExecutionLocalize.testDebugNoBinary(binPath.toString()).get());
        }

        List<String> arguments = new ArrayList<>();
        if (getFramework() == SwiftTestFramework.SWIFT_TESTING) {
            arguments.add("--testing-library");
            arguments.add("swift-testing");
            if (!getFilter().isEmpty()) {
                arguments.add("--filter");
                arguments.add(getFilter());
            }
        }
        else if (!getFilter().isEmpty()) {
            arguments.add(getFilter());
        }
        arguments.addAll(getProgramArguments());

        Map<String, String> variables = new LinkedHashMap<>(getEnvs());
        launch.toolchain().configureProgramEnvironment(variables);
        return NativeDebugTarget.launch(testBinary, arguments, launch.workingDirectory(), variables, null)
            .withSetup(SwiftDebuggerSetup.create(getProject(), launch.toolchain()));
    }

    private static @Nullable Path findTestBinary(Path binPath) throws ExecutionException {
        try (Stream<Path> children = Files.list(binPath)) {
            return children.filter(child -> {
                Path name = child.getFileName();
                String fileName = name == null ? "" : name.toString();
                return fileName.endsWith(TEST_BINARY_SUFFIX) || fileName.endsWith(TEST_BINARY_SUFFIX + ".exe");
            }).findFirst().orElse(null);
        }
        catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
    }



    private static @Nullable SwiftTestFramework parseFramework(@Nullable String name) {
        for (SwiftTestFramework framework : SwiftTestFramework.values()) {
            if (framework.name().equals(name)) {
                return framework;
            }
        }
        return null;
    }

    /**
     * @param workingDirectory the configured one, or the package directory
     */
    public record SwiftTestLaunch(SwiftToolchain toolchain, Path packageDirectory, Path workingDirectory) {
    }
}
