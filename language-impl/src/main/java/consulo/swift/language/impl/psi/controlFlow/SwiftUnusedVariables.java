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
import consulo.language.ast.IElementType;
import consulo.language.controlFlow.Instruction;
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
 * The variables of an owner never read, as the VarDeclUsageChecker of swiftc reports them (verified with Swift 6.4): not
 * by the flow - a value overwritten before it is read is not reported, a variable read anywhere is used - but by the
 * reads and the writes of the variable in the owner and in the closures and the functions nested in it, taken from their
 * graphs. Nothing is reported if one of them does not parse.
 * <ul>
 * <li>"variable 'x' was written to, but never read" - a var assigned after its declaration</li>
 * <li>"initialization of variable 'x' was never used; ...", "initialization of immutable value 'x' was never used; ..." -
 * only the value of its declaration</li>
 * <li>"variable 'x' was never used; ...", "immutable value 'x' was never used; ..." - no value at all, or in a tuple</li>
 * <li>"value 'x' was defined but never used; consider replacing with boolean test" - if let, guard let, while let</li>
 * </ul>
 *
 * @author VISTALL
 */
public final class SwiftUnusedVariables {
    private SwiftUnusedVariables() {
    }

    @RequiredReadAction
    static void compute(SwiftControlFlow flow, List<Problem> problems) {
        PsiElement owner = flow.getOwner();
        if (owner instanceof PsiFile) {
            // globals
            return;
        }

        Set<PsiElement> variables = new LinkedHashSet<>();
        for (Instruction instruction : flow.getInstructions()) {
            if (instruction instanceof SwiftReadWriteInstruction readWrite && readWrite.getElement() == readWrite.getVariable()
                && (readWrite.getAccess() == Access.WRITE || readWrite.getAccess() == Access.DECLARE) && isChecked(readWrite.getVariable())) {
                variables.add(readWrite.getVariable());
            }
        }
        if (variables.isEmpty()) {
            return;
        }

        Set<PsiElement> read = new HashSet<>();
        Set<PsiElement> written = new HashSet<>();
        usages(flow, variables, read, written);
        List<PsiElement> nested = new ArrayList<>();
        nestedOwners(owner, nested);
        for (PsiElement nestedOwner : nested) {
            SwiftControlFlow nestedFlow = SwiftControlFlow.of(nestedOwner);
            if (nestedFlow == null) {
                // not known what it uses
                return;
            }
            usages(nestedFlow, variables, read, written);
            captureList(nestedOwner, variables, read);
        }

        for (PsiElement variable : variables) {
            if (read.contains(variable)) {
                continue;
            }
            LocalizeValue message = message(variable, written.contains(variable));
            if (message != null) {
                problems.add(new Problem(SwiftFlowVariables.nameElement(variable).getTextRange(), false, message));
            }
        }
    }

    /**
     * A let or a var declared, the variable of a for-in, the binding of a condition - not a parameter, a case or a catch
     * binding
     */
    private static boolean isChecked(PsiElement variable) {
        SwiftFlowVariables.Kind kind = SwiftFlowVariables.kind(variable);
        return kind == SwiftFlowVariables.Kind.LET || kind == SwiftFlowVariables.Kind.VAR || kind == SwiftFlowVariables.Kind.FOR_BINDING
            || kind == SwiftFlowVariables.Kind.CONDITION_BINDING;
    }

    private static void usages(SwiftControlFlow flow, Set<PsiElement> variables, Set<PsiElement> read, Set<PsiElement> written) {
        for (Instruction instruction : flow.getInstructions()) {
            if (!(instruction instanceof SwiftReadWriteInstruction readWrite) || !variables.contains(readWrite.getVariable())) {
                continue;
            }
            if (readWrite.isRead()) {
                read.add(readWrite.getVariable());
            }
            if (readWrite.isWrite() && readWrite.getElement() != readWrite.getVariable()) {
                written.add(readWrite.getVariable());
            }
        }
    }

    private static void nestedOwners(PsiElement element, List<PsiElement> result) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() == null) {
                continue;
            }
            if (SwiftControlFlow.isOwner(child)) {
                result.add(child);
            }
            nestedOwners(child, result);
        }
    }

    /**
     * {@code { [x] in ... }} - the names captured, read where the closure is created; not a part of any graph
     */
    private static void captureList(PsiElement owner, Set<PsiElement> variables, Set<PsiElement> read) {
        if (PsiUtilCore.getElementType(owner) != SwiftTypes.CLOSURE_EXPRESSION) {
            return;
        }
        PsiElement captures = SwiftFlowVariables.firstChildOfType(SwiftFlowVariables.firstChildOfType(owner, SwiftTypes.CLOSURE_SIGNATURE),
            SwiftTypes.CAPTURE_LIST);
        if (captures == null) {
            return;
        }
        Set<String> names = new HashSet<>();
        collectIdentifiers(captures, names);
        for (PsiElement variable : variables) {
            if (names.contains(SwiftFlowVariables.name(variable))) {
                read.add(variable);
            }
        }
    }

    private static void collectIdentifiers(PsiElement element, Set<String> names) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == SwiftTypes.IDENTIFIER) {
                names.add(SwiftFlowVariables.unquote(child.getText()));
            }
            else if (child.getFirstChild() != null) {
                collectIdentifiers(child, names);
            }
        }
    }

    private static @Nullable LocalizeValue message(PsiElement variable, boolean written) {
        String name = SwiftFlowVariables.name(variable);
        SwiftFlowVariables.Kind kind = SwiftFlowVariables.kind(variable);
        boolean constant = SwiftFlowVariables.isConstant(variable);
        if (kind == SwiftFlowVariables.Kind.CONDITION_BINDING) {
            if (!constant) {
                return written ? SwiftFlowLocalize.unusedWrittenNeverRead(name) : null;
            }
            return isSimple(variable) ? SwiftFlowLocalize.unusedBinding(name) : null;
        }
        if (kind == SwiftFlowVariables.Kind.FOR_BINDING) {
            return SwiftFlowLocalize.unusedPatternConstant(name);
        }
        boolean ownValue = isSimple(variable) && hasValue(variable);
        if (constant) {
            if (ownValue) {
                return SwiftFlowLocalize.unusedInitializationConstant(name);
            }
            return isInTuple(variable) ? SwiftFlowLocalize.unusedPatternConstant(name) : SwiftFlowLocalize.unusedConstant(name);
        }
        if (written) {
            return SwiftFlowLocalize.unusedWrittenNeverRead(name);
        }
        return ownValue ? SwiftFlowLocalize.unusedInitializationVariable(name) : SwiftFlowLocalize.unusedVariable(name);
    }

    /**
     * The variable is the whole pattern - not a part of a tuple
     */
    private static boolean isSimple(PsiElement variable) {
        return !isInTuple(variable);
    }

    private static boolean isInTuple(PsiElement variable) {
        for (PsiElement parent = variable.getParent(); parent != null; parent = parent.getParent()) {
            IElementType type = PsiUtilCore.getElementType(parent);
            if (type == SwiftTypes.TUPLE_PATTERN || type == SwiftTypes.ENUM_CASE_PATTERN) {
                return true;
            }
            if (type != SwiftTypes.PATTERN && type != SwiftTypes.VALUE_BINDING_PATTERN) {
                return false;
            }
        }
        return false;
    }

    private static boolean hasValue(PsiElement variable) {
        PsiElement initializer = SwiftFlowVariables.patternInitializer(variable);
        return initializer != null && SwiftFlowVariables.firstChildOfType(initializer, SwiftTypes.INITIALIZER) != null;
    }
}
