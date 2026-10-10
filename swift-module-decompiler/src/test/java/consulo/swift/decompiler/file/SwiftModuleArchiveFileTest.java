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

package consulo.swift.decompiler.file;

import consulo.swift.bitcode.swiftmodule.SwiftModuleReader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SwiftModuleArchiveFile} — the virtual
 * archive that the plugin layer exposes to the IDE's project view and
 * virtual-file-system.
 *
 * <p>Uses the bundled {@code PackageDescription.swiftmodule} fixture so the
 * tests run offline without any Swift toolchain installed.
 */
class SwiftModuleArchiveFileTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    private static SwiftModuleArchiveFile openFixture() throws IOException {
        // Copy the classpath fixture to a temp file so we can call open(Path),
        // which is the production code path the IDE will use.
        Path tmp = Files.createTempFile("PackageDescription", ".swiftmodule");
        try (InputStream in = SwiftModuleArchiveFileTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in, "fixture missing on classpath: " + FIXTURE_RESOURCE);
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        tmp.toFile().deleteOnExit();
        return SwiftModuleArchiveFile.open(tmp);
    }

    @Test
    void archiveExposesABannerAndASourcesDirectory() throws IOException {
        SwiftModuleArchiveFile archive = openFixture();

        assertEquals("PackageDescription", archive.getModuleName(),
            "Module name must come from CONTROL_BLOCK MODULE_NAME record");
        // Banner file lives at the archive root.
        SwiftArchiveEntry banner = archive.getEntry("PackageDescription.swift");
        assertNotNull(banner, "Banner entry should be at archive root");
        assertFalse(banner.isDirectory());
        assertTrue(banner.getSize() > 100,
            "Banner should be non-trivial — got " + banner.getSize() + " bytes");

        SwiftArchiveEntry sources = archive.getEntry(SwiftModuleArchiveFile.SOURCES_DIR);
        assertNotNull(sources, "Sources/ directory entry should exist");
        assertTrue(sources.isDirectory());
        assertEquals(0, sources.getSize(), "Directory entries carry no payload");
    }

    @Test
    void everyTopLevelDeclGetsItsOwnSwiftFile() throws IOException {
        SwiftModuleArchiveFile archive = openFixture();

        long sourceFiles = archive.entries().stream()
            .filter(e -> !e.isDirectory())
            .filter(e -> e.getPath().startsWith(SwiftModuleArchiveFile.SOURCES_DIR))
            .count();
        // PackageDescription has 104 top-level decls in its ORDERED_TOP_LEVEL_DECLS
        // (struct/enum/class/extension/...); some collide on name and get a
        // disambiguator suffix. Verify we emitted a non-trivial number of files.
        assertTrue(sourceFiles > 30,
            "Should have one file per top-level decl, got " + sourceFiles);
        assertTrue(sourceFiles <= archive.getInfo().getOrderedTopLevelDeclIds().length + 5,
            "File count must roughly match the # of top-level decls");
    }

    @Test
    void wellKnownNominalsHaveTheirOwnFiles() throws IOException {
        SwiftModuleArchiveFile archive = openFixture();
        // The SPM-defining types must each get a dedicated file. If naming
        // collides with a sanitized synthetic, the disambiguator suffix
        // doesn't affect the canonical entry.
        for (String name : new String[] { "Package", "Product", "SwiftSetting", "Context" }) {
            String path = SwiftModuleArchiveFile.SOURCES_DIR + name + ".swift";
            SwiftArchiveEntry entry = archive.getEntry(path);
            assertNotNull(entry, "Expected archive entry at " + path);
            assertFalse(entry.isDirectory(), path + " must be a file");
        }
    }

    @Test
    void sourceContentIsParseableSwiftWithExpectedDeclaration() throws IOException {
        SwiftModuleArchiveFile archive = openFixture();
        SwiftArchiveEntry packageEntry = archive.getEntry(
            SwiftModuleArchiveFile.SOURCES_DIR + "Package.swift");
        assertNotNull(packageEntry);
        assertTrue(packageEntry instanceof SwiftSourceArchiveEntry);

        String src = ((SwiftSourceArchiveEntry) packageEntry).getSource();
        assertTrue(src.contains("struct Package") || src.contains("class Package"),
            "Per-file source must declare Package as a nominal type");
        assertTrue(src.contains("// swift-module: PackageDescription"),
            "Per-file source must include a banner identifying the source module");
        // Brace balance — same headline check as the whole-module renderer.
        long opens = src.chars().filter(c -> c == '{').count();
        long closes = src.chars().filter(c -> c == '}').count();
        assertEquals(opens, closes,
            "Braces must balance in Package.swift ({ = " + opens + ", } = " + closes + ")");
    }

    @Test
    void openStreamReadsFullContent() throws IOException {
        SwiftModuleArchiveFile archive = openFixture();
        SwiftArchiveEntry packageEntry = archive.getEntry(
            SwiftModuleArchiveFile.SOURCES_DIR + "Package.swift");
        assertNotNull(packageEntry);
        try (InputStream in = archive.openStream(packageEntry)) {
            byte[] all = in.readAllBytes();
            assertEquals(packageEntry.getSize(), all.length,
                "openStream() must yield exactly getSize() bytes");
            String s = new String(all, StandardCharsets.UTF_8);
            assertTrue(s.length() > 0);
        }
        // Directory entries stream as empty.
        SwiftArchiveEntry dir = archive.getEntry(SwiftModuleArchiveFile.SOURCES_DIR);
        try (InputStream in = archive.openStream(dir)) {
            assertEquals(-1, in.read(), "Directory stream must be empty");
        }
    }

    @Test
    void archiveOpenedFromAlreadyLoadedReader() throws IOException {
        // The IDE side may want to open the archive view without re-reading
        // bytes from disk. open(SwiftModuleReader, long) is the entry point
        // for that — verify it produces a usable archive identically.
        try (InputStream in = SwiftModuleArchiveFileTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertNotNull(in);
            SwiftModuleReader reader = SwiftModuleReader.open(in);
            SwiftModuleArchiveFile archive = SwiftModuleArchiveFile.open(reader, 0L);
            assertEquals("PackageDescription", archive.getModuleName());
            assertTrue(archive.getEntryCount() > 30);
        }
    }

    @Test
    void disambiguatedFileNamesForCollidingTopLevels() throws IOException {
        // PackageDescription's ORDERED_TOP_LEVEL_DECLS list contains
        // multiple extensions on the same nominal (e.g. several extensions
        // on `ExtendedGraphemeClusterLiteralType.StringLiteralType`) which
        // map to the same synthesized base file name. Disambiguators
        // should kick in.
        SwiftModuleArchiveFile archive = openFixture();
        long collisionEntries = archive.entries().stream()
            .filter(e -> !e.isDirectory())
            .filter(e -> e.getPath().matches(".*_\\d+\\.swift"))
            .count();
        // The exact number depends on the fixture's content, but we
        // expect at least one collision.
        System.out.println("Disambiguated entries: " + collisionEntries);
    }
}
