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
import consulo.swift.language.SwiftFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Documents (and locks in) how generic-type-of-optional shapes are handled.
 *
 * <p>Swift's source-level syntax {@code Foo<T>?} is ambiguous at the lexer
 * level — without parser context, {@code >?} could be a custom binary
 * operator. Real Swift's compiler resolves this with contextual re-lexing
 * in the parser (similar to IntelliJ Java's {@code consumeGt()} pattern
 * that splits {@code >>} when closing nested generics). Our PsiBuilder
 * doesn't expose a mid-token split primitive, and splitting at the lexer
 * with whitespace heuristics breaks brace-pair highlighting for {@code <}
 * and {@code >} tokens.
 *
 * <p>Current state (post {@code >}-as-primitive lexer refactor + parser-side
 * compound composition):
 *
 * <ul>
 *   <li><b>Bare {@code Foo<T>?}</b> — supported. The lexer emits {@code >}
 *       and {@code ?} as separate primitive tokens; the type grammar
 *       consumes {@code GT} to close the generic and the next
 *       {@code QUESTION} becomes the postfix optional marker.</li>
 *   <li><b>Bare {@code Foo<T>!}</b> — supported, same path.</li>
 *   <li><b>{@code a >= b}, {@code a >> b}, {@code a >>= b}</b> — supported
 *       via parser-side composition ({@code SwiftParserUtil#gtCompoundWidth}).
 *       The primitives are collapsed back into a single
 *       {@code BINARY_OP} element via {@code PsiBuilder.Marker.collapse}.</li>
 *   <li><b>Custom {@code >?} / {@code >!} / {@code >>=} / {@code >>>=}
 *       operator declarations</b> — supported. The same compound
 *       composition fires for {@code POSTFIX_OP} and {@code PREFIX_OP}
 *       positions, so {@code infix operator >?} parses too.</li>
 * </ul>
 */
public class SwiftGenericOptionalSplitTest extends SimpleParsingTest<Object> {

    public SwiftGenericOptionalSplitTest() {
        super("inline", "swift");
    }

    @RequiredReadAction
    @Test
    public void testBareGenericOptionalParses(Context context) throws Exception {
        // The whole point of making {@code >} a primitive lexer token: bare
        // {@code Foo<T>?} now parses natively. Lexer sees {@code GT QUESTION}
        // as two tokens; type grammar consumes {@code GT} to close the
        // generic and {@code QUESTION} as the postfix optional marker.
        assertParsesCleanly(context,
            "let a: Array<Int>? = nil\n"
                + "let b: Dictionary<String, Int>! = nil\n"
                + "func unwrap(_ value: Array<String>?) -> Array<String> {\n"
                + "    return value ?? []\n"
                + "}\n");
    }

    @RequiredReadAction
    @Test
    public void testComparisonAndShiftOperatorsComposeBackToBinaryOp(Context context) throws Exception {
        // {@code >=} and {@code >>} now arrive as separate raw tokens (the
        // lexer no longer glues {@code >} with anything). The parser-side
        // composition in {@code SwiftParserUtil.gtCompoundWidth} recognises
        // these GT-prefixed compounds when the grammar asks for
        // {@code BINARY_OP} and collapses them via
        // {@code PsiBuilder.Marker.collapse}.
        assertParsesCleanly(context,
            "func compare(a: Int, b: Int) -> Bool {\n"
                + "    if a >= b { return true }\n"
                + "    if a > b { return true }\n"
                + "    let shifted = a >> 2\n"
                + "    return shifted >= b\n"
                + "}\n");
    }

    @RequiredReadAction
    @Test
    public void testCustomGreaterQuestionOperatorDeclaration(Context context) throws Exception {
        // The same compound composition also covers operator-DECLARATION
        // position (the grammar matches operator names as
        // {@code BINARY_OP / PREFIX_OP / POSTFIX_OP} too). So defining
        // and calling a custom {@code >?} operator now parses end-to-end.
        assertParsesCleanly(context,
            "infix operator >? : ComparisonPrecedence\n"
                + "func >? (lhs: Int, rhs: Int) -> Int { return lhs > rhs ? lhs : rhs }\n"
                + "let result = 1 >? 2\n");
    }

    private void assertParsesCleanly(@Nonnull Context context, @Nonnull String source) throws Exception {
        PsiFile file = createFile(context, null, "Inline.swift", getFileType(context, null), source);
        List<PsiErrorElement> errors = collectErrors(file);
        if (!errors.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append(errors.size()).append(" parse error(s) in:\n").append(source).append("\n---\n");
            for (PsiErrorElement err : errors) {
                report.append("  offset=").append(err.getTextRange().getStartOffset())
                    .append(" : ").append(err.getErrorDescription()).append('\n');
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

    @Nonnull
    @Override
    protected LanguageFileType getFileType(@Nonnull Context context, @Nullable Object o) {
        return SwiftFileType.INSTANCE;
    }

    @Override
    protected void checkResult(@Nonnull Context context, @Nullable Object testContext, PsiFile file) {
        // No-op: assertions live in the inline source helpers.
    }
}
