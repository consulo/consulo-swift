/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.swift.language.impl.parsing;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.swift.bitcode.swiftmodule.SwiftModuleInfo;
import consulo.swift.bitcode.swiftmodule.SwiftModuleReader;
import consulo.swift.bitcode.swiftmodule.SwiftModuleSearchPath;
import consulo.swift.bitcode.swiftmodule.SwiftModuleSourcePrinter;
import consulo.swift.language.SwiftFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>Passive</b> stress test: decompiles the real
 * {@code Swift.swiftmodule} (the stdlib) from the system Swift toolchain
 * and runs the synthesized 600+ KB output through our PSI parser. Counts
 * {@link PsiErrorElement}s and asserts the total stays under a regression
 * budget.
 *
 * <p>Companion to {@code SwiftNativeValidationTest.swiftStdlibSynthesizedOutputPassesSwiftcParse}
 * in the {@code swift-bitcode} module — that one verifies the native
 * compiler accepts the same text; this one verifies our own parser does
 * too. Together they pin both sides of the decompiler↔parser contract on
 * the largest .swiftmodule in the wild.
 *
 * <p>Gated behind {@code -Dswift.bitcode.native=true} because it requires
 * a Swift toolchain installed on the host. Skipped (assumption failure)
 * when no {@code Swift.swiftmodule} is on the default search path.
 *
 * <pre>
 *   mvn test -Dswift.bitcode.native=true -Dtest=SwiftStdlibDecompiledParsingTest
 * </pre>
 */
@EnabledIfSystemProperty(named = "swift.bitcode.native", matches = "true")
public class SwiftStdlibDecompiledParsingTest extends SimpleParsingTest<Object> {

    /**
     * Cap on tolerated {@code PsiErrorElement} count. Extraction is still
     * maturing, so we accept a generous floor and ratchet down over time;
     * the assertion is here to catch sudden regressions, not require
     * zero-defect output on day one.
     */
    private static final int MAX_ERROR_BUDGET = 5000;

    /** Cap on how many error excerpts we dump on failure. */
    private static final int MAX_ERRORS_REPORTED = 25;

    public SwiftStdlibDecompiledParsingTest() {
        super("stdlib", "swift");
    }

    @RequiredReadAction
    @Test
    public void testStdlibDecompileParsesThroughPsi(Context context) throws Exception {
        Path swiftBinary = SwiftModuleSearchPath.system().resolve("Swift");
        Assumptions.assumeTrue(swiftBinary != null && Files.exists(swiftBinary),
            "Swift.swiftmodule not on system search path — install Swift toolchain to run");

        SwiftModuleInfo info;
        try (InputStream in = Files.newInputStream(swiftBinary)) {
            // 128 MiB cap explicit — the default is 256 MiB now but be
            // explicit so this test's intent is obvious.
            info = SwiftModuleReader.open(in, 128 * 1024 * 1024).readModuleInfo();
        }
        assertEquals("Swift", info.getModuleName(),
            "CONTROL block should self-identify as Swift");

        String synthesized = SwiftModuleSourcePrinter.render(info);
        // Persist the decompiled stdlib next to the test resources so it's
        // easy to inspect / diff after a run. We don't commit the file
        // (it's toolchain-version-specific — Swift 5.10 vs 6.3 produce
        // different sets of decls), but having it on disk makes triaging
        // PSI / swiftc errors much easier than regenerating from memory
        // every time.
        Path outDir = Path.of("target");
        Files.createDirectories(outDir);
        Path snapshot = outDir.resolve("Swift.decompiled.swift");
        Files.writeString(snapshot, synthesized);
        System.out.println("=== Swift.swiftmodule decompile ===");
        System.out.println("  path:      " + swiftBinary);
        System.out.println("  rendered:  " + synthesized.length() + " chars");
        System.out.println("  written:   " + snapshot.toAbsolutePath());

        // Feed straight into the PSI parser. We bypass the snapshot/.txt
        // comparison path entirely — we only care whether the parser
        // chokes anywhere.
        PsiFile file = createFile(context, null, "Swift.synthesized.swift",
            getFileType(context, null), synthesized);
        int[] errors = countErrors(file);
        System.out.println("  PSI errors: " + errors[0]);
        if (errors[0] > 0) {
            // Dump a sample so the failure log is actionable but bounded.
            int[] shownCount = { 0 };
            file.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitErrorElement(@Nonnull PsiErrorElement err) {
                    if (shownCount[0]++ < MAX_ERRORS_REPORTED) {
                        int off = err.getTextRange().getStartOffset();
                        int line = lineAt(synthesized, off);
                        String near = excerpt(synthesized, off, 80);
                        System.out.println("  line " + line + ": "
                            + err.getErrorDescription() + "  // " + near);
                    }
                    super.visitErrorElement(err);
                }
            });
        }
        assertTrue(errors[0] < MAX_ERROR_BUDGET,
            "PSI produced " + errors[0] + " errors on decompiled stdlib "
                + "(budget: " + MAX_ERROR_BUDGET + "). Tighten the budget "
                + "once the extractor improves further; loosen only with a "
                + "specific reason.");
    }

    private static int[] countErrors(@Nonnull PsiFile file) {
        int[] count = { 0 };
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitErrorElement(@Nonnull PsiErrorElement element) {
                count[0]++;
                super.visitErrorElement(element);
            }
        });
        return count;
    }

    private static int lineAt(@Nonnull String text, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static String excerpt(@Nonnull String text, int offset, int width) {
        int start = Math.max(0, offset - 10);
        int end = Math.min(text.length(), offset + width);
        return text.substring(start, end).replace('\n', ' ').replace('\r', ' ');
    }

    @Nonnull
    @Override
    protected LanguageFileType getFileType(@Nonnull Context context, @Nullable Object o) {
        return SwiftFileType.INSTANCE;
    }

    @Override
    protected void checkResult(@Nonnull Context context, @Nullable Object testContext, PsiFile file) {
        // No-op: assertions live in the test body, not in a .txt diff.
    }
}
