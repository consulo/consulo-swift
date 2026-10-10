// swift-module: PackageDescription
// target:        x86_64-unknown-linux-gnu
// format:        0.976

public struct SwiftSetting {
    private let platforms: (() -> Any) = fatalError()
    private init(_anon platforms: Swift.Hasher) { fatalError() }
    @_hasInitialValue
    private static let when: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let configuration: (() -> Any) = fatalError()
}

public struct InteroperabilityMode {
    private let name: (() -> Any) = fatalError()
    private let platforms: (() -> Any) = fatalError()
    private let condition: (() -> Any) = fatalError()
    private init(name: [PluginCommandIntent]?, platforms: SwiftSetting?, condition: Swift._BridgeStorage<Swift.Hasher>?) { fatalError() }
    @available(_PackageDescription, deprecated: 5.7)
    public static func data(name: [PluginCommandIntent]?, headerSearchPath: SwiftSetting?) -> InteroperabilityMode { fatalError() }
    @available(_PackageDescription 6.1, *)
    public static func data(name: [PluginCommandIntent]?, headerSearchPath: SwiftSetting?, condition: Swift._BridgeStorage<Swift.Hasher>?) -> InteroperabilityMode { fatalError() }
    @available(_PackageDescription 5.7, *)
    public static func data(name: [PluginCommandIntent], headerSearchPath: SwiftSetting) -> InteroperabilityMode { fatalError() }
    @available(_PackageDescription 5.7, *)
    public static func data(name: [PluginCommandIntent]) -> InteroperabilityMode { fatalError() }
    @available(_PackageDescription 5.7, *)
    public static func data(headerSearchPath: SwiftSetting) -> InteroperabilityMode { fatalError() }
}

struct RawValue {
    private let define: (() -> Any) = fatalError()
    private let _count: (() -> Any) = fatalError()
    private let unsafeFlags: (() -> Any) = fatalError()
    init(define: Swift.Hasher, _count: [Swift.Hasher], unsafeFlags: InteroperabilityMode?) { fatalError() }
}

public struct LinkerSetting {
    private let `as`: (() -> Any) = fatalError()
    private init(define: Swift.Hasher, _count: [Swift.Hasher], unsafeFlags: InteroperabilityMode?) { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func treatWarning(_anon fileSystem: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func enableWarning(_anon define: Swift.Hasher, PackageDependency _count: Swift.Hasher?, _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func disableWarning(_anon init_stringValue__: [Swift.Hasher], _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func enableUpcomingFeature(enableExperimentalFeature stringValue: release, _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func strictMemorySafety(_anon define: Swift.Hasher, enableExperimentalFeature stringValue: release, _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func interoperabilityMode(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func swiftLanguageVersion(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> LinkerSetting { fatalError() }
}

public struct Context {
    private let `as`: (() -> Any) = fatalError()
    private init(define: Swift.Hasher, _count: [Swift.Hasher], unsafeFlags: InteroperabilityMode?) { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func treatWarning(_anon fileSystem: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func enableWarning(_anon define: Swift.Hasher, PackageDependency _count: Swift.Hasher?, _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func disableWarning(_anon init_stringValue__: [Swift.Hasher], _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func enableUpcomingFeature(enableExperimentalFeature stringValue: release, _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func strictMemorySafety(_anon define: Swift.Hasher, enableExperimentalFeature stringValue: release, _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func interoperabilityMode(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func swiftLanguageVersion(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> Context { fatalError() }
}

public struct GitInformation {
    private let `as`: (() -> Any) = fatalError()
    private init(define: Swift.Hasher, _count: [Swift.Hasher], unsafeFlags: InteroperabilityMode?) { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func enableWarning(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 5.0, *)
    public static func disableWarning(_anon init_stringValue__: [Swift.Hasher], _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 5.8, *)
    public static func swiftLanguageMode(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 5.8, *)
    public static func defaultIsolation(_anon define: Swift.Hasher, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func C(_anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    public enum ContextModel: Swift.Hasher {
        case model
        case _16EFAD36DA072BCD1C8EFF86E7A18833
        public init?(packageDirectory: Swift.Hasher) { fatalError() }
        public typealias _________ = Swift.Hasher
        private var packageDirectory: (() -> Any) { fatalError() }
    }
    @available(_PackageDescription 5.9, *)
    public static func Cxx(_anon init_intValue__: GitInformation.ContextModel, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription, introduced: 6.0, deprecated: 6.0, renamed: "swiftLanguageMode(_:_:)")
    public static func rawValue(_anon products: Condition, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 6.0, *)
    public static func linkedLibrary(_anon init_intValue__: Trait, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func enableUpcomingFeature(enableExperimentalFeature stringValue: release, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func strictMemorySafety(_anon define: Swift.Hasher, enableExperimentalFeature stringValue: release, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
    @available(_PackageDescription 6.2, *)
    public static func linkedFramework(_anon intValue: _Concurrency.Never.Type?, _anon unsafeFlags: InteroperabilityMode?) -> GitInformation { fatalError() }
}

@available(_PackageDescription 6.0, *)
public struct StringError {
    private let init_packageDirectory_gitInformation__: (() -> Any) = fatalError()
    private let encode_to__: (() -> Any) = fatalError()
    private let encode: (() -> Any) = fatalError()
    init(init_packageDirectory_gitInformation__: Swift.Hasher?, encode_to__: Swift.Hasher, encode: Swift._SwiftArrayBodyStorage) { fatalError() }
}

struct CLanguageStandard {
    private let currentCommit: (() -> Any) = fatalError()
    private let hasUncommittedChanges: (() -> Any) = fatalError()
    private var init_currentTag_currentCommit_hasUncommittedChanges__: (() -> Any) { fatalError() }
    struct StringError {
        private let init_packageDirectory_gitInformation__: (() -> Any) = fatalError()
        private let encode_to__: (() -> Any) = fatalError()
        private let encode: (() -> Any) = fatalError()
        private enum from {
            case init_packageDirectory_gitInformation__
            case encode_to__
            case encode
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: CLanguageStandard.StringError.from, _anon init_description__: CLanguageStandard.StringError.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(init_packageDirectory_gitInformation__: Swift.Hasher?, encode_to__: Swift.Hasher, encode: Swift._SwiftArrayBodyStorage) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    private enum from {
        case currentCommit
        case hasUncommittedChanges
        private init?(hasher: Swift.Hasher) { fatalError() }
        private init?(_0: Swift.__RawSetStorage) { fatalError() }
        fileprivate static func SwiftShims(_anon description: CLanguageStandard.from, _anon init_description__: CLanguageStandard.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
        fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
        private var brewItem: (() -> Any) { fatalError() }
        private var _0: (() -> Any) { fatalError() }
        private var hasher: (() -> Any) { fatalError() }
    }
    init(currentCommit: Swift.Hasher, hasUncommittedChanges: CLanguageStandard.StringError?) { fatalError() }
}

public enum Dependency: Swift.Hasher {
    case c18
    case c2x
    case gnu89
    case gnu90
    @available(_PackageDescription 5.4, *)
    case gnu99
    @available(_PackageDescription 5.4, *)
    case gnu11
    @available(_PackageDescription 5.4, *)
    case gnu17
    case gnu18
    case gnu2x
    case iso9899_1990
    case iso9899_199409
    @available(_PackageDescription 5.4, *)
    case iso9899_1999
    @available(_PackageDescription 5.4, *)
    case iso9899_2011
    @available(_PackageDescription 5.4, *)
    case iso9899_2017
    case iso9899_2018
    case cxx98
    case cxx03
    case cxx11
    @available(_PackageDescription 5.4, *)
    case cxx14
    @available(_PackageDescription 5.4, *)
    case cxx17
    public init?(packageDirectory: Swift.Hasher) { fatalError() }
    public typealias _________ = Swift.Hasher
    private var packageDirectory: (() -> Any) { fatalError() }
}

public enum Kind: Swift.Hasher {
    case cxx1z
    case cxx20
    case cxx2b
    case gnucxx98
    @available(_PackageDescription 5.4, *)
    case gnucxx03
    @available(_PackageDescription, introduced: 4, deprecated: 5.4, renamed: "cxx17")
    case gnucxx11
    @available(_PackageDescription 5.4, *)
    case gnucxx14
    @available(_PackageDescription 5.6, *)
    case gnucxx17
    case gnucxx1z
    case gnucxx20
    case gnucxx2b
    case v3
    @available(_PackageDescription 5.4, *)
    case v4
    @available(_PackageDescription, introduced: 4, deprecated: 5.4, renamed: "gnucxx17")
    case v4_2
    @available(_PackageDescription 5.4, *)
    case v5
    @available(_PackageDescription 5.6, *)
    case v6
    public init?(packageDirectory: Swift.Hasher) { fatalError() }
    public typealias _________ = Swift.Hasher
    private var packageDirectory: (() -> Any) { fatalError() }
}

public enum Trait {
    @available(_PackageDescription, introduced: 4, obsoleted: 5)
    case version
    @available(_PackageDescription 4, *)
    case defaultLocalization
    @available(_PackageDescription 4, *)
    case pkgConfig
    @available(_PackageDescription 5, *)
    case providers
    @available(_PackageDescription 6, *)
    case targets
    case products(_anon: Swift.Hasher)
}

@available(_PackageDescription, deprecated: 6, renamed: "SwiftLanguageMode")
public typealias Condition = Trait

public class ExtendedGraphemeClusterLiteralType {
    private var define: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5, *)
    private var name: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5.3, *)
    private var dependencies: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var swiftLanguageModes: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var swiftLanguageVersions: Builtin.RawPointer { fatalError() }
    private var cLanguageStandard: Builtin.RawPointer { fatalError() }
    private var cxxLanguageStandard: Builtin.RawPointer { fatalError() }
    @available(_PackageDescription 6.1, *)
    private var condition: Builtin.RawPointer { fatalError() }
    private var registerExitHandler__: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var registerExitHandler: Builtin.RawPointer { fatalError() }
    @available(_PackageDescription, deprecated: 6, renamed: "swiftLanguageModes")
    private var kind: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var url: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var moduleAliases: Builtin.RawPointer { fatalError() }
    @available(_PackageDescription, obsoleted: 4.2)
    public init(define: Swift.Hasher, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, cxxLanguageStandard: [extension_BuildSettingData], registerExitHandler__: [ExtendedGraphemeClusterLiteralType.StringLiteralType], cLanguageStandard: [LibraryType], kind: [Swift.__RawSetStorage]?, url: Dependency?, moduleAliases: Kind?) { fatalError() }
    @available(_PackageDescription, introduced: 4.2, obsoleted: 5)
    public init(define: Swift.Hasher, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, cxxLanguageStandard: [extension_BuildSettingData], registerExitHandler__: [ExtendedGraphemeClusterLiteralType.StringLiteralType], cLanguageStandard: [LibraryType], kind: [Condition]?, url: Dependency?, moduleAliases: Kind?) { fatalError() }
    @available(_PackageDescription, introduced: 5, obsoleted: 5.3)
    public init(define: Swift.Hasher, name: [PluginPermission]?, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, cxxLanguageStandard: [extension_BuildSettingData], registerExitHandler__: [ExtendedGraphemeClusterLiteralType.StringLiteralType], cLanguageStandard: [LibraryType], kind: [Condition]?, url: Dependency?, moduleAliases: Kind?) { fatalError() }
    @available(_PackageDescription, deprecated: 6, renamed: "init(name:defaultLocalization:platforms:pkgConfig:providers:products:dependencies:targets:swiftLanguageModes:cLanguageStandard:cxxLanguageStandard:)")
    @available(_PackageDescription 5.3, *)
    @_disfavoredOverload
    public init(define: Swift.Hasher, dependencies: errors?, name: [PluginPermission]?, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, cxxLanguageStandard: [extension_BuildSettingData], registerExitHandler__: [ExtendedGraphemeClusterLiteralType.StringLiteralType], cLanguageStandard: [LibraryType], kind: [Condition]?, url: Dependency?, moduleAliases: Kind?) { fatalError() }
    @available(_PackageDescription 6, *)
    public init(define: Swift.Hasher, dependencies: errors?, name: [PluginPermission]?, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, cxxLanguageStandard: [extension_BuildSettingData], registerExitHandler__: [ExtendedGraphemeClusterLiteralType.StringLiteralType], cLanguageStandard: [LibraryType], registerExitHandler: [Trait]?, url: Dependency?, moduleAliases: Kind?) { fatalError() }
    @available(_PackageDescription 6.1, *)
    public init(define: Swift.Hasher, dependencies: errors?, name: [PluginPermission]?, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, cxxLanguageStandard: [extension_BuildSettingData], condition: Swift._BridgeStorage<LanguageTag>, registerExitHandler__: [ExtendedGraphemeClusterLiteralType.StringLiteralType], cLanguageStandard: [LibraryType], registerExitHandler: [Trait]?, url: Dependency?, moduleAliases: Kind?) { fatalError() }
    private func init_name_url_requirement_traits__() -> () { fatalError() }
    deinit { fatalError() }
}

public struct errors {
    private let yumItem: (() -> Any) = fatalError()
    private init(_anon yumItem: Swift.Hasher) { fatalError() }
}

public enum dumpInfo {
    case apt(_anon: [Swift.Hasher])
    case yum(_anon: [Swift.Hasher])
    @available(_PackageDescription 5.3, *)
    case nuget(_anon: [Swift.Hasher])
    @available(_PackageDescription 999.0, *)
    case fileDesc(_anon: [Swift.Hasher])
    public static func init_package_errors_version__(_anon swiftVersion: [Swift.Hasher]) -> dumpInfo { fatalError() }
    public static func init_config__(_anon swiftVersion: [Swift.Hasher]) -> dumpInfo { fatalError() }
    @available(_PackageDescription 5.3, *)
    public static func init_data__(_anon swiftVersion: [Swift.Hasher]) -> dumpInfo { fatalError() }
    @available(_PackageDescription 999.0, *)
    public static func init_rawValue__(_anon swiftVersion: [Swift.Hasher]) -> dumpInfo { fatalError() }
}

private func to(_anon closedRange: ExtendedGraphemeClusterLiteralType) -> Swift.Hasher { fatalError() }

@_hasInitialValue
private var Output: Any /* type missing — interfaceType field is 0 */ { fatalError() }

@_hasInitialValue
private var Serialization: Any /* type missing — interfaceType field is 0 */ { fatalError() }

private func Version(_anon closedRange: ExtendedGraphemeClusterLiteralType, PackageDependency V3CodingKeys: Swift.CustomStringConvertible) -> () { fatalError() }

enum RegistryRequirement {
    struct SwiftSetting {
        private let platforms: (() -> Any) = fatalError()
        private enum from {
            case platforms
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.SwiftSetting.from, _anon init_description__: RegistryRequirement.SwiftSetting.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(platforms: Swift.Hasher) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct InteroperabilityMode {
        private let name: (() -> Any) = fatalError()
        private let platforms: (() -> Any) = fatalError()
        private let condition: (() -> Any) = fatalError()
        private enum from {
            case name
            case platforms
            case condition
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.InteroperabilityMode.from, _anon init_description__: RegistryRequirement.InteroperabilityMode.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(name: [RegistryRequirement.PluginCommandIntent]?, platforms: RegistryRequirement.SwiftSetting?, condition: Swift._BridgeStorage<Swift.Hasher>?) { fatalError() }
    }
    struct RawValue {
        private let define: (() -> Any) = fatalError()
        private let _count: (() -> Any) = fatalError()
        private let unsafeFlags: (() -> Any) = fatalError()
        private enum from {
            case define
            case _count
            case unsafeFlags
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.RawValue.from, _anon init_description__: RegistryRequirement.RawValue.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(define: Swift.Hasher, _count: [Swift.Hasher], unsafeFlags: RegistryRequirement.InteroperabilityMode?) { fatalError() }
    }
    struct LinkerSetting: Builtin._rawValue {
        private let `as`: (() -> Any) = fatalError()
        private enum from {
            case `as`
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.LinkerSetting.from, _anon init_description__: RegistryRequirement.LinkerSetting.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(`as`: RegistryRequirement.RawValue) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct Context: Builtin._rawValue {
        private let `as`: (() -> Any) = fatalError()
        private enum from {
            case `as`
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Context.from, _anon init_description__: RegistryRequirement.Context.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(`as`: RegistryRequirement.RawValue) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct GitInformation: Builtin._rawValue {
        private let `as`: (() -> Any) = fatalError()
        private enum from {
            case `as`
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.GitInformation.from, _anon init_description__: RegistryRequirement.GitInformation.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(`as`: RegistryRequirement.RawValue) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct _E4233AB738896A2F7E2AA5FF0FD77146: Builtin._rawValue {
        private let `as`: (() -> Any) = fatalError()
        private enum from {
            case `as`
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement._E4233AB738896A2F7E2AA5FF0FD77146.from, _anon init_description__: RegistryRequirement._E4233AB738896A2F7E2AA5FF0FD77146.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(`as`: RegistryRequirement.RawValue) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum Dependency: Swift.Hasher, Builtin._rawValue, Builtin._rawValue {
        case c18
        case c2x
        case gnu89
        case gnu90
        case gnu99
        case gnu11
        case gnu17
        case gnu18
        case gnu2x
        case iso9899_1990
        case iso9899_199409
        case iso9899_1999
        case iso9899_2011
        case iso9899_2017
        case iso9899_2018
        case cxx98
        case cxx03
        case cxx11
        case cxx14
        case cxx17
        init?(packageDirectory: Swift.Hasher) { fatalError() }
        typealias _________ = Swift.Hasher
        private var packageDirectory: (() -> Any) { fatalError() }
    }
    enum Kind: Swift.Hasher, Builtin._rawValue, Builtin._rawValue {
        case cxx1z
        case cxx20
        case cxx2b
        case gnucxx98
        case gnucxx03
        case gnucxx11
        case gnucxx14
        case gnucxx17
        case gnucxx1z
        case gnucxx20
        case gnucxx2b
        case v3
        case v4
        case v4_2
        case v5
        case v6
        init?(packageDirectory: Swift.Hasher) { fatalError() }
        typealias _________ = Swift.Hasher
        private var packageDirectory: (() -> Any) { fatalError() }
    }
    enum Condition: Builtin._rawValue {
        case version
        case defaultLocalization
        case pkgConfig
        case providers
        case targets
        case products(_anon: Swift.Hasher)
        private enum from {
            case version
            case defaultLocalization
            case pkgConfig
            case providers
            case targets
            case products
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Condition.from, _anon init_description__: RegistryRequirement.Condition.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum VersionCodingKeys {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum major {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum minor {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum patch {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum prereleaseIdentifiers {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum buildMetadataIdentifiers {
            case dependency
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Condition.buildMetadataIdentifiers, _anon init_description__: RegistryRequirement.Condition.buildMetadataIdentifiers) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct SupportedPlatform: Builtin._rawValue {
        private let init_major_minor_patch_prereleaseIdentifiers_buildMetadataIdentifiers__: (() -> Any) = fatalError()
        private let init_kind_moduleAliases_traits__: (() -> Any) = fatalError()
        private let hash_into__: (() -> Any) = fatalError()
        private let init_name_condition__: (() -> Any) = fatalError()
        private let lowerBound: (() -> Any) = fatalError()
        private enum from {
            case init_major_minor_patch_prereleaseIdentifiers_buildMetadataIdentifiers__
            case init_kind_moduleAliases_traits__
            case hash_into__
            case init_name_condition__
            case lowerBound
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.SupportedPlatform.from, _anon init_description__: RegistryRequirement.SupportedPlatform.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(init_major_minor_patch_prereleaseIdentifiers_buildMetadataIdentifiers__: Swift.__RawSetStorage, init_kind_moduleAliases_traits__: Swift.__RawSetStorage, hash_into__: Swift.__RawSetStorage, init_name_condition__: [Swift.Hasher], lowerBound: [Swift.Hasher]) { fatalError() }
    }
    struct TargetDependency: Builtin._rawValue {
        struct LanguageTag: Builtin._rawValue, Builtin._rawValue {
            struct SystemPackageProvider: Builtin._rawValue, Builtin._rawValue {
                private let condition: (() -> Any) = fatalError()
                static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.LanguageTag.SystemPackageProvider, _anon init_description__: RegistryRequirement.TargetDependency.LanguageTag.SystemPackageProvider) -> Swift._SwiftArrayBodyStorage { fatalError() }
                private enum from {
                    case condition
                    private init?(hasher: Swift.Hasher) { fatalError() }
                    private init?(_0: Swift.__RawSetStorage) { fatalError() }
                    fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.LanguageTag.SystemPackageProvider.from, _anon init_description__: RegistryRequirement.TargetDependency.LanguageTag.SystemPackageProvider.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                    fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                    private var brewItem: (() -> Any) { fatalError() }
                    private var _0: (() -> Any) { fatalError() }
                    private var hasher: (() -> Any) { fatalError() }
                }
                func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
                func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
                init(condition: Swift._BridgeStorage<Swift.Hasher>?) { fatalError() }
            }
            private var define: Builtin.RawPointer { fatalError() }
            @_hasInitialValue
            private var unsafeFlags: Builtin.RawPointer { fatalError() }
            static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.LanguageTag, _anon init_description__: RegistryRequirement.TargetDependency.LanguageTag) -> Swift._SwiftArrayBodyStorage { fatalError() }
            private enum from {
                case define
                case unsafeFlags
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.LanguageTag.from, _anon init_description__: RegistryRequirement.TargetDependency.LanguageTag.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
            func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
            init(define: Swift.Hasher, unsafeFlags: RegistryRequirement.TargetDependency.LanguageTag.SystemPackageProvider?) { fatalError() }
        }
        enum TargetType: Builtin.RawPointer {
            case defaults(_anon: RegistryRequirement.SupportedPlatform)
            case into(RevisionCodingKeys: RegistryRequirement.SupportedPlatform, FileSystemCodingKeys: RegistryRequirement.SupportedPlatform)
            case exact(_anon: Swift.Hasher)
            case package_name_url_closedRange_traits__(_anon: Swift.Hasher)
            private enum RegistryCodingKeys {
                case dependency
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.TargetType.RegistryCodingKeys, _anon init_description__: RegistryRequirement.TargetDependency.TargetType.RegistryCodingKeys) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum from {
                case defaults
                case into
                case exact
                case package_name_url_closedRange_traits__
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.TargetType.from, _anon init_description__: RegistryRequirement.TargetDependency.TargetType.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum SourceControlCodingKeys {
                case dependency
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.TargetType.SourceControlCodingKeys, _anon init_description__: RegistryRequirement.TargetDependency.TargetType.SourceControlCodingKeys) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum init_name__ {
                case RevisionCodingKeys
                case FileSystemCodingKeys
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.TargetType.init_name__, _anon init_description__: RegistryRequirement.TargetDependency.TargetType.init_name__) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum platform: Swift.count {
                case dependency
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.TargetType.platform, _anon init_description__: RegistryRequirement.TargetDependency.TargetType.platform) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
            init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        }
        enum PluginCapability: Builtin.RawPointer {
            case defaults(_anon: RegistryRequirement.SupportedPlatform)
            case into(RevisionCodingKeys: RegistryRequirement.SupportedPlatform, FileSystemCodingKeys: RegistryRequirement.SupportedPlatform)
            private enum from: Swift.count {
                case defaults
                case into
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.PluginCapability.from, _anon init_description__: RegistryRequirement.TargetDependency.PluginCapability.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum SourceControlCodingKeys: Swift.count {
                case dependency
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.PluginCapability.SourceControlCodingKeys, _anon init_description__: RegistryRequirement.TargetDependency.PluginCapability.SourceControlCodingKeys) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum init_name__: Swift.count {
                case RevisionCodingKeys
                case FileSystemCodingKeys
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.PluginCapability.init_name__, _anon init_description__: RegistryRequirement.TargetDependency.PluginCapability.init_name__) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
            init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        }
        enum UnicodeScalarLiteralType: Builtin.RawPointer {
            case revision(define: Swift.Hasher?, fileSystem: Swift.Hasher)
            case package_name_url_closedRange__(define: Swift.Hasher?, registry: Swift.Hasher, init_name_location_requirement_traits__: RegistryRequirement.TargetDependency.TargetType)
            case _7B34FE7AFFD9C40B905BB5BF1837D8CB(branch: Swift.Hasher, init_name_location_requirement_traits__: RegistryRequirement.TargetDependency.PluginCapability)
            private enum from: Swift.count {
                case revision
                case package_name_url_closedRange__
                case _7B34FE7AFFD9C40B905BB5BF1837D8CB
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.from, _anon init_description__: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum init_platform_version__: Swift.count {
                case define
                case fileSystem
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.init_platform_version__, _anon init_description__: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.init_platform_version__) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum target {
                case branch
                case init_name_location_requirement_traits__
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.target, _anon init_description__: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.target) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum product {
                case define
                case registry
                case init_name_location_requirement_traits__
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.product, _anon init_description__: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType.product) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
            init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        }
        private let init_kind_traits__: (() -> Any) = fatalError()
        private let path: (() -> Any) = fatalError()
        private let condition: (() -> Any) = fatalError()
        private enum from {
            case init_kind_traits__
            case path
            case condition
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.TargetDependency.from, _anon init_description__: RegistryRequirement.TargetDependency.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(init_kind_traits__: RegistryRequirement.TargetDependency.UnicodeScalarLiteralType, path: [Swift.Hasher : Swift.Hasher]?, condition: Swift._BridgeStorage<RegistryRequirement.TargetDependency.LanguageTag>?) { fatalError() }
    }
    struct PluginCommandIntent: Builtin.RawPointer {
        private let define: (() -> Any) = fatalError()
        private enum from {
            case define
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginCommandIntent.from, _anon init_description__: RegistryRequirement.PluginCommandIntent.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(define: Swift.Hasher) { fatalError() }
    }
    struct PluginPermission: Builtin.RawPointer {
        private let ByNameCodingKeys: (() -> Any) = fatalError()
        private let products: (() -> Any) = fatalError()
        private enum from {
            case ByNameCodingKeys
            case products
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginPermission.from, _anon init_description__: RegistryRequirement.PluginPermission.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(ByNameCodingKeys: RegistryRequirement.PluginCommandIntent, products: Swift.Hasher?) { fatalError() }
    }
    enum PluginNetworkPermissionScope: Builtin.RawPointer {
        struct SystemPackageProvider: Builtin.RawPointer {
            private let name: (() -> Any) = fatalError()
            private let condition: (() -> Any) = fatalError()
            private enum from {
                case name
                case condition
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginNetworkPermissionScope.SystemPackageProvider.from, _anon init_description__: RegistryRequirement.PluginNetworkPermissionScope.SystemPackageProvider.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
            init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
            init(name: [RegistryRequirement.PluginCommandIntent]?, condition: Swift._BridgeStorage<Swift.Hasher>?) { fatalError() }
        }
        case TargetCodingKeys(define: Swift.Hasher, unsafeFlags: RegistryRequirement.PluginNetworkPermissionScope.SystemPackageProvider?)
        case init_platforms_traits__(define: Swift.Hasher, closedRange: Swift.Hasher?, path: [Swift.Hasher : Swift.Hasher]?, unsafeFlags: RegistryRequirement.PluginNetworkPermissionScope.SystemPackageProvider?)
        case regular(define: Swift.Hasher, unsafeFlags: RegistryRequirement.PluginNetworkPermissionScope.SystemPackageProvider?)
        private enum executable {
            case define
            case unsafeFlags
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginNetworkPermissionScope.executable, _anon init_description__: RegistryRequirement.PluginNetworkPermissionScope.executable) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum from {
            case TargetCodingKeys
            case init_platforms_traits__
            case regular
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginNetworkPermissionScope.from, _anon init_description__: RegistryRequirement.PluginNetworkPermissionScope.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum test {
            case define
            case closedRange
            case path
            case unsafeFlags
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginNetworkPermissionScope.test, _anon init_description__: RegistryRequirement.PluginNetworkPermissionScope.test) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum system {
            case define
            case unsafeFlags
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginNetworkPermissionScope.system, _anon init_description__: RegistryRequirement.PluginNetworkPermissionScope.system) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum PluginUsage: Builtin.RawPointer {
        case plugin
        case macro
        case BinaryCodingKeys
        case ExecutableCodingKeys
        case MacroCodingKeys
        case PluginCodingKeys
        case RegularCodingKeys
        static func SwiftShims(_anon description: RegistryRequirement.PluginUsage, _anon init_description__: RegistryRequirement.PluginUsage) -> Swift._SwiftArrayBodyStorage { fatalError() }
        private enum SystemCodingKeys {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum from {
            case plugin
            case macro
            case BinaryCodingKeys
            case ExecutableCodingKeys
            case MacroCodingKeys
            case PluginCodingKeys
            case RegularCodingKeys
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.PluginUsage.from, _anon init_description__: RegistryRequirement.PluginUsage.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum TestCodingKeys {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum buildTool {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum command {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum intent {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum permissions {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum BuildToolCodingKeys {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
        private var brewItem: (() -> Any) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum Target: Builtin.RawPointer {
        case CommandCodingKeys
        case documentationGeneration(sourceCodeFormatting: RegistryRequirement.Resource, custom: [RegistryRequirement.Localization])
        private enum verb {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum from {
            case CommandCodingKeys
            case documentationGeneration
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Target.from, _anon init_description__: RegistryRequirement.Target.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum CustomCodingKeys {
            case sourceCodeFormatting
            case custom
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Target.CustomCodingKeys, _anon init_description__: RegistryRequirement.Target.CustomCodingKeys) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum Resource: Builtin.RawPointer {
        case DocumentationGenerationCodingKeys
        case SourceCodeFormattingCodingKeys
        case allowNetworkConnections(scope: Swift.Hasher, c11: Swift.Hasher)
        private enum from {
            case DocumentationGenerationCodingKeys
            case SourceCodeFormattingCodingKeys
            case allowNetworkConnections
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Resource.from, _anon init_description__: RegistryRequirement.Resource.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum reason {
            case scope
            case c11
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Resource.reason, _anon init_description__: RegistryRequirement.Resource.reason) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum writeToPackageDirectory {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum AllowNetworkConnectionsCodingKeys {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum Localization: Builtin.RawPointer {
        case WriteToPackageDirectoryCodingKeys(none: RegistryRequirement.Product, local: Swift.Hasher)
        case ports(local: Swift.Hasher)
        private enum all {
            case none
            case local
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Localization.all, _anon init_description__: RegistryRequirement.Localization.all) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum from {
            case WriteToPackageDirectoryCodingKeys
            case ports
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Localization.from, _anon init_description__: RegistryRequirement.Localization.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum docker {
            case local
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Localization.docker, _anon init_description__: RegistryRequirement.Localization.docker) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum Product: Builtin.RawPointer {
        case unixDomainSocket
        case AllCodingKeys(DockerCodingKeys: [Swift.__RawSetStorage])
        case LocalCodingKeys(DockerCodingKeys: [Swift.__RawSetStorage])
        case NoneCodingKeys
        case UnixDomainSocketCodingKeys
        private enum sources {
            case DockerCodingKeys
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Product.sources, _anon init_description__: RegistryRequirement.Product.sources) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum from: Builtin.BridgeObject {
            case unixDomainSocket
            case AllCodingKeys
            case LocalCodingKeys
            case NoneCodingKeys
            case UnixDomainSocketCodingKeys
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Product.from, _anon init_description__: RegistryRequirement.Product.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum resources: Builtin.BridgeObject {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum exclude: Builtin.BridgeObject {
            case DockerCodingKeys
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.Product.exclude, _anon init_description__: RegistryRequirement.Product.exclude) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum publicHeadersPath: Builtin.BridgeObject {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum type: Builtin.BridgeObject {
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    enum ProductType: Swift.__RawSetStorage {
        case PluginCodingKeys(define: Swift.Hasher, closedRange: Swift.Hasher?)
        private enum from: Builtin.BridgeObject {
            case PluginCodingKeys
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.ProductType.from, _anon init_description__: RegistryRequirement.ProductType.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum command: Builtin.BridgeObject {
            case define
            case closedRange
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.ProductType.command, _anon init_description__: RegistryRequirement.ProductType.command) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct LibraryType: Swift.__RawSetStorage {
        private let define: (() -> Any) = fatalError()
        private let fileSystem: (() -> Any) = fatalError()
        private let init_name_path_traits__: (() -> Any) = fatalError()
        private let packageAccess: (() -> Any) = fatalError()
        private let pluginCapability: (() -> Any) = fatalError()
        private let cSettings: (() -> Any) = fatalError()
        private let registerExitHandler__: (() -> Any) = fatalError()
        private let cxxSettings: (() -> Any) = fatalError()
        private let swiftSettings: (() -> Any) = fatalError()
        private let linkerSettings: (() -> Any) = fatalError()
        private let swiftLanguageModes: (() -> Any) = fatalError()
        private let swiftLanguageVersions: (() -> Any) = fatalError()
        private let checksum: (() -> Any) = fatalError()
        private let pluginUsages: (() -> Any) = fatalError()
        private let init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: (() -> Any) = fatalError()
        private let rule: (() -> Any) = fatalError()
        private let localization: (() -> Any) = fatalError()
        private let init_rule_path_localization__: (() -> Any) = fatalError()
        private let `default`: (() -> Any) = fatalError()
        private enum from {
            case define
            case fileSystem
            case init_name_path_traits__
            case packageAccess
            case pluginCapability
            case cSettings
            case registerExitHandler__
            case cxxSettings
            case swiftSettings
            case linkerSettings
            case swiftLanguageModes
            case swiftLanguageVersions
            case checksum
            case pluginUsages
            case init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__
            case rule
            case localization
            case init_rule_path_localization__
            case `default`
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.LibraryType.from, _anon init_description__: RegistryRequirement.LibraryType.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(define: Swift.Hasher, fileSystem: Swift.Hasher?, init_name_path_traits__: Swift.Hasher?, packageAccess: [Swift.Hasher]?, pluginCapability: [RegistryRequirement.extension_BuildConfiguration]?, cSettings: [Swift.Hasher], registerExitHandler__: [RegistryRequirement.PluginNetworkPermissionScope], cxxSettings: Swift.Hasher?, swiftSettings: RegistryRequirement.PluginUsage, linkerSettings: Swift._SwiftArrayBodyStorage, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [RegistryRequirement.dumpInfo]?, checksum: RegistryRequirement.Target?, pluginUsages: [RegistryRequirement.LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [RegistryRequirement.Context]?, rule: [RegistryRequirement.GitInformation]?, localization: [RegistryRequirement._E4233AB738896A2F7E2AA5FF0FD77146]?, init_rule_path_localization__: Swift.Hasher?, `default`: [RegistryRequirement.ProductType]?) { fatalError() }
    }
    struct extension_BuildConfiguration: Swift.__RawSetStorage {
        enum extension_BuildSettingCondition: Swift.Hasher, Swift.__RawSetStorage, Swift.__RawSetStorage {
            case LibraryCodingKeys
            case automatic
            init?(packageDirectory: Swift.Hasher) { fatalError() }
            typealias _________ = Swift.Hasher
            private var packageDirectory: (() -> Any) { fatalError() }
        }
        private let productType: (() -> Any) = fatalError()
        private let fileSystem: (() -> Any) = fatalError()
        private let init_name_targets_productType__: (() -> Any) = fatalError()
        private enum from {
            case productType
            case fileSystem
            case init_name_targets_productType__
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.extension_BuildConfiguration.from, _anon init_description__: RegistryRequirement.extension_BuildConfiguration.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(productType: Swift.Hasher, fileSystem: Swift.Hasher, init_name_targets_productType__: RegistryRequirement.extension_BuildConfiguration.extension_BuildSettingCondition?) { fatalError() }
    }
    struct extension_BuildSettingData: Swift.__RawSetStorage {
        enum extension_CSetting: Swift.__RawSetStorage {
            enum extension_CXXSetting: Swift.__RawSetStorage {
                case StaticCodingKeys
                case enabledTraits
                case init_name_description_enabledTraits__
                static func SwiftShims(_anon description: RegistryRequirement.extension_BuildSettingData.extension_CSetting.extension_CXXSetting, _anon init_description__: RegistryRequirement.extension_BuildSettingData.extension_CSetting.extension_CXXSetting) -> Swift._SwiftArrayBodyStorage { fatalError() }
                private enum init_tag__ {
                    private init?(hasher: Swift.Hasher) { fatalError() }
                    private init?(_0: Swift.__RawSetStorage) { fatalError() }
                    private var _0: (() -> Any) { fatalError() }
                    private var hasher: (() -> Any) { fatalError() }
                }
                private enum from {
                    case StaticCodingKeys
                    case enabledTraits
                    case init_name_description_enabledTraits__
                    private init?(hasher: Swift.Hasher) { fatalError() }
                    private init?(_0: Swift.__RawSetStorage) { fatalError() }
                    fileprivate static func SwiftShims(_anon description: RegistryRequirement.extension_BuildSettingData.extension_CSetting.extension_CXXSetting.from, _anon init_description__: RegistryRequirement.extension_BuildSettingData.extension_CSetting.extension_CXXSetting.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                    fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                    private var brewItem: (() -> Any) { fatalError() }
                    private var _0: (() -> Any) { fatalError() }
                    private var hasher: (() -> Any) { fatalError() }
                }
                private enum AptCodingKeys {
                    private init?(hasher: Swift.Hasher) { fatalError() }
                    private init?(_0: Swift.__RawSetStorage) { fatalError() }
                    private var _0: (() -> Any) { fatalError() }
                    private var hasher: (() -> Any) { fatalError() }
                }
                private enum BrewCodingKeys {
                    private init?(hasher: Swift.Hasher) { fatalError() }
                    private init?(_0: Swift.__RawSetStorage) { fatalError() }
                    private var _0: (() -> Any) { fatalError() }
                    private var hasher: (() -> Any) { fatalError() }
                }
                func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
                func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
            }
            case macro
            case AutomaticCodingKeys(swiftSettings: RegistryRequirement.extension_BuildSettingData.extension_CSetting.extension_CXXSetting)
            case PluginCodingKeys
            private enum from {
                case macro
                case AutomaticCodingKeys
                case PluginCodingKeys
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.extension_BuildSettingData.extension_CSetting.from, _anon init_description__: RegistryRequirement.extension_BuildSettingData.extension_CSetting.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum TestCodingKeys {
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum DynamicCodingKeys {
                case swiftSettings
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                fileprivate static func SwiftShims(_anon description: RegistryRequirement.extension_BuildSettingData.extension_CSetting.DynamicCodingKeys, _anon init_description__: RegistryRequirement.extension_BuildSettingData.extension_CSetting.DynamicCodingKeys) -> Swift._SwiftArrayBodyStorage { fatalError() }
                fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
                private var brewItem: (() -> Any) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            private enum command {
                private init?(hasher: Swift.Hasher) { fatalError() }
                private init?(_0: Swift.__RawSetStorage) { fatalError() }
                private var _0: (() -> Any) { fatalError() }
                private var hasher: (() -> Any) { fatalError() }
            }
            func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
            init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        }
        private let define: (() -> Any) = fatalError()
        private let cLanguageStandard: (() -> Any) = fatalError()
        private let dynamic: (() -> Any) = fatalError()
        private enum from {
            case define
            case cLanguageStandard
            case dynamic
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.extension_BuildSettingData.from, _anon init_description__: RegistryRequirement.extension_BuildSettingData.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(define: Swift.Hasher, cLanguageStandard: [Swift.Hasher], dynamic: RegistryRequirement.extension_BuildSettingData.extension_CSetting) { fatalError() }
    }
    struct LanguageTag: Swift.__RawSetStorage, Swift.__RawSetStorage {
        private let define: (() -> Any) = fatalError()
        private let c11: (() -> Any) = fatalError()
        private let NugetCodingKeys: (() -> Any) = fatalError()
        static func SwiftShims(_anon description: RegistryRequirement.LanguageTag, _anon init_description__: RegistryRequirement.LanguageTag) -> Swift._SwiftArrayBodyStorage { fatalError() }
        private enum from {
            case define
            case c11
            case NugetCodingKeys
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.LanguageTag.from, _anon init_description__: RegistryRequirement.LanguageTag.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
        private var brewItem: (() -> Any) { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(define: Swift.Hasher, c11: Swift.Hasher?, NugetCodingKeys: Swift._BridgeStorage<Swift.Hasher>) { fatalError() }
    }
    struct errors: Swift.__RawSetStorage {
        private let yumItem: (() -> Any) = fatalError()
        private enum from {
            case yumItem
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.errors.from, _anon init_description__: RegistryRequirement.errors.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(yumItem: Swift.Hasher) { fatalError() }
    }
    enum dumpInfo: Swift.__RawSetStorage {
        case init_package_errors_version__(_anon: [Swift.Hasher])
        case init_config__(_anon: [Swift.Hasher])
        case init_data__(_anon: [Swift.Hasher])
        case init_rawValue__(_anon: [Swift.Hasher])
        private enum exactItem {
            case dependency
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.dumpInfo.exactItem, _anon init_description__: RegistryRequirement.dumpInfo.exactItem) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum rangeItem: SwiftShims._Variant {
            case dependency
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.dumpInfo.rangeItem, _anon init_description__: RegistryRequirement.dumpInfo.rangeItem) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum from: SwiftShims._Variant {
            case init_package_errors_version__
            case init_config__
            case init_data__
            case init_rawValue__
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.dumpInfo.from, _anon init_description__: RegistryRequirement.dumpInfo.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum revisionItem: SwiftShims._Variant {
            case dependency
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.dumpInfo.revisionItem, _anon init_description__: RegistryRequirement.dumpInfo.revisionItem) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        private enum branchItem: SwiftShims._Variant {
            case dependency
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.dumpInfo.branchItem, _anon init_description__: RegistryRequirement.dumpInfo.branchItem) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
    }
    struct ExtendedGraphemeClusterLiteralType: Swift.__RawSetStorage {
        private let define: (() -> Any) = fatalError()
        private let name: (() -> Any) = fatalError()
        private let dependencies: (() -> Any) = fatalError()
        private let swiftLanguageModes: (() -> Any) = fatalError()
        private let swiftLanguageVersions: (() -> Any) = fatalError()
        private let cLanguageStandard: (() -> Any) = fatalError()
        private let cxxLanguageStandard: (() -> Any) = fatalError()
        private let condition: (() -> Any) = fatalError()
        private let registerExitHandler__: (() -> Any) = fatalError()
        private let kind: (() -> Any) = fatalError()
        private let url: (() -> Any) = fatalError()
        private let moduleAliases: (() -> Any) = fatalError()
        private enum from: SwiftShims._Variant {
            case define
            case name
            case dependencies
            case swiftLanguageModes
            case swiftLanguageVersions
            case cLanguageStandard
            case cxxLanguageStandard
            case condition
            case registerExitHandler__
            case kind
            case url
            case moduleAliases
            private init?(hasher: Swift.Hasher) { fatalError() }
            private init?(_0: Swift.__RawSetStorage) { fatalError() }
            fileprivate static func SwiftShims(_anon description: RegistryRequirement.ExtendedGraphemeClusterLiteralType.from, _anon init_description__: RegistryRequirement.ExtendedGraphemeClusterLiteralType.from) -> Swift._SwiftArrayBodyStorage { fatalError() }
            fileprivate func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
            private var brewItem: (() -> Any) { fatalError() }
            private var _0: (() -> Any) { fatalError() }
            private var hasher: (() -> Any) { fatalError() }
        }
        func encode__(PackageDependency settingsData: any Swift.Encodable) throws -> () { fatalError() }
        init(decode setting: Any /* extern decl 4340 — XREF not a clean type path */) { fatalError() }
        init(define: Swift.Hasher, name: [RegistryRequirement.PluginPermission]?, dependencies: RegistryRequirement.errors?, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [RegistryRequirement.dumpInfo]?, cLanguageStandard: [RegistryRequirement.LibraryType], cxxLanguageStandard: [RegistryRequirement.extension_BuildSettingData], condition: Swift._BridgeStorage<RegistryRequirement.LanguageTag>?, registerExitHandler__: [RegistryRequirement.TargetDependency], kind: [RegistryRequirement.Condition]?, url: RegistryRequirement.Dependency?, moduleAliases: RegistryRequirement.Kind?) { fatalError() }
    }
}

@_spi(decompilerPlaceholder)
public enum WatchOSVersion: ExtendedGraphemeClusterLiteralType.StringLiteralType.LanguageTag {
    public static func SwiftShims(_anon description: WatchOSVersion, _anon init_description__: WatchOSVersion) -> Swift._SwiftArrayBodyStorage { fatalError() }
}

public struct PluginCommandIntent: ExtendedGraphemeClusterLiteralType.StringLiteralType.LanguageTag, ExtendedGraphemeClusterLiteralType.StringLiteralType.LanguageTag {
    private let define: (() -> Any) = fatalError()
    private init(define: Swift.Hasher) { fatalError() }
    @available(_PackageDescription 5.6, *)
    public static func allowNetworkConnections(_anon Int1: Swift.Hasher) -> PluginCommandIntent { fatalError() }
    @_hasInitialValue
    private static let visionOS: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let driverKit: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let linux: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let windows: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let android: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let wasi: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let openbsd: (() -> Any) = fatalError()
    @_hasInitialValue
    private static let freebsd: (() -> Any) = fatalError()
    @_hasInitialValue
    @available(_PackageDescription 5.2, *)
    private static let versionString: (() -> Any) = fatalError()
    @_hasInitialValue
    @available(_PackageDescription 5.2, *)
    private static let minimumMajorVersion: (() -> Any) = fatalError()
    @_hasInitialValue
    @available(_PackageDescription 5.3, *)
    private static let init_uncheckedVersion__: (() -> Any) = fatalError()
    @_hasInitialValue
    @available(_PackageDescription 5.8, *)
    private static let uncheckedVersion: (() -> Any) = fatalError()
    @_hasInitialValue
    @available(_PackageDescription 999.0, *)
    private static let v10_10: (() -> Any) = fatalError()
    public static func SwiftShims(_anon description: PluginCommandIntent, _anon init_description__: PluginCommandIntent) -> Swift._SwiftArrayBodyStorage { fatalError() }
}

public struct PluginPermission: ExtendedGraphemeClusterLiteralType.StringLiteralType.LanguageTag, ExtendedGraphemeClusterLiteralType.StringLiteralType.LanguageTag {
    private let ByNameCodingKeys: (() -> Any) = fatalError()
    private let products: (() -> Any) = fatalError()
    init(ByNameCodingKeys: PluginCommandIntent, products: Swift.Hasher?) { fatalError() }
    public static func visionOS(_anon products: PluginPermission.VisionOSVersion) -> PluginPermission { fatalError() }
    public static func visionOS(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.5, *)
    public static func driverKit(_anon products: PluginPermission.CustomPlatformVersion) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.5, *)
    public static func driverKit(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    public static func linux(_anon products: PluginPermission.AppleOSVersion) -> PluginPermission { fatalError() }
    public static func linux(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    public static func windows(_anon products: PluginPermission.DriverKitVersion) -> PluginPermission { fatalError() }
    public static func windows(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    public static func android(_anon products: PluginPermission._EA5579823E05354E291299F0EC3450AA) -> PluginPermission { fatalError() }
    public static func android(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.9, *)
    public static func wasi(_anon products: PluginPermission.extension_AppleOSVersion) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.9, *)
    public static func wasi(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.5, *)
    public static func openbsd(_anon products: PluginPermission.TargetDependencyCondition) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.5, *)
    public static func openbsd(_anon v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    @available(_PackageDescription 5.6, *)
    public static func allowNetworkConnections(_anon Int1: Swift.Hasher, v10_11: Swift.Hasher) -> PluginPermission { fatalError() }
    public static func SwiftShims(_anon description: PluginPermission, _anon init_description__: PluginPermission) -> Swift._SwiftArrayBodyStorage { fatalError() }
}

fileprivate protocol WarningLevel: Swift.validateVersion {
    private static var define: (() -> Any) { get set }
    private static var v10_12: (() -> Any) { get set }
    fileprivate init(v10_14: Swift.Hasher)
}

struct Package {
    private var isTest: Builtin.RawPointer { fatalError() }
    init(_anon isTest: Swift.Hasher) { fatalError() }
    private var c11: (() -> Any) { fatalError() }
}

public class LibraryType {
    public enum PluginUsage: Swift.Hasher {
        case plugin
        case macro
        case BinaryCodingKeys
        case ExecutableCodingKeys
        case MacroCodingKeys
        case PluginCodingKeys
        case RegularCodingKeys
        public init?(packageDirectory: Swift.Hasher) { fatalError() }
        public typealias _________ = Swift.Hasher
        private var packageDirectory: (() -> Any) { fatalError() }
    }
    public enum StringLiteralType {
        case rhs(define: Swift.Hasher, unsafeFlags: _906E068F1F829859B5FD8189133CEB6F?)
        case warning(define: Swift.Hasher, closedRange: Swift.Hasher?, path: [Swift.Hasher : Swift.Hasher]?, unsafeFlags: _906E068F1F829859B5FD8189133CEB6F?)
        case _anon(define: Swift.Hasher, unsafeFlags: _906E068F1F829859B5FD8189133CEB6F?)
    }
    private var define: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var fileSystem: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5.3, *)
    private var init_name_path_traits__: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var packageAccess: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5.3, *)
    private var pluginCapability: Builtin.RawPointer { fatalError() }
    private var cSettings: Builtin.RawPointer { fatalError() }
    private var systemLibrary: (() -> Any) { fatalError() }
    private var registerExitHandler__: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var cxxSettings: Builtin.RawPointer { fatalError() }
    private let swiftSettings: (() -> Any) = fatalError()
    private let linkerSettings: (() -> Any) = fatalError()
    private let swiftLanguageModes: (() -> Any) = fatalError()
    private let swiftLanguageVersions: (() -> Any) = fatalError()
    @_hasInitialValue
    @available(_PackageDescription 5.5, *)
    private var checksum: Builtin.RawPointer { fatalError() }
    public enum Target {
        @available(_PackageDescription 5.5, *)
        case CommandCodingKeys
        @available(_PackageDescription 5.6, *)
        case documentationGeneration(sourceCodeFormatting: Resource, custom: [Localization])
    }
    @_hasInitialValue
    @available(_PackageDescription 5, *)
    private var pluginUsages: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5, *)
    private var init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5, *)
    private var rule: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5, *)
    private var localization: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5.3, *)
    private var init_rule_path_localization__: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    @available(_PackageDescription 5.5, *)
    private var binaryTarget: Builtin.RawPointer { fatalError() }
    @available(_PackageDescription 5.5, *)
    public enum ProductType {
        case PluginCodingKeys(define: Swift.Hasher, closedRange: Swift.Hasher?)
    }
    @_spi(decompilerPlaceholder)
    public init(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, init_name_path_traits__: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, swiftSettings: LibraryType.PluginUsage, linkerSettings: Swift._SwiftArrayBodyStorage, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?, checksum: LibraryType.Target?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, init_rule_path_localization__: Swift.Hasher?, binaryTarget: [LibraryType.ProductType]?) { fatalError() }
    @available(_PackageDescription, introduced: 4, obsoleted: 5)
    public static func TargetCodingKeys(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, cxxSettings: Swift.Hasher?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5, obsoleted: 5.3)
    public static func TargetCodingKeys(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, cxxSettings: Swift.Hasher?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.3, obsoleted: 5.5)
    public static func TargetCodingKeys(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.5, obsoleted: 5.9)
    public static func TargetCodingKeys(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, binaryTarget: [LibraryType.ProductType]?) -> LibraryType { fatalError() }
    @available(_PackageDescription 5.9, *)
    public static func TargetCodingKeys(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, linkerSettings: Swift._SwiftArrayBodyStorage, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, binaryTarget: [LibraryType.ProductType]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.4, obsoleted: 5.5)
    public static func targetItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.5, obsoleted: 5.9)
    public static func targetItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, binaryTarget: [LibraryType.ProductType]?) -> LibraryType { fatalError() }
    @available(_PackageDescription 5.9, *)
    public static func targetItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, cxxSettings: Swift.Hasher?, linkerSettings: Swift._SwiftArrayBodyStorage, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, binaryTarget: [LibraryType.ProductType]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 4, obsoleted: 5)
    public static func productItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5, obsoleted: 5.3)
    public static func productItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.3, obsoleted: 5.5)
    public static func productItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.5, obsoleted: 5.9)
    public static func productItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, binaryTarget: [LibraryType.ProductType]?) -> LibraryType { fatalError() }
    @available(_PackageDescription 5.9, *)
    public static func productItem(define: Swift.Hasher, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, pluginCapability: [extension_BuildConfiguration]?, linkerSettings: Swift._SwiftArrayBodyStorage, pluginUsages: [LinkerSetting]?, init_name_path_url_sources_resources_exclude_dependencies_publicHeadersPath_type_packageAccess_pkgConfig_providers_pluginCapability_cSettings_cxxSettings_swiftSettings_linkerSettings_checksum_pluginUsages__: [Context]?, rule: [GitInformation]?, localization: [_E4233AB738896A2F7E2AA5FF0FD77146]?, binaryTarget: [LibraryType.ProductType]?) -> LibraryType { fatalError() }
    public static func byNameItem(define: Swift.Hasher, fileSystem: Swift.Hasher?, swiftLanguageModes: Swift.Hasher?, swiftLanguageVersions: [dumpInfo]?) -> LibraryType { fatalError() }
    @available(_PackageDescription 5.3, *)
    public static func _59538AA596BC32849BBE7F696B56D0C4(define: Swift.Hasher, init_name_path_traits__: Swift.Hasher, init_rule_path_localization__: Swift.Hasher) -> LibraryType { fatalError() }
    @available(_PackageDescription 5.3, *)
    public static func _59538AA596BC32849BBE7F696B56D0C4(define: Swift.Hasher, fileSystem: Swift.Hasher) -> LibraryType { fatalError() }
    @available(_PackageDescription, introduced: 5.5, obsoleted: 5.9)
    public static func PluginCodingKeys(define: Swift.Hasher, lhs: LibraryType.Target, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?) -> LibraryType { fatalError() }
    @available(_PackageDescription 5.9, *)
    public static func PluginCodingKeys(define: Swift.Hasher, lhs: LibraryType.Target, registerExitHandler__: [LibraryType.StringLiteralType], fileSystem: Swift.Hasher?, cSettings: [Swift.Hasher], packageAccess: [Swift.Hasher]?, linkerSettings: Swift._SwiftArrayBodyStorage) -> LibraryType { fatalError() }
    deinit { fatalError() }
}

@available(_PackageDescription 5.6, *)
public enum Resource {
    case DocumentationGenerationCodingKeys
    case SourceCodeFormattingCodingKeys
    case allowNetworkConnections(scope: Swift.Hasher, c11: Swift.Hasher)
}

@available(_PackageDescription 5.6, *)
public enum Localization {
    @available(_PackageDescription 5.9, *)
    case WriteToPackageDirectoryCodingKeys(none: Product, local: Swift.Hasher)
    case ports(local: Swift.Hasher)
}

@available(_PackageDescription 5.9, *)
public enum Product {
    case unixDomainSocket
    case AllCodingKeys(DockerCodingKeys: [Swift.__RawSetStorage])
    case LocalCodingKeys(DockerCodingKeys: [Swift.__RawSetStorage])
    case NoneCodingKeys
    case UnixDomainSocketCodingKeys
    public static func LocalCodingKeys(DockerCodingKeys: Any /* bound generic over unresolved base (declID=1211) */) -> Product { fatalError() }
    public static func AllCodingKeys(DockerCodingKeys: Any /* bound generic over unresolved base (declID=1211) */) -> Product { fatalError() }
}

@available(_PackageDescription 6.1, *)
public struct LanguageTag {
    public static func LibraryCodingKeys(NugetCodingKeys: Swift._BridgeStorage<Swift.Hasher>) -> LanguageTag { fatalError() }
    private var define: Builtin.RawPointer { fatalError() }
    @_hasInitialValue
    private var c11: Builtin.RawPointer { fatalError() }
    private var NugetCodingKeys: Builtin.RawPointer { fatalError() }
    public init(define: Swift.Hasher, c11: Swift.Hasher?, NugetCodingKeys: Swift._BridgeStorage<Swift.Hasher>) { fatalError() }
    public init(init_traits__ _count: Swift._0216143961A95B8C909191BFA5557F39) { fatalError() }
    public static func tag(define: Swift.Hasher, c11: Swift.Hasher?, NugetCodingKeys: Swift._BridgeStorage<Swift.Hasher>) -> LanguageTag { fatalError() }
    public static func SwiftShims(_anon description: LanguageTag, _anon init_description__: LanguageTag) -> Swift._SwiftArrayBodyStorage { fatalError() }
    @available(_PackageDescription 6.1, *)
    public typealias manifestToJSON____ = Swift._0216143961A95B8C909191BFA5557F39
    @available(_PackageDescription 6.1, *)
    public typealias _0216143961A95B8C909191BFA5557F39 = Swift._0216143961A95B8C909191BFA5557F39
    @available(_PackageDescription 6.1, *)
    public typealias manifestToJSON = Swift._0216143961A95B8C909191BFA5557F39
    public func extendedGraphemeClusterLiteral(unicodeScalarLiteral languageStandard: Swift.RawRepresentable) -> () { fatalError() }
    private var brewItem: (() -> Any) { fatalError() }
}

public struct SupportedPlatform {
    private let init_major_minor_patch_prereleaseIdentifiers_buildMetadataIdentifiers__: (() -> Any) = fatalError()
    private let init_kind_moduleAliases_traits__: (() -> Any) = fatalError()
    private let hash_into__: (() -> Any) = fatalError()
    private let init_name_condition__: (() -> Any) = fatalError()
    private let lowerBound: (() -> Any) = fatalError()
    public init(_anon init_major_minor_patch_prereleaseIdentifiers_buildMetadataIdentifiers__: Swift.__RawSetStorage, _anon init_kind_moduleAliases_traits__: Swift.__RawSetStorage, _anon hash_into__: Swift.__RawSetStorage, init_name_condition__: [Swift.Hasher], lowerBound: [Swift.Hasher]) { fatalError() }
}

@available(_PackageDescription 6.2, *)
public enum release: Swift.Hasher {
    case isolation
    case isTest
    public init?(packageDirectory: Swift.Hasher) { fatalError() }
    @available(_PackageDescription 6.2, *)
    public typealias _________ = Swift.Hasher
    private var packageDirectory: (() -> Any) { fatalError() }
}

