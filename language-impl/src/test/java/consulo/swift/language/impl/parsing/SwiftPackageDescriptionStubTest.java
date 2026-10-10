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
import consulo.language.ast.IElementType;
import consulo.language.file.LanguageFileType;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.util.IncorrectOperationException;
import consulo.swift.language.SwiftFileType;
import consulo.swift.language.impl.SwiftLexer;
import consulo.test.junit.impl.language.SimpleParsingTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Active tests against the stored {@code PackageDescription.expected.swift}
 * snapshot. Two contracts on the same file:
 *
 * <ol>
 *   <li><b>Lexer continuity</b> — every byte of the snapshot must belong to
 *       some emitted token (or whitespace). Discontinuities crash Consulo's
 *       editor with {@code "Discontinuous sequence of tokens"} from
 *       {@code ValidatingLexerWrapper}. Catching it here means the same
 *       breakage never reaches a user opening a decompiled file.</li>
 *   <li><b>PSI parse cleanliness</b> — the snapshot must produce zero
 *       {@link PsiErrorElement}s. Companion to
 *       {@link SwiftDecompiledParsingTest}, but with a sharper
 *       fail-on-non-zero assertion instead of a budget.</li>
 * </ol>
 *
 * <p>This is the snapshot that the {@code swift-bitcode} regenerate flow
 * produces from the bundled {@code PackageDescription.swiftmodule} fixture;
 * copy it over after a snapshot refresh.
 */
public class SwiftPackageDescriptionStubTest extends SimpleParsingTest<Object> {

    private static final String SNAPSHOT_RESOURCE =
        "/decompiled/PackageDescription.expected.swift";

    /** Classpath location of the stored PSI parse-tree dump. */
    private static final String PSI_SNAPSHOT_RESOURCE =
        "/decompiled/PackageDescription.expected.txt";

    /** Filesystem path the regenerate flag writes the PSI snapshot to. */
    private static final String PSI_SNAPSHOT_FS_PATH =
        "src/test/resources/decompiled/PackageDescription.expected.txt";

    public SwiftPackageDescriptionStubTest() {
        super("decompiled", "swift");
    }

    @Test
    public void testLexerProducesContinuousTokenStream() throws Exception {
        String source = loadSnapshot();
        Lexer lexer = new SwiftLexer();
        lexer.start(source);
        int expectedStart = 0;
        int tokenCount = 0;
        List<String> gaps = new ArrayList<>();
        IElementType type;
        while ((type = lexer.getTokenType()) != null) {
            int actualStart = lexer.getTokenStart();
            int actualEnd = lexer.getTokenEnd();
            if (actualStart != expectedStart) {
                if (gaps.size() < 5) {
                    int near = Math.max(0, expectedStart - 20);
                    int far = Math.min(source.length(), actualEnd + 20);
                    String excerpt = source.substring(near, far)
                        .replace("\n", "\\n");
                    gaps.add("token #" + tokenCount + " (" + type + ") "
                        + "starts at " + actualStart + " but previous ended at "
                        + expectedStart + " — gap of " + (actualStart - expectedStart)
                        + " byte(s). context: \"" + excerpt + "\"");
                }
            }
            expectedStart = actualEnd;
            tokenCount++;
            lexer.advance();
        }
        if (!gaps.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append("Lexer emitted a DISCONTINUOUS token stream — every byte must "
                + "be covered by some token. Editor highlighter (ValidatingLexerWrapper) "
                + "rejects this. First ").append(gaps.size()).append(" gap(s):\n");
            for (String g : gaps) {
                report.append("  ").append(g).append('\n');
            }
            fail(report.toString());
        }
        // Sanity: snapshot should produce thousands of tokens.
        assertTrue(tokenCount > 1000,
            "Snapshot has " + tokenCount + " tokens — too few, lexer didn't read the file?");
    }

    @RequiredReadAction
    @Test
    public void testSnapshotParsesWithoutPsiErrors(Context context) throws Exception {
        String source = loadSnapshot();
        PsiFile file = createFile(context, null,
            "PackageDescription.expected.swift",
            getFileType(context, null),
            source);
        List<PsiErrorElement> errors = new ArrayList<>();
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitErrorElement(@Nonnull PsiErrorElement element) {
                errors.add(element);
                super.visitErrorElement(element);
            }
        });
        if (!errors.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append(errors.size())
                .append(" PSI error(s) parsing PackageDescription.expected.swift:\n");
            int shown = 0;
            for (PsiErrorElement err : errors) {
                if (shown++ >= 10) {
                    report.append("  ... (").append(errors.size() - 10).append(" more)\n");
                    break;
                }
                int off = err.getTextRange().getStartOffset();
                int line = lineAt(source, off);
                int s = Math.max(0, off - 10);
                int e = Math.min(source.length(), off + 70);
                report.append("  line ").append(line).append(": ")
                    .append(err.getErrorDescription()).append("  // ")
                    .append(source.substring(s, e).replace('\n', ' '))
                    .append('\n');
            }
            fail(report.toString());
        }
    }

    /**
     * Dump the full PSI tree for the snapshot and compare against the
     * stored {@code PackageDescription.expected.txt} fixture. Pairs with
     * the source-snapshot file the way the standard parsing tests pair
     * {@code Foo.swift} ↔ {@code Foo.txt}.
     *
     * <p>Regenerate the {@code .txt} after a meaningful printer or
     * grammar change:
     * <pre>
     *   mvn test -Dswift.bitcode.regenerate=true \
     *     -Dtest=SwiftPackageDescriptionStubTest#testSnapshotPsiTreeMatches
     * </pre>
     */
    @RequiredReadAction
    @Test
    public void testSnapshotPsiTreeMatches(Context context) throws Exception {
        String source = loadSnapshot();
        PsiFile file = createFile(context, null,
            "PackageDescription.expected.swift",
            getFileType(context, null),
            source);
        String actual = renderTree(file).trim();

        if (Boolean.getBoolean("swift.bitcode.regenerate")) {
            Path target = Path.of(PSI_SNAPSHOT_FS_PATH);
            // Try the module-rooted path first (running from language-impl),
            // fall back to repo-relative (running from project root).
            if (!Files.exists(target.getParent())) {
                target = Path.of("language-impl", PSI_SNAPSHOT_FS_PATH);
            }
            Files.writeString(target, actual);
            System.out.println("[regenerate] wrote " + actual.length()
                + " bytes of PSI tree to " + target.toAbsolutePath());
            return;
        }

        String expected;
        try (InputStream in = SwiftPackageDescriptionStubTest.class
            .getResourceAsStream(PSI_SNAPSHOT_RESOURCE)) {
            assertNotNull(in,
                "PSI snapshot stub missing at " + PSI_SNAPSHOT_RESOURCE
                    + " — run with -Dswift.bitcode.regenerate=true to create it");
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        if (!expected.equals(actual)) {
            // Friendly diff: surface the first diverging line, full
            // expected-vs-actual is too big to dump.
            String[] e = expected.split("\n", -1);
            String[] a = actual.split("\n", -1);
            int firstDiff = -1;
            for (int i = 0; i < Math.min(e.length, a.length); i++) {
                if (!e[i].equals(a[i])) { firstDiff = i; break; }
            }
            String summary;
            if (firstDiff < 0) {
                summary = "lengths differ: expected " + e.length
                    + " lines, actual " + a.length + " lines";
            }
            else {
                summary = "first diverging line: " + (firstDiff + 1)
                    + "\n  expected: " + e[firstDiff]
                    + "\n  actual:   " + a[firstDiff];
            }
            fail("PSI tree drifted from PackageDescription.expected.txt.\n" + summary
                + "\nRun `mvn test -Dswift.bitcode.regenerate=true "
                + "-Dtest=SwiftPackageDescriptionStubTest#testSnapshotPsiTreeMatches` to update.");
        }
    }

    /** Same indent-tree format as {@code SimpleParsingTest.toParseTreeText}. */
    private static String renderTree(@Nonnull PsiFile root) {
        StringBuilder sb = new StringBuilder();
        renderTree(sb, root, 0);
        return sb.toString();
    }

    private static void renderTree(StringBuilder sb, PsiElement el, int depth) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        // Match what SimpleParsingTest emits: PsiKind('text') for leaves.
        if (el.getFirstChild() == null) {
            String text = el.getText().replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
            sb.append(el.getClass().getSimpleName().replace("Impl", ""))
                .append("(").append(el.getNode().getElementType()).append(")")
                .append("('").append(text).append("')\n");
        }
        else {
            sb.append(el.getClass().getSimpleName().replace("Impl", ""))
                .append("(").append(el.getNode().getElementType()).append(")\n");
            PsiElement c = el.getFirstChild();
            while (c != null) {
                renderTree(sb, c, depth + 1);
                c = c.getNextSibling();
            }
        }
    }

    private static String loadSnapshot() throws Exception {
        try (InputStream in = SwiftPackageDescriptionStubTest.class
            .getResourceAsStream(SNAPSHOT_RESOURCE)) {
            assertNotNull(in, "snapshot missing on classpath: " + SNAPSHOT_RESOURCE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int lineAt(@Nonnull String text, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    @Nonnull
    @Override
    protected LanguageFileType getFileType(@Nonnull Context context, @Nullable Object o) {
        return SwiftFileType.INSTANCE;
    }

    @Override
    protected void checkResult(@Nonnull Context context, @Nullable Object testContext, PsiFile file) {
        // No-op: assertions live in the test body, not a tree-snapshot diff.
    }
}
