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

import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.project.Project;
import consulo.swift.packageManager.SwiftPackageManagerUtil;
import consulo.swift.toolchain.SwiftToolchain;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command lines of {@code swift build} for a package.
 * <p>
 * Where the binaries land depends on the build system ({@code .build/<triple>/debug} for the native one,
 * {@code .build/out/Products/Debug-<platform>} for swift-build, the default of recent toolchains), on the configuration and on
 * flags like {@code --scratch-path} - so it is always asked from SwiftPM with {@code --show-bin-path} and the very same
 * arguments the build used.
 */
public final class SwiftBuildCommand {
    public static final String DEBUG = "debug";
    public static final String RELEASE = "release";

    private static final int BIN_PATH_TIMEOUT_MS = 60_000;

    private SwiftBuildCommand() {
    }

    /**
     * @param product executable product to build, {@code null} for the whole package
     */
    public static List<String> build(Project project, Path packageDirectory, @Nullable String product, String configuration) {
        List<String> arguments = new ArrayList<>();
        arguments.add("build");
        arguments.add("-c");
        arguments.add(configuration);
        if (product != null) {
            arguments.add("--product");
            arguments.add(product);
        }
        arguments.addAll(SwiftPackageManagerUtil.getBuildArguments(project, packageDirectory.toString()));
        return arguments;
    }

    public static List<String> clean() {
        return List.of("package", "clean");
    }

    /**
     * @return the directory {@code swift build} with the same arguments puts its binaries into, or {@code null} when
     * SwiftPM cannot tell
     */
    public static @Nullable Path queryBinPath(SwiftToolchain toolchain, Path packageDirectory, List<String> buildArguments) {
        List<String> arguments = new ArrayList<>(buildArguments);
        arguments.add("--show-bin-path");
        try {
            GeneralCommandLine commandLine = toolchain.createSwiftCommandLine(packageDirectory, arguments);
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(commandLine, BIN_PATH_TIMEOUT_MS);
            if (output.isTimeout() || output.getExitCode() != 0) {
                return null;
            }
            List<String> lines = output.getStdoutLines();
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    return Path.of(line);
                }
            }
            return null;
        }
        catch (ExecutionException e) {
            return null;
        }
    }

    public static Path executable(SwiftToolchain toolchain, Path binPath, String product) {
        return binPath.resolve(SwiftToolchain.executableName(toolchain.getPlatform(), product));
    }
}
