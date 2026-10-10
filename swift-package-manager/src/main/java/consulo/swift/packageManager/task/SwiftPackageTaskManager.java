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
import consulo.externalSystem.task.ExternalSystemTaskManager;
import consulo.swift.packageManager.SwiftPackageManagerConstants;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.packageManager.localize.SwiftPackageManagerLocalize;
import consulo.swift.packageManager.setting.SwiftPackageExecutionSettings;
import consulo.swift.toolchain.SwiftToolchain;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the tasks the resolver offers: {@code build}, {@code test}, {@code clean}, {@code resolve}, {@code update}.
 * Script parameters of a task run configuration are appended to the command.
 */
public class SwiftPackageTaskManager implements ExternalSystemTaskManager<SwiftPackageExecutionSettings> {
    private final SwiftPackageCommand myCommand = new SwiftPackageCommand();

    @Override
    public void executeTasks(ExternalSystemTaskId id,
                             List<String> taskNames,
                             String projectPath,
                             @Nullable SwiftPackageExecutionSettings settings,
                             List<String> vmOptions,
                             List<String> scriptParameters,
                             @Nullable String debuggerSetup,
                             ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
        SwiftToolchain toolchain = SwiftPackageManagerUtil.toToolchain(settings);
        Path packageDirectory = SwiftPackageManagerUtil.toPackageDirectory(Path.of(projectPath));
        List<String> buildArguments = settings == null ? List.of() : settings.getBuildArguments();

        for (String taskName : taskNames) {
            List<String> arguments = new ArrayList<>(toArguments(taskName, buildArguments));
            arguments.addAll(scriptParameters);
            myCommand.run(id, toolchain, packageDirectory, arguments, true, listener);
        }
    }

    @Override
    public boolean cancelTask(ExternalSystemTaskId id, ExternalSystemTaskNotificationListener listener) {
        return myCommand.cancel();
    }

    private static List<String> toArguments(String taskName, List<String> buildArguments) {
        List<String> arguments = new ArrayList<>();
        switch (taskName) {
            case SwiftPackageManagerConstants.TASK_BUILD, SwiftPackageManagerConstants.TASK_TEST -> {
                arguments.add(taskName);
                arguments.addAll(buildArguments);
            }
            case SwiftPackageManagerConstants.TASK_CLEAN,
                 SwiftPackageManagerConstants.TASK_RESOLVE,
                 SwiftPackageManagerConstants.TASK_UPDATE -> {
                arguments.add("package");
                arguments.add(taskName);
            }
            default -> throw new ExternalSystemException(SwiftPackageManagerLocalize.errorUnknownTask(taskName).get());
        }
        return arguments;
    }
}
