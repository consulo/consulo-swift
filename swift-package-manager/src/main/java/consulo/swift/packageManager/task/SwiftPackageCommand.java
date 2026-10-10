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
package consulo.swift.packageManager.task;

import consulo.externalSystem.model.task.ExternalSystemTaskId;
import consulo.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import consulo.externalSystem.rt.model.ExternalSystemException;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.toolchain.SwiftToolchain;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs {@code swift ...} for a package on behalf of an external system task, echoing the output to the task console
 * and allowing the task to be cancelled.
 */
public class SwiftPackageCommand {
    private volatile @Nullable ProcessHandler myProcessHandler;
    private volatile boolean myCancelled;

    /**
     * @param echoStdout whether stdout goes to the task console - off for commands whose stdout is JSON to parse
     * @return stdout of the command
     */
    public String run(ExternalSystemTaskId id,
                      SwiftToolchain toolchain,
                      Path packageDirectory,
                      List<String> arguments,
                      boolean echoStdout,
                      ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
        GeneralCommandLine commandLine = toolchain.createSwiftCommandLine(packageDirectory, arguments);
        String presentation = "swift " + String.join(" ", arguments);
        listener.onTaskOutput(id, "> " + presentation + "\n", true);

        ProcessHandler processHandler;
        try {
            processHandler = ProcessHandlerBuilder.create(commandLine).killable().build();
        }
        catch (ExecutionException e) {
            String reason = String.valueOf(e.getMessage());
            throw new ExternalSystemException(SwiftPackageManagerLocalize.errorCannotRun(presentation, reason).get());
        }

        // filled on the output reader thread
        StringBuffer stdout = new StringBuffer();
        AtomicInteger exitCode = new AtomicInteger(-1);
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (outputType == ProcessOutputTypes.STDOUT) {
                    stdout.append(event.getText());
                    if (echoStdout) {
                        listener.onTaskOutput(id, event.getText(), true);
                    }
                }
                else if (outputType == ProcessOutputTypes.STDERR) {
                    listener.onTaskOutput(id, event.getText(), false);
                }
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                exitCode.set(event.getExitCode());
            }
        });

        myProcessHandler = processHandler;
        try {
            processHandler.startNotify();
            processHandler.waitFor();
        }
        finally {
            myProcessHandler = null;
        }

        if (myCancelled) {
            throw new ExternalSystemException(SwiftPackageManagerLocalize.errorCancelled(presentation).get());
        }
        int code = exitCode.get();
        if (code != 0) {
            throw new ExternalSystemException(SwiftPackageManagerLocalize.errorCommandFailed(presentation, code).get());
        }
        return stdout.toString();
    }

    public boolean cancel() {
        myCancelled = true;
        ProcessHandler processHandler = myProcessHandler;
        if (processHandler == null) {
            return false;
        }
        processHandler.destroyProcess();
        return true;
    }
}
