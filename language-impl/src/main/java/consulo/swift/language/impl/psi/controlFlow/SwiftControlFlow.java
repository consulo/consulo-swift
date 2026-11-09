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
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.controlFlow.ControlFlow;
import consulo.language.controlFlow.Instruction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.swift.language.psi.SwiftExpression;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * The control flow graph of a flow owner - a function, an initializer, a deinitializer, an accessor (a getter, a setter,
 * willSet, didSet, the body of a computed variable or a subscript), a closure, the top-level code of a file - with the
 * reachability of its nodes. Nested owners are opaque: a closure is one node, a nested function none.
 * <p>
 * Two reachabilities: by the structure of the code, as SILGen emits it (a call that never returns and a literal
 * condition do not stop the flow - "code after 'return' will never be executed"), and after the mandatory folding of
 * the compiler (they do - missing return, guard falling through, definite initialization).
 *
 * @author VISTALL
 */
public final class SwiftControlFlow implements ControlFlow {
    private static final TokenSet ACCESSORS = TokenSet.create(SwiftTypes.GETTER_CLAUSE, SwiftTypes.SETTER_CLAUSE, SwiftTypes.WILL_SET_CLAUSE,
        SwiftTypes.DID_SET_CLAUSE, SwiftTypes.DEINITIALIZER_DECLARATION, SwiftTypes.INITIALIZER_DECLARATION);

    private final PsiElement myOwner;
    private final Instruction[] myInstructions;
    private final Instruction myEnd;
    private final Map<Instruction, PsiElement> myReturns;
    private final Map<Instruction, PsiElement> myGuardEnds;
    private final List<Instruction> myNoReturns;
    private final Map<PsiElement, List<Instruction>> myIndex = new HashMap<>();
    private final BitSet myStructural = new BitSet();
    private final BitSet myReachable = new BitSet();

    SwiftControlFlow(PsiElement owner,
                     Instruction[] instructions,
                     Instruction end,
                     Map<Instruction, PsiElement> returns,
                     Map<Instruction, PsiElement> guardEnds,
                     List<Instruction> noReturns) {
        myOwner = owner;
        myInstructions = instructions;
        myEnd = end;
        myReturns = returns;
        myGuardEnds = guardEnds;
        myNoReturns = noReturns;

        for (Instruction instruction : instructions) {
            PsiElement element = instruction.getElement();
            if (element != null) {
                myIndex.computeIfAbsent(element, it -> new ArrayList<>(1)).add(instruction);
            }
        }

        if (instructions.length > 0) {
            walk(instructions[0], myStructural, false);
            walk(instructions[0], myReachable, true);
        }
    }

    private static void walk(Instruction start, BitSet reached, boolean folded) {
        Deque<Instruction> queue = new ArrayDeque<>();
        reached.set(start.num());
        queue.add(start);
        while (!queue.isEmpty()) {
            Instruction instruction = queue.poll();
            if (folded && instruction instanceof SwiftInstruction marked && marked.is(SwiftInstruction.NO_RETURN)) {
                continue;
            }
            for (Instruction next : instruction.allSucc()) {
                if (folded && next instanceof SwiftConditionalInstruction conditional && conditional.isImpossible()) {
                    continue;
                }
                if (!reached.get(next.num())) {
                    reached.set(next.num());
                    queue.add(next);
                }
            }
        }
    }

    // region owners

    /**
     * A function, an initializer, a deinitializer, an accessor, a computed variable or subscript, a closure, a file
     */
    public static boolean isOwner(PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        if (element instanceof PsiFile || type == SwiftTypes.CLOSURE_EXPRESSION || ACCESSORS.contains(type)) {
            return true;
        }
        if (type == SwiftTypes.FUNCTION_DECLARATION) {
            return SwiftFlowVariables.firstChildOfType(element, SwiftTypes.FUNCTION_BODY) != null;
        }
        if (type == SwiftTypes.VARIABLE_DECLARATION || type == SwiftTypes.SUBSCRIPT_DECLARATION) {
            // var x: Int { 1 } - the getter
            return SwiftFlowVariables.firstChildOfType(element, SwiftTypes.CODE_BLOCK) != null;
        }
        return false;
    }

    /**
     * The code block of the body of an owner - null for a closure and a file
     */
    public static @Nullable PsiElement bodyBlock(PsiElement owner) {
        IElementType type = PsiUtilCore.getElementType(owner);
        if (type == SwiftTypes.FUNCTION_DECLARATION) {
            return SwiftFlowVariables.firstChildOfType(SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.FUNCTION_BODY), SwiftTypes.CODE_BLOCK);
        }
        return SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.CODE_BLOCK);
    }

    /**
     * The statements of the body - a STATEMENTS element, the file for the top-level code
     */
    public static @Nullable PsiElement bodyStatements(PsiElement owner) {
        if (owner instanceof PsiFile) {
            return owner;
        }
        if (PsiUtilCore.getElementType(owner) == SwiftTypes.CLOSURE_EXPRESSION) {
            return SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.STATEMENTS);
        }
        return SwiftFlowVariables.firstChildOfType(bodyBlock(owner), SwiftTypes.STATEMENTS);
    }

    /**
     * The only statement of a body returning a value, an expression returned implicitly - null if it is not one
     */
    public static @Nullable PsiElement implicitReturn(PsiElement owner) {
        IElementType type = PsiUtilCore.getElementType(owner);
        if (owner instanceof PsiFile || type == SwiftTypes.INITIALIZER_DECLARATION || type == SwiftTypes.DEINITIALIZER_DECLARATION
            || type == SwiftTypes.SETTER_CLAUSE || type == SwiftTypes.WILL_SET_CLAUSE || type == SwiftTypes.DID_SET_CLAUSE) {
            return null;
        }
        PsiElement statements = bodyStatements(owner);
        PsiElement single = null;
        for (PsiElement child = statements == null ? null : statements.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() != null) {
                if (single != null) {
                    return null;
                }
                single = child;
            }
        }
        if (PsiUtilCore.getElementType(single) != SwiftTypes.STATEMENT) {
            return null;
        }
        PsiElement inner = SwiftFlowVariables.firstComposite(single);
        return inner instanceof SwiftExpression ? single : null;
    }

    /**
     * The outermost owner of the element which is not a file - the scope of the names of its variables
     */
    public static PsiElement topOwner(PsiElement element) {
        PsiElement top = null;
        for (PsiElement parent = element; parent != null && !(parent instanceof PsiFile); parent = parent.getParent()) {
            if (isOwner(parent)) {
                top = parent;
            }
        }
        return top != null ? top : element.getContainingFile();
    }

    /**
     * The innermost owner of the element - itself if it is one
     */
    public static @Nullable PsiElement ownerOf(PsiElement element) {
        for (PsiElement parent = element; parent != null; parent = parent.getParent()) {
            if (isOwner(parent)) {
                return parent;
            }
        }
        return null;
    }

    /**
     * The names of the functions never returning: of the standard library, and the global ones of the file declared
     * returning Never
     */
    @RequiredReadAction
    public static Set<String> noReturnFunctions(PsiFile file) {
        if (!file.isPhysical()) {
            return collectNoReturnFunctions(file);
        }
        return LanguageCachedValueUtil.getCachedValue(file,
            () -> CachedValueProvider.Result.create(collectNoReturnFunctions(file), PsiModificationTracker.MODIFICATION_COUNT));
    }

    private static Set<String> collectNoReturnFunctions(PsiFile file) {
        Set<String> names = new HashSet<>(SwiftControlFlowBuilder.NO_RETURN_FUNCTIONS);
        for (PsiElement statement = file.getFirstChild(); statement != null; statement = statement.getNextSibling()) {
            PsiElement function = SwiftFlowVariables.firstChildOfType(statement, SwiftTypes.FUNCTION_DECLARATION);
            if (function != null && isNever(resultType(function))) {
                PsiElement name = SwiftFlowVariables.firstChildOfType(function, SwiftTypes.FUNCTION_NAME);
                if (name != null) {
                    names.add(SwiftFlowVariables.unquote(name.getText()));
                }
            }
        }
        return names;
    }

    /**
     * The declared result type of a function, a closure, a getter, a computed variable or a subscript - null if none
     */
    public static @Nullable PsiElement resultType(PsiElement owner) {
        IElementType type = PsiUtilCore.getElementType(owner);
        if (type == SwiftTypes.FUNCTION_DECLARATION) {
            return typeOf(SwiftFlowVariables.firstChildOfType(SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.FUNCTION_SIGNATURE),
                SwiftTypes.FUNCTION_RESULT));
        }
        if (type == SwiftTypes.CLOSURE_EXPRESSION) {
            return typeOf(SwiftFlowVariables.firstChildOfType(SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.CLOSURE_SIGNATURE),
                SwiftTypes.FUNCTION_RESULT));
        }
        if (type == SwiftTypes.VARIABLE_DECLARATION) {
            return typeOf(SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.TYPE_ANNOTATION));
        }
        if (type == SwiftTypes.SUBSCRIPT_DECLARATION) {
            return typeOf(owner);
        }
        if (type == SwiftTypes.GETTER_CLAUSE) {
            PsiElement property = owner.getParent() == null ? null : owner.getParent().getParent();
            return property == null ? null : resultType(property);
        }
        return null;
    }

    /**
     * The type after the arrow or the colon - TYPE, or a type of one part (THE_SELF_TYPE, THE_ANY_TYPE)
     */
    private static @Nullable PsiElement typeOf(@Nullable PsiElement element) {
        boolean after = false;
        for (PsiElement child = element == null ? null : element.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (type == SwiftTypes.ARROW || type == SwiftTypes.COLON) {
                after = true;
            }
            else if (after && child.getFirstChild() != null && type != SwiftTypes.ATTRIBUTES) {
                return child;
            }
        }
        return null;
    }

    public static boolean isVoid(@Nullable PsiElement type) {
        if (type == null) {
            return true;
        }
        String text = type.getText().replaceAll("\\s+", "");
        return text.equals("Void") || text.equals("()") || text.equals("Swift.Void");
    }

    public static boolean isNever(@Nullable PsiElement type) {
        if (type == null) {
            return false;
        }
        String text = type.getText().replaceAll("\\s+", "");
        return text.equals("Never") || text.equals("Swift.Never");
    }

    // endregion

    /**
     * The graph of the owner - null if it is not one or does not parse
     */
    @RequiredReadAction
    public static @Nullable SwiftControlFlow of(PsiElement owner) {
        if (!isOwner(owner)) {
            return null;
        }
        // a completion copy - not cached
        if (!owner.isPhysical()) {
            return compute(owner);
        }
        return LanguageCachedValueUtil.getCachedValue(owner, () -> CachedValueProvider.Result.create(compute(owner), PsiModificationTracker.MODIFICATION_COUNT));
    }

    @RequiredReadAction
    private static @Nullable SwiftControlFlow compute(PsiElement owner) {
        if (PsiTreeUtil.hasErrorElements(owner)) {
            return null;
        }
        return SwiftControlFlowBuilder.build(owner, bodyStatements(owner), implicitReturn(owner));
    }

    public PsiElement getOwner() {
        return myOwner;
    }

    @Override
    public Instruction[] getInstructions() {
        return myInstructions;
    }

    public List<Instruction> getInstructions(PsiElement element) {
        return myIndex.getOrDefault(element, List.of());
    }

    /**
     * Reached after the compiler folds the constants and stops at the calls never returning
     */
    public boolean isReachable(Instruction instruction) {
        return myReachable.get(instruction.num());
    }

    /**
     * Reached by the structure of the code
     */
    public boolean isStructurallyReachable(Instruction instruction) {
        return myStructural.get(instruction.num());
    }

    /**
     * Any node of the element is reachable by the structure of the code - true for an element without one
     */
    public boolean isStructurallyReachable(PsiElement element) {
        List<Instruction> instructions = myIndex.get(element);
        if (instructions == null) {
            return true;
        }
        for (Instruction instruction : instructions) {
            if (isStructurallyReachable(instruction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The end of the body - the control falls off it
     */
    public Instruction getEnd() {
        return myEnd;
    }

    public boolean isEndReachable() {
        return isReachable(myEnd);
    }

    /**
     * The nodes a return leaves from - with its statement, the statement of a body of one expression
     */
    public Map<Instruction, PsiElement> getReturns() {
        return myReturns;
    }

    /**
     * The ends of the else bodies of guards which fall through - with the code block
     */
    public Map<Instruction, PsiElement> getGuardEnds() {
        return myGuardEnds;
    }

    /**
     * The calls never returning
     */
    public List<Instruction> getNoReturns() {
        return myNoReturns;
    }
}
