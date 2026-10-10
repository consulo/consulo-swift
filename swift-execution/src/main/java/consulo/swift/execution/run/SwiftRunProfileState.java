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

import consulo.execution.configuration.CommandLineState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;

/**
 * Runs the built product directly - it was built before launch, so {@code swift run} would only build again.
 */
public class SwiftRunProfileState extends CommandLineState {
    private final SwiftRunConfiguration myConfiguration;

    public SwiftRunProfileState(ExecutionEnvironment environment, SwiftRunConfiguration configuration) {
        super(environment);
        myConfiguration = configuration;
    }

    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        GeneralCommandLine commandLine = myConfiguration.createCommandLine(getEnvironment());
        return getEnvironment().getProject().getApplication().getInstance(ProcessHandlerBuilderFactory.class)
            .newBuilder(commandLine)
            .killable()
            .colored()
            .build();
    }
}
