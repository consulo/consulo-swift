# swift-bitcode

LLVM-bitstream reader + Swift `.swiftmodule` parser.

**License:** MIT — see [LICENSE.txt](./LICENSE.txt).

This is the only module of consulo-swift distributed under the MIT license;
the rest of the repository is Apache 2.0. MIT is used here to match the
upstream license of the vendored bitstream-reader code, keeping the whole
module under a single license.

## Layout

| Package | Origin | Purpose |
|---|---|---|
| `uk.ac.man.cs.llvm.bc` | vendored from [beehive-lab/llvm-bcj](https://github.com/beehive-lab/llvm-bcj) (MIT) | generic LLVM bitstream reader (variable-width integers, blocks, records, abbreviations). Format-agnostic. |
| `consulo.swift.bitcode.swiftmodule` | consulo.io | Swift `.swiftmodule` parser layered on top — signature check, block/record schema, decl extraction. |

Only the generic `bc/` portion of llvm-bcj is vendored. The upstream
`ir/` package (an LLVM-IR-specific decoder) is omitted because Swift's
`.swiftmodule` files use the same bitstream container but a completely
different block/record schema.

The vendored files keep their original package path and per-file MIT
copyright header unchanged.

## No external dependencies

Pure Java. Reads `.swiftmodule` directly without invoking the Swift
toolchain or any native code.

## Tests

The standard `mvn test` run is offline-only. It executes the unit and
golden-file tests; no native tools are touched.

Two extra suites are gated behind system properties so they only run on
demand:

### Snapshot regeneration

`SwiftModuleSnapshotTest` compares the synthesized output for the bundled
`PackageDescription.swiftmodule` fixture against a stored stub at
`src/test/resources/swiftmodule/PackageDescription.expected.swift`.
When you intentionally change the printer or the extractor, regenerate
the stub with:

```sh
mvn test -Dswift.bitcode.regenerate=true -Dtest=SwiftModuleSnapshotTest
```

The test will overwrite the file with the current renderer's output and
pass. Inspect the diff before committing.

### Native Swift validation

`SwiftNativeValidationTest` is the cross-check that requires a Swift
toolchain (`swift` + `swiftc`) on `PATH`. It is **skipped by default**
because CI environments rarely have one installed. To run it manually:

```sh
mvn test -Dswift.bitcode.native=true -Dtest=SwiftNativeValidationTest
```

The class runs two passive checks:

1. **`swiftc -parse`** — pipes the synthesized `.swift` text through
   the real Swift parser and asserts it accepts the file. This is the
   strongest "does our decompiler emit valid Swift?" smoke test.
2. **`swift symbolgraph-extract`** — invokes Apple's native symbol-graph
   extractor on the same `.swiftmodule` fixture and confirms that the
   well-known SPM symbols (`Package`, `Product`, `Target`) show up in
   the native output, then cross-checks that our extracted decl names
   appear in the native graph above a sanity floor (30%).

If you're working on the extractor or printer, running both gates is a
good habit before pushing a change.
