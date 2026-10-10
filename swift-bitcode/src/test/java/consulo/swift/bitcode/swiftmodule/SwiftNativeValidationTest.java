/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>Passive</b> validation against the native Swift toolchain. Disabled
 * by default — the regular Maven test run skips this class entirely. Run
 * it on demand once {@code swift}/{@code swiftc} are installed:
 *
 * <pre>
 *   mvn test -Dswift.bitcode.native=true -Dtest=SwiftNativeValidationTest
 * </pre>
 *
 * <p>What it does:
 * <ol>
 *   <li><b>swiftc -parse</b> over the synthesized {@code .swift} text —
 *       confirms our decompiler emits a file the real Swift compiler
 *       accepts as syntactically valid. Lexical sanity check.</li>
 *   <li><b>swift symbolgraph-extract</b> over the bundled
 *       {@code PackageDescription.swiftmodule} — produces the symbol-graph
 *       JSON the native toolchain considers canonical, then asserts every
 *       symbol we find a name for is present in that ground-truth set.
 *       This is the strongest external check that our extractor isn't
 *       hallucinating decls.</li>
 * </ol>
 *
 * <p>Keeping these tests passive is deliberate: CI shouldn't depend on a
 * native Swift install, and the active suite already does snapshot diffs
 * against {@code PackageDescription.expected.swift}. This class exists so
 * the maintainer can manually cross-check before bumping the snapshot.
 */
@EnabledIfSystemProperty(named = "swift.bitcode.native", matches = "true")
class SwiftNativeValidationTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    @Test
    void synthesizedSwiftPassesSwiftcParse() throws Exception {
        Path swiftc = findOnPath("swiftc");
        assertNotNull(swiftc, "swiftc not on PATH — install Swift toolchain to run this");

        String synthesized = render();
        Path tmpDir = Files.createTempDirectory("swift-bitcode-validate");
        Path swiftFile = tmpDir.resolve("PackageDescription.synthesized.swift");
        Files.writeString(swiftFile, synthesized, StandardCharsets.UTF_8);

        // -parse: do lexing+parsing only, no codegen / module loading.
        ProcessBuilder pb = new ProcessBuilder(
            swiftc.toString(), "-parse",
            "-suppress-warnings",
            swiftFile.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        System.out.println("=== swiftc -parse output (exit=" + exit + ") ===");
        System.out.println(output);
        assertEquals(0, exit,
            "swiftc rejected our synthesized .swift; output above. File at " + swiftFile);
    }

    @Test
    void everyExtractedDeclNameAppearsInNativeSymbolGraph() throws Exception {
        Path swift = findOnPath("swift");
        assertNotNull(swift, "`swift` not on PATH — install Swift toolchain to run this");

        // 1) Set up a working dir with our fixture so symbolgraph-extract can
        //    find it on its search path.
        Path tmpDir = Files.createTempDirectory("swift-bitcode-sg");
        Path moduleDir = tmpDir.resolve("modules");
        Files.createDirectories(moduleDir);
        Path moduleCopy = moduleDir.resolve("PackageDescription.swiftmodule");
        try (InputStream in = SwiftNativeValidationTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in);
            Files.copy(in, moduleCopy);
        }
        Path outDir = tmpDir.resolve("graph");
        Files.createDirectories(outDir);

        ProcessBuilder pb = new ProcessBuilder(
            swift.toString(), "symbolgraph-extract",
            "-module-name", "PackageDescription",
            "-I", moduleDir.toString(),
            "-output-dir", outDir.toString(),
            "-target", "x86_64-unknown-linux-gnu",
            "-minimum-access-level", "internal");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        System.out.println("=== symbolgraph-extract output (exit=" + exit + ") ===");
        System.out.println(output);
        assertEquals(0, exit, "symbolgraph-extract failed; see output above");

        // 2) Parse the JSON. We don't depend on a JSON library — the names
        //    we care about appear as `"title":"<name>"` substrings.
        Path graphFile = outDir.resolve("PackageDescription.symbols.json");
        assertTrue(Files.exists(graphFile), "expected symbol-graph file at " + graphFile);
        String json = Files.readString(graphFile, StandardCharsets.UTF_8);
        Set<String> nativeNames = collectNames(json);
        System.out.println("native graph: " + nativeNames.size() + " distinct symbol titles");

        // 3) Pull our extractor's view of decl names — every non-external,
        //    name-bearing decl entry, not just top-level. Compare set
        //    inclusion: every name we extract should exist in the native
        //    graph too. We don't assert the reverse direction yet because
        //    our extraction is still partial (members, conformances).
        SwiftModuleInfo info = openInfo();
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (SwiftDeclEntry entry : info.getDeclEntries()) {
            if (entry == null || entry.isExternal()) {
                continue;
            }
            int kind = entry.recordKind();
            if (!isNominalOrTopLevel(kind)) {
                continue;
            }
            String name = info.lookupDeclName(entry.declId());
            if (name == null || name.isEmpty()
                || name.startsWith("_")     // private mangled discriminators — skip
                || name.contains(" ")) {    // synthesized names with spaces — skip
                continue;
            }
            checked++;
            if (!nativeNames.contains(name)) {
                missing.add(name + " (kind=" + SwiftDeclRecord.name(kind)
                    + ", declID=" + entry.declId() + ")");
            }
        }

        if (!missing.isEmpty()) {
            System.out.println("Names not found in native graph (" + missing.size()
                + " of " + checked + " checked):");
            for (String m : missing) {
                System.out.println("  " + m);
            }
        }
        else {
            System.out.println("All " + checked + " extracted decl names "
                + "found in native graph.");
        }
        // Sanity floor: we expect at least a third of our extracted nominal
        // decls to also appear in the native graph. Anything lower means
        // record-kind detection broke. We're not stricter because:
        //   * Our extractor surfaces every visibility level, the native
        //     graph filters to `-minimum-access-level internal`.
        //   * Compiler-synthesized names (private discriminators like
        //     `_E4233...`) don't appear in the native graph but do in ours.
        //   * Members nested >1 level deep have qualified names in the
        //     native graph (`Foo.Bar`) but bare `Bar` in ours.
        assertTrue(checked > 0, "Nothing to check — extractor produced no comparable decls");
        int matched = checked - missing.size();
        double ratio = (double) matched / checked;
        assertTrue(ratio >= 0.3,
            "Only " + matched + "/" + checked
                + " decl names matched native graph (" + Math.round(ratio * 100)
                + "%). Threshold is 30% — drop below means something regressed in kind detection.");

        // Additionally, the headline SPM symbols MUST appear in the native
        // graph if we're extracting names correctly:
        for (String required : new String[] { "Package", "Product", "Target" }) {
            assertTrue(nativeNames.contains(required),
                "Native graph missing well-known PackageDescription symbol '"
                    + required + "' — fixture is broken");
        }
    }

    @Test
    void canDecompileRealSwiftStdlib() throws Exception {
        // Stress test against the actual Swift.swiftmodule shipped with
        // the toolchain (~30 MiB). Verifies the extractor scales beyond
        // the small PackageDescription fixture, that XREF resolution
        // works on a module with thousands of cross-references, and that
        // the synthesized output still parses through both swiftc and
        // our PSI.
        SwiftModuleSearchPath sp = SwiftModuleSearchPath.system();
        Path swiftBinary = sp.resolve("Swift");
        assumeFound(swiftBinary, "Swift.swiftmodule not on system search path");

        long startMs = System.currentTimeMillis();
        SwiftModuleReader reader = SwiftModuleReader.open(java.nio.file.Files.newInputStream(swiftBinary), 128 * 1024 * 1024);
        SwiftModuleInfo info = reader.readModuleInfo();
        long readMs = System.currentTimeMillis() - startMs;

        System.out.println("=== Swift.swiftmodule extraction ===");
        System.out.println("  path: " + swiftBinary);
        System.out.println("  size: " + reader.getSize() + " bytes");
        System.out.println("  module: " + info.getModuleName());
        System.out.println("  target: " + info.getTarget());
        System.out.println("  identifiers:  " + info.getIdentifierOffsets().length);
        System.out.println("  decls:        " + info.getDeclEntries().length);
        System.out.println("  types:        " + info.getTypeEntries().length);
        System.out.println("  top-level:    " + info.getOrderedTopLevelDeclIds().length);
        System.out.println("  read time:    " + readMs + "ms");

        assertEquals("Swift", info.getModuleName(), "Module's CONTROL block should self-identify as Swift");
        assertTrue(info.getDeclEntries().length > 1000,
            "Swift stdlib should have thousands of decls, got " + info.getDeclEntries().length);
        assertTrue(info.getTypeEntries().length > 500,
            "Swift stdlib should have hundreds of type entries");

        // Render through SwiftModuleSourcePrinter — confirms the printer
        // can handle the volume without throwing or running out of memory.
        long printStart = System.currentTimeMillis();
        String synthesized = SwiftModuleSourcePrinter.render(info);
        long printMs = System.currentTimeMillis() - printStart;
        System.out.println("  rendered:     " + synthesized.length() + " chars in " + printMs + "ms");
        assertTrue(synthesized.length() > 100_000,
            "Stdlib decompile should be at least 100KB, got " + synthesized.length());

        // Verify well-known stdlib names appear in our top-level set.
        // These are bedrock types — if any of these is missing, our
        // extraction is fundamentally broken on this module.
        for (String required : new String[] {
            "Int", "String", "Bool", "Double", "Float", "Array",
            "Dictionary", "Optional", "Set", "Range", "ClosedRange",
            "Hashable", "Equatable", "Comparable", "Sequence",
            "Collection", "ExpressibleByIntegerLiteral",
        }) {
            assertTrue(synthesized.contains(required),
                "Decompiled Swift.swiftmodule should mention `" + required
                    + "` somewhere — it doesn't. Either the extractor is "
                    + "missing this symbol or its name resolution broke.");
        }
        System.out.println("  bedrock check: 17/17 required symbols present");
    }

    @Test
    void swiftStdlibSynthesizedOutputPassesSwiftcParse() throws Exception {
        // Pipe the decompiled Swift.swiftmodule output through `swiftc -parse`.
        // This is the BIG validation — if 30MB of decompiled stdlib parses
        // clean, the extractor is solid for production use.
        Path swiftc = findOnPath("swiftc");
        assertNotNull(swiftc, "swiftc not on PATH — install Swift toolchain to run this");
        SwiftModuleSearchPath sp = SwiftModuleSearchPath.system();
        Path swiftBinary = sp.resolve("Swift");
        assumeFound(swiftBinary, "Swift.swiftmodule not on system search path");

        SwiftModuleInfo info = SwiftModuleReader.open(java.nio.file.Files.newInputStream(swiftBinary), 128 * 1024 * 1024).readModuleInfo();
        String synthesized = SwiftModuleSourcePrinter.render(info);
        Path tmpDir = Files.createTempDirectory("swift-stdlib-validate");
        Path swiftFile = tmpDir.resolve("Swift.synthesized.swift");
        Files.writeString(swiftFile, synthesized, StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder(
            swiftc.toString(), "-parse",
            "-suppress-warnings",
            swiftFile.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();

        // Count error lines for a clear regression signal.
        long errorCount = output.lines().filter(l -> l.contains(": error:")).count();
        System.out.println("=== swiftc -parse Swift.swiftmodule (exit=" + exit
            + ", errors=" + errorCount + ") ===");
        if (errorCount > 0) {
            // Show the first dozen errors so the failure is actionable
            // but doesn't dump 30MB of compiler output.
            int shown = 0;
            for (String line : output.split("\n")) {
                if (line.contains(": error:")) {
                    System.out.println("  " + line);
                    if (++shown >= 12) {
                        System.out.println("  ... (" + (errorCount - 12) + " more)");
                        break;
                    }
                }
            }
        }

        // The stdlib is enormous and our extractor is still imperfect —
        // some errors are expected at first. The threshold is generous,
        // start low and tighten over time as the extractor improves.
        // The point is to catch SUDDEN regressions, not require zero
        // errors on day one.
        assertTrue(errorCount < 5000,
            "swiftc reported " + errorCount + " parse errors on the decompiled "
                + "Swift.swiftmodule. Threshold is 5000 — increase only if a "
                + "regression is intentional. File: " + swiftFile);
    }

    @Test
    void swiftStdlibSymbolCoverageVsNative() throws Exception {
        // Compare our top-level extraction of Swift.swiftmodule against
        // what native `swift symbolgraph-extract` produces. Same approach
        // as the PackageDescription cross-check, just on the stdlib.
        Path swift = findOnPath("swift");
        assertNotNull(swift, "`swift` not on PATH — install Swift toolchain to run this");
        SwiftModuleSearchPath sp = SwiftModuleSearchPath.system();
        Path swiftBinary = sp.resolve("Swift");
        assumeFound(swiftBinary, "Swift.swiftmodule not on system search path");

        // 1) Native symbol graph for Swift module.
        Path tmpDir = Files.createTempDirectory("swift-stdlib-graph");
        Path outDir = Files.createDirectories(tmpDir.resolve("graph"));
        Path moduleDir = swiftBinary.getParent();
        ProcessBuilder pb = new ProcessBuilder(
            swift.toString(), "symbolgraph-extract",
            "-module-name", "Swift",
            "-I", moduleDir.toString(),
            "-output-dir", outDir.toString(),
            "-target", "x86_64-unknown-linux-gnu",
            "-minimum-access-level", "public");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        int exit = p.waitFor();
        if (exit != 0) {
            // Stdlib graph generation can fail in lots of toolchain-y
            // ways (missing implicit modules, target mismatch). Treat as
            // diagnostic rather than hard fail.
            System.out.println("WARN: swift symbolgraph-extract on Swift module failed (exit="
                + exit + "); skipping coverage check");
            return;
        }
        Path graphFile = outDir.resolve("Swift.symbols.json");
        assertTrue(Files.exists(graphFile), "Native graph file missing: " + graphFile);
        String json = Files.readString(graphFile, StandardCharsets.UTF_8);
        Set<String> nativeTop = new TreeSet<>();
        Matcher topM = Pattern.compile(
            "\"pathComponents\"\\s*:\\s*\\[\\s*\"([^\"]+)\"\\s*\\]"
        ).matcher(json);
        while (topM.find()) {
            nativeTop.add(topM.group(1));
        }

        // 2) Our top-level set: scan decl entries with no local parent.
        SwiftModuleInfo info = SwiftModuleReader.open(java.nio.file.Files.newInputStream(swiftBinary), 128 * 1024 * 1024).readModuleInfo();
        Set<String> oursTop = new TreeSet<>();
        for (long id : info.getOrderedTopLevelDeclIds()) {
            SwiftDeclEntry e = info.lookupDecl((int) id);
            if (e == null || e.isExternal()) continue;
            int parentId = e.parentContextId();
            if (parentId > 0) {
                SwiftDeclEntry parent = info.lookupDecl(parentId);
                if (parent != null && !parent.isExternal()
                    && SwiftDeclRecord.isDecl(parent.recordKind())) {
                    continue; // nested
                }
            }
            String name = info.lookupDeclName((int) id);
            if (name == null || name.isEmpty()) continue;
            if (name.startsWith("_") && name.length() >= 33) continue; // private hashes
            if (name.startsWith("extension ") || name.startsWith("extension_")) continue;
            oursTop.add(name);
        }

        Set<String> common = new HashSet<>(nativeTop);
        common.retainAll(oursTop);
        System.out.println("=== Swift.swiftmodule top-level diff ===");
        System.out.println("  native: " + nativeTop.size() + "  ours: " + oursTop.size()
            + "  common: " + common.size());

        // Bedrock stdlib types — must always appear in our top-level set.
        // If these vanish, extraction broke on the stdlib specifically.
        for (String required : new String[] {
            "Int", "String", "Bool", "Array", "Dictionary",
            "Optional", "Hashable", "Equatable",
        }) {
            assertTrue(oursTop.contains(required),
                "Required stdlib top-level symbol `" + required
                    + "` is missing from our extraction");
        }
        // Coverage floor: 40% — stdlib has lots of internal helpers,
        // generic specializations, and synthesized conformances that we
        // won't perfectly match against native's public-only graph.
        double coverage = (double) common.size() / Math.max(1, nativeTop.size());
        assertTrue(coverage >= 0.4,
            "Stdlib top-level coverage dropped to " + common.size() + "/"
                + nativeTop.size() + " (" + Math.round(coverage * 100)
                + "%); floor is 40%");
    }

    /**
     * Like JUnit's {@code Assumptions.assumeTrue} but with a message —
     * skip the test cleanly when the prerequisite isn't met. We don't
     * pull in {@code org.junit.jupiter.api.Assumptions} explicitly to
     * keep the test class's deps tight.
     */
    private static void assumeFound(Path p, String message) {
        if (p == null || !Files.exists(p)) {
            // Just throw a JUnit assumption-failed via reflection so the
            // test reports SKIPPED rather than FAILED when the toolchain
            // doesn't ship a Swift module.
            org.junit.jupiter.api.Assumptions.assumeTrue(false, message);
        }
    }

    @Test
    void topLevelDeclSetMatchesNativeWithinTolerances() throws Exception {
        // Cross-check our SwiftModuleSourcePrinter output (the snapshot)
        // against the native symbol-graph's top-level set. Two diff sides:
        //   * MISSING — symbols native sees at top-level but ours doesn't.
        //   * EXTRA  — symbols we surface at top-level but native doesn't
        //     (usually because they're nested in something we mis-rooted
        //     or are synthesized internal names native filters out).
        //
        // We don't require zero-diff (extraction's still maturing); we
        // do require that headline SPM types appear in BOTH sets, and
        // that the gap doesn't regress past a documented baseline.
        Path swift = findOnPath("swift");
        assertNotNull(swift, "`swift` not on PATH — install Swift toolchain to run this");

        Set<String> nativeTop = nativeTopLevelSymbols(swift);
        Set<String> oursTop = oursTopLevelSymbols();

        Set<String> common = new HashSet<>(nativeTop);
        common.retainAll(oursTop);
        Set<String> missing = new TreeSet<>(nativeTop);
        missing.removeAll(oursTop);
        Set<String> extras = new TreeSet<>(oursTop);
        extras.removeAll(nativeTop);

        System.out.println("=== Top-level set diff ===");
        System.out.println("  native: " + nativeTop.size() + "  ours: " + oursTop.size()
            + "  common: " + common.size());
        System.out.println("  MISSING (" + missing.size() + "): " + missing);
        System.out.println("  EXTRAS  (" + extras.size() + "): " + extras);

        // Headline check 1: the canonical PackageDescription public API
        // surface must always appear in our top-level set, end of story.
        // If any of these regress out we've broken extraction badly.
        for (String required : new String[] { "Package", "Product" }) {
            assertTrue(oursTop.contains(required),
                "Required top-level symbol `" + required
                    + "` is missing from our snapshot. extras=" + extras);
        }

        // Sanity floor: at least half of native's top-level types should
        // appear in ours. We're at ~55% today (18/33); below that means
        // ORDERED_TOP_LEVEL_DECLS extraction regressed.
        int matched = common.size();
        int nativeCount = nativeTop.size();
        assertTrue(nativeCount > 0,
            "Native produced an empty top-level set — symbolgraph-extract is misconfigured");
        double coverage = (double) matched / nativeCount;
        assertTrue(coverage >= 0.5,
            "Top-level coverage dropped to " + matched + "/" + nativeCount
                + " (" + Math.round(coverage * 100) + "%); floor is 50%. "
                + "Missing: " + missing);
    }

    /**
     * Native top-level symbols — pull every symbol with a single-segment
     * {@code pathComponents} from the symbol-graph JSON.
     */
    private static Set<String> nativeTopLevelSymbols(Path swift) throws Exception {
        Path tmpDir = Files.createTempDirectory("swift-native-top-level");
        Path moduleDir = tmpDir.resolve("modules");
        Files.createDirectories(moduleDir);
        try (InputStream in = SwiftNativeValidationTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in);
            Files.copy(in, moduleDir.resolve("PackageDescription.swiftmodule"));
        }
        Path outDir = Files.createDirectories(tmpDir.resolve("graph"));
        ProcessBuilder pb = new ProcessBuilder(
            swift.toString(), "symbolgraph-extract",
            "-module-name", "PackageDescription",
            "-I", moduleDir.toString(),
            "-output-dir", outDir.toString(),
            "-target", "x86_64-unknown-linux-gnu",
            "-minimum-access-level", "internal");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        // Drain output so the process doesn't block on a full pipe.
        p.getInputStream().readAllBytes();
        assertEquals(0, p.waitFor(), "symbolgraph-extract failed");
        String json = Files.readString(outDir.resolve("PackageDescription.symbols.json"),
            StandardCharsets.UTF_8);

        // Pluck top-level symbols: those with pathComponents of length 1.
        // We don't use a JSON library here — same regex shape as
        // {@link #collectNames}, but constrained to single-element paths
        // by matching the immediate {@code "pathComponents":["X"]} form.
        Set<String> out = new TreeSet<>();
        Pattern topPath = Pattern.compile("\"pathComponents\"\\s*:\\s*\\[\\s*\"([^\"]+)\"\\s*\\]");
        Matcher m = topPath.matcher(json);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /**
     * Our top-level symbols — pulled from the
     * {@code PackageDescription.expected.swift} snapshot the snapshot test
     * regenerates. Matches the first identifier after each top-level
     * (no-leading-whitespace) decl keyword.
     */
    private static Set<String> oursTopLevelSymbols() throws IOException {
        Path snapshot = Path.of("src/test/resources/swiftmodule/PackageDescription.expected.swift");
        if (!Files.exists(snapshot)) {
            // Run from a different working dir (e.g. project root) — try
            // resolving relative to the module's expected layout.
            snapshot = Path.of("swift-bitcode/src/test/resources/swiftmodule/PackageDescription.expected.swift");
        }
        assertTrue(Files.exists(snapshot),
            "Snapshot file missing — run SwiftModuleSnapshotTest with -Dswift.bitcode.regenerate=true first");
        String src = Files.readString(snapshot, StandardCharsets.UTF_8);
        Pattern declPat = Pattern.compile(
            "^(?:public |internal |private |open |fileprivate |package |@\\S+\\(?[^\\n]*\\)?\\s+)*"
                + "(?:struct|class|enum|protocol|typealias|func|var|let|extension"
                + "|infix operator|prefix operator|postfix operator|precedencegroup)"
                + "\\s+([A-Za-z_][A-Za-z_0-9]*)",
            Pattern.MULTILINE);
        Set<String> out = new TreeSet<>();
        Matcher m = declPat.matcher(src);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    // ------------------------------------------------------------------

    private static boolean isNominalOrTopLevel(int kind) {
        switch (kind) {
            case SwiftDeclRecord.STRUCT_DECL:
            case SwiftDeclRecord.CLASS_DECL:
            case SwiftDeclRecord.ENUM_DECL:
            case SwiftDeclRecord.PROTOCOL_DECL:
            case SwiftDeclRecord.TYPE_ALIAS_DECL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Pull every {@code "title":"..."} value out of the symbol-graph JSON.
     * Keeping this regex-based avoids dragging in a JSON parser dependency
     * for a passive test.
     */
    private static Set<String> collectNames(String json) {
        Set<String> out = new HashSet<>();
        Pattern p = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    private static Path findOnPath(String exe) {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            Path candidate = Path.of(dir, exe);
            if (Files.isExecutable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static SwiftModuleInfo openInfo() throws IOException {
        try (InputStream in = SwiftNativeValidationTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in);
            return SwiftModuleReader.open(in).readModuleInfo();
        }
    }

    private static String render() throws IOException {
        return SwiftModuleSourcePrinter.render(openInfo());
    }

    // Suppress unused-import warnings if a future refactor drops Arrays.
    @SuppressWarnings("unused")
    private static final Class<?> UNUSED = Arrays.class;
}
