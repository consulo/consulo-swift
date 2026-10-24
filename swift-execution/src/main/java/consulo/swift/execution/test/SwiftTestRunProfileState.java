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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.sm.SMTestRunnerConnectionUtil;
import consulo.execution.ui.console.ConsoleView;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.swift.execution.compiler.SwiftCompilerRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * {@code swift test} with its results in a test tree. XCTest results come from the console output, swift-testing ones
 * from its event stream (Swift 6+), which a {@link SwiftTestEventStreamTailer} follows.
 */
public class SwiftTestRunProfileState extends CommandLineState {
    private final SwiftTestRunConfiguration myConfiguration;

    public SwiftTestRunProfileState(ExecutionEnvironment environment, SwiftTestRunConfiguration configuration) {
        super(environment);
        myConfiguration = configuration;
    }

    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        SwiftTestRunConfiguration.SwiftTestLaunch launch = myConfiguration.resolveLaunch();
        boolean built = getEnvironment().getUserData(SwiftCompilerRunner.BIN_PATH) != null;
        Path eventStream = launch.toolchain().isAtLeast(6, 0) ? createEventStreamPath() : null;

        List<String> arguments = myConfiguration.getTestArguments(launch.packageDirectory(), built, eventStream);
        GeneralCommandLine commandLine = launch.toolchain().createSwiftCommandLine(launch.packageDirectory(), arguments);
        commandLine.withWorkingDirectory(launch.workingDirectory());
        commandLine.withEnvironment(myConfiguration.getEnvs());
        launch.toolchain().configureProgramEnvironment(commandLine.getEnvironment());

        // no colors - the test console reads the lines
        ProcessHandler processHandler = getEnvironment().getProject().getApplication().getInstance(ProcessHandlerBuilderFactory.class)
            .newBuilder(commandLine)
            .killable()
            .build();
        if (eventStream != null) {
            new SwiftTestEventStreamTailer(eventStream, processHandler).start(AppExecutorUtil.getAppExecutorService());
        }
        return processHandler;
    }

    @Override
    protected ConsoleView createConsole(Executor executor) {
        return SMTestRunnerConnectionUtil.createConsole(
            SwiftTestConsoleProperties.TEST_FRAMEWORK_NAME,
            myConfiguration.createTestConsoleProperties(executor)
        );
    }

    /**
     * Deleted by the tailer when the run ends.
     */
    private static Path createEventStreamPath() throws ExecutionException {
        try {
            return Files.createTempFile("swift-testing-events", ".jsonl");
        }
        catch (IOException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
    }
}
