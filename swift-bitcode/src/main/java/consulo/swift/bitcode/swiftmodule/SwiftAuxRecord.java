/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Auxiliary record captured next to a {@link SwiftDeclEntry} or
 * {@link SwiftTypeEntry}. Holds the literal record kind + raw operand
 * array, with no decoding applied — callers interpret it via the
 * {@code lib/Serialization/ModuleFormat.h} layout for the matching
 * record code.
 *
 * <p>Common producers:
 * <ul>
 *   <li>{@link SwiftDeclRecord#MEMBERS} (148) — array of member decl IDs;
 *       attached to nominal types and extensions.</li>
 *   <li>{@link SwiftDeclRecord#PARAMETERLIST} (100) — array of param decl
 *       IDs; attached to functions, constructors, subscripts.</li>
 *   <li>{@link SwiftDeclRecord#GENERIC_PARAM_LIST} (120) — array of
 *       generic-type-param decl IDs.</li>
 *   <li>{@link SwiftDeclRecord#INHERITED_PROTOCOLS} (162) — array of
 *       protocol decl IDs inherited by a {@code PROTOCOL_DECL}.</li>
 * </ul>
 */
public final class SwiftAuxRecord {

    private final int kind;
    private final long[] args;

    public SwiftAuxRecord(int kind, long[] args) {
        this.kind = kind;
        this.args = args == null ? new long[0] : args;
    }

    public int kind() {
        return kind;
    }

    public long[] args() {
        return args;
    }

    @Override
    public String toString() {
        return "SwiftAuxRecord{kind=" + kind + ", args=" + args.length + '}';
    }
}
