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
import consulo.application.progress.ProgressManager;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.controlFlow.ControlFlow;
import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.Instruction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Binary;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Cast;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Kind;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Node;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Operand;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Postfix;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Ternary;
import consulo.swift.language.impl.psi.controlFlow.SwiftExpressionTree.Try;
import consulo.swift.language.impl.psi.controlFlow.SwiftReadWriteInstruction.Access;
import consulo.swift.language.psi.SwiftExpression;
import consulo.swift.language.psi.SwiftTypes;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Builds the {@link SwiftControlFlow} of one flow owner on the platform {@link ControlFlowBuilder}: a node per statement,
 * conditional nodes for the branches of if, guard and loops (the branch a literal true or false never takes is marked
 * impossible), the condition lists with their bindings written on the way they hold, jumps (break, continue with labels,
 * return, throw, fallthrough) as pending edges, the defer blocks of every scope left built again for each way out of it
 * (the end of the block, a jump, an error thrown) in reverse order, a switch without implicit fall-through, an edge from
 * every try that may throw to the catches of the enclosing do, and a node after a call that never returns. Reads and
 * writes of the variables ({@link SwiftFlowVariables}) are nodes, {@code &&}, {@code ||}, {@code ??} and {@code ?:}
 * branch. A closure is one node, its body another owner; a nested function none.
 *
 * @author VISTALL
 */
public class SwiftControlFlowBuilder {
    /**
     * The functions declared returning Never in the standard library
     */
    public static final Set<String> NO_RETURN_FUNCTIONS = Set.of("fatalError", "preconditionFailure", "abort", "exit", "dispatchMain");

    /**
     * A block - with its defer statements so far
     */
    private record Block(PsiElement statements, List<PsiElement> defers) {
    }

    /**
     * A statement break or continue can jump out of - a loop (continue goes to the end of its body), a switch, a labeled if
     * or do
     */
    private record JumpTarget(PsiElement statement, @Nullable String label, boolean loop, boolean switchStatement, @Nullable PsiElement body) {
    }

    /**
     * A do with catches - the nodes an error thrown in its body leaves from
     */
    private record DoContext(PsiElement body, List<Instruction> throwing) {
    }

    /**
     * The nodes a condition ends in when it is true, and when it is false
     */
    private record Exits(List<Instruction> whenTrue, List<Instruction> whenFalse) {
        private Exits swap() {
            return new Exits(whenFalse, whenTrue);
        }
    }

    private final ControlFlowBuilder myBuilder = new ControlFlowBuilder();

    private final Set<String> myNames;
    private final Set<String> myNoReturnFunctions;
    private final SwiftFlowVariables.Resolver myResolver = new SwiftFlowVariables.Resolver();
    // the stored properties an initializer must initialize
    private final @Nullable SwiftInitializerInfo myInitializer;

    private final Deque<Block> myBlocks = new ArrayDeque<>();
    private final Deque<JumpTarget> myTargets = new ArrayDeque<>();
    private final Deque<DoContext> myDos = new ArrayDeque<>();
    // the fallthrough statements of the case being built - to the body of the next one
    private @Nullable List<Instruction> myFallthroughs;
    private @Nullable PsiElement myCase;
    private @Nullable String myLabel;
    // the function called with the arguments being built - where &x passed to it is reported
    private @Nullable PsiElement myCallee;

    private final Map<Instruction, PsiElement> myReturns = new LinkedHashMap<>();
    private final Map<Instruction, PsiElement> myGuardEnds = new LinkedHashMap<>();
    private final List<Instruction> myNoReturns = new ArrayList<>();

    @RequiredReadAction
    private SwiftControlFlowBuilder(PsiElement owner) {
        myNames = SwiftFlowVariables.names(SwiftControlFlow.topOwner(owner));
        myNoReturnFunctions = SwiftControlFlow.noReturnFunctions(owner.getContainingFile());
        myInitializer = SwiftInitializerInfo.of(owner);
    }

    @RequiredReadAction
    public static SwiftControlFlow build(PsiElement owner, @Nullable PsiElement statements, @Nullable PsiElement implicitReturn) {
        return new SwiftControlFlowBuilder(owner).buildImpl(owner, statements, implicitReturn);
    }

    @RequiredReadAction
    private SwiftControlFlow buildImpl(PsiElement owner, @Nullable PsiElement statements, @Nullable PsiElement implicitReturn) {
        node(null, SwiftInstruction.ENTRY);

        statements(statements);
        if (implicitReturn != null && myBuilder.prevInstruction != null) {
            // a body of one expression - returns it
            returnTo(implicitReturn);
        }

        SwiftInstruction end = node(owner, SwiftInstruction.END);
        node(null, SwiftInstruction.EXIT);

        ControlFlow flow = myBuilder.completeControlFlow();
        return new SwiftControlFlow(owner, flow.getInstructions(), end, myReturns, myGuardEnds, myNoReturns, myInitializer);
    }

    // region nodes

    private SwiftInstruction node(@Nullable PsiElement element) {
        return node(element, null);
    }

    private SwiftInstruction node(@Nullable PsiElement element, @Nullable String marker) {
        SwiftInstruction instruction = new SwiftInstruction(myBuilder, element, marker);
        myBuilder.addNodeAndCheckPending(instruction);
        return instruction;
    }

    private void readWrite(PsiElement element, PsiElement variable, Access access, TextRange range) {
        myBuilder.addNodeAndCheckPending(new SwiftReadWriteInstruction(myBuilder, element, variable, access, range));
    }

    /**
     * The flow continues after the first node out of the scope (null - the exit), and stops here
     */
    private void exitTo(@Nullable PsiElement scope) {
        myBuilder.addPendingEdge(scope, myBuilder.prevInstruction);
        myBuilder.flowAbrupted();
    }

    /**
     * A return - the defer blocks of the owner, then the exit
     */
    private void returnTo(PsiElement statement) {
        Instruction instruction = myBuilder.prevInstruction;
        if (instruction != null) {
            myReturns.put(instruction, statement);
        }
        runDefers(null);
        exitTo(null);
    }

    /**
     * The flow goes on from all the ends - joined in a node if more than one
     */
    private void join(List<Instruction> ends, PsiElement element) {
        Set<Instruction> distinct = new LinkedHashSet<>(ends);
        myBuilder.flowAbrupted();
        if (distinct.size() == 1) {
            myBuilder.prevInstruction = distinct.iterator().next();
        }
        else if (!distinct.isEmpty()) {
            edges(List.copyOf(distinct), node(element, SwiftInstruction.JOIN));
        }
    }

    /**
     * The start of a branch of a condition, from its exits
     */
    private void branch(List<Instruction> exits, PsiElement element, PsiElement condition, boolean result, boolean impossible) {
        myBuilder.flowAbrupted();
        SwiftConditionalInstruction instruction = new SwiftConditionalInstruction(myBuilder, element, condition, result, impossible);
        myBuilder.addNodeAndCheckPending(instruction);
        edges(exits, instruction);
    }

    private List<Instruction> current() {
        Instruction instruction = myBuilder.prevInstruction;
        return instruction == null ? List.of() : List.of(instruction);
    }

    private static List<Instruction> concat(List<Instruction> first, List<Instruction> second) {
        List<Instruction> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    /**
     * The ends of a construct in the scope: the current node and the pending edges out of a statement in the scope -
     * taken from the pending list, the flow stopped
     */
    private List<Instruction> collectEnds(PsiElement scope) {
        List<Instruction> ends = new ArrayList<>();
        if (myBuilder.prevInstruction != null) {
            ends.add(myBuilder.prevInstruction);
        }
        myBuilder.processPending((pendingScope, instruction) -> {
            if (pendingScope != null && PsiTreeUtil.isAncestor(scope, pendingScope, false)) {
                ends.add(instruction);
            }
            else {
                myBuilder.addPendingEdge(pendingScope, instruction);
            }
        });
        myBuilder.flowAbrupted();
        return ends;
    }

    private void edges(List<Instruction> from, @Nullable Instruction to) {
        for (Instruction instruction : from) {
            myBuilder.addEdge(instruction, to);
        }
    }

    // endregion

    // region blocks and defer

    /**
     * The statements of a block (a STATEMENTS element, or the file for the top-level code) - then its defer blocks
     */
    @RequiredReadAction
    private void statements(@Nullable PsiElement statements) {
        if (statements == null) {
            return;
        }
        Block block = new Block(statements, new ArrayList<>());
        myBlocks.push(block);
        try {
            for (PsiElement child = statements.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getFirstChild() == null) {
                    continue;
                }
                PsiElement defer = deferOf(child);
                if (defer != null) {
                    node(child);
                    block.defers().add(defer);
                    continue;
                }
                statement(child);
            }
        }
        finally {
            myBlocks.pop();
        }

        if (!block.defers().isEmpty()) {
            List<Instruction> ends = collectEnds(statements);
            if (!ends.isEmpty()) {
                join(ends, statements);
                for (int i = block.defers().size() - 1; i >= 0; i--) {
                    deferCopy(block.defers().get(i));
                }
            }
        }
    }

    private static @Nullable PsiElement deferOf(PsiElement statement) {
        IElementType type = PsiUtilCore.getElementType(statement);
        if (type == SwiftTypes.DEFER_STATEMENT) {
            return statement;
        }
        if (type == SwiftTypes.STATEMENT) {
            PsiElement inner = SwiftFlowVariables.firstComposite(statement);
            return PsiUtilCore.getElementType(inner) == SwiftTypes.DEFER_STATEMENT ? inner : null;
        }
        return null;
    }

    /**
     * The defer blocks of the blocks in the scope (null - all of the owner) a jump leaves, the innermost first, each block's
     * in reverse order - built after the current node, apart from the pending edges of the code around
     */
    @RequiredReadAction
    private void runDefers(@Nullable PsiElement scope) {
        List<Block> blocks = new ArrayList<>();
        for (Block block : myBlocks) {
            if (scope != null && !PsiTreeUtil.isAncestor(scope, block.statements(), false)) {
                break;
            }
            if (!block.defers().isEmpty()) {
                blocks.add(block);
            }
        }
        if (blocks.isEmpty() || myBuilder.prevInstruction == null) {
            return;
        }
        List<Pair<PsiElement, Instruction>> pending = myBuilder.pending;
        myBuilder.pending = new ArrayList<>();
        try {
            for (Block block : blocks) {
                for (int i = block.defers().size() - 1; i >= 0; i--) {
                    deferCopy(block.defers().get(i));
                }
            }
        }
        finally {
            pending.addAll(myBuilder.pending);
            myBuilder.pending = pending;
        }
    }

    @RequiredReadAction
    private void deferCopy(PsiElement defer) {
        PsiElement block = SwiftFlowVariables.firstChildOfType(defer, SwiftTypes.CODE_BLOCK);
        if (block == null) {
            return;
        }
        node(block);
        statements(SwiftFlowVariables.firstChildOfType(block, SwiftTypes.STATEMENTS));
        join(collectEnds(block), block);
    }

    // endregion

    // region statements

    @RequiredReadAction
    private void statement(PsiElement statement) {
        ProgressManager.checkCanceled();
        node(statement);
        IElementType type = PsiUtilCore.getElementType(statement);
        if (type == SwiftTypes.STATEMENT) {
            PsiElement inner = SwiftFlowVariables.firstComposite(statement);
            if (inner != null) {
                inner(inner);
            }
            return;
        }
        inner(statement);
    }

    /**
     * A statement after its node
     */
    @RequiredReadAction
    private void inner(PsiElement statement) {
        IElementType type = PsiUtilCore.getElementType(statement);
        String label = myLabel;
        myLabel = null;
        if (type == SwiftTypes.CONSTANT_DECLARATION || type == SwiftTypes.VARIABLE_DECLARATION) {
            declaration(statement);
        }
        else if (type == SwiftTypes.IF_STATEMENT) {
            ifStatement(statement, label);
        }
        else if (type == SwiftTypes.GUARD_STATEMENT) {
            guardStatement(statement);
        }
        else if (type == SwiftTypes.WHILE_STATEMENT) {
            whileStatement(statement, label);
        }
        else if (type == SwiftTypes.REPEAT_WHILE_STATEMENT) {
            repeatStatement(statement, label);
        }
        else if (type == SwiftTypes.FOR_IN_STATEMENT) {
            forStatement(statement, label);
        }
        else if (type == SwiftTypes.SWITCH_STATEMENT) {
            switchStatement(statement, label);
        }
        else if (type == SwiftTypes.DO_STATEMENT) {
            doStatement(statement, label);
        }
        else if (type == SwiftTypes.LABELED_STATEMENT) {
            PsiElement labelElement = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.STATEMENT_LABEL);
            PsiElement name = SwiftFlowVariables.firstChildOfType(labelElement, SwiftTypes.IDENTIFIER);
            for (PsiElement child = statement.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getFirstChild() != null && child != labelElement) {
                    myLabel = name == null ? null : SwiftFlowVariables.unquote(name.getText());
                    inner(child);
                    myLabel = null;
                    break;
                }
            }
        }
        else if (type == SwiftTypes.RETURN_STATEMENT) {
            expressionChildren(statement);
            returnTo(statement);
        }
        else if (type == SwiftTypes.THROW_STATEMENT) {
            expressionChildren(statement);
            throwFrom();
            myBuilder.flowAbrupted();
        }
        else if (type == SwiftTypes.BREAK_STATEMENT) {
            jump(statement, false);
        }
        else if (type == SwiftTypes.CONTINUE_STATEMENT) {
            jump(statement, true);
        }
        else if (type == SwiftTypes.FALLTHROUGH_STATEMENT) {
            if (myFallthroughs != null && myCase != null) {
                runDefers(myCase);
                if (myBuilder.prevInstruction != null) {
                    myFallthroughs.add(myBuilder.prevInstruction);
                }
                myBuilder.flowAbrupted();
            }
        }
        else if (type == SwiftTypes.CONDITIONAL_COMPILATION_BLOCK || type == SwiftTypes.COMPILER_CONTROL_STATEMENT) {
            compilationBlock(statement);
        }
        else if (isExpression(statement)) {
            expression(statement);
        }
        // a nested function, a type, an import, an operator - no flow
    }

    /**
     * {@code let x = 1, y: Int} - the values, then the variables written; declared uninitialized without one
     */
    @RequiredReadAction
    private void declaration(PsiElement declaration) {
        PsiElement list = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.PATTERN_INITIALIZER_LIST);
        if (list == null) {
            // var x: Int = 1 { didSet { } } - the value; an observed or computed variable is not followed
            expressionChildren(declaration);
            return;
        }
        for (PsiElement initializer = list.getFirstChild(); initializer != null; initializer = initializer.getNextSibling()) {
            if (PsiUtilCore.getElementType(initializer) != SwiftTypes.PATTERN_INITIALIZER) {
                continue;
            }
            PsiElement pattern = SwiftFlowVariables.firstComposite(initializer);
            PsiElement value = SwiftFlowVariables.firstChildOfType(initializer, SwiftTypes.INITIALIZER);
            if (value != null) {
                expressionChildren(value);
            }
            List<PsiElement> variables = new ArrayList<>();
            SwiftFlowVariables.bindings(pattern, true, variables);
            for (PsiElement variable : variables) {
                readWrite(variable, variable, value != null ? Access.WRITE : Access.DECLARE, SwiftFlowVariables.nameElement(variable).getTextRange());
            }
        }
    }

    @RequiredReadAction
    private void ifStatement(PsiElement statement, @Nullable String label) {
        PsiElement list = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CONDITION_LIST);
        PsiElement block = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK);
        PsiElement elseClause = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.ELSE_CLAUSE);
        if (label != null) {
            myTargets.push(new JumpTarget(statement, label, false, false, null));
        }
        try {
            Exits exits = conditionList(list);
            Boolean value = constant(list);
            PsiElement condition = list != null ? list : statement;

            branch(exits.whenTrue(), block != null ? block : statement, condition, true, value == Boolean.FALSE);
            blockStatements(block);
            myBuilder.addPendingEdge(statement, myBuilder.prevInstruction);

            branch(exits.whenFalse(), elseClause != null ? elseClause : statement, condition, false, value == Boolean.TRUE);
            if (elseClause != null) {
                PsiElement nested = SwiftFlowVariables.firstChildOfType(elseClause, SwiftTypes.IF_STATEMENT);
                if (nested != null) {
                    statement(nested);
                }
                else {
                    blockStatements(SwiftFlowVariables.firstChildOfType(elseClause, SwiftTypes.CODE_BLOCK));
                }
            }
        }
        finally {
            if (label != null) {
                myTargets.pop();
            }
        }
    }

    @RequiredReadAction
    private void guardStatement(PsiElement statement) {
        PsiElement list = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CONDITION_LIST);
        PsiElement block = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK);
        Exits exits = conditionList(list);
        Boolean value = constant(list);
        PsiElement condition = list != null ? list : statement;

        branch(exits.whenFalse(), block != null ? block : statement, condition, false, value == Boolean.TRUE);
        if (block != null) {
            blockStatements(block);
            List<Instruction> ends = collectEnds(block);
            if (!ends.isEmpty()) {
                // the else body falls through - an error, the flow stops
                join(ends, block);
                myGuardEnds.put(node(block, SwiftInstruction.GUARD_END), block);
            }
        }
        myBuilder.flowAbrupted();

        branch(exits.whenTrue(), statement, condition, true, value == Boolean.FALSE);
    }

    @RequiredReadAction
    private void whileStatement(PsiElement statement, @Nullable String label) {
        Instruction head = myBuilder.prevInstruction;
        PsiElement list = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CONDITION_LIST);
        PsiElement block = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK);
        PsiElement condition = list != null ? list : statement;
        Boolean value = constant(list);

        myTargets.push(new JumpTarget(statement, label, true, false, block));
        try {
            Exits exits = conditionList(list);
            branch(exits.whenTrue(), block != null ? block : statement, condition, true, value == Boolean.FALSE);
            blockStatements(block);
            edges(collectEnds(block != null ? block : statement), head);

            branch(exits.whenFalse(), statement, condition, false, value == Boolean.TRUE);
        }
        finally {
            myTargets.pop();
        }
    }

    @RequiredReadAction
    private void repeatStatement(PsiElement statement, @Nullable String label) {
        Instruction head = myBuilder.prevInstruction;
        PsiElement block = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK);
        PsiElement condition = null;
        for (PsiElement child = block == null ? null : block.getNextSibling(); child != null; child = child.getNextSibling()) {
            if (isExpression(child)) {
                condition = child;
            }
        }

        List<Instruction> ends;
        myTargets.push(new JumpTarget(statement, label, true, false, block));
        try {
            blockStatements(block);
            ends = collectEnds(block != null ? block : statement);
        }
        finally {
            myTargets.pop();
        }
        if (condition == null) {
            return;
        }

        edges(ends, node(condition));
        Exits exits = condition(condition);
        Boolean value = constantExpression(condition);

        branch(exits.whenTrue(), block != null ? block : statement, condition, true, value == Boolean.FALSE);
        myBuilder.addEdge(myBuilder.prevInstruction, head);

        branch(exits.whenFalse(), statement, condition, false, value == Boolean.TRUE);
    }

    @RequiredReadAction
    private void forStatement(PsiElement statement, @Nullable String label) {
        PsiElement block = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK);
        PsiElement where = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.WHERE_CLAUSE);
        boolean inSeen = false;
        for (PsiElement child = statement.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (type == SwiftTypes.IN_KEYWORD) {
                inSeen = true;
            }
            else if (inSeen && isExpression(child)) {
                expression(child);
                break;
            }
        }

        // the next element - written to the variables, or the end
        Instruction head = node(statement, SwiftInstruction.NEXT);
        List<PsiElement> variables = new ArrayList<>();
        boolean caseFor = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CASE_KEYWORD) != null;
        SwiftFlowVariables.bindings(SwiftFlowVariables.firstPattern(statement), !caseFor, variables);
        for (PsiElement variable : variables) {
            readWrite(variable, variable, Access.WRITE, SwiftFlowVariables.nameElement(variable).getTextRange());
        }

        myTargets.push(new JumpTarget(statement, label, true, false, block));
        try {
            if (where != null) {
                Exits exits = condition(expressionChild(where));
                edges(exits.whenFalse(), head);
                join(exits.whenTrue(), where);
            }
            blockStatements(block);
            edges(collectEnds(block != null ? block : statement), head);
        }
        finally {
            myTargets.pop();
        }
        myBuilder.flowAbrupted();
        myBuilder.addPendingEdge(statement, head);
    }

    @RequiredReadAction
    private void switchStatement(PsiElement statement, @Nullable String label) {
        for (PsiElement child = statement.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isExpression(child)) {
                expression(child);
                break;
            }
        }
        List<Instruction> unmatched = current();
        List<Instruction> fallthroughs = new ArrayList<>();

        List<Instruction> outerFallthroughs = myFallthroughs;
        PsiElement outerCase = myCase;
        myTargets.push(new JumpTarget(statement, label, false, true, null));
        try {
            for (PsiElement switchCase = statement.getFirstChild(); switchCase != null; switchCase = switchCase.getNextSibling()) {
                if (PsiUtilCore.getElementType(switchCase) != SwiftTypes.SWITCH_CASE) {
                    continue;
                }
                PsiElement caseLabel = SwiftFlowVariables.firstChildOfType(switchCase, SwiftTypes.CASE_LABEL);
                PsiElement defaultLabel = SwiftFlowVariables.firstChildOfType(switchCase, SwiftTypes.DEFAULT_LABEL);
                PsiElement body = SwiftFlowVariables.firstChildOfType(switchCase, SwiftTypes.STATEMENTS);

                myBuilder.flowAbrupted();
                edges(unmatched, node(caseLabel != null ? caseLabel : defaultLabel != null ? defaultLabel : switchCase));
                List<Instruction> noMatch = new ArrayList<>();
                if (caseLabel != null) {
                    noMatch.addAll(caseLabel(caseLabel));
                }

                edges(fallthroughs, node(body != null ? body : switchCase, SwiftInstruction.CASE_BODY));
                fallthroughs = new ArrayList<>();
                myFallthroughs = fallthroughs;
                myCase = switchCase;
                statements(body);
                for (Instruction end : collectEnds(switchCase)) {
                    myBuilder.addPendingEdge(statement, end);
                }
                unmatched = noMatch;
            }
        }
        finally {
            myTargets.pop();
            myFallthroughs = outerFallthroughs;
            myCase = outerCase;
        }
        // a switch is exhaustive - no way past all its cases
        myBuilder.flowAbrupted();
    }

    /**
     * The patterns of a case matched - the expressions in them read, the variables bound written; the nodes it does not
     * match from
     */
    @RequiredReadAction
    private List<Instruction> caseLabel(PsiElement caseLabel) {
        PsiElement items = SwiftFlowVariables.firstChildOfType(caseLabel, SwiftTypes.CASE_ITEM_LIST);
        List<PsiElement> caseItems = new ArrayList<>();
        for (PsiElement item = items == null ? null : items.getFirstChild(); item != null; item = item.getNextSibling()) {
            if (PsiUtilCore.getElementType(item) == SwiftTypes.CASE_ITEM) {
                caseItems.add(item);
            }
        }
        for (PsiElement item : caseItems) {
            patternExpressions(SwiftFlowVariables.firstPattern(item));
        }
        List<Instruction> noMatch = new ArrayList<>(current());
        if (!caseItems.isEmpty()) {
            // every item binds the same variables
            patternWrites(SwiftFlowVariables.firstPattern(caseItems.get(0)));
        }
        for (PsiElement item : caseItems) {
            PsiElement where = SwiftFlowVariables.firstChildOfType(item, SwiftTypes.WHERE_CLAUSE);
            if (where != null) {
                Exits exits = condition(expressionChild(where));
                noMatch.addAll(exits.whenFalse());
                join(exits.whenTrue(), where);
            }
        }
        return noMatch;
    }

    /**
     * The expressions a pattern compares with - read before it matches
     */
    @RequiredReadAction
    private void patternExpressions(@Nullable PsiElement pattern) {
        if (pattern == null) {
            return;
        }
        if (PsiUtilCore.getElementType(pattern) == SwiftTypes.EXPRESSION_PATTERN) {
            expressionChildren(pattern);
            return;
        }
        for (PsiElement child = pattern.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() != null) {
                patternExpressions(child);
            }
        }
    }

    private void patternWrites(@Nullable PsiElement pattern) {
        List<PsiElement> variables = new ArrayList<>();
        SwiftFlowVariables.bindings(pattern, false, variables);
        for (PsiElement variable : variables) {
            readWrite(variable, variable, Access.WRITE, SwiftFlowVariables.nameElement(variable).getTextRange());
        }
    }

    @RequiredReadAction
    private void doStatement(PsiElement statement, @Nullable String label) {
        PsiElement block = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.CODE_BLOCK);
        List<PsiElement> catches = new ArrayList<>();
        for (PsiElement child = statement.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == SwiftTypes.CATCH_CLAUSE) {
                catches.add(child);
            }
        }
        if (label != null) {
            myTargets.push(new JumpTarget(statement, label, false, false, null));
        }
        try {
            if (catches.isEmpty()) {
                blockStatements(block);
                return;
            }

            DoContext context = new DoContext(block != null ? block : statement, new ArrayList<>());
            myDos.push(context);
            List<Instruction> normal;
            try {
                blockStatements(block);
                normal = collectEnds(block != null ? block : statement);
            }
            finally {
                myDos.pop();
            }
            for (Instruction end : normal) {
                myBuilder.addPendingEdge(statement, end);
            }

            // an error thrown - to the first catch matching it
            Instruction dispatch = node(statement, SwiftInstruction.CATCH);
            edges(context.throwing(), dispatch);
            List<Instruction> unmatched = List.of(dispatch);
            for (PsiElement catchClause : catches) {
                myBuilder.flowAbrupted();
                edges(unmatched, node(catchClause));
                PsiElement pattern = SwiftFlowVariables.firstPattern(catchClause);
                PsiElement where = SwiftFlowVariables.firstChildOfType(catchClause, SwiftTypes.WHERE_CLAUSE);
                patternExpressions(pattern);
                List<Instruction> noMatch = isCatchAll(pattern) && where == null ? List.of() : new ArrayList<>(current());
                patternWrites(pattern);
                if (where != null) {
                    Exits exits = condition(expressionChild(where));
                    noMatch.addAll(exits.whenFalse());
                    join(exits.whenTrue(), where);
                }
                blockStatements(SwiftFlowVariables.firstChildOfType(catchClause, SwiftTypes.CODE_BLOCK));
                for (Instruction end : collectEnds(catchClause)) {
                    myBuilder.addPendingEdge(statement, end);
                }
                unmatched = noMatch;
            }
            if (!unmatched.isEmpty()) {
                // matched by no catch - thrown out of the do
                join(unmatched, statement);
                throwFrom();
            }
            myBuilder.flowAbrupted();
        }
        finally {
            if (label != null) {
                myTargets.pop();
            }
        }
    }

    /**
     * {@code catch}, {@code catch let e}, {@code catch _} - matches every error
     */
    private static boolean isCatchAll(@Nullable PsiElement pattern) {
        if (pattern == null) {
            return true;
        }
        String text = pattern.getText().replaceAll("\\s+", " ").trim();
        return text.matches("(let |var )?[A-Za-z_][A-Za-z0-9_]*");
    }

    /**
     * {@code #if} - one of its branches, or none without an else
     */
    @RequiredReadAction
    private void compilationBlock(PsiElement statement) {
        PsiElement block = PsiUtilCore.getElementType(statement) == SwiftTypes.COMPILER_CONTROL_STATEMENT
            ? SwiftFlowVariables.firstComposite(statement) : statement;
        if (PsiUtilCore.getElementType(block) != SwiftTypes.CONDITIONAL_COMPILATION_BLOCK) {
            return;
        }
        List<Instruction> start = current();
        List<Instruction> ends = new ArrayList<>();
        boolean hasElse = false;
        for (PsiElement child = block.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (type == SwiftTypes.HASH_ELSE) {
                hasElse = true;
            }
            else if (type == SwiftTypes.STATEMENTS) {
                join(start, child);
                statements(child);
                ends.addAll(collectEnds(child));
            }
        }
        if (!hasElse) {
            ends.addAll(start);
        }
        join(ends, block);
    }

    /**
     * break and continue - through the defer blocks of the scopes left, to the end of the target, of its body for continue
     */
    @RequiredReadAction
    private void jump(PsiElement statement, boolean isContinue) {
        PsiElement labelElement = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.IDENTIFIER);
        String label = labelElement == null ? null : SwiftFlowVariables.unquote(labelElement.getText());
        JumpTarget target = null;
        for (JumpTarget candidate : myTargets) {
            if (label != null ? label.equals(candidate.label()) && (!isContinue || candidate.loop())
                : isContinue ? candidate.loop() : candidate.loop() || candidate.switchStatement()) {
                target = candidate;
                break;
            }
        }
        // out of nothing - an error, the flow goes on
        if (target == null) {
            return;
        }
        PsiElement scope = isContinue ? target.body() : target.statement();
        if (scope == null) {
            scope = target.statement();
        }
        runDefers(scope);
        exitTo(scope);
    }

    /**
     * An error thrown at the current node - through the defer blocks to the catches of the enclosing do, or out of the
     * owner; the flow goes on from the node
     */
    @RequiredReadAction
    private void throwFrom() {
        Instruction from = myBuilder.prevInstruction;
        if (from == null) {
            return;
        }
        DoContext context = myDos.peek();
        runDefers(context == null ? null : context.body());
        if (context != null) {
            context.throwing().add(myBuilder.prevInstruction);
        }
        else {
            myBuilder.addPendingEdge(null, myBuilder.prevInstruction);
        }
        myBuilder.prevInstruction = from;
    }

    @RequiredReadAction
    private void blockStatements(@Nullable PsiElement block) {
        statements(SwiftFlowVariables.firstChildOfType(block, SwiftTypes.STATEMENTS));
    }

    // endregion

    // region conditions

    /**
     * {@code if a, let b = c, case .x(let y) = d} - one after another, each binding its variables when it holds; false
     * when any is false
     */
    @RequiredReadAction
    private Exits conditionList(@Nullable PsiElement list) {
        if (list == null) {
            return new Exits(current(), current());
        }
        List<Instruction> whenFalse = new ArrayList<>();
        for (PsiElement condition = list.getFirstChild(); condition != null; condition = condition.getNextSibling()) {
            if (PsiUtilCore.getElementType(condition) != SwiftTypes.CONDITION) {
                continue;
            }
            PsiElement inner = SwiftFlowVariables.firstComposite(condition);
            IElementType type = PsiUtilCore.getElementType(inner);
            if (inner == null || type == SwiftTypes.AVAILABILITY_CONDITION) {
                whenFalse.addAll(current());
            }
            else if (type == SwiftTypes.OPTIONAL_BINDING_CONDITION) {
                expression(expressionChild(inner));
                whenFalse.addAll(current());
                List<PsiElement> variables = new ArrayList<>();
                SwiftFlowVariables.bindings(SwiftFlowVariables.firstPattern(inner), true, variables);
                for (PsiElement variable : variables) {
                    readWrite(variable, variable, Access.WRITE, SwiftFlowVariables.nameElement(variable).getTextRange());
                }
            }
            else if (type == SwiftTypes.CASE_CONDITION) {
                PsiElement pattern = SwiftFlowVariables.firstPattern(inner);
                patternExpressions(pattern);
                expression(expressionChild(inner));
                whenFalse.addAll(current());
                patternWrites(pattern);
                PsiElement where = SwiftFlowVariables.firstChildOfType(inner, SwiftTypes.WHERE_CLAUSE);
                if (where != null) {
                    Exits exits = condition(expressionChild(where));
                    whenFalse.addAll(exits.whenFalse());
                    join(exits.whenTrue(), where);
                }
            }
            else {
                Exits exits = condition(inner);
                whenFalse.addAll(exits.whenFalse());
                join(exits.whenTrue(), condition);
            }
        }
        return new Exits(current(), whenFalse);
    }

    /**
     * A literal true or false - the only condition of the list
     */
    private static @Nullable Boolean constant(@Nullable PsiElement list) {
        if (list == null) {
            return null;
        }
        PsiElement single = null;
        for (PsiElement condition = list.getFirstChild(); condition != null; condition = condition.getNextSibling()) {
            if (PsiUtilCore.getElementType(condition) == SwiftTypes.CONDITION) {
                if (single != null) {
                    return null;
                }
                single = condition;
            }
        }
        return single == null ? null : constantExpression(SwiftFlowVariables.firstComposite(single));
    }

    private static @Nullable Boolean constantExpression(@Nullable PsiElement expression) {
        if (expression == null) {
            return null;
        }
        String text = expression.getText().replaceAll("[\\s()]", "");
        if (!text.equals("true") && !text.equals("false")) {
            return null;
        }
        // not a variable named `true`
        PsiElement leaf = PsiTreeUtil.getDeepestFirst(expression);
        while (leaf != null && PsiUtilCore.getElementType(leaf) == SwiftTypes.LPAREN) {
            leaf = PsiTreeUtil.nextVisibleLeaf(leaf);
        }
        IElementType type = PsiUtilCore.getElementType(leaf);
        return type == SwiftTypes.TRUE_KEYWORD || type == SwiftTypes.FALSE_KEYWORD ? Boolean.valueOf(text) : null;
    }

    @RequiredReadAction
    private Exits condition(@Nullable PsiElement expression) {
        return condition(expression == null ? null : SwiftExpressionTree.of(expression));
    }

    /**
     * The exits of a boolean expression - &&, || and ! branch, anything else is evaluated
     */
    @RequiredReadAction
    private Exits condition(@Nullable Node node) {
        if (node == null) {
            return new Exits(current(), current());
        }
        if (node instanceof Binary binary && (binary.kind() == Kind.AND || binary.kind() == Kind.OR)) {
            boolean and = binary.kind() == Kind.AND;
            Exits left = condition(binary.left());
            join(and ? left.whenTrue() : left.whenFalse(), binary.operator());
            Exits right = condition(binary.right());
            return and
                ? new Exits(right.whenTrue(), concat(left.whenFalse(), right.whenFalse()))
                : new Exits(concat(left.whenTrue(), right.whenTrue()), right.whenFalse());
        }
        if (node instanceof Operand operand) {
            if (!operand.prefix().isEmpty() && PsiUtilCore.getElementType(operand.prefix().get(0)) == SwiftTypes.EXCL) {
                List<PsiElement> prefix = operand.prefix().subList(1, operand.prefix().size());
                return condition(new Operand(prefix, operand.head(), operand.suffixes())).swap();
            }
            if (operand.isSimple() && PsiUtilCore.getElementType(operand.head()) == SwiftTypes.PARENTHESIZED_OR_TUPLE_EXPRESSION) {
                List<PsiElement> elements = tupleElements(operand.head());
                if (elements.size() == 1 && SwiftFlowVariables.firstChildOfType(elements.get(0), SwiftTypes.COLON) == null) {
                    return condition(expressionChild(elements.get(0)));
                }
            }
        }
        evaluate(node);
        return new Exits(current(), current());
    }

    private static List<PsiElement> tupleElements(PsiElement tuple) {
        List<PsiElement> elements = new ArrayList<>();
        for (PsiElement child = tuple.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == SwiftTypes.TUPLE_ELEMENT) {
                elements.add(child);
            }
        }
        return elements;
    }

    // endregion

    // region expressions

    private static boolean isExpression(@Nullable PsiElement element) {
        if (element == null || element.getFirstChild() == null) {
            return false;
        }
        IElementType type = PsiUtilCore.getElementType(element);
        return type == SwiftTypes.EXPRESSION || type == SwiftTypes.COND_EXPRESSION || element instanceof SwiftExpression;
    }

    /**
     * The first expression child - the value of an initializer, a where clause, a condition binding
     */
    private static @Nullable PsiElement expressionChild(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isExpression(child)) {
                return child;
            }
        }
        return null;
    }

    /**
     * The expression children of an element, one after another
     */
    @RequiredReadAction
    private void expressionChildren(PsiElement element) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isExpression(child)) {
                expression(child);
            }
        }
    }

    @RequiredReadAction
    private void expression(@Nullable PsiElement expression) {
        if (expression == null) {
            return;
        }
        Node node = SwiftExpressionTree.of(expression);
        if (node != null) {
            evaluate(node);
        }
    }

    @RequiredReadAction
    private void evaluate(Node node) {
        if (node instanceof Operand operand) {
            operand(operand, Access.READ);
        }
        else if (node instanceof Binary binary) {
            switch (binary.kind()) {
                case ASSIGN -> {
                    evaluate(binary.right());
                    target(binary.left(), Access.WRITE, binary.operator().getTextRange());
                }
                case COMPOUND_ASSIGN -> {
                    evaluate(binary.right());
                    target(binary.left(), Access.INOUT, binary.operator().getTextRange());
                }
                case AND, OR -> {
                    Exits exits = condition(binary);
                    join(concat(exits.whenTrue(), exits.whenFalse()), binary.operator());
                }
                case COALESCE -> {
                    // x ?? y - y is not evaluated when x is not nil
                    evaluate(binary.left());
                    List<Instruction> notNil = current();
                    evaluate(binary.right());
                    join(concat(notNil, current()), binary.operator());
                }
                default -> {
                    evaluate(binary.left());
                    evaluate(binary.right());
                }
            }
        }
        else if (node instanceof Ternary ternary) {
            Exits exits = condition(ternary.condition());
            join(exits.whenTrue(), ternary.operator());
            expression(expressionChild(ternary.operator()));
            List<Instruction> trueEnd = current();
            join(exits.whenFalse(), ternary.operator());
            evaluate(ternary.elseNode());
            join(concat(trueEnd, current()), ternary.operator());
        }
        else if (node instanceof Cast cast) {
            evaluate(cast.operand());
        }
        else if (node instanceof Postfix postfix) {
            evaluate(postfix.operand());
        }
        else if (node instanceof Try tryNode) {
            evaluate(tryNode.operand());
            if (tryNode.isThrowing() && myBuilder.prevInstruction != null) {
                node(tryNode.operator(), SwiftInstruction.THROWS);
                throwFrom();
            }
        }
    }

    /**
     * The target of an assignment - a variable written, a tuple of them, or anything else evaluated with its base read
     */
    @RequiredReadAction
    private void target(Node target, Access access, TextRange range) {
        if (!(target instanceof Operand operand)) {
            evaluate(target);
            return;
        }
        PsiElement head = operand.head();
        IElementType type = PsiUtilCore.getElementType(head);
        if (operand.isSimple()) {
            if (type == SwiftTypes.PRIMARY_EXPRESSION) {
                PsiElement identifier = identifierOf(head);
                if (identifier != null) {
                    reference(head, identifier, access, range);
                    return;
                }
            }
            else if (type == SwiftTypes.PARENTHESIZED_OR_TUPLE_EXPRESSION) {
                for (PsiElement element : tupleElements(head)) {
                    PsiElement expression = expressionChild(element);
                    Node node = expression == null ? null : SwiftExpressionTree.of(expression);
                    if (node != null) {
                        target(node, access, range);
                    }
                }
                return;
            }
            else if (type == SwiftTypes.WILDCARD_EXPRESSION) {
                return;
            }
            else if (type == SwiftTypes.SELF_EXPRESSION && myInitializer != null) {
                // self.x = 1
                PsiElement identifier = SwiftFlowVariables.firstChildOfType(head, SwiftTypes.IDENTIFIER);
                PsiElement property = identifier == null ? null : myInitializer.properties().get(SwiftFlowVariables.unquote(identifier.getText()));
                if (property != null) {
                    readWrite(head, property, access, range);
                    return;
                }
            }
        }
        operand(operand, Access.MEMBER);
    }

    /**
     * An operand - its head read (or, as the base of a member assigned, used), then its suffixes: the arguments of a call,
     * the subscripts, the trailing closures; a call that never returns ends the flow
     */
    @RequiredReadAction
    private void operand(Operand operand, Access access) {
        PsiElement superInit = null;
        if (myInitializer == null || !initializerHead(operand, access)) {
            head(operand.head(), access);
        }
        else if (PsiUtilCore.getElementType(operand.head()) == SwiftTypes.SUPERCLASS_EXPRESSION) {
            superInit = operand.head();
        }
        PsiElement outerCallee = myCallee;
        myCallee = operand.head();
        try {
            for (PsiElement suffix : operand.suffixes()) {
                suffix(suffix);
            }
        }
        finally {
            myCallee = outerCallee;
        }
        if (superInit != null) {
            node(superInit, SwiftInstruction.SUPER_INIT);
        }
        if (isNoReturnCall(operand)) {
            myNoReturns.add(node(operand.head(), SwiftInstruction.NO_RETURN));
        }
    }

    /**
     * The head of an operand using self in an initializer - a stored property read, a method called, a computed property
     * read, self itself, super.init; false if it is not one
     */
    @RequiredReadAction
    private boolean initializerHead(Operand operand, Access access) {
        PsiElement head = operand.head();
        IElementType type = PsiUtilCore.getElementType(head);
        PsiElement identifier;
        if (type == SwiftTypes.SUPERCLASS_EXPRESSION) {
            return SwiftFlowVariables.firstChildOfType(head, SwiftTypes.INIT_KEYWORD) != null;
        }
        if (type == SwiftTypes.SELF_EXPRESSION) {
            identifier = SwiftFlowVariables.firstChildOfType(head, SwiftTypes.IDENTIFIER);
            if (identifier == null) {
                if (SwiftFlowVariables.firstChildOfType(head, SwiftTypes.LBRACK) != null) {
                    return false;
                }
                myBuilder.addNodeAndCheckPending(new SwiftSelfUseInstruction(myBuilder, head, SwiftSelfUseInstruction.Kind.SELF, null, head.getTextRange()));
                return true;
            }
        }
        else if (type == SwiftTypes.PRIMARY_EXPRESSION) {
            identifier = identifierOf(head);
            if (identifier == null) {
                return false;
            }
            String name = SwiftFlowVariables.unquote(identifier.getText());
            if (myNames.contains(name) && myResolver.resolve(name, identifier) != null) {
                return false;
            }
        }
        else {
            return false;
        }

        String name = SwiftFlowVariables.unquote(identifier.getText());
        PsiElement property = myInitializer.properties().get(name);
        if (property != null) {
            readWrite(head, property, access == Access.MEMBER ? Access.MEMBER : Access.READ, identifier.getTextRange());
            return true;
        }
        IElementType first = operand.suffixes().isEmpty() ? null : PsiUtilCore.getElementType(operand.suffixes().get(0));
        boolean call = first == SwiftTypes.FUNCTION_CALL_ARGUMENT_CLAUSE || first == SwiftTypes.TRAILING_CLOSURE;
        SwiftSelfUseInstruction.Kind kind = call && myInitializer.methods().contains(name) ? SwiftSelfUseInstruction.Kind.METHOD
            : myInitializer.computed().contains(name) ? SwiftSelfUseInstruction.Kind.PROPERTY : null;
        if (kind == null) {
            return type == SwiftTypes.SELF_EXPRESSION;
        }
        myBuilder.addNodeAndCheckPending(new SwiftSelfUseInstruction(myBuilder, head, kind, name, identifier.getTextRange()));
        return true;
    }

    @RequiredReadAction
    private boolean isNoReturnCall(Operand operand) {
        if (operand.suffixes().isEmpty() || PsiUtilCore.getElementType(operand.head()) != SwiftTypes.PRIMARY_EXPRESSION) {
            return false;
        }
        IElementType first = PsiUtilCore.getElementType(operand.suffixes().get(0));
        if (first != SwiftTypes.FUNCTION_CALL_ARGUMENT_CLAUSE && first != SwiftTypes.TRAILING_CLOSURE) {
            return false;
        }
        PsiElement identifier = identifierOf(operand.head());
        if (identifier == null) {
            return false;
        }
        String name = SwiftFlowVariables.unquote(identifier.getText());
        return myNoReturnFunctions.contains(name) && (!myNames.contains(name) || myResolver.resolve(name, identifier) == null);
    }

    /**
     * The IDENTIFIER of a primary expression which is only a name
     */
    private static @Nullable PsiElement identifierOf(PsiElement primary) {
        PsiElement identifier = null;
        for (PsiElement child = primary.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!SwiftExpressionTree.isSignificant(child)) {
                continue;
            }
            if (PsiUtilCore.getElementType(child) == SwiftTypes.IDENTIFIER && identifier == null) {
                identifier = child;
            }
            else if (PsiUtilCore.getElementType(child) != SwiftTypes.GENERIC_ARGUMENT_CLAUSE) {
                return null;
            }
        }
        return identifier;
    }

    @RequiredReadAction
    private void head(PsiElement head, Access access) {
        IElementType type = PsiUtilCore.getElementType(head);
        if (type == SwiftTypes.PRIMARY_EXPRESSION) {
            PsiElement identifier = identifierOf(head);
            if (identifier != null) {
                reference(head, identifier, access, identifier.getTextRange());
                return;
            }
            elementChildren(head);
        }
        else if (type == SwiftTypes.IN_OUT_EXPRESSION) {
            // &x - read and written by the call, reported at it
            PsiElement identifier = SwiftFlowVariables.firstChildOfType(head, SwiftTypes.IDENTIFIER);
            if (identifier != null) {
                reference(head, identifier, Access.INOUT, (myCallee != null ? myCallee : head).getTextRange());
            }
        }
        else {
            element(head);
        }
    }

    /**
     * A reference to a variable by its name - read, written, used
     */
    @RequiredReadAction
    private void reference(PsiElement element, PsiElement identifier, Access access, TextRange range) {
        String name = SwiftFlowVariables.unquote(identifier.getText());
        PsiElement variable = myNames.contains(name) ? myResolver.resolve(name, identifier) : null;
        if (variable == null && myInitializer != null) {
            // x = 1 in an initializer - a stored property
            variable = myInitializer.properties().get(name);
        }
        if (variable != null) {
            readWrite(element, variable, access, range);
        }
    }

    @RequiredReadAction
    private void suffix(PsiElement suffix) {
        IElementType type = PsiUtilCore.getElementType(suffix);
        if (type == SwiftTypes.FUNCTION_CALL_ARGUMENT_CLAUSE) {
            PsiElement list = SwiftFlowVariables.firstChildOfType(suffix, SwiftTypes.FUNCTION_CALL_ARGUMENT_LIST);
            for (PsiElement argument = list == null ? null : list.getFirstChild(); argument != null; argument = argument.getNextSibling()) {
                if (PsiUtilCore.getElementType(argument) == SwiftTypes.FUNCTION_CALL_ARGUMENT) {
                    expressionChildren(argument);
                }
            }
        }
        else if (isExpression(suffix) || type == SwiftTypes.TRAILING_CLOSURE) {
            element(suffix);
        }
    }

    /**
     * Any element of an expression - a closure created, an if or a switch as a value, an expression evaluated, the
     * interpolations of a string read, or its children
     */
    @RequiredReadAction
    private void element(PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        if (type == SwiftTypes.CLOSURE_EXPRESSION) {
            node(element);
        }
        else if (type == SwiftTypes.IF_STATEMENT || type == SwiftTypes.SWITCH_STATEMENT) {
            statement(element);
        }
        else if (type == SwiftTypes.EXPRESSION || type == SwiftTypes.COND_EXPRESSION || type == SwiftTypes.POSTFIX_EXPRESSION
            || type == SwiftTypes.PREFIX_EXPRESSION || type == SwiftTypes.IN_OUT_EXPRESSION || type == SwiftTypes.PRIMARY_EXPRESSION) {
            expression(element);
        }
        else if (type == SwiftTypes.INTERPOLATED_STRING_LITERAL || type == SwiftTypes.STATIC_STRING_LITERAL) {
            interpolation(element);
        }
        else if (element.getFirstChild() != null) {
            elementChildren(element);
        }
    }

    @RequiredReadAction
    private void elementChildren(PsiElement element) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (child.getFirstChild() != null || type == SwiftTypes.INTERPOLATED_STRING_LITERAL || type == SwiftTypes.STATIC_STRING_LITERAL) {
                if (type == SwiftTypes.TYPE || type == SwiftTypes.GENERIC_ARGUMENT_CLAUSE || type == SwiftTypes.ARGUMENT_NAMES) {
                    continue;
                }
                element(child);
            }
        }
    }

    /**
     * {@code "\(x)"}, {@code #"\#(x)"#} - the names in the interpolations of a string read; the lexer keeps the string one
     * token
     */
    @RequiredReadAction
    private void interpolation(PsiElement literal) {
        String text = literal.getText();
        int offset = literal.getTextRange().getStartOffset();
        int hashes = 0;
        while (hashes < text.length() && text.charAt(hashes) == '#') {
            hashes++;
        }
        String open = "\\" + "#".repeat(hashes) + "(";
        int index = text.indexOf(open);
        while (index >= 0) {
            int start = index + open.length();
            int depth = 1;
            int end = start;
            while (end < text.length() && depth > 0) {
                char c = text.charAt(end);
                if (c == '(') {
                    depth++;
                }
                else if (c == ')') {
                    depth--;
                }
                end++;
            }
            int i = start;
            while (i < end - 1) {
                char c = text.charAt(i);
                if (Character.isJavaIdentifierStart(c) && c != '$' && (i == start || text.charAt(i - 1) != '.')) {
                    int nameEnd = i;
                    while (nameEnd < end - 1 && Character.isJavaIdentifierPart(text.charAt(nameEnd)) && text.charAt(nameEnd) != '$') {
                        nameEnd++;
                    }
                    String name = text.substring(i, nameEnd);
                    if (myNames.contains(name)) {
                        PsiElement variable = myResolver.resolve(name, literal);
                        if (variable != null) {
                            readWrite(literal, variable, Access.READ, new TextRange(offset + i, offset + nameEnd));
                        }
                    }
                    i = nameEnd;
                }
                else {
                    i++;
                }
            }
            index = text.indexOf(open, end);
        }
    }

    // endregion
}
