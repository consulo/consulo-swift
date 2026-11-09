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
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.controlFlow.Instruction;
import consulo.language.dataFlow.DFAEngine;
import consulo.language.dataFlow.DFALimitExceededException;
import consulo.language.dataFlow.DfaInstance;
import consulo.language.dataFlow.Semilattice;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.swift.language.impl.localize.SwiftFlowLocalize;
import consulo.swift.language.impl.psi.controlFlow.SwiftFlowDiagnostics.Problem;
import consulo.swift.language.impl.psi.controlFlow.SwiftReadWriteInstruction.Access;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Definite initialization (the DefiniteInitialization pass of swiftc, verified with Swift 6.4) on a
 * {@link SwiftControlFlow} folded: the variables initialized on every path to a node (intersection) and on some path
 * (union), on the platform {@link DFAEngine}; everything at a node the folded flow does not reach.
 * <ul>
 * <li>a let or var declared without a value of the owner, read before ("variable 'x' used before being initialized"),
 * passed by reference ({@code x += 1}, {@code &x}), captured by a closure created, a let initialized when it may already
 * be ("immutable value 'x' may only be initialized once")</li>
 * <li>in an initializer of a struct or a class, its stored properties: read before, self used (a method called, a
 * computed property, self itself), at super.init, at a return and at the end of the body</li>
 * </ul>
 *
 * @author VISTALL
 */
public final class SwiftDefiniteInitialization {
    /**
     * Initialized on every path (null - a node not reached: all of them), on some path
     */
    private record State(@Nullable BitSet definite, BitSet maybe) {
        boolean isDefinite(int index) {
            return definite == null || definite.get(index);
        }
    }

    private static final State UNREACHED = new State(null, new BitSet());

    private SwiftDefiniteInitialization() {
    }

    @RequiredReadAction
    static void compute(SwiftControlFlow flow, List<Problem> problems) {
        PsiElement owner = flow.getOwner();
        if (owner instanceof PsiFile) {
            return;
        }
        SwiftInitializerInfo initializer = flow.getInitializer();
        Instruction[] instructions = flow.getInstructions();

        // the variables of the owner declared without a value, the stored properties
        Map<PsiElement, Integer> indexes = new LinkedHashMap<>();
        if (initializer != null) {
            for (PsiElement property : initializer.properties().values()) {
                indexes.put(property, indexes.size());
            }
        }
        boolean closures = false;
        for (Instruction instruction : instructions) {
            if (instruction instanceof SwiftReadWriteInstruction readWrite && readWrite.getAccess() == Access.DECLARE
                && !SwiftFlowVariables.isImplicitlyInitialized(readWrite.getVariable())) {
                indexes.putIfAbsent(readWrite.getVariable(), indexes.size());
            }
            closures |= isClosureCreated(instruction);
        }
        if (indexes.isEmpty()) {
            return;
        }

        List<State> states;
        try {
            states = new DFAEngine<>(instructions, new Transfer(flow, indexes), new Join()).performDFA();
        }
        catch (DFALimitExceededException e) {
            // too complex - nothing reported
            return;
        }

        Set<String> reported = new HashSet<>();
        for (Instruction instruction : instructions) {
            if (!flow.isReachable(instruction)) {
                continue;
            }
            State after = states.get(instruction.num());
            if (instruction instanceof SwiftReadWriteInstruction readWrite) {
                Integer index = indexes.get(readWrite.getVariable());
                if (index == null) {
                    continue;
                }
                boolean property = initializer != null && initializer.isProperty(readWrite.getVariable());
                if (readWrite.getAccess() == Access.WRITE) {
                    State before = before(instruction, states);
                    if (before.maybe().get(index) && SwiftFlowVariables.isConstant(readWrite.getVariable())) {
                        String name = (property ? "self." : "") + SwiftFlowVariables.name(readWrite.getVariable());
                        add(problems, reported, readWrite.getRange(), SwiftFlowLocalize.initializedTwice(name));
                    }
                }
                else if ((readWrite.getAccess() == Access.READ || readWrite.getAccess() == Access.INOUT) && !before(instruction, states).isDefinite(index)) {
                    add(problems, reported, readWrite.getRange(), uninitialized(readWrite, property, initializer));
                }
                continue;
            }
            if (initializer != null && instruction instanceof SwiftSelfUseInstruction selfUse) {
                String first = firstUninitialized(after, indexes, initializer);
                if (first != null) {
                    add(problems, reported, selfUse.getRange(), selfUse(selfUse, initializer, first));
                }
                continue;
            }
            if (initializer != null && instruction instanceof SwiftInstruction marked && marked.is(SwiftInstruction.SUPER_INIT)) {
                PsiElement init = SwiftFlowVariables.firstChildOfType(marked.getElement(), SwiftTypes.INIT_KEYWORD);
                TextRange range = (init != null ? init : marked.getElement()).getTextRange();
                for (Map.Entry<String, PsiElement> property : initializer.properties().entrySet()) {
                    if (!after.isDefinite(indexes.get(property.getValue()))) {
                        add(problems, reported, range, SwiftFlowLocalize.propertyNotInitializedAtSuperInit(property.getKey()));
                    }
                }
                continue;
            }
            if (closures && isClosureCreated(instruction)) {
                captures(instruction.getElement(), after, indexes, initializer, problems, reported);
            }
        }

        if (initializer != null && !initializer.properties().isEmpty()) {
            for (Map.Entry<Instruction, PsiElement> exit : flow.getReturns().entrySet()) {
                PsiElement statement = exit.getValue();
                if (flow.isReachable(exit.getKey()) && !isReturnNil(statement)
                    && firstUninitialized(states.get(exit.getKey().num()), indexes, initializer) != null) {
                    add(problems, reported, statement.getFirstChild().getTextRange(), SwiftFlowLocalize.returnWithoutInitializing());
                }
            }
            Instruction end = flow.getEnd();
            PsiElement block = SwiftControlFlow.bodyBlock(owner);
            if (flow.isReachable(end) && block != null && firstUninitialized(states.get(end.num()), indexes, initializer) != null) {
                PsiElement brace = block.getLastChild();
                add(problems, reported, brace.getTextRange(), SwiftFlowLocalize.returnWithoutInitializing());
            }
        }
    }

    private static void add(List<Problem> problems, Set<String> reported, TextRange range, LocalizeValue message) {
        if (reported.add(range + message.toString())) {
            problems.add(new Problem(range, true, message));
        }
    }

    /**
     * The state before the node - the join of its predecessors
     */
    private static State before(Instruction instruction, List<State> states) {
        ArrayList<State> ins = new ArrayList<>();
        for (Instruction predecessor : instruction.allPred()) {
            ins.add(states.get(predecessor.num()));
        }
        return Join.join(ins, instruction.num() == 0);
    }

    private static boolean isReturnNil(PsiElement statement) {
        return PsiUtilCore.getElementType(statement) == SwiftTypes.RETURN_STATEMENT && statement.getText().replaceAll("\\s+", "").equals("returnnil");
    }

    /**
     * The first stored property in the order of the declarations not initialized - null if all are
     */
    private static @Nullable String firstUninitialized(State state, Map<PsiElement, Integer> indexes, SwiftInitializerInfo initializer) {
        for (Map.Entry<String, PsiElement> property : initializer.properties().entrySet()) {
            if (!state.isDefinite(indexes.get(property.getValue()))) {
                return property.getKey();
            }
        }
        return null;
    }

    private static LocalizeValue uninitialized(SwiftReadWriteInstruction readWrite, boolean property, @Nullable SwiftInitializerInfo initializer) {
        String name = SwiftFlowVariables.name(readWrite.getVariable());
        if (property) {
            // swiftc: a struct is self, a property of a class is of the constant self
            return initializer != null && initializer.isClass()
                ? SwiftFlowLocalize.usedBeforeInitialized("constant", "self." + name)
                : SwiftFlowLocalize.selfUsedBeforeInitialized();
        }
        String kind = SwiftFlowVariables.isConstant(readWrite.getVariable()) ? "constant" : "variable";
        return readWrite.getAccess() == Access.INOUT
            ? SwiftFlowLocalize.passedByReferenceBeforeInitialized(kind, name)
            : SwiftFlowLocalize.usedBeforeInitialized(kind, name);
    }

    private static LocalizeValue selfUse(SwiftSelfUseInstruction selfUse, SwiftInitializerInfo initializer, String first) {
        if (!initializer.isClass()) {
            return SwiftFlowLocalize.selfUsedBeforeInitialized();
        }
        return switch (selfUse.getKind()) {
            case METHOD -> SwiftFlowLocalize.selfUsedInMethodCall(selfUse.getName());
            case PROPERTY -> SwiftFlowLocalize.selfUsedInPropertyAccess(selfUse.getName());
            case SELF -> SwiftFlowLocalize.usedBeforeInitialized("variable", "self." + first);
        };
    }

    /**
     * A closure created - the variables of the owner it reads, in its body or in the closures in it, must be initialized
     */
    @RequiredReadAction
    private static void captures(PsiElement closure,
                                 State state,
                                 Map<PsiElement, Integer> indexes,
                                 @Nullable SwiftInitializerInfo initializer,
                                 List<Problem> problems,
                                 Set<String> reported) {
        Set<PsiElement> read = new LinkedHashSet<>();
        capturedReads(closure, read);
        for (PsiElement variable : read) {
            Integer index = indexes.get(variable);
            if (index == null || state.isDefinite(index) || initializer != null && initializer.isProperty(variable)) {
                continue;
            }
            String kind = SwiftFlowVariables.isConstant(variable) ? "constant" : "variable";
            TextRange brace = closure.getFirstChild().getTextRange();
            add(problems, reported, brace, SwiftFlowLocalize.capturedBeforeInitialized(kind, SwiftFlowVariables.name(variable)));
        }
    }

    @RequiredReadAction
    private static void capturedReads(PsiElement closure, Set<PsiElement> read) {
        SwiftControlFlow flow = SwiftControlFlow.of(closure);
        if (flow == null) {
            return;
        }
        for (Instruction instruction : flow.getInstructions()) {
            if (instruction instanceof SwiftReadWriteInstruction readWrite && readWrite.isRead()) {
                read.add(readWrite.getVariable());
            }
            else if (isClosureCreated(instruction)) {
                capturedReads(instruction.getElement(), read);
            }
        }
    }

    /**
     * The node of a closure created - not the end of the closure owning the graph
     */
    private static boolean isClosureCreated(Instruction instruction) {
        return instruction instanceof SwiftInstruction marked && marked.getMarker() == null
            && PsiUtilCore.getElementType(instruction.getElement()) == SwiftTypes.CLOSURE_EXPRESSION;
    }

    private record Transfer(SwiftControlFlow flow, Map<PsiElement, Integer> indexes) implements DfaInstance<State> {
        @Override
        public State fun(State state, Instruction instruction) {
            // a call never returning - nothing after it
            if (!flow.isReachable(instruction) || instruction instanceof SwiftInstruction marked && marked.is(SwiftInstruction.NO_RETURN)) {
                return UNREACHED;
            }
            if (!(instruction instanceof SwiftReadWriteInstruction readWrite)) {
                return state;
            }
            Integer index = indexes.get(readWrite.getVariable());
            Access access = readWrite.getAccess();
            if (index == null || access == Access.READ || access == Access.MEMBER) {
                return state;
            }
            boolean initialize = access != Access.DECLARE;
            if (state.isDefinite(index) == initialize && state.maybe().get(index) == initialize) {
                return state;
            }
            BitSet definite = state.definite() == null ? all(indexes.size()) : (BitSet) state.definite().clone();
            BitSet maybe = (BitSet) state.maybe().clone();
            definite.set(index, initialize);
            maybe.set(index, initialize);
            return new State(definite, maybe);
        }

        @Override
        public State initial() {
            return UNREACHED;
        }

        @Override
        public boolean isForward() {
            return true;
        }

        private static BitSet all(int size) {
            BitSet bitSet = new BitSet(size);
            bitSet.set(0, size);
            return bitSet;
        }
    }

    /**
     * Definitely initialized on every path, maybe on some - the entry, with no predecessor, has nothing initialized
     */
    private static final class Join implements Semilattice<State> {
        @Override
        public State join(ArrayList<State> ins) {
            return join(ins, ins.isEmpty());
        }

        static State join(List<State> ins, boolean entry) {
            if (entry && ins.isEmpty()) {
                return new State(new BitSet(), new BitSet());
            }
            BitSet definite = null;
            BitSet maybe = new BitSet();
            boolean reached = false;
            for (State state : ins) {
                maybe.or(state.maybe());
                if (state.definite() == null) {
                    continue;
                }
                reached = true;
                if (definite == null) {
                    definite = (BitSet) state.definite().clone();
                }
                else {
                    definite.and(state.definite());
                }
            }
            return reached ? new State(definite, maybe) : new State(null, maybe);
        }

        @Override
        public boolean eq(State first, State second) {
            return Objects.equals(first.definite(), second.definite()) && first.maybe().equals(second.maybe());
        }
    }
}
