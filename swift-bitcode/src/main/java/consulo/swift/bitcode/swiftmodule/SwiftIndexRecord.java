/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Record IDs that may appear inside the Swift {@code INDEX_BLOCK}.
 * Transcribed from the {@code RecordKind} enum in
 * {@code namespace index_block} in
 * {@code lib/Serialization/ModuleFormat.h}.
 * <p>
 * Records of category <em>Offsets</em> ({@link #TYPE_OFFSETS},
 * {@link #DECL_OFFSETS}, etc.) use {@code OffsetsLayout}:
 * {@code recordID + BCArray<BCFixed<31> bit-offset>}.
 * <p>
 * Records of category <em>OrderedDecls</em> ({@link #ORDERED_TOP_LEVEL_DECLS},
 * {@link #LOCAL_TYPE_DECLS}, etc.) use {@code OrderedDeclsLayout}:
 * {@code recordID + BCArray<BCFixed<31> declID>}.
 * <p>
 * Records of category <em>DeclList</em> ({@link #TOP_LEVEL_DECLS},
 * {@link #OPERATORS}, etc.) use {@code DeclListLayout}:
 * {@code recordID + BCVBR<16> tableOffset + BCBlob} (serialized on-disk hash table).
 */
public final class SwiftIndexRecord {
    public static final int TYPE_OFFSETS                       = 1;
    public static final int DECL_OFFSETS                       = 2;
    public static final int IDENTIFIER_OFFSETS                 = 3;
    public static final int TOP_LEVEL_DECLS                    = 4;
    public static final int OPERATORS                          = 5;
    public static final int EXTENSIONS                         = 6;
    public static final int CLASS_MEMBERS_FOR_DYNAMIC_LOOKUP   = 7;
    public static final int OPERATOR_METHODS                   = 8;
    public static final int OBJC_METHODS                       = 9;
    public static final int DERIVATIVE_FUNCTION_CONFIGURATIONS = 10;
    public static final int ENTRY_POINT                        = 11;
    public static final int LOCAL_DECL_CONTEXT_OFFSETS         = 12;
    public static final int LOCAL_TYPE_DECLS                   = 13;
    public static final int OPAQUE_RETURN_TYPE_DECLS           = 14;
    public static final int GENERIC_SIGNATURE_OFFSETS          = 15;
    public static final int GENERIC_ENVIRONMENT_OFFSETS        = 16;
    public static final int PROTOCOL_CONFORMANCE_OFFSETS       = 17;
    public static final int ABSTRACT_CONFORMANCE_OFFSETS       = 18;
    public static final int PACK_CONFORMANCE_OFFSETS           = 19;
    public static final int SIL_LAYOUT_OFFSETS                 = 20;
    public static final int PRECEDENCE_GROUPS                  = 21;
    public static final int NESTED_TYPE_DECLS                  = 22;
    public static final int DECL_MEMBER_NAMES                  = 23;
    public static final int DECL_FINGERPRINTS                  = 24;
    public static final int ORDERED_TOP_LEVEL_DECLS            = 25;
    public static final int SUBSTITUTION_MAP_OFFSETS           = 26;
    public static final int CLANG_TYPE_OFFSETS                 = 27;
    public static final int EXPORTED_PRESPECIALIZATION_DECLS   = 28;

    private SwiftIndexRecord() {
    }
}
