/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

/**
 * Record IDs that may appear inside the Swift {@code DECLS_AND_TYPES_BLOCK}.
 * Transcribed from {@code lib/Serialization/DeclTypeRecordNodes.def} in the
 * upstream Swift project.
 * <p>
 * Numbering rules from the {@code .def} file:
 * <ul>
 *   <li>Types are numbered starting at <b>1</b> with sequential
 *       {@code TYPE(...)} entries plus {@code TRAILING_INFO} interlopers.</li>
 *   <li>Decls start at <b>50</b> ({@code FIRST_DECL(TYPE_ALIAS, 50)}).</li>
 *   <li>Patterns start at <b>80</b> ({@code FIRST_PATTERN(PAREN, 80)}).</li>
 *   <li>Other records use explicit {@code OTHER(...)} values starting at 100.</li>
 * </ul>
 * <p>
 * Only the kinds we actively decode are exposed here; the rest live in the
 * upstream {@code .def} reference. New kinds get added on demand so the
 * surface stays small.
 */
public final class SwiftDeclRecord {

    // --- Types (1..) ----------------------------------------------------
    public static final int BUILTIN_ALIAS_TYPE          = 1;
    public static final int GENERIC_TYPE_PARAM_TYPE     = 2;
    public static final int DEPENDENT_MEMBER_TYPE       = 3;
    public static final int NOMINAL_TYPE                = 4;
    public static final int TUPLE_TYPE                  = 5;
    public static final int TUPLE_TYPE_ELT              = 6;
    public static final int FUNCTION_TYPE               = 7;
    public static final int FUNCTION_PARAM              = 8;
    public static final int METATYPE_TYPE               = 9;
    public static final int PRIMARY_ARCHETYPE_TYPE      = 10;
    public static final int EXISTENTIAL_ARCHETYPE_TYPE  = 11;
    public static final int OPAQUE_ARCHETYPE_TYPE       = 12;
    public static final int PACK_ARCHETYPE_TYPE         = 13;
    public static final int ELEMENT_ARCHETYPE_TYPE      = 14;
    public static final int PROTOCOL_COMPOSITION_TYPE   = 15;
    public static final int PARAMETERIZED_PROTOCOL_TYPE = 16;
    public static final int EXISTENTIAL_TYPE            = 17;
    public static final int BOUND_GENERIC_TYPE          = 18;
    public static final int GENERIC_FUNCTION_TYPE       = 19;
    public static final int ARRAY_SLICE_TYPE            = 20;
    public static final int INLINE_ARRAY_TYPE           = 21;
    public static final int DICTIONARY_TYPE             = 22;
    public static final int REFERENCE_STORAGE_TYPE      = 23;
    public static final int UNBOUND_GENERIC_TYPE        = 24;
    public static final int OPTIONAL_TYPE               = 25;
    public static final int VARIADIC_SEQUENCE_TYPE      = 26;
    public static final int SIL_FUNCTION_TYPE           = 27;
    public static final int DYNAMIC_SELF_TYPE           = 28;
    public static final int EXISTENTIAL_METATYPE_TYPE   = 29;
    public static final int SIL_BLOCK_STORAGE_TYPE      = 30;
    public static final int SIL_MOVE_ONLY_TYPE          = 31;
    public static final int SIL_BOX_TYPE                = 32;
    public static final int NAME_ALIAS_TYPE             = 33;
    public static final int PACK_EXPANSION_TYPE         = 34;
    public static final int PACK_ELEMENT_TYPE           = 35;
    public static final int PACK_TYPE                   = 36;
    public static final int SIL_PACK_TYPE               = 37;
    public static final int ERROR_TYPE                  = 38;
    public static final int INTEGER_TYPE                = 39;
    public static final int BUILTIN_FIXED_ARRAY_TYPE    = 40;
    public static final int BUILTIN_BORROW_TYPE         = 41;
    public static final int HIDDEN_TYPE                 = 42;

    // --- Decls (50..) ---------------------------------------------------
    public static final int TYPE_ALIAS_DECL          = 50;
    public static final int GENERIC_TYPE_PARAM_DECL  = 51;
    public static final int ASSOCIATED_TYPE_DECL     = 52;
    public static final int STRUCT_DECL              = 53;
    public static final int CONSTRUCTOR_DECL         = 54;
    public static final int VAR_DECL                 = 55;
    public static final int PARAM_DECL               = 56;
    public static final int FUNC_DECL                = 57;
    public static final int OPAQUE_TYPE_DECL         = 58;
    public static final int PATTERN_BINDING_DECL     = 59;
    public static final int PROTOCOL_DECL            = 60;
    public static final int DEFAULT_WITNESS_TABLE    = 61;   // trailing info
    public static final int ASSOCIATED_TYPE          = 62;   // trailing info
    public static final int PRIMARY_ASSOCIATED_TYPE  = 63;   // trailing info
    public static final int PREFIX_OPERATOR_DECL     = 64;
    public static final int POSTFIX_OPERATOR_DECL    = 65;
    public static final int INFIX_OPERATOR_DECL      = 66;
    public static final int CLASS_DECL               = 67;
    public static final int ENUM_DECL                = 68;
    public static final int ENUM_ELEMENT_DECL        = 69;
    public static final int SUBSCRIPT_DECL           = 70;
    public static final int EXTENSION_DECL           = 71;
    public static final int DESTRUCTOR_DECL          = 72;
    public static final int PRECEDENCE_GROUP_DECL    = 73;
    public static final int ACCESSOR_DECL            = 74;
    public static final int MACRO_DECL               = 75;

    // --- Patterns (80..) ------------------------------------------------
    public static final int PAREN_PATTERN     = 80;
    public static final int TUPLE_PATTERN     = 81;
    public static final int TUPLE_PATTERN_ELT = 82;
    public static final int NAMED_PATTERN     = 83;
    public static final int ANY_PATTERN       = 84;
    public static final int TYPED_PATTERN     = 85;
    public static final int VAR_PATTERN       = 86;

    // --- Others ---------------------------------------------------------
    public static final int PARAMETERLIST                            = 100;
    public static final int FOREIGN_ERROR_CONVENTION                 = 101;
    public static final int FOREIGN_ASYNC_CONVENTION                 = 102;
    public static final int XREF_TYPE_PATH_PIECE                     = 103;
    public static final int XREF_VALUE_PATH_PIECE                    = 104;
    public static final int XREF_EXTENSION_PATH_PIECE                = 105;
    public static final int XREF_OPERATOR_OR_ACCESSOR_PATH_PIECE     = 106;
    public static final int XREF_GENERIC_PARAM_PATH_PIECE            = 107;
    public static final int XREF_INITIALIZER_PATH_PIECE              = 108;
    public static final int ABSTRACT_CLOSURE_EXPR_CONTEXT            = 109;
    public static final int PATTERN_BINDING_INITIALIZER_CONTEXT      = 110;
    public static final int DEFAULT_ARGUMENT_INITIALIZER_CONTEXT     = 111;
    public static final int TOP_LEVEL_CODE_DECL_CONTEXT              = 112;
    public static final int GENERIC_PARAM_LIST                       = 120;
    public static final int GENERIC_SIGNATURE                        = 121;
    public static final int REQUIREMENT_SIGNATURE                    = 122;
    public static final int GENERIC_ENVIRONMENT                      = 123;
    public static final int BUILTIN_PROTOCOL_CONFORMANCE             = 124;
    public static final int SIL_GENERIC_SIGNATURE                    = 125;
    public static final int SUBSTITUTION_MAP                         = 126;
    public static final int LOCAL_DISCRIMINATOR                      = 127;
    public static final int PRIVATE_DISCRIMINATOR                    = 128;
    public static final int FILENAME_FOR_PRIVATE                     = 129;
    public static final int DESERIALIZATION_SAFETY                   = 130;
    public static final int EXPANDED_MACRO_DEFINITION                = 131;
    public static final int EXPANDED_MACRO_REPLACEMENTS              = 132;
    public static final int ABSTRACT_CONFORMANCE                     = 140;
    public static final int NORMAL_PROTOCOL_CONFORMANCE              = 141;
    public static final int SPECIALIZED_PROTOCOL_CONFORMANCE         = 142;
    public static final int INHERITED_PROTOCOL_CONFORMANCE           = 143;
    public static final int PACK_CONFORMANCE                         = 144;
    public static final int SIL_LAYOUT                               = 145;
    public static final int PROTOCOL_CONFORMANCE_XREF                = 147;
    public static final int MEMBERS                                  = 148;
    public static final int XREF                                     = 149;
    public static final int INLINABLE_BODY_TEXT                      = 150;
    public static final int SELF_PROTOCOL_CONFORMANCE                = 151;
    public static final int XREF_OPAQUE_RETURN_TYPE_PATH_PIECE       = 152;
    public static final int CLANG_TYPE                               = 153;
    public static final int DERIVATIVE_FUNCTION_CONFIGURATION        = 154;
    public static final int ERROR_FLAG                               = 155;
    public static final int ABI_ONLY_COUNTERPART                     = 156;
    public static final int DECL_NAME_REF                            = 157;
    public static final int LIFETIME_DEPENDENCE                      = 161;
    public static final int INHERITED_PROTOCOLS                      = 162;   // TRAILING_INFO right after LIFETIME_DEPENDENCE

    /** Human-readable name for a record kind, or {@code "UNKNOWN(<id>)"}. */
    public static String name(int id) {
        switch (id) {
            case TYPE_ALIAS_DECL:         return "TYPE_ALIAS_DECL";
            case GENERIC_TYPE_PARAM_DECL: return "GENERIC_TYPE_PARAM_DECL";
            case ASSOCIATED_TYPE_DECL:    return "ASSOCIATED_TYPE_DECL";
            case STRUCT_DECL:             return "STRUCT_DECL";
            case CONSTRUCTOR_DECL:        return "CONSTRUCTOR_DECL";
            case VAR_DECL:                return "VAR_DECL";
            case PARAM_DECL:              return "PARAM_DECL";
            case FUNC_DECL:               return "FUNC_DECL";
            case OPAQUE_TYPE_DECL:        return "OPAQUE_TYPE_DECL";
            case PATTERN_BINDING_DECL:    return "PATTERN_BINDING_DECL";
            case PROTOCOL_DECL:           return "PROTOCOL_DECL";
            case PREFIX_OPERATOR_DECL:    return "PREFIX_OPERATOR_DECL";
            case POSTFIX_OPERATOR_DECL:   return "POSTFIX_OPERATOR_DECL";
            case INFIX_OPERATOR_DECL:     return "INFIX_OPERATOR_DECL";
            case CLASS_DECL:              return "CLASS_DECL";
            case ENUM_DECL:               return "ENUM_DECL";
            case ENUM_ELEMENT_DECL:       return "ENUM_ELEMENT_DECL";
            case SUBSCRIPT_DECL:          return "SUBSCRIPT_DECL";
            case EXTENSION_DECL:          return "EXTENSION_DECL";
            case DESTRUCTOR_DECL:         return "DESTRUCTOR_DECL";
            case PRECEDENCE_GROUP_DECL:   return "PRECEDENCE_GROUP_DECL";
            case ACCESSOR_DECL:           return "ACCESSOR_DECL";
            case MACRO_DECL:              return "MACRO_DECL";
            default:                      return "UNKNOWN(" + id + ")";
        }
    }

    /** True if {@code id} falls in the decl range used by Swift modules. */
    public static boolean isDecl(int id) {
        return id >= TYPE_ALIAS_DECL && id <= MACRO_DECL;
    }

    private SwiftDeclRecord() {
    }
}
