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

package consulo.swift.language.impl.lexer;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.swift.language.impl.SwiftLexer;
import consulo.swift.language.psi.SwiftTypes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct lexer tests — drive the lexer over a source string and assert the
 * exact token stream. Focused on:
 *   - whitespace-sensitive operator classification (PREFIX_OP / POSTFIX_OP /
 *     BINARY_OP), which is stateful inside the lexer
 *   - the IN_BLOCK_COMMENT state (depth-tracked nested block comments)
 *   - separation of single-char reserved operators (EXCL, QUESTION, AMP)
 *     from multi-char operator runs
 *   - longest-match ordering between `..<` / `...` / `..=` and the OP_CHAR
 *     catch-all
 */
public class SwiftLexerTest {

    private record Tok(IElementType type, String text) {
    }

    private static List<Tok> lex(String source) {
        SwiftLexer lexer = new SwiftLexer();
        lexer.start(source, 0, source.length(), 0);
        List<Tok> out = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            out.add(new Tok(lexer.getTokenType(),
                source.substring(lexer.getTokenStart(), lexer.getTokenEnd())));
            lexer.advance();
        }
        return out;
    }

    private static List<Tok> lexSkipWS(String source) {
        List<Tok> all = lex(source);
        List<Tok> filtered = new ArrayList<>();
        for (Tok t : all) {
            if (t.type() != TokenType.WHITE_SPACE) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    private static void assertTypes(String source, IElementType... expected) {
        List<Tok> tokens = lexSkipWS(source);
        StringBuilder msg = new StringBuilder("source: ").append(source).append('\n');
        for (Tok t : tokens) {
            msg.append("  ").append(t.type()).append(" = '").append(t.text()).append("'\n");
        }
        assertEquals(expected.length, tokens.size(),
            "token count mismatch\n" + msg);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).type(),
                "token " + i + " mismatch\n" + msg);
        }
    }

    // ====================================================================
    //  Operator whitespace classification
    // ====================================================================

    @Test
    public void binaryOpWithSpacesBothSides() {
        // 'a + b' — '+' surrounded by whitespace → binary
        assertTypes("a + b",
            SwiftTypes.IDENTIFIER, SwiftTypes.BINARY_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void binaryOpWithNoSpacesEitherSide() {
        // 'a+b' — no whitespace on either side → still binary
        assertTypes("a+b",
            SwiftTypes.IDENTIFIER, SwiftTypes.BINARY_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void prefixOpWithSpaceOnLeftOnly() {
        // 'a ++b' — '++' has WS only on left → prefix
        assertTypes("a ++b",
            SwiftTypes.IDENTIFIER, SwiftTypes.PREFIX_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void postfixOpWithSpaceOnRightOnly() {
        // 'a++ b' — '++' has WS only on right → postfix
        assertTypes("a++ b",
            SwiftTypes.IDENTIFIER, SwiftTypes.POSTFIX_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void postfixOpWhenLeftNoWSAndRightIsDot() {
        // 'a++.b' — left has no WS and immediately followed by '.' → postfix
        assertTypes("a++.b",
            SwiftTypes.IDENTIFIER, SwiftTypes.POSTFIX_OP,
            SwiftTypes.DOT, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void openParenIsLeftWSEquivalent() {
        // '(+x)' — '(' counts as left-WS for the next operator → '+' is prefix
        assertTypes("(+x)",
            SwiftTypes.LPAREN, SwiftTypes.PREFIX_OP, SwiftTypes.IDENTIFIER, SwiftTypes.RPAREN);
    }

    @Test
    public void closeParenIsTerm() {
        // ')+1' — ')' is term-like → '+' has no left-WS, no right-WS (right is digit) → binary
        assertTypes("foo()+1",
            SwiftTypes.IDENTIFIER, SwiftTypes.LPAREN, SwiftTypes.RPAREN,
            SwiftTypes.BINARY_OP, SwiftTypes.DECIMAL_LITERAL);
    }

    @Test
    public void commaIsLeftWSEquivalent() {
        // 'f(a, -b)' — ',' counts as left-WS for next op → '-' is prefix
        assertTypes("f(a, -b)",
            SwiftTypes.IDENTIFIER, SwiftTypes.LPAREN,
            SwiftTypes.IDENTIFIER, SwiftTypes.COMMA,
            SwiftTypes.PREFIX_OP, SwiftTypes.IDENTIFIER,
            SwiftTypes.RPAREN);
    }

    @Test
    public void negativeNumberLiteralIsPrefixOp() {
        // 'let x = -1' — '=' is term-marking, then WS, then '-' has WS on left, none on right → prefix
        assertTypes("let x = -1",
            SwiftTypes.LET_KEYWORD, SwiftTypes.IDENTIFIER, SwiftTypes.EQ,
            SwiftTypes.PREFIX_OP, SwiftTypes.DECIMAL_LITERAL);
    }

    @Test
    public void questionMarkIsSingleCharNotOperatorClass() {
        // '?' is always emitted as QUESTION (its own token), not classified.
        assertTypes("x?",
            SwiftTypes.IDENTIFIER, SwiftTypes.QUESTION);
    }

    @Test
    public void exclamationMarkIsSingleCharNotOperatorClass() {
        assertTypes("x!",
            SwiftTypes.IDENTIFIER, SwiftTypes.EXCL);
    }

    @Test
    public void ampersandIsSingleCharNotOperatorClass() {
        // '&x' for inout — '&' is AMP token.
        assertTypes("&x",
            SwiftTypes.AMP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void doubleAmpersandIsBinaryOp() {
        // '&&' is two OP_CHARs → matched by OP_CHAR+ → classified
        assertTypes("a && b",
            SwiftTypes.IDENTIFIER, SwiftTypes.BINARY_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void doublePipeIsBinaryOp() {
        assertTypes("a || b",
            SwiftTypes.IDENTIFIER, SwiftTypes.BINARY_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void equalsEqualsIsBinaryOp() {
        assertTypes("a == b",
            SwiftTypes.IDENTIFIER, SwiftTypes.BINARY_OP, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void bangEqualsIsBinaryOp() {
        assertTypes("a != b",
            SwiftTypes.IDENTIFIER, SwiftTypes.BINARY_OP, SwiftTypes.IDENTIFIER);
    }

    // ====================================================================
    //  Multi-char punctuation longest-match
    // ====================================================================

    @Test
    public void arrowBeatsMinusGreater() {
        assertTypes("a -> b",
            SwiftTypes.IDENTIFIER, SwiftTypes.ARROW, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void ellipsisAndHalfOpenRange() {
        assertTypes("0 ... 10",
            SwiftTypes.DECIMAL_LITERAL, SwiftTypes.ELLIPSIS, SwiftTypes.DECIMAL_LITERAL);
        assertTypes("0 ..< 10",
            SwiftTypes.DECIMAL_LITERAL, SwiftTypes.HALF_OPEN_RANGE, SwiftTypes.DECIMAL_LITERAL);
    }

    // ====================================================================
    //  Block-comment IN_BLOCK_COMMENT state with nesting
    // ====================================================================

    @Test
    public void simpleBlockComment() {
        assertTypes("/* hi */ x",
            SwiftTypes.BLOCK_COMMENT, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void nestedBlockComment() {
        assertTypes("/* outer /* inner */ still outer */ x",
            SwiftTypes.BLOCK_COMMENT, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void deeplyNestedBlockComment() {
        assertTypes("/* a /* b /* c */ b */ a */ x",
            SwiftTypes.BLOCK_COMMENT, SwiftTypes.IDENTIFIER);
    }

    @Test
    public void unterminatedBlockCommentRecovers() {
        // EOF inside block comment should still emit BLOCK_COMMENT, not error
        List<Tok> tokens = lexSkipWS("/* never closed");
        assertEquals(1, tokens.size());
        assertEquals(SwiftTypes.BLOCK_COMMENT, tokens.get(0).type());
    }

    // ====================================================================
    //  Literal forms
    // ====================================================================

    @Test
    public void numericLiteralForms() {
        assertTypes("0b1010 0o777 0xFF 42 3.14 1.5e10",
            SwiftTypes.BINARY_LITERAL,
            SwiftTypes.OCTAL_LITERAL,
            SwiftTypes.HEXADECIMAL_LITERAL,
            SwiftTypes.DECIMAL_LITERAL,
            SwiftTypes.FLOATING_POINT_LITERAL,
            SwiftTypes.FLOATING_POINT_LITERAL);
    }

    @Test
    public void numericLiteralsWithUnderscoreSeparators() {
        assertTypes("1_000_000 0xFF_AA 0b1010_1010",
            SwiftTypes.DECIMAL_LITERAL,
            SwiftTypes.HEXADECIMAL_LITERAL,
            SwiftTypes.BINARY_LITERAL);
    }

    @Test
    public void stringLiteralEscapes() {
        assertTypes("\"a\\nb\"",
            SwiftTypes.STATIC_STRING_LITERAL);
    }

    // ====================================================================
    //  Keyword vs identifier
    // ====================================================================

    @Test
    public void keywordsTakePrecedenceOverIdentifier() {
        assertTypes("let var func class",
            SwiftTypes.LET_KEYWORD,
            SwiftTypes.VAR_KEYWORD,
            SwiftTypes.FUNC_KEYWORD,
            SwiftTypes.CLASS_KEYWORD);
    }

    @Test
    public void contextSensitiveKeywords() {
        // Contextual keywords are lexed as plain IDENTIFIER. The parser util
        // remaps them to their dedicated keyword tokens only when the grammar
        // expects the keyword in a specific position. This is why the same
        // text can also serve as a regular identifier (`let lazy = 5`).
        assertTypes("get set willSet didSet associativity higherThan",
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER);
    }

    @Test
    public void someAndModifiersAreContextual() {
        // The full set of context-sensitive keywords all lex as IDENTIFIER.
        assertTypes("some final lazy mutating override convenience indirect",
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.IDENTIFIER);
    }

    @Test
    public void hardAccessModifiersStayKeywords() {
        // private/fileprivate/internal/public/open/static remain hard keywords.
        assertTypes("private public static internal",
            SwiftTypes.PRIVATE_KEYWORD,
            SwiftTypes.PUBLIC_KEYWORD,
            SwiftTypes.STATIC_KEYWORD,
            SwiftTypes.INTERNAL_KEYWORD);
    }

    @Test
    public void backtickEscapesKeyword() {
        // `class` is a backtick-escaped identifier, not the class keyword.
        assertTypes("`class`",
            SwiftTypes.IDENTIFIER);
    }

    @Test
    public void implicitParameter() {
        assertTypes("$0 $1 $42",
            SwiftTypes.IMPLICIT_PARAMETER_NAME,
            SwiftTypes.IMPLICIT_PARAMETER_NAME,
            SwiftTypes.IMPLICIT_PARAMETER_NAME);
    }

    // ====================================================================
    //  Compiler directives
    // ====================================================================

    @Test
    public void compilerDirectiveTokens() {
        assertTypes("#if #elseif #else #endif",
            SwiftTypes.HASH_IF,
            SwiftTypes.HASH_ELSEIF,
            SwiftTypes.HASH_ELSE,
            SwiftTypes.HASH_ENDIF);
    }

    @Test
    public void poundExpressionsAndPlaygroundLiterals() {
        assertTypes("#file #line #colorLiteral #imageLiteral #selector",
            SwiftTypes.HASH_FILE,
            SwiftTypes.HASH_LINE,
            SwiftTypes.HASH_COLOR_LITERAL,
            SwiftTypes.HASH_IMAGE_LITERAL,
            SwiftTypes.HASH_SELECTOR);
    }

    // ====================================================================
    //  Combined realistic snippet
    // ====================================================================

    @Test
    public void realisticDeclaration() {
        assertTypes("func add(_ a: Int, _ b: Int) -> Int { return a + b }",
            SwiftTypes.FUNC_KEYWORD,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.LPAREN,
            SwiftTypes.UNDERSCORE, SwiftTypes.IDENTIFIER, SwiftTypes.COLON, SwiftTypes.IDENTIFIER,
            SwiftTypes.COMMA,
            SwiftTypes.UNDERSCORE, SwiftTypes.IDENTIFIER, SwiftTypes.COLON, SwiftTypes.IDENTIFIER,
            SwiftTypes.RPAREN,
            SwiftTypes.ARROW,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.LBRACE,
            SwiftTypes.RETURN_KEYWORD,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.BINARY_OP,
            SwiftTypes.IDENTIFIER,
            SwiftTypes.RBRACE);
    }

    @Test
    public void noWhitespaceAroundOperatorChain() {
        // 'a*b+c' — '*' and '+' are both binary (no WS on either side)
        List<Tok> tokens = lexSkipWS("a*b+c");
        assertEquals(5, tokens.size());
        assertEquals(SwiftTypes.IDENTIFIER, tokens.get(0).type());
        assertEquals(SwiftTypes.BINARY_OP,  tokens.get(1).type());
        assertEquals(SwiftTypes.IDENTIFIER, tokens.get(2).type());
        assertEquals(SwiftTypes.BINARY_OP,  tokens.get(3).type());
        assertEquals(SwiftTypes.IDENTIFIER, tokens.get(4).type());
    }

    @Test
    public void lineCommentMakesNextOpPrefix() {
        // 'a //comment\n -b' — line comment ends with newline (whitespace).
        // After newline the lexer marks left-WS=true → '-' has WS on left,
        // and 'b' has no WS on right → PREFIX_OP.
        List<Tok> tokens = lexSkipWS("a //comment\n -b");
        // tokens: IDENTIFIER, LINE_COMMENT, PREFIX_OP, IDENTIFIER
        boolean sawPrefix = tokens.stream()
            .anyMatch(t -> t.type() == SwiftTypes.PREFIX_OP);
        assertTrue(sawPrefix, "expected a PREFIX_OP after line-comment + whitespace; got " + tokens);
    }
}
