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
package consulo.swift.toolchain;

import consulo.content.bundle.Sdk;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Swift toolchain, addressed by its home - the directory holding {@code bin/swift}. That is {@code usr} of an
 * installed toolchain on every platform:
 * <ul>
 *     <li>Linux tarball / swiftly: {@code <toolchain>/usr}, distro packages: e.g. {@code /usr/lib/swift}</li>
 *     <li>Windows: {@code %LOCALAPPDATA%/Programs/Swift/Toolchains/<version>+Asserts/usr}</li>
 *     <li>macOS: {@code <name>.xctoolchain/usr}</li>
 * </ul>
 * Tools of a toolchain are not always on {@code PATH} - distro packages keep {@code llvm-ar} & co. in the toolchain only,
 * and on Windows the Swift runtime DLLs live in a separate {@code Runtimes} directory. So every process started for a
 * toolchain gets its directories prepended to {@code PATH} - see {@link #configureEnvironment(Map)}.
 */
public final class SwiftToolchain {
    public static final String SWIFT = "swift";

    private static final Pattern VERSION_PATTERN = Pattern.compile("Swift version (\\S+)");
    private static final String ASSERTS_SUFFIX = "+Asserts";
    private static final String SDKROOT = "SDKROOT";
    private static final int VERSION_TIMEOUT_MS = 10_000;

    private final Platform myPlatform;
    private final Path myHome;
    private final @Nullable String myVersion;

    public SwiftToolchain(Platform platform, Path home) {
        this(platform, home, null);
    }

    /**
     * @param version as {@link #queryVersion()} reports it, when already known (e.g. from the bundle)
     */
    public SwiftToolchain(Platform platform, Path home, @Nullable String version) {
        myPlatform = platform;
        myHome = home;
        myVersion = version;
    }

    public static @Nullable SwiftToolchain fromSdk(@Nullable Sdk sdk) {
        if (sdk == null || !(sdk.getSdkType() instanceof SwiftSdkType)) {
            return null;
        }
        String homePath = sdk.getHomePath();
        if (homePath == null || homePath.isEmpty()) {
            return null;
        }
        return new SwiftToolchain(sdk.getPlatform(), sdk.getHomeNioPath(), sdk.getVersionString());
    }

    public static boolean isValidHome(Platform platform, Path home) {
        Path bin = home.resolve("bin");
        return Files.isRegularFile(bin.resolve(executableName(platform, SWIFT)))
            && Files.isRegularFile(bin.resolve(executableName(platform, "swift-frontend")));
    }

    public static String executableName(Platform platform, String tool) {
        return platform.os().isWindows() ? tool + ".exe" : tool;
    }

    public Platform getPlatform() {
        return myPlatform;
    }

    public Path getHome() {
        return myHome;
    }

    /**
     * @return {@code true} when the version is known and at least {@code major.minor}
     */
    public boolean isAtLeast(int major, int minor) {
        String version = myVersion;
        if (version == null) {
            return false;
        }
        String[] parts = version.split("[.-]");
        try {
            int actualMajor = Integer.parseInt(parts[0]);
            int actualMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return actualMajor > major || actualMajor == major && actualMinor >= minor;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public Path getBinDirectory() {
        return myHome.resolve("bin");
    }

    public Path getExecutable(String tool) {
        return getBinDirectory().resolve(executableName(myPlatform, tool));
    }

    public Path getSwiftExecutable() {
        return getExecutable(SWIFT);
    }

    /**
     * The debug adapter of the toolchain's own LLDB - the one built with Swift support. It is {@code lldb-dap} since
     * Swift 6 and {@code lldb-vscode} before.
     */
    public @Nullable Path findDebugAdapter() {
        for (String name : List.of("lldb-dap", "lldb-vscode")) {
            Path executable = getExecutable(name);
            if (Files.isRegularFile(executable)) {
                return executable;
            }
        }
        return null;
    }

    /**
     * Directories that have to be on {@code PATH} for the tools of this toolchain and for programs built by it.
     */
    public List<Path> getPathDirectories() {
        List<Path> directories = new ArrayList<>();
        directories.add(getBinDirectory());
        Path runtime = findWindowsRuntimeDirectory();
        if (runtime != null) {
            directories.add(runtime);
        }
        return directories;
    }

    /**
     * Puts {@link #getPathDirectories()} in front of {@code PATH} (keeping the spelling of the parent's variable, which
     * is {@code Path} on Windows), and on Windows points {@code SDKROOT} at the platform SDK when nothing does yet.
     *
     * @param environment variables to add to the parent environment; changed in place
     */
    public void configureEnvironment(Map<String, String> environment) {
        Map<String, String> parent = myPlatform.os().environmentVariables();
        prependPath(environment, parent, getPathDirectories());

        if (myPlatform.os().isWindows()) {
            String sdkRootName = findVariableName(parent, SDKROOT);
            if (!environment.containsKey(sdkRootName) && parent.get(sdkRootName) == null) {
                Path sdkRoot = findWindowsSdkRoot();
                if (sdkRoot != null) {
                    environment.put(sdkRootName, sdkRoot.toString());
                }
            }
        }
    }

    /**
     * Environment for a program built by this toolchain. Only Windows needs something - the Swift runtime DLLs, which
     * are not next to the program (on Linux/macOS the program finds the runtime through its rpath).
     *
     * @param environment variables to add to the parent environment; changed in place
     */
    public void configureProgramEnvironment(Map<String, String> environment) {
        Path runtime = findWindowsRuntimeDirectory();
        if (runtime != null) {
            prependPath(environment, myPlatform.os().environmentVariables(), List.of(runtime));
        }
    }

    private void prependPath(Map<String, String> environment, Map<String, String> parent, List<Path> directories) {
        String pathName = findVariableName(parent, "PATH");
        String current = environment.containsKey(pathName) ? environment.get(pathName) : parent.get(pathName);

        StringBuilder builder = new StringBuilder();
        String separator = myPlatform.os().isWindows() ? ";" : ":";
        for (Path directory : directories) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(directory);
        }
        if (current != null && !current.isEmpty()) {
            builder.append(separator).append(current);
        }
        environment.put(pathName, builder.toString());
    }

    public GeneralCommandLine createSwiftCommandLine(@Nullable Path workDirectory, List<String> arguments) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withExecutablePath(getSwiftExecutable());
        commandLine.withWorkingDirectory(workDirectory);
        commandLine.addParameters(arguments);
        commandLine.withPlatform(myPlatform);
        configureEnvironment(commandLine.getEnvironment());
        return commandLine;
    }

    /**
     * @return version from {@code swift --version}, e.g. {@code 6.0.3}, or {@code null} when the toolchain does not run
     */
    public @Nullable String queryVersion() {
        try {
            GeneralCommandLine commandLine = createSwiftCommandLine(myHome, List.of("--version"));
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(commandLine, VERSION_TIMEOUT_MS);
            if (output.isTimeout() || output.getExitCode() != 0) {
                return null;
            }
            String text = output.getStdout() + "\n" + output.getStderr();
            Matcher matcher = VERSION_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            List<String> lines = output.getStdoutLines();
            return lines.isEmpty() ? null : lines.get(0).trim();
        }
        catch (ExecutionException e) {
            return null;
        }
    }

    /**
     * {@code <root>/Toolchains/<version>+Asserts/usr} has its runtime in {@code <root>/Runtimes/<version>/usr/bin};
     * the legacy {@code C:/Library/Developer/Toolchains/...} layout has it in {@code C:/Library/Swift-development/bin}.
     */
    private @Nullable Path findWindowsRuntimeDirectory() {
        WindowsLayout layout = getWindowsLayout();
        if (layout == null) {
            return null;
        }

        Path runtime = layout.root().resolve("Runtimes").resolve(layout.version()).resolve("usr").resolve("bin");
        if (Files.isDirectory(runtime)) {
            return runtime;
        }

        Path developer = layout.root().getParent();
        Path legacyRuntime = developer == null ? null : developer.resolve("Swift-development").resolve("bin");
        return legacyRuntime != null && Files.isDirectory(legacyRuntime) ? legacyRuntime : null;
    }

    /**
     * {@code <root>/Platforms/<version>/Windows.platform/Developer/SDKs/Windows.sdk}, or without the version directory in
     * older layouts.
     */
    private @Nullable Path findWindowsSdkRoot() {
        WindowsLayout layout = getWindowsLayout();
        if (layout == null) {
            return null;
        }

        Path platforms = layout.root().resolve("Platforms");
        for (Path platform : List.of(platforms.resolve(layout.version()), platforms)) {
            Path sdk = platform.resolve("Windows.platform").resolve("Developer").resolve("SDKs").resolve("Windows.sdk");
            if (Files.isDirectory(sdk)) {
                return sdk;
            }
        }
        return null;
    }

    private @Nullable WindowsLayout getWindowsLayout() {
        if (!myPlatform.os().isWindows()) {
            return null;
        }
        Path versionDirectory = myHome.getParent();
        Path toolchains = versionDirectory == null ? null : versionDirectory.getParent();
        Path root = toolchains == null ? null : toolchains.getParent();
        Path versionName = versionDirectory == null ? null : versionDirectory.getFileName();
        if (root == null || versionName == null) {
            return null;
        }

        String version = versionName.toString();
        if (version.endsWith(ASSERTS_SUFFIX)) {
            version = version.substring(0, version.length() - ASSERTS_SUFFIX.length());
        }
        return new WindowsLayout(root, version);
    }

    /**
     * Windows environment variables are case-insensitive - reuse the parent's spelling so a child does not get both.
     */
    private String findVariableName(Map<String, String> environment, String name) {
        if (!myPlatform.os().isWindows()) {
            return name;
        }
        for (String key : environment.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return name;
    }

    /**
     * @param root    the install root holding {@code Toolchains}, {@code Runtimes} and {@code Platforms}
     * @param version the toolchain version without the {@code +Asserts} suffix
     */
    private record WindowsLayout(Path root, String version) {
    }

    @Override
    public String toString() {
        return "SwiftToolchain[" + myHome + "]";
    }
}
