/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the swift-bitcode module root for full MIT terms.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;
import uk.ac.man.cs.llvm.bc.blocks.Block;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the forward-only, low-memory streaming scanner against both
 * synthetic bitstreams (assert linear behavior) and the real
 * PackageDescription.swiftmodule fixture (assert correctness matches the
 * full byte[] reader).
 */
class StreamingSwiftModuleScannerTest {

    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    private static InputStream openFixture() {
        InputStream in = StreamingSwiftModuleScannerTest.class.getResourceAsStream(FIXTURE_RESOURCE);
        if (in == null) {
            throw new AssertionError("Test fixture not on classpath: " + FIXTURE_RESOURCE);
        }
        return in;
    }

    @Test
    void quickInfoExtractsModuleNameFromPackageDescription() throws IOException {
        try (InputStream in = openFixture()) {
            SwiftModuleInfo info = SwiftModuleReader.quickInfo(in);
            assertEquals("PackageDescription", info.getModuleName());
            assertNotNull(info.getTarget());
            assertTrue(info.getFormatMinor() > 0);
            // Identifier / decl tables are NOT populated by quickInfo —
            // those require the full reader.
            assertEquals(0, info.getIdentifiers().blobSize());
            assertEquals(0, info.getDeclOffsets().length);
        }
    }

    @Test
    void quickInfoAgreesWithFullReaderOnIdentityFields() throws IOException {
        SwiftModuleInfo full;
        SwiftModuleInfo quick;
        try (InputStream in = openFixture()) {
            full = SwiftModuleReader.open(in).readModuleInfo();
        }
        try (InputStream in = openFixture()) {
            quick = SwiftModuleReader.quickInfo(in);
        }
        assertEquals(full.getModuleName(), quick.getModuleName());
        assertEquals(full.getTarget(),     quick.getTarget());
        assertEquals(full.getFormatMajor(), quick.getFormatMajor());
        assertEquals(full.getFormatMinor(), quick.getFormatMinor());
    }

    @Test
    void quickInfoConsumesFarFewerBytesThanFullSize() throws IOException {
        // Wrap the fixture stream in a byte-counting filter so we can
        // measure how much was actually read. The fixture is ~495 KB —
        // quickInfo should bail out after the CONTROL block, which is
        // measured in hundreds of bytes.
        long fullSize;
        try (InputStream in = openFixture()) {
            fullSize = in.readAllBytes().length;
        }

        ByteCountingStream counter;
        try (InputStream raw = openFixture()) {
            counter = new ByteCountingStream(raw);
            SwiftModuleInfo info = SwiftModuleReader.quickInfo(counter);
            assertEquals("PackageDescription", info.getModuleName());
        }

        System.out.println("quickInfo read " + counter.bytesRead
            + " of " + fullSize + " bytes ("
            + (100.0 * counter.bytesRead / fullSize) + "% of file)");

        // Sanity: we should be well under a few percent of the full file.
        assertTrue(counter.bytesRead < fullSize / 10,
            "quickInfo should read <10% of the file (read " + counter.bytesRead
                + " of " + fullSize + ")");
    }

    @Test
    void quickInfoRejectsNonSwiftSignature() {
        assertThrows(IOException.class, () ->
            SwiftModuleReader.quickInfo(new ByteArrayInputStream(new byte[]{ 1, 2, 3, 4 })));
    }

    @Test
    void quickInfoOnSyntheticControlBlock() throws IOException {
        // Build a minimal valid .swiftmodule: signature + MODULE { CONTROL { ... } }
        // CONTROL has METADATA (UNABBREV with major/minor versions) and a
        // MODULE_NAME blob (abbreviated). Nothing else.
        BitstreamWriter w = new BitstreamWriter()
            .writeSwiftModuleSignature()
            .enterSubblock(Block.MODULE.id(), 3)
                .enterSubblock(Block.CONTROL.id(), 5);
        int nameAbbrev = w.defineBlobAbbrev(SwiftControlRecord.MODULE_NAME);
        int targetAbbrev = w.defineBlobAbbrev(SwiftControlRecord.TARGET);
        // METADATA: major=0, minor=1234, plus 5 zero-padding fields
        w.unabbrevRecord(SwiftControlRecord.METADATA, 0L, 1234L, 0L, 0L, 0L, 0L, 0L)
         .writeBlobAbbrevRecord(nameAbbrev, "Synthetic".getBytes(StandardCharsets.UTF_8))
         .writeBlobAbbrevRecord(targetAbbrev, "x86_64-apple-macos".getBytes(StandardCharsets.UTF_8))
                .endBlock()
            .endBlock();

        SwiftModuleInfo info = SwiftModuleReader.quickInfo(new ByteArrayInputStream(w.toByteArray()));
        assertEquals("Synthetic", info.getModuleName());
        assertEquals("x86_64-apple-macos", info.getTarget());
        assertEquals(0, info.getFormatMajor());
        assertEquals(1234, info.getFormatMinor());
    }

    /** Counts every byte pulled from an underlying stream. */
    private static final class ByteCountingStream extends InputStream {
        private final InputStream delegate;
        int bytesRead;

        ByteCountingStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) bytesRead++;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            if (n > 0) bytesRead += n;
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = delegate.skip(n);
            if (skipped > 0) bytesRead += skipped;
            return skipped;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
