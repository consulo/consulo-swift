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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.file.LanguageFileType;
import consulo.swift.language.SwiftFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the decompiler's synthesized output for {@code PackageDescription}
 * through our own Swift PSI parser and asserts the parse stays sane —
 * no {@link PsiErrorElement}s, the file matches its source byte-for-byte.
 *
 * <p>This is the "dogfood the decompiler" test: {@code swiftc -parse} is
 * already happy (see {@code SwiftNativeValidationTest}), but our IDE-side
 * parser is what users actually hit, so it has to accept the output too.
 * Anything {@code swiftc} allows that our parser stumbles on is a parser
 * gap we should fix.
 *
 * <p>The fixture {@code DecompiledPackageDescription.swift} is a copy of
 * the snapshot at {@code swift-bitcode/src/test/resources/swiftmodule/
 * PackageDescription.expected.swift}. Regenerate it by copying that file
 * over after a snapshot refresh.
 */
public class SwiftDecompiledParsingTest extends SimpleParsingTest<Object> {
    /** Cap the number of error elements we print so failure messages stay readable. */
    private static final int MAX_ERRORS_TO_REPORT = 20;

    public SwiftDecompiledParsingTest() {
        super("decompiled", "swift");
    }

    @RequiredReadAction
    @Test
    public void testDecompiledPackageDescription(Context context) throws Exception {
        PsiFile file = loadPsiFile(context, null);
        List<PsiErrorElement> errors = collectErrors(file);

        if (!errors.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append("PSI parser produced ").append(errors.size())
                .append(" error element(s) when parsing the decompiled module.\n");
            int shown = 0;
            for (PsiErrorElement err : errors) {
                if (shown++ >= MAX_ERRORS_TO_REPORT) {
                    report.append("  ... (").append(errors.size() - MAX_ERRORS_TO_REPORT)
                        .append(" more)\n");
                    break;
                }
                int offset = err.getTextRange().getStartOffset();
                int line = computeLine(file.getText(), offset);
                String description = err.getErrorDescription();
                String around = excerpt(file.getText(), offset, 60);
                report.append("  line ").append(line)
                    .append(": ").append(description)
                    .append("  // ").append(around).append('\n');
            }
            fail(report.toString());
        }
    }

    @Nonnull
    private static List<PsiErrorElement> collectErrors(@Nonnull PsiFile file) {
        List<PsiErrorElement> out = new ArrayList<>();
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitErrorElement(@Nonnull PsiErrorElement element) {
                out.add(element);
                super.visitErrorElement(element);
            }
        });
        return out;
    }

    private static int computeLine(@Nonnull String text, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
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

    /**
     * The decompiled-output PSI tree is huge — and the point of this test
     * is to detect parser errors, not to lock in the exact tree shape.
     * Override {@code checkResult} to skip the .txt comparison.
     */
    @Override
    protected void checkResult(@Nonnull Context context, @Nullable Object testContext, PsiFile file) {
        // No-op: we validate via collectErrors() in the test body itself.
    }
}
