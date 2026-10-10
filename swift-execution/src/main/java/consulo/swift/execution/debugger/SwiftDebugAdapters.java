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
package consulo.swift.execution.debugger;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.swift.toolchain.SwiftToolchain;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether the debug adapter of a toolchain can start at all - the LLDB of a toolchain links against a particular
 * Python, which a distribution package may not have (e.g. {@code libpython3.12.so.1.0} on a system with a newer Python).
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class SwiftDebugAdapters {
    private static final int TIMEOUT_MS = 10_000;

    private record Checked(Path adapter, long lastModified) {
    }

    private final Map<Checked, Optional<String>> myProblems = new ConcurrentHashMap<>();
    private final Set<Path> myReported = ConcurrentHashMap.newKeySet();

    public static SwiftDebugAdapters getInstance() {
        return Application.get().getInstance(SwiftDebugAdapters.class);
    }

    /**
     * @return why {@code adapter} cannot start, {@code null} when it can
     */
    public @Nullable String findStartProblem(SwiftToolchain toolchain, Path adapter) {
        long lastModified;
        try {
            lastModified = Files.getLastModifiedTime(adapter).toMillis();
        }
        catch (IOException e) {
            return String.valueOf(e.getMessage());
        }
        return myProblems.computeIfAbsent(new Checked(adapter, lastModified), checked -> Optional.ofNullable(check(toolchain, adapter)))
            .orElse(null);
    }

    /**
     * @return whether this is the first time the problem of {@code adapter} is to be reported
     */
    public boolean markReported(Path adapter) {
        return myReported.add(adapter);
    }

    private static @Nullable String check(SwiftToolchain toolchain, Path adapter) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withExecutablePath(adapter);
        commandLine.addParameter("--help");
        commandLine.withPlatform(toolchain.getPlatform());
        toolchain.configureEnvironment(commandLine.getEnvironment());
        try {
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(commandLine, TIMEOUT_MS);
            if (output.isTimeout() || output.getExitCode() == 0) {
                // a timeout means it runs - it waits for a client
                return null;
            }
            List<String> errors = output.getStderrLines();
            return errors.isEmpty() ? "exit code " + output.getExitCode() : errors.get(0).strip();
        }
        catch (ExecutionException e) {
            return String.valueOf(e.getMessage());
        }
    }
}
