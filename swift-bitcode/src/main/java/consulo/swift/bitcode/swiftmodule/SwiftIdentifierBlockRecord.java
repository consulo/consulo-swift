/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Record IDs inside the Swift {@code IDENTIFIER_DATA_BLOCK}. Transcribed
 * from {@code namespace identifier_block} in
 * {@code lib/Serialization/ModuleFormat.h}.
 */
public final class SwiftIdentifierBlockRecord {
    /** {@code BCBlob} — the concatenated NUL-separated UTF-8 identifier table. */
    public static final int IDENTIFIER_DATA = 1;

    private SwiftIdentifierBlockRecord() {
    }
}
