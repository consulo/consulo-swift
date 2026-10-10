/*
 * MIT License
 *
 * Copyright (c) 2026 consulo.io
 *
 * See LICENSE.txt at the module root for the full MIT license text.
 */

package consulo.swift.bitcode.swiftmodule;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Swift attribute serialization codes to their source spelling and
 * payload shape. The codes are the {@code CODE} arguments in
 * {@code include/swift/AST/DeclAttr.def}; the bitstream stores each
 * attribute as a record whose kind is {@code 180 + CODE} (see the
 * {@code #include "swift/AST/DeclAttr.def"} block at the bottom of
 * {@code lib/Serialization/DeclTypeRecordNodes.def}).
 *
 * <p>Each table entry knows:
 * <ul>
 *   <li>The Swift source spelling ({@code "inlinable"} → emit
 *       {@code @inlinable}).</li>
 *   <li>Whether the attribute has a payload (e.g. {@code @available(...)},
 *       {@code @objc(name)}, {@code @_silgen_name("...")}). Simple
 *       attributes have no payload and render as bare {@code @Name}.</li>
 * </ul>
 *
 * <p>Payload-bearing attributes get rendered as {@code @Name(...)} when we
 * recognise them but don't yet decode the args — we mark them with a
 * comment so it's clear the printer is being conservative rather than
 * pretending the attribute is parameter-less.
 *
 * <p>Coverage focuses on the attributes that actually appear in
 * {@code .swiftinterface} files for Apple stdlib / SwiftPM / SwiftSyntax;
 * the long tail of internal {@code _xxx} attributes is covered by name
 * but treated as payload-bearing comments so the rendering is conservative.
 */
public final class SwiftDeclAttribute {

    /** Offset between {@code DECL_ATTR(CODE)} and the bitstream record kind. */
    public static final int ATTR_BASE = 180;

    /** A decoded attribute spec — used to render an attribute back to Swift source. */
    public static final class Spec {
        /** Source spelling without the leading {@code @}, e.g. {@code "inlinable"}. */
        public final String name;
        /**
         * True when the attribute legally appears with parentheses in source
         * ({@code @available(macOS 10.15, *)}, {@code @objc(name)}). The
         * printer emits {@code @Name /* payload elided *}{@code /} for these
         * since we don't decode the inner args yet.
         */
        public final boolean hasPayload;

        public Spec(String name, boolean hasPayload) {
            this.name = name;
            this.hasPayload = hasPayload;
        }
    }

    /** Swift's {@code PlatformKind} enum from {@code PlatformKinds.def}. */
    private static final String[] PLATFORMS = {
        "*",                              // 0 = none — never appears in source
        "swift",                          // 1
        "*",                              // 2 = anyAppleOS — render as universal
        "macOS",                          // 3
        "iOS",                            // 4
        "tvOS",                           // 5
        "watchOS",                        // 6
        "macCatalyst",                    // 7
        "visionOS",                       // 8
        "DriverKit",                      // 9
        "macOSApplicationExtension",      // 10
        "iOSApplicationExtension",        // 11
        "tvOSApplicationExtension",       // 12
        "watchOSApplicationExtension",    // 13
        "macCatalystApplicationExtension",// 14
        "visionOSApplicationExtension",   // 15
        "FreeBSD",                        // 16
        "OpenBSD",                        // 17
        "Windows",                        // 18
        "Android",                        // 19
    };

    /** Swift's {@code AvailabilityDomainKind} enum from {@code ModuleFormat.h}. */
    private static final int DOMAIN_UNIVERSAL          = 0;
    private static final int DOMAIN_SWIFT_LANGUAGE_MODE = 1;
    private static final int DOMAIN_PACKAGE_DESCRIPTION = 2;
    private static final int DOMAIN_EMBEDDED           = 3;
    private static final int DOMAIN_PLATFORM           = 4;
    private static final int DOMAIN_CUSTOM             = 5;

    /**
     * Look up an attribute by its bitstream record kind ({@code 180+CODE}).
     * Returns {@code null} when the kind doesn't map to a known attribute
     * (e.g. it's a prelude record like {@code DESERIALIZATION_SAFETY}).
     */
    public static Spec byRecordKind(int kind) {
        if (kind < ATTR_BASE) {
            return null;
        }
        return TABLE.get(kind - ATTR_BASE);
    }

    /**
     * Render the attribute as a Swift source snippet (with leading
     * {@code @}). Same idea as {@link #renderAsSource(int)} but receives
     * the full record so payload-bearing attributes that we know how to
     * decode ({@code @available}, etc.) can emit their real arguments
     * instead of a generic placeholder.
     */
    public static String renderAsSource(SwiftAuxRecord aux) {
        if (aux == null) return null;
        int recordKind = aux.kind();
        Spec s = byRecordKind(recordKind);
        if (s == null) return null;
        // Source-keyword / compiler-internal markers — skip (see renderAsSource(int)).
        if (isSkippedSpec(s.name)) return null;
        if ("available".equals(s.name)) {
            return renderAvailable(aux.args());
        }
        if (s.hasPayload) {
            return "@" + s.name + parseablePayload(s.name);
        }
        return "@" + s.name;
    }

    private static boolean isSkippedSpec(String name) {
        return name.equals("private") || name.equals("fileprivate")
            || name.equals("internal") || name.equals("public")
            || name.equals("package") || name.equals("open")
            || name.equals("weak") || name.equals("unowned")
            || name.equals("rethrows") || name.equals("reasync")
            || name.equals("nonisolated") || name.equals("unsafe")
            || name.equals("safe") || name.equals("const")
            || name.equals("__setter_access") || name.equals("__raw_doc_comment");
    }

    /**
     * Render {@code @available(...)} from its raw record args, following
     * the {@code AvailableDeclAttrLayout} schema. Produces real version
     * triples per platform — matches the form you see in
     * {@code .swiftinterface} files like {@code @available(macOS 10.15, *)}
     * or {@code @available(*, deprecated, message: "use foo instead")}.
     *
     * <p>Args layout (see {@code lib/Serialization/ModuleFormat.h}
     * {@code AvailableDeclAttrLayout}):
     * <pre>
     * args[0]  : BCFixed<1>    implicit
     * args[1]  : BCFixed<1>    isUnconditionallyUnavailable
     * args[2]  : BCFixed<1>    isUnconditionallyDeprecated
     * args[3]  : BCFixed<1>    isUnavailableFromAsync
     * args[4]  : BCFixed<1>    isSPI
     * args[5]  : BCFixed<3>    AvailabilityDomainKind
     * args[6]  : BCVBR<5>      platform kind
     * args[7]  : DeclID        custom domain decl
     * args[8..12]   introduced version tuple (major, minor, sub, hasMinor, hasSub)
     * args[13..17]  deprecated version tuple
     * args[18..22]  obsoleted version tuple
     * args[23] : BCVBR<5>      message length in bytes
     * args[24] : BCVBR<5>      rename length in bytes
     * args[25..] BCBlob        message bytes + rename bytes (concatenated)
     * </pre>
     */
    private static String renderAvailable(long[] a) {
        boolean unavailable = at(a, 1) != 0;
        boolean uncondDeprecated = at(a, 2) != 0;
        int domainKind = (int) at(a, 5);
        int platformIdx = (int) at(a, 6);

        StringBuilder sb = new StringBuilder("@available(");
        sb.append(platformLabel(domainKind, platformIdx));

        String introduced = versionAt(a, 8);
        String deprecated = versionAt(a, 13);
        String obsoleted  = versionAt(a, 18);

        // The compact form `@available(macOS 10.15, *)` requires a CONCRETE
        // platform label — universal {@code *} alone with a bare version
        // is invalid (Swift only allows that without a version, as the
        // wildcard trailing entry). Bail out of the compact form when the
        // platform label resolved to {@code *} (i.e. Universal domain, or
        // {@code anyAppleOS} which our table maps to {@code *}).
        String platformLabel = platformLabel(domainKind, platformIdx);
        boolean platformIsBareVersion =
            (domainKind == DOMAIN_PLATFORM || domainKind == DOMAIN_SWIFT_LANGUAGE_MODE
                || domainKind == DOMAIN_PACKAGE_DESCRIPTION)
                && introduced != null
                && !unavailable
                && !uncondDeprecated
                && deprecated == null
                && obsoleted == null
                && !"*".equals(platformLabel);
        if (platformIsBareVersion) {
            // Compact form `@available(macOS 10.15, *)` — only when the
            // attribute is purely an "introduced" announcement.
            sb.append(' ').append(introduced).append(", *)");
            return sb.toString();
        }

        if (unavailable) {
            sb.append(", unavailable");
        }
        if (uncondDeprecated) {
            sb.append(", deprecated");
        }
        if (introduced != null) {
            sb.append(", introduced: ").append(introduced);
        }
        if (deprecated != null) {
            sb.append(", deprecated: ").append(deprecated);
        }
        if (obsoleted != null) {
            sb.append(", obsoleted: ").append(obsoleted);
        }
        // Message + rename strings live in a trailing blob; lengths in
        // args[23]/args[24], bytes in args[25..].
        int msgLen = (int) at(a, 23);
        int renLen = (int) at(a, 24);
        String[] strings = readMessageRename(a, msgLen, renLen);
        if (strings[0] != null && !strings[0].isEmpty()) {
            sb.append(", message: \"").append(escape(strings[0])).append('"');
        }
        if (strings[1] != null && !strings[1].isEmpty()) {
            sb.append(", renamed: \"").append(escape(strings[1])).append('"');
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Label for the @available() first argument — platform name, {@code *},
     * {@code swift}, {@code _PackageDescription}, depending on domain.
     */
    private static String platformLabel(int domainKind, int platformIdx) {
        switch (domainKind) {
            case DOMAIN_UNIVERSAL:           return "*";
            case DOMAIN_SWIFT_LANGUAGE_MODE: return "swift";
            case DOMAIN_PACKAGE_DESCRIPTION: return "_PackageDescription";
            case DOMAIN_EMBEDDED:            return "Embedded";
            case DOMAIN_PLATFORM: {
                if (platformIdx >= 0 && platformIdx < PLATFORMS.length) {
                    return PLATFORMS[platformIdx];
                }
                return "*";
            }
            case DOMAIN_CUSTOM:              return "*"; // custom domains use a decl ref
            default:                         return "*";
        }
    }

    /** Read a version tuple at args[start..start+4]. Returns null if absent. */
    private static String versionAt(long[] a, int start) {
        if (start + 4 >= a.length) return null;
        int major      = (int) at(a, start);
        int minor      = (int) at(a, start + 1);
        int subminor   = (int) at(a, start + 2);
        boolean hasMin = at(a, start + 3) != 0;
        boolean hasSub = at(a, start + 4) != 0;
        // A non-introduced/deprecated/obsoleted slot uses (0,0,0,0,0).
        // hasMinor=false also means "no version specified at all" (because
        // even {@code @available(macOS 10, *)} has major=10 and hasMinor=false
        // but the entry is still "present" — we distinguish via major>0 or
        // any of the bools).
        if (major == 0 && !hasMin && !hasSub) {
            return null;
        }
        StringBuilder sb = new StringBuilder().append(major);
        if (hasMin) {
            sb.append('.').append(minor);
            if (hasSub) {
                sb.append('.').append(subminor);
            }
        }
        return sb.toString();
    }

    /**
     * Pull the message + rename strings out of the trailing blob.
     * Index {@code args[25..]} holds {@code msgLen + renLen} byte values
     * (each as a long); split into the two strings.
     */
    private static String[] readMessageRename(long[] a, int msgLen, int renLen) {
        int blobStart = 25;
        int total = msgLen + renLen;
        if (blobStart + total > a.length) {
            return new String[]{ null, null };
        }
        byte[] msg = new byte[msgLen];
        for (int i = 0; i < msgLen; i++) {
            msg[i] = (byte) (a[blobStart + i] & 0xFF);
        }
        byte[] ren = new byte[renLen];
        for (int i = 0; i < renLen; i++) {
            ren[i] = (byte) (a[blobStart + msgLen + i] & 0xFF);
        }
        return new String[]{
            new String(msg, java.nio.charset.StandardCharsets.UTF_8),
            new String(ren, java.nio.charset.StandardCharsets.UTF_8),
        };
    }

    /** Minimal string escape for Swift double-quoted literals. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private static long at(long[] a, int i) {
        return i < a.length ? a[i] : 0L;
    }

    /**
     * Render the attribute matching {@code recordKind} as a Swift source
     * snippet (with leading {@code @}). Returns {@code null} when the
     * record isn't a known attribute. The renderer is conservative — it
     * names the attribute but elides any payload args. Prefer
     * {@link #renderAsSource(SwiftAuxRecord)} when you have the record
     * payload — that path decodes attributes like {@code @available}
     * properly.
     */
    public static String renderAsSource(int recordKind) {
        Spec s = byRecordKind(recordKind);
        if (s == null) {
            return null;
        }
        // Swift-source-keyword attributes — emit verbatim, no leading @.
        if (s.name.equals("private") || s.name.equals("fileprivate")
            || s.name.equals("internal") || s.name.equals("public")
            || s.name.equals("package") || s.name.equals("open")
            || s.name.equals("weak") || s.name.equals("unowned")
            || s.name.equals("rethrows") || s.name.equals("reasync")
            || s.name.equals("nonisolated") || s.name.equals("unsafe")
            || s.name.equals("safe") || s.name.equals("const")
            || s.name.equals("__setter_access") || s.name.equals("__raw_doc_comment")) {
            // Skip — these are either rendered elsewhere from the decl's
            // own access-level field, or are internal compiler markers
            // that don't belong in source.
            return null;
        }
        if (s.hasPayload) {
            return "@" + s.name + parseablePayload(s.name);
        }
        return "@" + s.name;
    }

    /**
     * A SYNTACTICALLY-VALID placeholder payload for an attribute that
     * carries args. We don't decode the bitstream payload yet — but we
     * can't emit {@code (/* elided *}{@code /)} because {@code swiftc}
     * validates the inside of attributes like {@code @available},
     * {@code @objc}, {@code @_silgen_name}.
     *
     * <p>Each branch returns a string that starts with {@code (} and ends
     * with {@code )} and parses (and type-checks) cleanly per the Swift
     * grammar. Anything not listed gets the safe empty-args form
     * {@code ()} — works for many attrs that accept zero args even though
     * they're classified as payload-bearing.
     */
    private static String parseablePayload(String name) {
        switch (name) {
            case "available":
                return "(*, deprecated, message: \"decompiler-placeholder\")";
            case "objc":
                return "(decompilerPlaceholder)";
            case "_silgen_name":
            case "_semantics":
            case "_originallyDefinedIn":
            case "_dynamicReplacement":
            case "_documentation":
                return "(\"decompiler-placeholder\")";
            case "_spi":
                // @_spi takes a bare identifier (the SPI group name), not a string.
                return "(decompilerPlaceholder)";
            case "_specialize":
            case "_specializeExtension":
                // {@code @_specialize(where T == Int)} is the real syntax
                // but our PSI parser doesn't accept {@code where} clauses
                // inside attribute argument lists yet. {@code exported}
                // is a valid alternate-form arg that parses cleanly.
                return "(exported: false)";
            case "_alignment":
                return "(1)";
            case "_optimize":
                return "(speed)";
            case "differentiable":
                return "(reverse)";
            case "derivative":
            case "transpose":
                return "(of: decompilerPlaceholder)";
            case "_effects":
                return "(readnone)";
            case "_typeEraser":
                return "(decompilerPlaceholder)";
            case "globalActor":
                return "";  // no parens — usage is just @<Type>, payload handled by macro
            case "backDeployed":
                return "(before: macOS 11.0)";
            case "_expose":
                return "(Cxx)";
            case "_extern":
                return "(c)";
            case "_implements":
                return "(decompilerPlaceholder, decompilerPlaceholder)";
            case "_private":
            case "_implementationOnly":
                return "(sourceFile: \"decompiler-placeholder\")";
            case "attached":
                return "(member)";
            case "inline":
                return "(__always)";
            case "section":
                return "(\"__TEXT,__placeholder\")";
            case "storageRestrictions":
                return "(initializes: decompilerPlaceholder)";
            case "lifetime":
                return "(borrow self)";
            case "abi":
                return "(decompilerPlaceholder)";
            case "_unsafeSelfDependentResult":
            case "diagnose":
                return "(decompilerPlaceholder)";
            case "c":
                return "(decompilerPlaceholder)";
            case "_custom":
                return "(decompilerPlaceholder)";
            case "_objcRuntimeName":
            case "_swift_native_objc_runtime_base":
                return "(_DecompilerPlaceholder)";
            case "__objc_bridged":
            case "_restatedObjCConformance":
                return "(decompilerPlaceholder)";
            case "_nonescapable":
            case "_allowFeatureSuppression":
                return "(decompilerPlaceholder)";
            case "_spiOnly":
                return "";  // takes no args in source; payload here is metadata
            default:
                // Conservative: emit zero-args form. Many attributes
                // accept this (e.g. @attached(member) is parameterized,
                // but @objc takes optional args, so bare @attached and
                // bare @objc both parse for SOME usages).
                return "";
        }
    }

    private SwiftDeclAttribute() {
    }

    // -------------------------- table ---------------------------

    private static final Map<Integer, Spec> TABLE = new HashMap<>();

    static {
        // Simple (no payload) attributes — bare @Name.
        simple(6, "dynamicCallable");
        simple(7, "main");
        simple(8, "_exported");
        simple(9, "dynamicMemberLookup");
        simple(10, "NSCopying");
        simple(11, "IBAction");
        simple(12, "IBDesignable");
        simple(13, "IBInspectable");
        simple(14, "IBOutlet");
        simple(15, "NSManaged");
        simple(17, "LLDBDebuggerFunction");
        simple(18, "UIApplicationMain");
        simple(19, "unsafe_no_objc_tagged_pointer");
        simple(26, "_transparent");
        simple(27, "requires_stored_property_inits");
        simple(28, "_neverEmitIntoClient");
        simple(30, "nonobjc");
        simple(31, "_fixed_layout");
        simple(32, "inlinable");
        simple(34, "objcMembers");
        simple(36, "_lexicalLifetimes");
        simple(45, "_hasStorage");
        simple(52, "NSApplicationMain");
        simple(53, "_objc_non_lazy_realization");
        simple(54, "__synthesized_protocol");
        simple(55, "testable");
        simple(61, "warn_unqualified_access");
        simple(62, "_show_in_interface");
        simple(64, "usableFromInline");
        simple(65, "discardableResult");
        simple(66, "GKInspectable");
        simple(69, "_staticInitializeObjCMetadata");
        simple(72, "implementation");
        simple(74, "_clangImporterSynthesizedType");
        simple(75, "_weakLinked");
        simple(76, "frozen");
        simple(77, "_forbidSerializingReference");
        simple(78, "_hasInitialValue");
        simple(79, "_nonoverride");
        simple(81, "_borrowed");
        simple(83, "_alwaysEmitIntoClient");
        simple(84, "_implementationOnly");
        simple(86, "propertyWrapper");
        simple(87, "_disfavoredOverload");
        simple(88, "resultBuilder");
        simple(89, "_projectedValueProperty");
        simple(90, "_nonEphemeral");
        simple(92, "_hasMissingDesignatedInitializers");
        simple(93, "_inheritsConvenienceInitializers");
        simple(95, "IBSegueAction");
        simple(100, "noDerivative");
        simple(101, "export");
        simple(107, "Sendable");
        simple(108, "_marker");
        simple(111, "_owned");
        simple(114, "_unsafeInheritExecutor");
        simple(115, "_implicitSelfCapture");
        simple(116, "_inheritActorContext");
        simple(117, "_eagerMove");
        simple(119, "_noEagerMove");
        simple(120, "_assemblyVision");
        simple(121, "_nonSendable");
        simple(122, "_noImplicitCopy");
        simple(123, "_noLocks");
        simple(124, "_noAllocation");
        simple(125, "preconcurrency");
        simple(127, "_unavailableFromAsync");
        simple(128, "exclusivity");
        simple(131, "_moveOnly");
        simple(132, "_alwaysEmitConformanceMetadata");
        simple(138, "_noMetadata");
        simple(143, "used");
        simple(146, "_rawLayout");
        simple(149, "_unsafeNonescapableResult");
        simple(151, "_staticExclusiveOnly");
        simple(152, "extractConstantsFromMembers");
        simple(153, "_noRuntime");
        simple(154, "_noExistentials");
        simple(155, "_noObjCBridging");
        simple(156, "_noManualOwnership");
        simple(158, "_preInverseGenerics");
        simple(159, "sensitive");
        simple(162, "_addressableSelf");
        simple(163, "_addressableForDependencies");
        simple(166, "reparentable");
        simple(167, "const");
        simple(168, "constInitialized");
        simple(169, "nonexhaustive");
        simple(170, "concurrent");
        simple(172, "specialized");

        // Source-keyword / pseudo-attributes — emit nothing in @form (handled
        // by other code paths or compiler-internal).
        simple(46, "private");
        simple(47, "__setter_access");
        simple(48, "__raw_doc_comment");
        simple(49, "weak");
        simple(57, "rethrows");
        simple(58, "rethrows");
        simple(109, "reasync");
        simple(110, "reasync");
        simple(112, "nonisolated");
        simple(160, "unsafe");
        simple(164, "safe");

        // Parameterized attributes — render as @Name(/* payload elided */).
        payload(0, "_silgen_name");
        payload(1, "available");
        payload(3, "objc");
        payload(20, "inline");
        payload(21, "_semantics");
        payload(33, "_specialize");
        payload(50, "_effects");
        payload(51, "__objc_bridged");
        payload(56, "_alignment");
        payload(59, "_swift_native_objc_runtime_base");
        payload(63, "c");
        payload(67, "_implements");
        payload(68, "_objcRuntimeName");
        payload(70, "_restatedObjCConformance");
        payload(73, "_optimize");
        payload(80, "_dynamicReplacement");
        payload(82, "_private");
        payload(85, "_custom");
        payload(91, "differentiable");
        payload(94, "_typeEraser");
        payload(96, "_originallyDefinedIn");
        payload(97, "derivative");
        payload(98, "_spi");
        payload(99, "transpose");
        payload(104, "globalActor");
        payload(105, "_specializeExtension");
        payload(129, "backDeployed");
        payload(133, "_expose");
        payload(135, "_spiOnly");
        payload(136, "_documentation");
        payload(142, "attached");
        payload(144, "section");
        payload(145, "storageRestrictions");
        payload(147, "_extern");
        payload(148, "_nonescapable");
        payload(157, "_allowFeatureSuppression");
        payload(161, "lifetime");
        payload(165, "abi");
        payload(173, "_unsafeSelfDependentResult");
        payload(174, "diagnose");
    }

    private static void simple(int code, String name) {
        TABLE.put(code, new Spec(name, false));
    }

    private static void payload(int code, String name) {
        TABLE.put(code, new Spec(name, true));
    }
}
