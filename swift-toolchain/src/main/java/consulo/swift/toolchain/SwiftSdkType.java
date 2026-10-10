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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.content.bundle.PlatformAwareSdkType;
import consulo.content.bundle.SdkType;
import consulo.platform.Platform;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.toolchain.localize.SwiftToolchainLocalize;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Swift toolchain bundle. Its home is the directory holding {@code bin/swift} - see {@link SwiftToolchain}.
 */
@ExtensionImpl
public class SwiftSdkType extends PlatformAwareSdkType {
    public static final String ID = "SWIFT";

    public static SwiftSdkType getInstance() {
        return Application.get().getExtensionPoint(SdkType.class).findExtensionOrFail(SwiftSdkType.class);
    }

    public SwiftSdkType() {
        super(ID, SwiftToolchainLocalize.sdkTypeName(), SwiftIconGroup.swift());
    }

    @Override
    public void collectHomePaths(Platform platform, Consumer<Path> consumer) {
        Set<Path> homes = new LinkedHashSet<>();
        collectFromPath(platform, homes);
        collectWellKnown(platform, homes);

        for (Path home : homes) {
            if (SwiftToolchain.isValidHome(platform, home)) {
                consumer.accept(home);
            }
        }
    }

    @Override
    public boolean canCreatePredefinedSdks(Platform platform) {
        return Platform.LOCAL.equals(platform.getId());
    }

    @Override
    public boolean isValidSdkHome(Platform platform, Path path) {
        return SwiftToolchain.isValidHome(platform, path);
    }

    /**
     * Accepts the toolchain directory itself ({@code 6.1.2+Asserts}, {@code *.xctoolchain}) and its {@code bin}.
     */
    @Override
    public Path adjustSelectedSdkHome(Platform platform, Path homePath) {
        if (SwiftToolchain.isValidHome(platform, homePath)) {
            return homePath;
        }
        Path usr = homePath.resolve("usr");
        if (SwiftToolchain.isValidHome(platform, usr)) {
            return usr;
        }
        Path parent = homePath.getParent();
        if (parent != null && SwiftToolchain.isValidHome(platform, parent)) {
            return parent;
        }
        return homePath;
    }

    @Override
    public @Nullable String getVersionString(Platform platform, Path path) {
        if (!SwiftToolchain.isValidHome(platform, path)) {
            return null;
        }
        return new SwiftToolchain(platform, path).queryVersion();
    }

    /**
     * {@code swift} on {@code PATH} is usually a link into a toolchain (e.g. {@code /usr/bin/swift} of a distro package),
     * so the toolchain is where the link resolves to. A swiftly proxy resolves to swiftly itself and is skipped by
     * {@link SwiftToolchain#isValidHome}, since it has no {@code swift-frontend} next to it.
     */
    private static void collectFromPath(Platform platform, Set<Path> homes) {
        String pathVariable = platform.os().getEnvironmentVariable("PATH");
        if (pathVariable == null || pathVariable.isEmpty()) {
            return;
        }

        String separator = platform.os().isWindows() ? ";" : ":";
        String swift = SwiftToolchain.executableName(platform, SwiftToolchain.SWIFT);
        for (String entry : pathVariable.split(separator)) {
            if (entry.isBlank()) {
                continue;
            }
            try {
                Path executable = platform.fs().getPath(entry.trim()).resolve(swift);
                if (!Files.isRegularFile(executable)) {
                    continue;
                }
                Path bin = executable.toRealPath().getParent();
                Path home = bin == null ? null : bin.getParent();
                if (home != null) {
                    homes.add(home);
                }
            }
            catch (IOException | InvalidPathException ignored) {
            }
        }
    }

    private static void collectWellKnown(Platform platform, Set<Path> homes) {
        Path userHome = platform.user().homePath();
        if (platform.os().isWindows()) {
            // per-user install (default since 5.10), per-machine install, and the layout before 5.9
            addToolchains(homes, envPath(platform, "LOCALAPPDATA", "Programs", "Swift", "Toolchains"));
            addToolchains(homes, envPath(platform, "ProgramFiles", "Swift", "Toolchains"));
            addToolchains(homes, envPath(platform, "SystemDrive", "Library", "Developer", "Toolchains"));
        }
        else if (platform.os().isMac()) {
            addToolchains(homes, platform.fs().getPath("/Library/Developer/Toolchains"));
            addToolchains(homes, userHome.resolve("Library/Developer/Toolchains"));
            homes.add(platform.fs().getPath("/Library/Developer/CommandLineTools/usr"));
            for (Path xcode : list(platform.fs().getPath("/Applications"))) {
                Path name = xcode.getFileName();
                if (name != null && name.toString().startsWith("Xcode")) {
                    homes.add(xcode.resolve("Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr"));
                }
            }
        }
        else {
            homes.add(platform.fs().getPath("/usr/lib/swift"));
            homes.add(platform.fs().getPath("/usr/share/swift/usr"));
            homes.add(platform.fs().getPath("/opt/swift/usr"));
            addToolchains(homes, platform.fs().getPath("/opt"));

            // swiftly - https://github.com/swiftlang/swiftly
            addToolchains(homes, envPath(platform, "SWIFTLY_TOOLCHAINS_DIR"));
            addToolchains(homes, envPath(platform, "SWIFTLY_HOME_DIR", "toolchains"));
            Path dataHome = envPath(platform, "XDG_DATA_HOME");
            addToolchains(homes, (dataHome != null ? dataHome : userHome.resolve(".local/share")).resolve("swiftly/toolchains"));
        }
    }

    /**
     * Every child of {@code directory} is a toolchain whose {@code usr} may be a home.
     */
    private static void addToolchains(Set<Path> homes, @Nullable Path directory) {
        if (directory == null) {
            return;
        }
        for (Path toolchain : list(directory)) {
            homes.add(toolchain.resolve("usr"));
        }
    }

    private static @Nullable Path envPath(Platform platform, String variable, String... children) {
        String value = platform.os().getEnvironmentVariable(variable);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path path = platform.fs().getPath(value.trim());
            for (String child : children) {
                path = path.resolve(child);
            }
            return path;
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    private static List<Path> list(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.filter(Files::isDirectory).toList();
        }
        catch (IOException e) {
            return List.of();
        }
    }
}
