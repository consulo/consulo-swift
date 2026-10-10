/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Metadata extracted from the CONTROL_BLOCK of a {@code .swiftmodule}.
 * Populated by walking the bitstream once; fields are {@code null} when
 * the corresponding record was absent or not yet supported.
 */
public final class SwiftModuleInfo {
    private String moduleName;
    private String target;
    private String sdkName;
    private String sdkVersion;
    private String revision;
    private String channel;
    private int formatMajor = -1;
    private int formatMinor = -1;
    private SwiftIdentifierTable identifiers = SwiftIdentifierTable.empty();

    /** Bit offsets into DECLS_AND_TYPES_BLOCK for each decl ID. Index = declID - 1. */
    private long[] declOffsets = EMPTY_LONGS;
    /** Bit offsets into DECLS_AND_TYPES_BLOCK for each type ID. Index = typeID - 1. */
    private long[] typeOffsets = EMPTY_LONGS;
    /** Byte offsets into the identifier blob for each identifier ID. Index = identifierID - 1. */
    private long[] identifierOffsets = EMPTY_LONGS;
    /** Decl IDs of top-level declarations in source order. */
    private long[] orderedTopLevelDeclIds = EMPTY_LONGS;

    /** Decoded records from DECLS_AND_TYPES_BLOCK, indexed by declID-1. */
    private SwiftDeclEntry[] declEntries = EMPTY_DECL_ENTRIES;

    /** Decoded type records from DECLS_AND_TYPES_BLOCK, indexed by typeID-1. */
    private SwiftTypeEntry[] typeEntries = EMPTY_TYPE_ENTRIES;

    private static final long[] EMPTY_LONGS = new long[0];
    private static final SwiftDeclEntry[] EMPTY_DECL_ENTRIES = new SwiftDeclEntry[0];
    private static final SwiftTypeEntry[] EMPTY_TYPE_ENTRIES = new SwiftTypeEntry[0];

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSdkName() {
        return sdkName;
    }

    public void setSdkName(String sdkName) {
        this.sdkName = sdkName;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getFormatMajor() {
        return formatMajor;
    }

    public void setFormatMajor(int formatMajor) {
        this.formatMajor = formatMajor;
    }

    public int getFormatMinor() {
        return formatMinor;
    }

    public void setFormatMinor(int formatMinor) {
        this.formatMinor = formatMinor;
    }

    public SwiftIdentifierTable getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(SwiftIdentifierTable identifiers) {
        this.identifiers = identifiers == null ? SwiftIdentifierTable.empty() : identifiers;
    }

    public long[] getDeclOffsets() {
        return declOffsets;
    }

    public void setDeclOffsets(long[] declOffsets) {
        this.declOffsets = declOffsets == null ? EMPTY_LONGS : declOffsets;
    }

    public long[] getTypeOffsets() {
        return typeOffsets;
    }

    public void setTypeOffsets(long[] typeOffsets) {
        this.typeOffsets = typeOffsets == null ? EMPTY_LONGS : typeOffsets;
    }

    public long[] getIdentifierOffsets() {
        return identifierOffsets;
    }

    public void setIdentifierOffsets(long[] identifierOffsets) {
        this.identifierOffsets = identifierOffsets == null ? EMPTY_LONGS : identifierOffsets;
    }

    public long[] getOrderedTopLevelDeclIds() {
        return orderedTopLevelDeclIds;
    }

    public void setOrderedTopLevelDeclIds(long[] ids) {
        this.orderedTopLevelDeclIds = ids == null ? EMPTY_LONGS : ids;
    }

    /**
     * Resolves a Swift identifier ID (1-based; 0 is the no-id sentinel) to its
     * UTF-8 string via the {@link #getIdentifierOffsets() identifier-offsets}
     * table and {@link #getIdentifiers() identifier blob}.
     */
    public String lookupIdentifier(int id) {
        return identifiers.lookupById(identifierOffsets, id);
    }

    /**
     * Number of special IdentifierID values that Swift reserves for the
     * Builtin module, current module, ObjC-header module, and the special
     * subscript / init / deinit names. Module references in XREF records
     * skip past these — see {@code enum SpecialIdentifierID} in
     * {@code lib/Serialization/ModuleFormat.h}.
     */
    public static final int NUM_SPECIAL_IDS = 6;

    /**
     * Resolves an XREF module ID to its name. XREF records ({@code kind=149})
     * shift the IdentifierID by {@link #NUM_SPECIAL_IDS} so the first six
     * values can encode special modules (Builtin, current, ObjC header).
     * Plain identifier args on regular decls don't use this shift —
     * {@link #lookupIdentifier(int)} is what they want.
     *
     * <p>Returns the module name, or {@code null} when the ID is one of
     * the special-module sentinels (Builtin / current / ObjCHeader) or
     * the offset table doesn't have the entry.
     */
    public String lookupModuleName(int id) {
        if (id < NUM_SPECIAL_IDS) {
            switch (id) {
                case 0: return "Builtin";        // BUILTIN_MODULE_ID
                case 1: return moduleName;       // CURRENT_MODULE_ID
                case 2: return "__ObjC";         // OBJC_HEADER_MODULE_ID
                default: return null;            // SUBSCRIPT/CONSTRUCTOR/DESTRUCTOR — not modules
            }
        }
        // Shift past the special-ID slots, then look up like a regular identifier.
        return lookupIdentifier(id - NUM_SPECIAL_IDS + 1);
    }

    /** Decoded decl records, indexed by declID-1. Slots may be {@code null}. */
    public SwiftDeclEntry[] getDeclEntries() {
        return declEntries;
    }

    public void setDeclEntries(SwiftDeclEntry[] declEntries) {
        this.declEntries = declEntries == null ? EMPTY_DECL_ENTRIES : declEntries;
    }

    /** Decoded type records, indexed by typeID-1. Slots may be {@code null}. */
    public SwiftTypeEntry[] getTypeEntries() {
        return typeEntries;
    }

    public void setTypeEntries(SwiftTypeEntry[] typeEntries) {
        this.typeEntries = typeEntries == null ? EMPTY_TYPE_ENTRIES : typeEntries;
    }

    /**
     * Looks up the {@link SwiftTypeEntry} for the given 1-based type ID, or
     * {@code null} if the ID is out of range or the record wasn't decoded.
     */
    public SwiftTypeEntry lookupType(int id) {
        if (id <= 0 || id > typeEntries.length) {
            return null;
        }
        return typeEntries[id - 1];
    }

    /**
     * Looks up the {@link SwiftDeclEntry} for the given 1-based decl ID, or
     * {@code null} if the ID is out of range or the record wasn't decoded
     * during the walk (e.g. {@code declOffsets[id-1] == 0}).
     */
    public SwiftDeclEntry lookupDecl(int id) {
        if (id <= 0 || id > declEntries.length) {
            return null;
        }
        return declEntries[id - 1];
    }

    /**
     * Convenience: resolve a decl ID directly to its base name, going
     * decl entry → identifier ID → identifier blob. Returns {@code null}
     * when any step is unresolved.
     */
    public String lookupDeclName(int id) {
        SwiftDeclEntry e = lookupDecl(id);
        if (e == null) {
            return null;
        }
        int nameId = e.baseNameIdentifierId();
        if (nameId == SwiftDeclEntry.NO_NAME) {
            return null;
        }
        return lookupIdentifier(nameId);
    }

    @Override
    public String toString() {
        return "SwiftModuleInfo{" +
            "name='" + moduleName + '\'' +
            ", target='" + target + '\'' +
            ", format=" + formatMajor + "." + formatMinor +
            ", sdk='" + sdkName + '\'' +
            ", sdkVersion='" + sdkVersion + '\'' +
            ", revision='" + revision + '\'' +
            ", channel='" + channel + '\'' +
            ", identifierBlobBytes=" + identifiers.blobSize() +
            ", identifierCount=" + identifierOffsets.length +
            ", declCount=" + declOffsets.length +
            ", typeCount=" + typeOffsets.length +
            ", topLevelDeclCount=" + orderedTopLevelDeclIds.length +
            ", resolvedDeclEntries=" + declEntries.length +
            ", resolvedTypeEntries=" + typeEntries.length +
            '}';
    }
}
