/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Record IDs that may appear inside the Swift {@code CONTROL_BLOCK}.
 * Transcribed from {@code namespace control_block} in
 * {@code lib/Serialization/ModuleFormat.h} of the apple/swift compiler.
 */
public final class SwiftControlRecord {
    /** {@code BCBlob} — module name as a UTF-8 string. */
    public static final int METADATA              = 1;
    /** {@code BCBlob} — module name. */
    public static final int MODULE_NAME           = 2;
    /** {@code BCBlob} — target triple. */
    public static final int TARGET                = 3;
    /** {@code BCBlob} — SDK name. */
    public static final int SDK_NAME              = 4;
    /** {@code BCBlob} — Swift compiler revision. */
    public static final int REVISION              = 5;
    /** {@code BCBlob} — allowable client name. */
    public static final int ALLOWABLE_CLIENT_NAME = 6;
    /** {@code BCBlob} — channel (e.g. release / development). */
    public static final int CHANNEL               = 7;
    /** {@code BCBlob} — SDK version. */
    public static final int SDK_VERSION           = 8;

    private SwiftControlRecord() {
    }
}
