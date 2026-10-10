/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SwiftModuleReader} against the stock
 * {@code PackageDescription.swiftmodule} shipped with the Swift Package
 * Manager. The fixture is bundled on the classpath so the test works
 * regardless of the working directory it is launched from.
 */
class SwiftModuleReaderTest {

    /** Classpath location of the bundled .swiftmodule fixture. */
    private static final String FIXTURE_RESOURCE =
        "/swiftmodule/PackageDescription.swiftmodule";

    /**
     * Opens the fixture as a classpath stream. The stream is owned by the
     * caller and must be closed (use try-with-resources).
     */
    private static InputStream openFixture() {
        InputStream in = SwiftModuleReaderTest.class.getResourceAsStream(FIXTURE_RESOURCE);
        if (in == null) {
            throw new AssertionError("Test fixture not on classpath: " + FIXTURE_RESOURCE);
        }
        return in;
    }

    private static SwiftModuleReader openReader() throws IOException {
        try (InputStream in = openFixture()) {
            return SwiftModuleReader.open(in);
        }
    }

    @Test
    void signatureBytesMatchExpectedConstants() {
        // Sanity-check the constants themselves (UTF-8 of '✨' + tag).
        byte[] valid = {
            SwiftModuleSignature.SPARKLE_BYTE_0,
            SwiftModuleSignature.SPARKLE_BYTE_1,
            SwiftModuleSignature.SPARKLE_BYTE_2,
            SwiftModuleSignature.TAG_SWIFTMODULE,
        };
        assertTrue(SwiftModuleSignature.isSwiftModule(valid));

        byte[] tooShort = { SwiftModuleSignature.SPARKLE_BYTE_0 };
        assertFalse(SwiftModuleSignature.isSwiftModule(tooShort));

        byte[] wrongTag = {
            SwiftModuleSignature.SPARKLE_BYTE_0,
            SwiftModuleSignature.SPARKLE_BYTE_1,
            SwiftModuleSignature.SPARKLE_BYTE_2,
            SwiftModuleSignature.TAG_SWIFTDOC,
        };
        assertFalse(SwiftModuleSignature.isSwiftModule(wrongTag));
        assertTrue(SwiftModuleSignature.matches(wrongTag, SwiftModuleSignature.TAG_SWIFTDOC));
    }

    @Test
    void opensRealPackageDescriptionSwiftModule() throws IOException {
        SwiftModuleReader reader = openReader();

        assertTrue(reader.getSize() > 1000,
            "PackageDescription.swiftmodule is non-trivial");
        assertTrue(SwiftModuleSignature.isSwiftModule(reader.getBytes()),
            "open() should only succeed on real .swiftmodule files");
    }

    @Test
    void extractsModuleInfoFromPackageDescription() throws IOException {
        SwiftModuleReader reader = openReader();
        SwiftModuleInfo info = reader.readModuleInfo();

        System.out.println(info);
        assertEquals("PackageDescription", info.getModuleName(),
            "MODULE_NAME record should yield the module's name");
        assertNotNull(info.getTarget(), "TARGET record should be present");
        assertTrue(info.getFormatMajor() >= 0,
            "METADATA record should populate the format major version");
        assertTrue(info.getFormatMinor() > 0,
            "METADATA record should populate the format minor version");
    }

    @Test
    void extractsIdentifierTableFromPackageDescription() throws IOException {
        SwiftModuleReader reader = openReader();
        SwiftModuleInfo info = reader.readModuleInfo();

        SwiftIdentifierTable ids = info.getIdentifiers();
        assertTrue(ids.blobSize() > 0,
            "IDENTIFIER_DATA_BLOCK should yield a non-empty string-table blob");

        List<String> names = ids.allIdentifiers();
        assertFalse(names.isEmpty(), "Identifier table should contain at least one name");
        assertTrue(names.contains("Package"),
            "PackageDescription should advertise the `Package` identifier");
        assertTrue(names.contains("Product"),
            "PackageDescription should advertise the `Product` identifier");
        assertTrue(names.contains("Target"),
            "PackageDescription should advertise the `Target` identifier");

        System.out.println("PackageDescription identifier-table: "
            + names.size() + " strings (" + ids.blobSize() + " bytes)");
        System.out.println("  first 20: " + sampleHead(names));
    }

    @Test
    void extractsIndexTablesFromPackageDescription() throws IOException {
        SwiftModuleReader reader = openReader();
        SwiftModuleInfo info = reader.readModuleInfo();

        assertTrue(info.getDeclOffsets().length > 0,
            "DECL_OFFSETS should be non-empty for a real module");
        assertTrue(info.getTypeOffsets().length > 0,
            "TYPE_OFFSETS should be non-empty");
        assertTrue(info.getIdentifierOffsets().length > 0,
            "IDENTIFIER_OFFSETS should be non-empty");
        assertTrue(info.getOrderedTopLevelDeclIds().length > 0,
            "ORDERED_TOP_LEVEL_DECLS should be non-empty for PackageDescription");

        // Identifier-ID -> string roundtrip via the offsets table.
        int resolved = 0;
        for (int id = 1; id <= Math.min(50, info.getIdentifierOffsets().length); id++) {
            String s = info.lookupIdentifier(id);
            if (s != null && !s.isEmpty()) {
                resolved++;
            }
        }
        assertTrue(resolved > 0,
            "Identifier-ID -> string resolution should yield non-empty strings");

        System.out.println("PackageDescription index summary:");
        System.out.println("  decl offsets:       " + info.getDeclOffsets().length);
        System.out.println("  type offsets:       " + info.getTypeOffsets().length);
        System.out.println("  identifier offsets: " + info.getIdentifierOffsets().length);
        System.out.println("  top-level decls:    " + info.getOrderedTopLevelDeclIds().length);
    }

    @Test
    void rejectsBytesWithoutSwiftSignature() {
        byte[] bogus = { 1, 2, 3, 4, 5, 6, 7, 8 };
        IOException ex = assertThrows(IOException.class,
            () -> SwiftModuleReader.open(new ByteArrayInputStream(bogus)));
        assertTrue(ex.getMessage().contains("bad signature"), ex.getMessage());
    }

    @Test
    void signatureCheckHappensBeforeBodyIsRead() {
        // A bogus stream that's "huge". The reader must reject after exactly
        // 4 bytes — proof we don't load the body for non-Swift content.
        ByteCountingStream counter = new ByteCountingStream(new byte[]{
            0x42, 0x43, (byte) 0xC0, (byte) 0xDE,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
        });
        assertThrows(IOException.class, () -> SwiftModuleReader.open(counter));
        assertEquals(4, counter.bytesRead,
            "Reader should consume exactly the signature before rejecting");
    }

    @Test
    void rejectsStreamThatExceedsMaxBytes() {
        // Valid signature, then 16 body bytes. Cap at 10 → must throw before
        // reading everything in.
        byte[] payload = new byte[20];
        payload[0] = SwiftModuleSignature.SPARKLE_BYTE_0;
        payload[1] = SwiftModuleSignature.SPARKLE_BYTE_1;
        payload[2] = SwiftModuleSignature.SPARKLE_BYTE_2;
        payload[3] = SwiftModuleSignature.TAG_SWIFTMODULE;

        IOException ex = assertThrows(IOException.class,
            () -> SwiftModuleReader.open(new ByteArrayInputStream(payload), 10));
        assertTrue(ex.getMessage().contains("exceeds maxBytes"), ex.getMessage());
    }

    @Test
    void acceptsStreamExactlyAtMaxBytes() throws IOException {
        // 4 signature bytes + 6 body bytes = 10 total. Cap at 10 → must succeed.
        byte[] payload = new byte[10];
        payload[0] = SwiftModuleSignature.SPARKLE_BYTE_0;
        payload[1] = SwiftModuleSignature.SPARKLE_BYTE_1;
        payload[2] = SwiftModuleSignature.SPARKLE_BYTE_2;
        payload[3] = SwiftModuleSignature.TAG_SWIFTMODULE;
        SwiftModuleReader r = SwiftModuleReader.open(new ByteArrayInputStream(payload), 10);
        assertEquals(10, r.getSize());
    }

    @Test
    void rejectsMaxBytesBelowSignatureLength() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[100]);
        assertThrows(IllegalArgumentException.class,
            () -> SwiftModuleReader.open(in, 2));
    }

    @Test
    void packageDescriptionFitsUnderDefaultLimit() throws IOException {
        try (InputStream in = openFixture()) {
            SwiftModuleReader r = SwiftModuleReader.open(in);
            assertTrue(r.getSize() < SwiftModuleReader.DEFAULT_MAX_BYTES);
        }
    }

    /** Wraps a byte source and counts every byte actually pulled. */
    private static final class ByteCountingStream extends InputStream {
        private final ByteArrayInputStream delegate;
        int bytesRead;

        ByteCountingStream(byte[] bytes) {
            this.delegate = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() {
            int b = delegate.read();
            if (b != -1) bytesRead++;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            int n = delegate.read(b, off, len);
            if (n > 0) bytesRead += n;
            return n;
        }
    }

    private static String sampleHead(List<String> xs) {
        return xs.subList(0, Math.min(xs.size(), 20)).toString();
    }
}
