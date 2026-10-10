/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden-file regression test: renders {@code PackageDescription.swiftmodule}
 * through {@link SwiftModuleSourcePrinter} and asserts the output is
 * byte-for-byte identical to a stored snapshot at
 * {@code src/test/resources/swiftmodule/PackageDescription.expected.swift}.
 *
 * <p>The snapshot makes regressions in the decompiler's output visible at
 * a glance — any change to a printer, an extractor, or a record layout
 * shows up as a diff in CI. There's no native-tool dependency: the file
 * is read from the classpath and compared as a UTF-8 string.
 *
 * <p><b>Updating the snapshot:</b> run the test JVM with
 * {@code -Dswift.bitcode.regenerate=true} — the test will overwrite the
 * stored file with the current renderer's output and pass. Inspect the
 * diff before committing. (The path is resolved from the working
 * directory; run from the module root or pass {@code -Dswift.bitcode.module=...}
 * to point at the module's {@code src} tree explicitly.)
 *
 * <pre>
 *   mvn test -Dswift.bitcode.regenerate=true -Dtest=SwiftModuleSnapshotTest
 * </pre>
 */
class SwiftModuleSnapshotTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    /** Classpath location of the golden file (UTF-8, LF line endings). */
    private static final String SNAPSHOT_RESOURCE =
        "/swiftmodule/PackageDescription.expected.swift";

    /**
     * Filesystem path the regenerate switch writes back to. Resolves from
     * the working directory by default; the test prints the absolute path
     * it picked so failures are easy to diagnose.
     */
    private static final String SNAPSHOT_FS_PATH =
        "src/test/resources/swiftmodule/PackageDescription.expected.swift";

    @Test
    void synthesizedSourceMatchesStoredSnapshot() throws IOException {
        String actual = renderFixture();

        if (Boolean.getBoolean("swift.bitcode.regenerate")) {
            Path target = resolveSnapshotPath();
            Files.writeString(target, actual, StandardCharsets.UTF_8);
            System.out.println("[regenerate] wrote " + actual.length()
                + " bytes to " + target.toAbsolutePath());
            return;
        }

        URL url = SwiftModuleSnapshotTest.class.getResource(SNAPSHOT_RESOURCE);
        assertNotNull(url, "Snapshot stub missing at " + SNAPSHOT_RESOURCE
            + " — run with -Dswift.bitcode.regenerate=true to create it");
        String expected;
        try (InputStream in = url.openStream()) {
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Friendly diff: surface the first differing line rather than a wall of text.
        if (!expected.equals(actual)) {
            String[] expLines = expected.split("\n", -1);
            String[] actLines = actual.split("\n", -1);
            int upto = Math.min(expLines.length, actLines.length);
            int firstDiff = -1;
            for (int i = 0; i < upto; i++) {
                if (!expLines[i].equals(actLines[i])) {
                    firstDiff = i;
                    break;
                }
            }
            String summary;
            if (firstDiff < 0) {
                summary = "lengths differ: expected " + expLines.length
                    + " lines, actual " + actLines.length + " lines";
            }
            else {
                summary = "first diverging line: " + (firstDiff + 1)
                    + "\n  expected: " + expLines[firstDiff]
                    + "\n  actual:   " + actLines[firstDiff];
            }
            fail("Decompiler output drifted from snapshot.\n" + summary
                + "\nRun `mvn test -Dswift.bitcode.regenerate=true -Dtest=SwiftModuleSnapshotTest` to update.");
        }
    }

    private static String renderFixture() throws IOException {
        try (InputStream in = SwiftModuleSnapshotTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in, "Fixture missing: " + FIXTURE_RESOURCE);
            SwiftModuleInfo info = SwiftModuleReader.open(in).readModuleInfo();
            return SwiftModuleSourcePrinter.render(info);
        }
    }

    /**
     * The regenerate path uses the filesystem (so the on-disk copy that
     * Maven packages into the jar gets updated). Default is the relative
     * path from the module root; callers can override it with the system
     * property {@code swift.bitcode.module} (absolute path) when running
     * from a worktree or out-of-tree IDE.
     */
    private static Path resolveSnapshotPath() {
        String moduleRoot = System.getProperty("swift.bitcode.module");
        if (moduleRoot != null && !moduleRoot.isEmpty()) {
            return Paths.get(moduleRoot, SNAPSHOT_FS_PATH);
        }
        return Paths.get(SNAPSHOT_FS_PATH);
    }
}
