/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves a Swift module name to a {@code .swiftmodule} file on disk by
 * scanning an ordered list of search-path directories — the same shape
 * the {@code swift-frontend} {@code -I} flag uses.
 *
 * <p>Each search-path directory may contain either:
 * <ul>
 *   <li>A bare {@code <ModuleName>.swiftmodule} file (one architecture,
 *       used for SwiftPM build products and per-target compiled modules).</li>
 *   <li>A {@code <ModuleName>.swiftmodule/} directory containing
 *       per-target binaries like
 *       {@code x86_64-unknown-linux-gnu.swiftmodule},
 *       {@code arm64-apple-macos.swiftmodule}, etc. (used by the Swift
 *       toolchain to ship platform-specific stdlib + Foundation builds).
 *       This is how {@code /usr/lib/swift/lib/swift/linux/Swift.swiftmodule/}
 *       organises {@code x86_64-unknown-linux-gnu.swiftmodule} alongside
 *       its sibling {@code .swiftdoc} / {@code .swiftinterface} files.</li>
 * </ul>
 *
 * <p>The resolver chooses the architecture binary using the {@code target}
 * triple supplied at construction (defaults to whatever the JVM thinks
 * the host architecture is). Multi-arch shipping wraps each binary in a
 * platform-suffixed file inside the {@code .swiftmodule/} bundle; we
 * just pick the one matching {@code target.swiftmodule}.
 */
public final class SwiftModuleSearchPath {

    private final List<Path> directories;
    private final String target;

    private SwiftModuleSearchPath(List<Path> directories, String target) {
        this.directories = directories;
        this.target = target;
    }

    /**
     * Builder. Order of {@link #addDirectory(Path)} calls is preserved —
     * earlier entries shadow later ones, matching how {@code swift-frontend}
     * handles {@code -I} flag stacks.
     */
    public static final class Builder {
        private final List<Path> dirs = new ArrayList<>(4);
        private String target = defaultTarget();

        /** Append a directory; ignored when {@code dir} is null or doesn't exist. */
        public Builder addDirectory(Path dir) {
            if (dir != null && Files.isDirectory(dir)) {
                dirs.add(dir);
            }
            return this;
        }

        /** Override the target triple used to pick per-arch binaries. */
        public Builder withTarget(String target) {
            if (target != null && !target.isEmpty()) {
                this.target = target;
            }
            return this;
        }

        public SwiftModuleSearchPath build() {
            return new SwiftModuleSearchPath(List.copyOf(dirs), target);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a search path that points at the default Swift toolchain
     * locations on the current host. Tries a handful of common layouts:
     *
     * <ul>
     *   <li>Linux installs: {@code /usr/lib/swift/lib/swift/linux},
     *       {@code /usr/lib/swift/linux}.</li>
     *   <li>Apple SDKs: {@code /Library/Developer/Toolchains/swift-latest.xctoolchain/usr/lib/swift/macosx},
     *       {@code /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/macosx}.</li>
     * </ul>
     *
     * <p>Returns a possibly-empty resolver when no toolchain is found —
     * callers should treat that as "no system modules available", same
     * as if {@code -nostdimport} had been passed to the compiler.
     */
    public static SwiftModuleSearchPath system() {
        Builder b = builder();
        // Linux toolchain layouts (Swift.org tarball and distro packages).
        b.addDirectory(Path.of("/usr/lib/swift/lib/swift/linux"));
        b.addDirectory(Path.of("/usr/lib/swift/linux"));
        b.addDirectory(Path.of("/usr/lib/swift_static/linux"));
        // Apple toolchain layouts.
        b.addDirectory(Path.of("/Library/Developer/Toolchains/swift-latest.xctoolchain/usr/lib/swift/macosx"));
        b.addDirectory(Path.of("/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/macosx"));
        return b.build();
    }

    /** The configured target triple (e.g. {@code "x86_64-unknown-linux-gnu"}). */
    public String getTarget() {
        return target;
    }

    /** The list of search directories, in priority order. */
    public List<Path> getDirectories() {
        return directories;
    }

    /**
     * Find the {@code .swiftmodule} for {@code moduleName} by scanning the
     * search-path entries in order. Returns the first match found, or
     * {@code null} when the module isn't on any path.
     *
     * <p>Per-arch wrapper directories are unwrapped: if a directory entry
     * is {@code <Module>.swiftmodule/} (a directory), we pick the file
     * named {@code <target>.swiftmodule} inside it. Otherwise the file
     * {@code <Module>.swiftmodule} is returned directly.
     */
    public Path resolve(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        String simpleFile = moduleName + ".swiftmodule";
        for (Path dir : directories) {
            Path candidate = dir.resolve(simpleFile);
            if (!Files.exists(candidate)) {
                continue;
            }
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            if (Files.isDirectory(candidate)) {
                Path archBinary = candidate.resolve(target + ".swiftmodule");
                if (Files.isRegularFile(archBinary)) {
                    return archBinary;
                }
                // Fallback: pick any .swiftmodule in the bundle. Some
                // distros ship single-arch but still bundle.
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(candidate, "*.swiftmodule")) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry)) {
                            return entry;
                        }
                    }
                }
                catch (IOException ignored) {
                    // Skip — try next search directory.
                }
            }
        }
        return null;
    }

    /**
     * List every module name available on the search path. Useful for
     * "show all installed Swift modules" UIs. The order isn't stable;
     * duplicates across directories are deduplicated by name.
     */
    public List<String> listAvailableModules() {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (Path dir : directories) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.swiftmodule")) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    int dot = name.lastIndexOf('.');
                    if (dot > 0) {
                        seen.add(name.substring(0, dot));
                    }
                }
            }
            catch (IOException ignored) {
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(seen));
    }

    /** Best-effort guess at the host target triple. */
    private static String defaultTarget() {
        String os = System.getProperty("os.name", "linux").toLowerCase();
        String arch = System.getProperty("os.arch", "x86_64").toLowerCase();
        // Normalise JVM arch names to Swift's triple convention.
        if (arch.equals("amd64")) arch = "x86_64";
        if (arch.equals("aarch64")) arch = "arm64";
        if (os.contains("linux")) {
            return arch + "-unknown-linux-gnu";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return arch + "-apple-macos";
        }
        if (os.contains("win")) {
            return arch + "-unknown-windows-msvc";
        }
        return arch + "-unknown-linux-gnu";
    }
}
