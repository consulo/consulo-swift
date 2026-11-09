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

package consulo.swift.language.impl.psi.controlFlow;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The operator tree of an expression. The parser keeps an expression flat - operands and operators one after another
 * ({@code a = b + c} is an EXPRESSION of {@code a}, {@code = b}, {@code + c}), and a condition (COND_EXPRESSION) even
 * the prefix and postfix parts of its operands; this groups them back by the precedence groups of the standard library
 * (assignment, ternary, ||, &&, comparison, ??, casting, range, addition, multiplication, shift).
 *
 * @author VISTALL
 */
public final class SwiftExpressionTree {
    public sealed interface Node permits Operand, Binary, Ternary, Cast, Try, Postfix {
    }

    /**
     * An operand: prefix operators, the primary expression, its suffixes (calls, members, subscripts, trailing closures)
     */
    public record Operand(List<PsiElement> prefix, PsiElement head, List<PsiElement> suffixes) implements Node {
        public boolean isSimple() {
            return prefix.isEmpty() && suffixes.isEmpty();
        }
    }

    public record Binary(Node left, PsiElement operator, Kind kind, Node right) implements Node {
    }

    /**
     * {@code c ? a : b} - the then expression is in the operator
     */
    public record Ternary(Node condition, PsiElement operator, Node elseNode) implements Node {
    }

    /**
     * {@code x as T}, {@code x is T}
     */
    public record Cast(Node operand, PsiElement operator) implements Node {
    }

    /**
     * {@code try}, {@code try?}, {@code try!} - covering everything to its right
     */
    public record Try(PsiElement operator, Node operand) implements Node {
        public boolean isThrowing() {
            return operator.getLastChild() == operator.getFirstChild();
        }
    }

    /**
     * A one-sided range {@code 0...}
     */
    public record Postfix(Node operand, PsiElement operator) implements Node {
    }

    public enum Kind {
        ASSIGN,
        COMPOUND_ASSIGN,
        OR,
        AND,
        COALESCE,
        OTHER
    }

    private static final Set<String> COMPARISONS = Set.of("==", "!=", "===", "!==", "<=", ">=", "~=");

    private record OperatorItem(PsiElement element, Kind kind, int precedence, boolean rightAssociative, boolean unary) {
    }

    private record TryItem(PsiElement element) {
    }

    private final List<Object> myItems;
    private int myIndex;

    private SwiftExpressionTree(List<Object> items) {
        myItems = items;
    }

    /**
     * The tree of an expression - null for one without an operand (broken code)
     */
    public static @Nullable Node of(PsiElement expression) {
        IElementType type = PsiUtilCore.getElementType(expression);
        List<PsiElement> pieces = new ArrayList<>();
        if (type == SwiftTypes.EXPRESSION || type == SwiftTypes.COND_EXPRESSION || type == SwiftTypes.BINARY_EXPRESSION) {
            flatten(expression, pieces);
        }
        else {
            pieces.add(expression);
        }
        return new SwiftExpressionTree(items(pieces)).parse(0);
    }

    private static void flatten(PsiElement expression, List<PsiElement> pieces) {
        for (PsiElement child = expression.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof PsiWhiteSpace || child instanceof PsiComment || PsiUtilCore.getElementType(child) == TokenType.WHITE_SPACE) {
                continue;
            }
            if (PsiUtilCore.getElementType(child) == SwiftTypes.BINARY_EXPRESSION) {
                flatten(child, pieces);
            }
            else {
                pieces.add(child);
            }
        }
    }

    /**
     * Groups the pieces into operands, operators and try markers
     */
    private static List<Object> items(List<PsiElement> pieces) {
        List<Object> items = new ArrayList<>();
        List<PsiElement> prefix = new ArrayList<>();
        List<PsiElement> suffixes = new ArrayList<>();
        PsiElement head = null;
        // after an operand, or after a unary operator (x as T) - an operator comes next
        boolean afterOperand = false;
        int brackets = 0;
        for (int i = 0; i < pieces.size(); i++) {
            PsiElement piece = pieces.get(i);
            IElementType type = PsiUtilCore.getElementType(piece);
            if (!afterOperand) {
                if (type == SwiftTypes.TRY_OPERATOR) {
                    items.add(new TryItem(piece));
                }
                else if (piece.getFirstChild() != null && type != SwiftTypes.CONDITIONAL_OPERATOR && type != SwiftTypes.TYPE_CASTING_OPERATOR) {
                    decompose(piece, prefix, suffixes);
                    head = innermostHead(piece);
                    afterOperand = true;
                }
                else {
                    // !x, -x, await x, ...x
                    prefix.add(piece);
                }
                continue;
            }

            if (brackets > 0) {
                suffixes.add(piece);
                if (type == SwiftTypes.LBRACK) {
                    brackets++;
                }
                else if (type == SwiftTypes.RBRACK) {
                    brackets--;
                }
                continue;
            }

            OperatorItem operator = operator(piece, type, i + 1 < pieces.size() ? pieces.get(i + 1) : null);
            if (operator == null) {
                // a call, a member, a subscript of the operand - ignored after a unary operator
                if (head != null) {
                    suffixes.add(piece);
                    if (type == SwiftTypes.LBRACK) {
                        brackets++;
                    }
                }
                continue;
            }

            if (head != null) {
                items.add(new Operand(List.copyOf(prefix), head, List.copyOf(suffixes)));
            }
            prefix.clear();
            suffixes.clear();
            head = null;
            items.add(operator);
            afterOperand = operator.unary();
        }
        if (head != null) {
            items.add(new Operand(List.copyOf(prefix), head, List.copyOf(suffixes)));
        }
        return items;
    }

    /**
     * The prefix operators and the suffixes of a PREFIX_EXPRESSION or a POSTFIX_EXPRESSION, around its primary expression
     */
    private static void decompose(PsiElement element, List<PsiElement> prefix, List<PsiElement> suffixes) {
        IElementType type = PsiUtilCore.getElementType(element);
        if (type != SwiftTypes.PREFIX_EXPRESSION && type != SwiftTypes.POSTFIX_EXPRESSION) {
            return;
        }
        boolean inner = false;
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!isSignificant(child)) {
                continue;
            }
            if (inner) {
                suffixes.add(child);
            }
            else if (child.getFirstChild() == null) {
                prefix.add(child);
            }
            else {
                inner = true;
                List<PsiElement> innerSuffixes = new ArrayList<>();
                decompose(child, prefix, innerSuffixes);
                suffixes.addAll(innerSuffixes);
                if (type == SwiftTypes.PREFIX_EXPRESSION) {
                    return;
                }
            }
        }
    }

    private static PsiElement innermostHead(PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        if (type != SwiftTypes.PREFIX_EXPRESSION && type != SwiftTypes.POSTFIX_EXPRESSION) {
            return element;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isSignificant(child) && child.getFirstChild() != null) {
                return innermostHead(child);
            }
        }
        return element;
    }

    public static boolean isSignificant(PsiElement element) {
        return !(element instanceof PsiWhiteSpace || element instanceof PsiComment);
    }

    private static @Nullable OperatorItem operator(PsiElement piece, IElementType type, @Nullable PsiElement next) {
        if (type == SwiftTypes.EQ) {
            return new OperatorItem(piece, Kind.ASSIGN, 1, true, false);
        }
        if (type == SwiftTypes.CONDITIONAL_OPERATOR) {
            return new OperatorItem(piece, Kind.OTHER, 2, true, false);
        }
        if (type == SwiftTypes.TYPE_CASTING_OPERATOR) {
            return new OperatorItem(piece, Kind.OTHER, 7, false, true);
        }
        if (type == SwiftTypes.LT || type == SwiftTypes.GT) {
            return new OperatorItem(piece, Kind.OTHER, 5, false, false);
        }
        if (type == SwiftTypes.ELLIPSIS || type == SwiftTypes.HALF_OPEN_RANGE) {
            // 0... - postfix when nothing follows
            return new OperatorItem(piece, Kind.OTHER, 8, false, next == null);
        }
        if (type != SwiftTypes.BINARY_OP) {
            return null;
        }
        String text = piece.getText();
        return switch (text) {
            case "||" -> new OperatorItem(piece, Kind.OR, 3, false, false);
            case "&&" -> new OperatorItem(piece, Kind.AND, 4, false, false);
            case "??" -> new OperatorItem(piece, Kind.COALESCE, 6, true, false);
            case "+", "-", "&+", "&-", "|", "^" -> new OperatorItem(piece, Kind.OTHER, 9, false, false);
            case "*", "/", "%", "&", "&*" -> new OperatorItem(piece, Kind.OTHER, 10, false, false);
            case "<<", ">>" -> new OperatorItem(piece, Kind.OTHER, 11, false, false);
            default -> {
                if (COMPARISONS.contains(text) || text.equals("<") || text.equals(">")) {
                    yield new OperatorItem(piece, Kind.OTHER, 5, false, false);
                }
                if (text.endsWith("=")) {
                    yield new OperatorItem(piece, Kind.COMPOUND_ASSIGN, 1, true, false);
                }
                // a custom operator - the default precedence group
                yield new OperatorItem(piece, Kind.OTHER, 5, false, false);
            }
        };
    }

    private @Nullable Node parse(int minPrecedence) {
        Node left = primary();
        if (left == null) {
            return null;
        }
        while (myIndex < myItems.size() && myItems.get(myIndex) instanceof OperatorItem operator && operator.precedence() >= minPrecedence) {
            myIndex++;
            if (operator.unary()) {
                left = PsiUtilCore.getElementType(operator.element()) == SwiftTypes.TYPE_CASTING_OPERATOR
                    ? new Cast(left, operator.element())
                    : new Postfix(left, operator.element());
                continue;
            }
            Node right = parse(operator.rightAssociative() ? operator.precedence() : operator.precedence() + 1);
            if (right == null) {
                return left;
            }
            left = PsiUtilCore.getElementType(operator.element()) == SwiftTypes.CONDITIONAL_OPERATOR
                ? new Ternary(left, operator.element(), right)
                : new Binary(left, operator.element(), operator.kind(), right);
        }
        return left;
    }

    private @Nullable Node primary() {
        if (myIndex >= myItems.size()) {
            return null;
        }
        Object item = myItems.get(myIndex);
        if (item instanceof TryItem tryItem) {
            myIndex++;
            Node operand = parse(0);
            return operand == null ? null : new Try(tryItem.element(), operand);
        }
        if (item instanceof Operand operand) {
            myIndex++;
            return operand;
        }
        return null;
    }
}
