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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.controlFlow.Instruction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.swift.language.impl.localize.SwiftFlowLocalize;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Binary;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Kind;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Node;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Operand;
import consulo.swift.language.psi.SwiftExpression;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The flow diagnostics of one owner, as swiftc reports them (verified with Swift 6.4):
 * <ul>
 * <li>"code after 'return' will never be executed", "will never be executed" (warnings, SILGen) - the first statement of a
 * block the structure of the code does not reach, once per block</li>
 * <li>"will never be executed" (warning, DiagnoseUnreachable) - the statement after a call never returning, at the place
 * its first instruction is from</li>
 * <li>"missing return in global function expected to return 'Int'" - at the closing brace, the end of a body returning a
 * value reached after the compiler folds the constants</li>
 * <li>"'guard' body must not fall through, ..." - at the closing brace of the else body</li>
 * </ul>
 * Nothing for an owner which does not parse.
 *
 * @author VISTALL
 */
public final class SwiftFlowDiagnostics {
    public record Problem(TextRange range, boolean error, LocalizeValue message) {
    }

    private static final TokenSet MEMBERS = TokenSet.create(SwiftTypes.STRUCT_MEMBER, SwiftTypes.CLASS_MEMBER, SwiftTypes.EXTENSION_MEMBER,
        SwiftTypes.UNION_STYLE_ENUM_MEMBER, SwiftTypes.PROTOCOL_MEMBER);

    private static final TokenSet BRANCHES = TokenSet.create(SwiftTypes.IF_STATEMENT, SwiftTypes.GUARD_STATEMENT, SwiftTypes.WHILE_STATEMENT,
        SwiftTypes.REPEAT_WHILE_STATEMENT, SwiftTypes.FOR_IN_STATEMENT, SwiftTypes.DO_STATEMENT, SwiftTypes.SWITCH_STATEMENT,
        SwiftTypes.LABELED_STATEMENT, SwiftTypes.ELSE_CLAUSE, SwiftTypes.CATCH_CLAUSE, SwiftTypes.SWITCH_CASE, SwiftTypes.CODE_BLOCK);

    private SwiftFlowDiagnostics() {
    }

    @RequiredReadAction
    public static List<Problem> of(PsiElement owner) {
        if (!owner.isPhysical()) {
            return compute(owner);
        }
        return LanguageCachedValueUtil.getCachedValue(owner, () -> CachedValueProvider.Result.create(compute(owner), PsiModificationTracker.MODIFICATION_COUNT));
    }

    @RequiredReadAction
    private static List<Problem> compute(PsiElement owner) {
        SwiftControlFlow flow = SwiftControlFlow.of(owner);
        if (flow == null) {
            return List.of();
        }
        List<Problem> problems = new ArrayList<>();
        unreachableStatements(flow, SwiftControlFlow.bodyStatements(owner), problems);
        afterNoReturnCalls(flow, problems);
        missingReturn(flow, problems);
        for (Map.Entry<Instruction, PsiElement> guardEnd : flow.getGuardEnds().entrySet()) {
            if (flow.isReachable(guardEnd.getKey())) {
                problems.add(new Problem(closingBrace(guardEnd.getValue()), true, SwiftFlowLocalize.guardFallThrough()));
            }
        }
        return List.copyOf(problems);
    }

    // region unreachable code

    /**
     * SILGen: once a block is left, its next statement is reported and the rest of the block is not emitted - after a
     * return, a throw, a break, a continue, a fallthrough by its name, after a statement ending all its ways (an if
     * returning in both branches) without one
     */
    @RequiredReadAction
    private static void unreachableStatements(SwiftControlFlow flow, @Nullable PsiElement statements, List<Problem> problems) {
        if (statements == null) {
            return;
        }
        PsiElement previous = null;
        for (PsiElement statement = statements.getFirstChild(); statement != null; statement = statement.getNextSibling()) {
            if (statement.getFirstChild() == null) {
                continue;
            }
            if (!flow.isStructurallyReachable(statement)) {
                // a block never entered (a catch of a do throwing nothing) is not emitted
                if (previous == null) {
                    return;
                }
                String jump = jumpName(previous);
                LocalizeValue message = jump != null ? SwiftFlowLocalize.unreachableAfterStatement(jump) : SwiftFlowLocalize.unreachable();
                problems.add(new Problem(statement.getTextRange(), false, message));
                return;
            }
            nestedBlocks(flow, statement, problems);
            previous = statement;
        }
    }

    @RequiredReadAction
    private static void nestedBlocks(SwiftControlFlow flow, PsiElement element, List<Problem> problems) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (type == SwiftTypes.STATEMENTS) {
                unreachableStatements(flow, child, problems);
            }
            else if (BRANCHES.contains(type)) {
                nestedBlocks(flow, child, problems);
            }
        }
    }

    private static @Nullable String jumpName(@Nullable PsiElement statement) {
        IElementType type = PsiUtilCore.getElementType(statement);
        if (type == SwiftTypes.RETURN_STATEMENT) {
            return "return";
        }
        if (type == SwiftTypes.THROW_STATEMENT) {
            return "throw";
        }
        if (type == SwiftTypes.BREAK_STATEMENT) {
            return "break";
        }
        if (type == SwiftTypes.CONTINUE_STATEMENT) {
            return "continue";
        }
        if (type == SwiftTypes.FALLTHROUGH_STATEMENT) {
            return "fallthrough";
        }
        return null;
    }

    /**
     * DiagnoseUnreachable: the statement after a call never returning, in the same block, at the place of its first SIL
     * instruction - only for the statements where it is known (a literal, a call, a variable, a return)
     */
    @RequiredReadAction
    private static void afterNoReturnCalls(SwiftControlFlow flow, List<Problem> problems) {
        for (Instruction call : flow.getNoReturns()) {
            if (!flow.isReachable(call) || call.getElement() == null) {
                continue;
            }
            PsiElement statement = call.getElement();
            while (statement != null && !(statement.getParent() instanceof PsiFile) && PsiUtilCore.getElementType(statement.getParent()) != SwiftTypes.STATEMENTS) {
                statement = statement.getParent();
            }
            PsiElement next = statement == null ? null : statement.getNextSibling();
            while (next != null && next.getFirstChild() == null) {
                next = next.getNextSibling();
            }
            if (next == null || !flow.isStructurallyReachable(next)) {
                continue;
            }
            PsiElement anchor = firstInstruction(next);
            if (anchor != null) {
                problems.add(new Problem(anchor.getTextRange(), false, SwiftFlowLocalize.unreachable()));
            }
        }
    }

    /**
     * The element the first SIL instruction of a statement is located at - null when not known (a parameter loaded, a loop)
     */
    @RequiredReadAction
    private static @Nullable PsiElement firstInstruction(PsiElement statement) {
        IElementType type = PsiUtilCore.getElementType(statement);
        if (type == SwiftTypes.STATEMENT) {
            PsiElement inner = SwiftFlowVariables.firstComposite(statement);
            IElementType innerType = PsiUtilCore.getElementType(inner);
            if (innerType == SwiftTypes.CONSTANT_DECLARATION) {
                // let x = value - its value
                PsiElement value = firstInitializerValue(inner);
                return value == null ? null : firstInstruction(SwiftExpressionTree.of(value));
            }
            if (innerType == SwiftTypes.VARIABLE_DECLARATION) {
                // var x = value - the box allocated for x
                List<PsiElement> variables = new ArrayList<>();
                PsiElement list = SwiftFlowVariables.firstChildOfType(inner, SwiftTypes.PATTERN_INITIALIZER_LIST);
                PsiElement first = SwiftFlowVariables.firstChildOfType(list, SwiftTypes.PATTERN_INITIALIZER);
                SwiftFlowVariables.bindings(SwiftFlowVariables.firstComposite(first), true, variables);
                return variables.isEmpty() ? null : SwiftFlowVariables.nameElement(variables.get(0));
            }
            if (inner instanceof SwiftExpression) {
                return firstInstruction(SwiftExpressionTree.of(inner));
            }
            return null;
        }
        if (type == SwiftTypes.RETURN_STATEMENT) {
            PsiElement value = expressionChild(statement);
            if (value == null) {
                return statement.getFirstChild();
            }
            Node node = SwiftExpressionTree.of(value);
            if (node instanceof Operand operand && operand.isSimple() && isParameter(operand.head())) {
                // a parameter returned - no instruction before the return
                return statement.getFirstChild();
            }
            return firstInstruction(node);
        }
        if (type == SwiftTypes.THROW_STATEMENT) {
            PsiElement value = expressionChild(statement);
            return value == null ? null : firstInstruction(SwiftExpressionTree.of(value));
        }
        if (type == SwiftTypes.IF_STATEMENT || type == SwiftTypes.GUARD_STATEMENT) {
            PsiElement list = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CONDITION_LIST);
            PsiElement condition = SwiftFlowVariables.firstComposite(SwiftFlowVariables.firstChildOfType(list, SwiftTypes.CONDITION));
            if (PsiUtilCore.getElementType(condition) == SwiftTypes.OPTIONAL_BINDING_CONDITION) {
                condition = expressionChild(condition);
            }
            return condition == null ? null : firstInstruction(SwiftExpressionTree.of(condition));
        }
        if (type == SwiftTypes.SWITCH_STATEMENT || type == SwiftTypes.FOR_IN_STATEMENT) {
            PsiElement value = null;
            boolean afterIn = type == SwiftTypes.SWITCH_STATEMENT;
            for (PsiElement child = statement.getFirstChild(); child != null && value == null; child = child.getNextSibling()) {
                if (PsiUtilCore.getElementType(child) == SwiftTypes.IN_KEYWORD) {
                    afterIn = true;
                }
                else if (afterIn && child instanceof SwiftExpression) {
                    value = child;
                }
            }
            return value == null ? null : firstInstruction(SwiftExpressionTree.of(value));
        }
        return null;
    }

    private static @Nullable PsiElement firstInitializerValue(PsiElement declaration) {
        PsiElement list = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.PATTERN_INITIALIZER_LIST);
        PsiElement first = SwiftFlowVariables.firstChildOfType(list, SwiftTypes.PATTERN_INITIALIZER);
        return expressionChild(SwiftFlowVariables.firstChildOfType(first, SwiftTypes.INITIALIZER));
    }

    @RequiredReadAction
    private static @Nullable PsiElement firstInstruction(@Nullable Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Binary binary) {
            return firstInstruction(binary.kind() == Kind.ASSIGN || binary.kind() == Kind.COMPOUND_ASSIGN ? binary.right() : binary.left());
        }
        if (node instanceof SwiftExpressionTree.Ternary ternary) {
            return firstInstruction(ternary.condition());
        }
        if (node instanceof SwiftExpressionTree.Try tryNode) {
            return firstInstruction(tryNode.operand());
        }
        if (node instanceof SwiftExpressionTree.Cast cast) {
            return firstInstruction(cast.operand());
        }
        if (node instanceof SwiftExpressionTree.Postfix postfix) {
            return firstInstruction(postfix.operand());
        }
        Operand operand = (Operand) node;
        if (!operand.prefix().isEmpty()) {
            return null;
        }
        PsiElement head = operand.head();
        IElementType headType = PsiUtilCore.getElementType(head);
        PsiElement call = null;
        PsiElement member = null;
        for (PsiElement suffix : operand.suffixes()) {
            IElementType type = PsiUtilCore.getElementType(suffix);
            if (type == SwiftTypes.FUNCTION_CALL_ARGUMENT_CLAUSE) {
                call = suffix;
                break;
            }
            if (type == SwiftTypes.IDENTIFIER) {
                member = suffix;
            }
            else if (type != SwiftTypes.DOT) {
                return null;
            }
        }
        if (call != null) {
            // f(a, b) - its first argument, f() - the function, x.f() - the method
            PsiElement list = SwiftFlowVariables.firstChildOfType(call, SwiftTypes.FUNCTION_CALL_ARGUMENT_LIST);
            PsiElement argument = SwiftFlowVariables.firstChildOfType(list, SwiftTypes.FUNCTION_CALL_ARGUMENT);
            if (argument != null) {
                PsiElement value = expressionChild(argument);
                Node valueNode = value == null ? null : SwiftExpressionTree.of(value);
                if (valueNode instanceof Operand valueOperand && valueOperand.isSimple() && isParameter(valueOperand.head())) {
                    return null;
                }
                return firstInstruction(valueNode);
            }
            return member != null ? member : headType == SwiftTypes.PRIMARY_EXPRESSION ? head : null;
        }
        if (member != null) {
            // E.e - the case constructed; x.y - x loaded
            String text = head.getText();
            return !text.isEmpty() && Character.isUpperCase(text.charAt(0)) ? member : headType == SwiftTypes.PRIMARY_EXPRESSION && !isParameter(head) ? head : null;
        }
        if (headType == SwiftTypes.LITERAL_EXPRESSION) {
            return head;
        }
        if (headType == SwiftTypes.PRIMARY_EXPRESSION && SwiftFlowVariables.firstChildOfType(head, SwiftTypes.IDENTIFIER) != null && !isParameter(head)) {
            return head;
        }
        return null;
    }

    @RequiredReadAction
    private static boolean isParameter(PsiElement head) {
        PsiElement identifier = SwiftFlowVariables.firstChildOfType(head, SwiftTypes.IDENTIFIER);
        if (identifier == null) {
            return false;
        }
        PsiElement variable = SwiftFlowVariables.resolve(SwiftFlowVariables.unquote(identifier.getText()), identifier);
        return variable != null && SwiftFlowVariables.kind(variable) == SwiftFlowVariables.Kind.PARAMETER;
    }

    private static @Nullable PsiElement expressionChild(@Nullable PsiElement element) {
        for (PsiElement child = element == null ? null : element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof SwiftExpression) {
                return child;
            }
        }
        return null;
    }

    // endregion

    // region missing return

    @RequiredReadAction
    private static void missingReturn(SwiftControlFlow flow, List<Problem> problems) {
        PsiElement owner = flow.getOwner();
        PsiElement type = SwiftControlFlow.resultType(owner);
        if (SwiftControlFlow.isVoid(type) || !flow.isEndReachable() || isExpressionBody(owner)) {
            return;
        }
        String kind = kind(owner);
        PsiElement brace = PsiUtilCore.getElementType(owner) == SwiftTypes.CLOSURE_EXPRESSION ? owner : SwiftControlFlow.bodyBlock(owner);
        if (kind == null || brace == null) {
            return;
        }
        if (SwiftControlFlow.isNever(type)) {
            problems.add(new Problem(closingBrace(brace), true, SwiftFlowLocalize.missingNeverCall(kind)));
            return;
        }
        String presentation = typeText(type, owner);
        if (presentation != null) {
            problems.add(new Problem(closingBrace(brace), true, SwiftFlowLocalize.missingReturn(kind, presentation)));
        }
    }

    /**
     * {@code if b { 1 } else { 2 }} as the only statement - an if or a switch expression returned: an else for every if,
     * each branch one expression or a throw
     */
    private static boolean isExpressionBody(PsiElement owner) {
        PsiElement single = single(SwiftControlFlow.bodyStatements(owner));
        IElementType type = PsiUtilCore.getElementType(single);
        return (type == SwiftTypes.IF_STATEMENT || type == SwiftTypes.SWITCH_STATEMENT) && isExpressionBranches(single);
    }

    private static boolean isExpressionBranches(PsiElement statement) {
        if (PsiUtilCore.getElementType(statement) == SwiftTypes.IF_STATEMENT) {
            PsiElement elseClause = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.ELSE_CLAUSE);
            if (elseClause == null || !isExpressionBranch(SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK))) {
                return false;
            }
            PsiElement nested = SwiftFlowVariables.firstChildOfType(elseClause, SwiftTypes.IF_STATEMENT);
            return nested != null ? isExpressionBranches(nested) : isExpressionBranch(SwiftFlowVariables.firstChildOfType(elseClause, SwiftTypes.CODE_BLOCK));
        }
        boolean any = false;
        for (PsiElement switchCase = statement.getFirstChild(); switchCase != null; switchCase = switchCase.getNextSibling()) {
            if (PsiUtilCore.getElementType(switchCase) == SwiftTypes.SWITCH_CASE) {
                any = true;
                if (!isExpressionStatement(single(SwiftFlowVariables.firstChildOfType(switchCase, SwiftTypes.STATEMENTS)))) {
                    return false;
                }
            }
        }
        return any;
    }

    private static boolean isExpressionBranch(@Nullable PsiElement block) {
        return isExpressionStatement(single(SwiftFlowVariables.firstChildOfType(block, SwiftTypes.STATEMENTS)));
    }

    private static boolean isExpressionStatement(@Nullable PsiElement statement) {
        IElementType type = PsiUtilCore.getElementType(statement);
        if (type == SwiftTypes.THROW_STATEMENT) {
            return true;
        }
        if (type == SwiftTypes.IF_STATEMENT || type == SwiftTypes.SWITCH_STATEMENT) {
            return isExpressionBranches(statement);
        }
        return type == SwiftTypes.STATEMENT && SwiftFlowVariables.firstComposite(statement) instanceof SwiftExpression;
    }

    /**
     * The only statement of a block - null if none or more
     */
    private static @Nullable PsiElement single(@Nullable PsiElement statements) {
        PsiElement single = null;
        for (PsiElement child = statements == null ? null : statements.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() != null) {
                if (single != null) {
                    return null;
                }
                single = child;
            }
        }
        return single;
    }

    /**
     * global function, local function, instance method, static method, class method, getter, closure
     */
    private static @Nullable String kind(PsiElement owner) {
        IElementType type = PsiUtilCore.getElementType(owner);
        if (type == SwiftTypes.CLOSURE_EXPRESSION) {
            return "closure";
        }
        if (type == SwiftTypes.GETTER_CLAUSE || type == SwiftTypes.VARIABLE_DECLARATION || type == SwiftTypes.SUBSCRIPT_DECLARATION) {
            return "getter";
        }
        if (type != SwiftTypes.FUNCTION_DECLARATION) {
            return null;
        }
        PsiElement parent = owner.getParent();
        if (parent instanceof PsiFile || parent != null && parent.getParent() instanceof PsiFile) {
            return "global function";
        }
        if (parent != null && MEMBERS.contains(PsiUtilCore.getElementType(parent))) {
            for (PsiElement child = owner.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (PsiUtilCore.getElementType(child) == SwiftTypes.DECLARATION_MODIFIER) {
                    IElementType modifier = PsiUtilCore.getElementType(child.getFirstChild());
                    if (modifier == SwiftTypes.STATIC_KEYWORD) {
                        return "static method";
                    }
                    if (modifier == SwiftTypes.CLASS_KEYWORD) {
                        return "class method";
                    }
                }
            }
            return "instance method";
        }
        return "local function";
    }

    /**
     * The type as the compiler prints it: {@code [Int : String]}, {@code (Int, Int)}, Self as the struct or enum - null when
     * it is not known (Self of a class)
     */
    private static @Nullable String typeText(@Nullable PsiElement type, PsiElement owner) {
        if (type == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int brackets = 0;
        for (PsiElement leaf = PsiTreeUtil.getDeepestFirst(type); leaf != null && type.getTextRange().contains(leaf.getTextRange());
             leaf = PsiTreeUtil.nextLeaf(leaf)) {
            if (!SwiftExpressionTree.isSignificant(leaf) || leaf.getText().isBlank()) {
                continue;
            }
            IElementType leafType = PsiUtilCore.getElementType(leaf);
            String text = leaf.getText();
            if (leafType == SwiftTypes.LBRACK) {
                brackets++;
            }
            else if (leafType == SwiftTypes.RBRACK) {
                brackets--;
            }
            if (leafType == SwiftTypes.COMMA) {
                builder.append(", ");
            }
            else if (leafType == SwiftTypes.COLON && brackets > 0) {
                builder.append(" : ");
            }
            else if (leafType == SwiftTypes.ARROW) {
                builder.append(" -> ");
            }
            else if (leafType == SwiftTypes.SELF_TYPE_KEYWORD) {
                String name = enclosingValueType(owner);
                if (name == null) {
                    return null;
                }
                builder.append(name);
            }
            else {
                if (!builder.isEmpty() && Character.isLetterOrDigit(builder.charAt(builder.length() - 1)) && Character.isLetterOrDigit(text.charAt(0))) {
                    builder.append(' ');
                }
                builder.append(text);
            }
        }
        return builder.toString();
    }

    /**
     * The name of the struct or the enum the member is in
     */
    private static @Nullable String enclosingValueType(PsiElement owner) {
        for (PsiElement parent = owner.getParent(); parent != null; parent = parent.getParent()) {
            IElementType type = PsiUtilCore.getElementType(parent);
            if (type == SwiftTypes.STRUCT_DECLARATION || type == SwiftTypes.ENUM_DECLARATION) {
                PsiElement holder = type == SwiftTypes.ENUM_DECLARATION ? SwiftFlowVariables.firstChildOfType(parent, SwiftTypes.UNION_STYLE_ENUM) : parent;
                PsiElement name = SwiftFlowVariables.firstChildOfType(holder, SwiftTypes.IDENTIFIER);
                return name == null ? null : name.getText();
            }
            if (type == SwiftTypes.CLASS_DECLARATION || type == SwiftTypes.ACTOR_DECLARATION || type == SwiftTypes.EXTENSION_DECLARATION) {
                return null;
            }
        }
        return null;
    }

    // endregion

    /**
     * The closing brace of a code block or a closure - its end when there is none
     */
    private static TextRange closingBrace(PsiElement block) {
        PsiElement last = block.getLastChild();
        if (PsiUtilCore.getElementType(last) == SwiftTypes.RBRACE) {
            return last.getTextRange();
        }
        int end = block.getTextRange().getEndOffset();
        return new TextRange(end, end);
    }
}
