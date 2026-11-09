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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * The variables of the flow owners and the resolve of a name used in an expression to one of them - by scope, as the
 * compiler does: the declarations of the statements before in the enclosing blocks, the bindings of the conditions of an
 * if, a while, a guard (after it), of the pattern of a for-in, a case, a catch, the parameters of a function, an
 * initializer, a subscript, a closure. A variable is the element declaring it: an IDENTIFIER_PATTERN, a PARAMETER, a
 * CLOSURE_PARAMETER, the IDENTIFIER of a closure parameter without parentheses.
 *
 * @author VISTALL
 */
public final class SwiftFlowVariables {
    public enum Kind {
        // let x = 1, let x: Int
        LET,
        // var x = 1, var x: Int
        VAR,
        // if let, guard let, while let
        CONDITION_BINDING,
        // for x in
        FOR_BINDING,
        // case let x, catch let e
        PATTERN_BINDING,
        PARAMETER
    }

    private static final TokenSet OWNER_BOUNDARIES = TokenSet.create(SwiftTypes.STRUCT_BODY, SwiftTypes.CLASS_BODY, SwiftTypes.EXTENSION_BODY,
        SwiftTypes.PROTOCOL_BODY, SwiftTypes.UNION_STYLE_ENUM);

    private SwiftFlowVariables() {
    }

    // region declarations

    public static String name(PsiElement variable) {
        IElementType type = PsiUtilCore.getElementType(variable);
        PsiElement identifier = variable;
        if (type == SwiftTypes.PARAMETER) {
            identifier = parameterName(variable);
        }
        else if (type != SwiftTypes.IDENTIFIER) {
            identifier = firstChildOfType(variable, SwiftTypes.IDENTIFIER);
        }
        return identifier == null ? "" : unquote(identifier.getText());
    }

    /**
     * The local name of a parameter - the label before the type
     */
    private static @Nullable PsiElement parameterName(PsiElement parameter) {
        PsiElement last = null;
        for (PsiElement child = parameter.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == SwiftTypes.TYPE_ANNOTATION) {
                break;
            }
            if (child.getFirstChild() == null && SwiftExpressionTree.isSignificant(child)) {
                last = child;
            }
        }
        return last;
    }

    /**
     * The element of the name of the variable - its IDENTIFIER
     */
    public static PsiElement nameElement(PsiElement variable) {
        IElementType type = PsiUtilCore.getElementType(variable);
        PsiElement identifier = type == SwiftTypes.PARAMETER ? parameterName(variable)
            : type == SwiftTypes.IDENTIFIER ? variable : firstChildOfType(variable, SwiftTypes.IDENTIFIER);
        return identifier == null ? variable : identifier;
    }

    public static String unquote(String name) {
        return name.length() > 1 && name.startsWith("`") && name.endsWith("`") ? name.substring(1, name.length() - 1) : name;
    }

    /**
     * The kind of the variable
     */
    public static Kind kind(PsiElement variable) {
        IElementType type = PsiUtilCore.getElementType(variable);
        if (type == SwiftTypes.PARAMETER || type == SwiftTypes.CLOSURE_PARAMETER || type == SwiftTypes.IDENTIFIER) {
            return Kind.PARAMETER;
        }
        for (PsiElement parent = variable.getParent(); parent != null; parent = parent.getParent()) {
            IElementType parentType = PsiUtilCore.getElementType(parent);
            if (parentType == SwiftTypes.CONSTANT_DECLARATION) {
                return Kind.LET;
            }
            if (parentType == SwiftTypes.VARIABLE_DECLARATION) {
                return Kind.VAR;
            }
            if (parentType == SwiftTypes.OPTIONAL_BINDING_CONDITION) {
                return Kind.CONDITION_BINDING;
            }
            if (parentType == SwiftTypes.FOR_IN_STATEMENT) {
                return Kind.FOR_BINDING;
            }
            if (parentType == SwiftTypes.CASE_ITEM || parentType == SwiftTypes.CASE_CONDITION || parentType == SwiftTypes.CATCH_CLAUSE) {
                return Kind.PATTERN_BINDING;
            }
        }
        return Kind.PATTERN_BINDING;
    }

    /**
     * A stored property of a type - declared in its body, not in a member of it
     */
    public static boolean isProperty(PsiElement variable) {
        for (PsiElement parent = variable.getParent(); parent != null; parent = parent.getParent()) {
            IElementType type = PsiUtilCore.getElementType(parent);
            if (type == SwiftTypes.STRUCT_BODY || type == SwiftTypes.CLASS_BODY) {
                return true;
            }
            if (SwiftControlFlow.isOwner(parent) && type != SwiftTypes.VARIABLE_DECLARATION) {
                return false;
            }
        }
        return false;
    }

    /**
     * Declared by let - a constant: a let declaration, a let binding of a condition, a for-in variable
     */
    public static boolean isConstant(PsiElement variable) {
        Kind kind = kind(variable);
        if (kind == Kind.LET || kind == Kind.FOR_BINDING || kind == Kind.PARAMETER) {
            return true;
        }
        if (kind == Kind.VAR) {
            return false;
        }
        for (PsiElement parent = variable.getParent(); parent != null; parent = parent.getParent()) {
            IElementType type = PsiUtilCore.getElementType(parent);
            if (type == SwiftTypes.VALUE_BINDING_PATTERN || type == SwiftTypes.OPTIONAL_BINDING_CONDITION) {
                return parent.getFirstChild() != null && PsiUtilCore.getElementType(parent.getFirstChild()) == SwiftTypes.LET_KEYWORD;
            }
        }
        return true;
    }

    /**
     * The pattern initializer declaring the variable of a let or var declaration
     */
    public static @Nullable PsiElement patternInitializer(PsiElement variable) {
        for (PsiElement parent = variable.getParent(); parent != null; parent = parent.getParent()) {
            IElementType type = PsiUtilCore.getElementType(parent);
            if (type == SwiftTypes.PATTERN_INITIALIZER) {
                return parent;
            }
            if (type != SwiftTypes.PATTERN && type != SwiftTypes.TUPLE_PATTERN && type != SwiftTypes.VALUE_BINDING_PATTERN) {
                return null;
            }
        }
        return null;
    }

    /**
     * {@code var x: Int?} - a variable of an optional type without an initializer is nil
     */
    public static boolean isImplicitlyInitialized(PsiElement variable) {
        if (kind(variable) != Kind.VAR) {
            return false;
        }
        PsiElement annotation = variable.getNextSibling();
        while (annotation != null && !SwiftExpressionTree.isSignificant(annotation)) {
            annotation = annotation.getNextSibling();
        }
        if (PsiUtilCore.getElementType(annotation) != SwiftTypes.TYPE_ANNOTATION) {
            return false;
        }
        String text = annotation.getText().replaceAll("\\s+", "");
        return text.endsWith("?") || text.endsWith("!") || text.startsWith(":Optional<") || text.startsWith(":Swift.Optional<");
    }

    /**
     * The variables bound by a pattern - all its identifiers in a declaration, a condition or a for-in (binding), the ones
     * under let or var in a case or a catch
     */
    public static void bindings(@Nullable PsiElement pattern, boolean binding, List<PsiElement> result) {
        if (pattern == null) {
            return;
        }
        IElementType type = PsiUtilCore.getElementType(pattern);
        if (type == SwiftTypes.IDENTIFIER_PATTERN) {
            if (binding) {
                result.add(pattern);
            }
            return;
        }
        if (type == SwiftTypes.EXPRESSION_PATTERN || type == SwiftTypes.TYPE || type == SwiftTypes.TYPE_ANNOTATION) {
            return;
        }
        boolean childBinding = binding || type == SwiftTypes.VALUE_BINDING_PATTERN;
        for (PsiElement child = pattern.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() != null) {
                bindings(child, childBinding, result);
            }
        }
    }

    /**
     * The variables a statement of a block declares for the statements after it: let, var, guard bindings
     */
    @RequiredReadAction
    private static void declarations(PsiElement statement, List<PsiElement> result) {
        IElementType type = PsiUtilCore.getElementType(statement);
        if (type == SwiftTypes.STATEMENT) {
            PsiElement declaration = firstComposite(statement);
            IElementType declarationType = PsiUtilCore.getElementType(declaration);
            if (declarationType == SwiftTypes.CONSTANT_DECLARATION || declarationType == SwiftTypes.VARIABLE_DECLARATION) {
                PsiElement list = firstChildOfType(declaration, SwiftTypes.PATTERN_INITIALIZER_LIST);
                if (list != null) {
                    for (PsiElement initializer = list.getFirstChild(); initializer != null; initializer = initializer.getNextSibling()) {
                        if (PsiUtilCore.getElementType(initializer) == SwiftTypes.PATTERN_INITIALIZER) {
                            bindings(firstComposite(initializer), true, result);
                        }
                    }
                }
            }
        }
        else if (type == SwiftTypes.GUARD_STATEMENT) {
            conditionBindings(firstChildOfType(statement, SwiftTypes.CONDITION_LIST), null, result);
        }
    }

    /**
     * The bindings of the conditions of a list - before the condition (null for all of them)
     */
    private static void conditionBindings(@Nullable PsiElement list, @Nullable PsiElement before, List<PsiElement> result) {
        if (list == null) {
            return;
        }
        for (PsiElement condition = list.getFirstChild(); condition != null && condition != before; condition = condition.getNextSibling()) {
            if (PsiUtilCore.getElementType(condition) != SwiftTypes.CONDITION) {
                continue;
            }
            PsiElement inner = firstComposite(condition);
            IElementType type = PsiUtilCore.getElementType(inner);
            if (type == SwiftTypes.OPTIONAL_BINDING_CONDITION) {
                bindings(firstPattern(inner), true, result);
            }
            else if (type == SwiftTypes.CASE_CONDITION) {
                bindings(firstPattern(inner), false, result);
            }
        }
    }

    /**
     * The pattern of an optional binding, a case condition, a for-in, a catch - its first child which is not an expression
     */
    public static @Nullable PsiElement firstPattern(PsiElement element) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (child.getFirstChild() == null) {
                continue;
            }
            if (type == SwiftTypes.PATTERN || type == SwiftTypes.IDENTIFIER_PATTERN || type == SwiftTypes.VALUE_BINDING_PATTERN
                || type == SwiftTypes.TUPLE_PATTERN || type == SwiftTypes.ENUM_CASE_PATTERN || type == SwiftTypes.EXPRESSION_PATTERN
                || type == SwiftTypes.WILDCARD_PATTERN || type == SwiftTypes.OPTIONAL_PATTERN) {
                return child;
            }
        }
        return null;
    }

    private static void parameters(@Nullable PsiElement clause, List<PsiElement> result) {
        if (clause == null) {
            return;
        }
        for (PsiElement child = clause.getFirstChild(); child != null; child = child.getNextSibling()) {
            IElementType type = PsiUtilCore.getElementType(child);
            if (type == SwiftTypes.PARAMETER || type == SwiftTypes.CLOSURE_PARAMETER || type == SwiftTypes.IDENTIFIER) {
                result.add(child);
            }
            else if (type == SwiftTypes.PARAMETER_LIST || type == SwiftTypes.CLOSURE_PARAMETER_LIST) {
                parameters(child, result);
            }
        }
    }

    /**
     * The variables a scope declares for its child - null if it is a boundary (a type body) the resolve stops at
     */
    @RequiredReadAction
    private static @Nullable List<PsiElement> visible(PsiElement scope, PsiElement child) {
        IElementType type = PsiUtilCore.getElementType(scope);
        List<PsiElement> result = new ArrayList<>();
        if (OWNER_BOUNDARIES.contains(type)) {
            return null;
        }
        if (type == SwiftTypes.IF_STATEMENT || type == SwiftTypes.WHILE_STATEMENT) {
            IElementType childType = PsiUtilCore.getElementType(child);
            if (childType == SwiftTypes.CODE_BLOCK) {
                conditionBindings(firstChildOfType(scope, SwiftTypes.CONDITION_LIST), null, result);
            }
        }
        else if (type == SwiftTypes.CONDITION_LIST) {
            conditionBindings(scope, child, result);
        }
        else if (type == SwiftTypes.FOR_IN_STATEMENT) {
            IElementType childType = PsiUtilCore.getElementType(child);
            if (childType == SwiftTypes.CODE_BLOCK || childType == SwiftTypes.WHERE_CLAUSE) {
                PsiElement pattern = firstPattern(scope);
                boolean caseFor = firstChildOfType(scope, SwiftTypes.CASE_KEYWORD) != null;
                bindings(pattern, !caseFor, result);
            }
        }
        else if (type == SwiftTypes.SWITCH_CASE) {
            if (PsiUtilCore.getElementType(child) == SwiftTypes.STATEMENTS) {
                PsiElement label = firstChildOfType(scope, SwiftTypes.CASE_LABEL);
                PsiElement items = label == null ? null : firstChildOfType(label, SwiftTypes.CASE_ITEM_LIST);
                PsiElement item = items == null ? null : firstChildOfType(items, SwiftTypes.CASE_ITEM);
                if (item != null) {
                    // every item binds the same names
                    bindings(firstPattern(item), false, result);
                }
            }
        }
        else if (type == SwiftTypes.CASE_ITEM || type == SwiftTypes.CASE_CONDITION || type == SwiftTypes.CATCH_CLAUSE) {
            IElementType childType = PsiUtilCore.getElementType(child);
            if (childType == SwiftTypes.WHERE_CLAUSE || childType == SwiftTypes.CODE_BLOCK) {
                bindings(firstPattern(scope), false, result);
            }
        }
        else if (type == SwiftTypes.CLOSURE_EXPRESSION) {
            PsiElement signature = firstChildOfType(scope, SwiftTypes.CLOSURE_SIGNATURE);
            if (signature != null && child != signature) {
                parameters(firstChildOfType(signature, SwiftTypes.CLOSURE_PARAM_CLAUSE), result);
            }
        }
        else if (type == SwiftTypes.FUNCTION_DECLARATION) {
            PsiElement signature = firstChildOfType(scope, SwiftTypes.FUNCTION_SIGNATURE);
            if (signature != null && PsiUtilCore.getElementType(child) == SwiftTypes.FUNCTION_BODY) {
                parameters(firstChildOfType(signature, SwiftTypes.PARAMETER_CLAUSE), result);
            }
        }
        else if (type == SwiftTypes.INITIALIZER_DECLARATION || type == SwiftTypes.SUBSCRIPT_DECLARATION) {
            if (PsiUtilCore.getElementType(child) != SwiftTypes.PARAMETER_CLAUSE) {
                parameters(firstChildOfType(scope, SwiftTypes.PARAMETER_CLAUSE), result);
            }
        }
        return result;
    }

    // endregion

    // region resolve

    /**
     * The variable a name used at the element is - null if it is not a variable of a flow owner (a member, a global of
     * another file, a function, a type)
     */
    @RequiredReadAction
    public static @Nullable PsiElement resolve(String name, PsiElement element) {
        return new Resolver().resolve(name, element);
    }

    /**
     * The resolve of the names of one owner - the declarations of each block indexed once, by name and position, not
     * searched again for each reference
     */
    public static final class Resolver {
        /**
         * The positions of the statements of a block, the declarations of each name with the position of their statement
         */
        private record BlockIndex(Map<PsiElement, Integer> positions, Map<String, List<Map.Entry<Integer, PsiElement>>> declarations) {
        }

        private final Map<PsiElement, BlockIndex> myBlocks = new HashMap<>();

        @RequiredReadAction
        public @Nullable PsiElement resolve(String name, PsiElement element) {
            PsiElement child = element;
            for (PsiElement scope = element.getParent(); scope != null; child = scope, scope = scope.getParent()) {
                if (PsiUtilCore.getElementType(scope) == SwiftTypes.STATEMENTS || scope instanceof PsiFile) {
                    PsiElement variable = inBlock(scope, child, name);
                    if (variable != null) {
                        return variable;
                    }
                    if (scope instanceof PsiFile) {
                        return null;
                    }
                    continue;
                }
                List<PsiElement> visible = visible(scope, child);
                if (visible == null) {
                    return null;
                }
                for (PsiElement variable : visible) {
                    if (name.equals(name(variable))) {
                        return variable;
                    }
                }
            }
            return null;
        }

        /**
         * The last declaration of the name in a statement of the block before the child
         */
        @RequiredReadAction
        private @Nullable PsiElement inBlock(PsiElement block, PsiElement child, String name) {
            BlockIndex index = myBlocks.computeIfAbsent(block, Resolver::index);
            List<Map.Entry<Integer, PsiElement>> declarations = index.declarations().get(name);
            Integer position = index.positions().get(child);
            if (declarations == null || position == null) {
                return null;
            }
            // sorted by position - the last one before the child
            int low = 0;
            int high = declarations.size() - 1;
            PsiElement result = null;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                Map.Entry<Integer, PsiElement> declaration = declarations.get(middle);
                if (declaration.getKey() < position) {
                    result = declaration.getValue();
                    low = middle + 1;
                }
                else {
                    high = middle - 1;
                }
            }
            return result;
        }

        @RequiredReadAction
        private static BlockIndex index(PsiElement block) {
            Map<PsiElement, Integer> positions = new HashMap<>();
            Map<String, List<Map.Entry<Integer, PsiElement>>> declarations = new HashMap<>();
            List<PsiElement> variables = new ArrayList<>();
            int position = 0;
            for (PsiElement statement = block.getFirstChild(); statement != null; statement = statement.getNextSibling()) {
                if (statement.getFirstChild() == null) {
                    continue;
                }
                positions.put(statement, position);
                variables.clear();
                declarations(statement, variables);
                for (PsiElement variable : variables) {
                    declarations.computeIfAbsent(name(variable), it -> new ArrayList<>(1)).add(Map.entry(position, variable));
                }
                position++;
            }
            return new BlockIndex(positions, declarations);
        }
    }

    /**
     * The names of the variables declared in the outermost flow owner of the element - a name not in it is not resolved
     */
    @RequiredReadAction
    public static Set<String> names(PsiElement topOwner) {
        if (!topOwner.isPhysical()) {
            return collectNames(topOwner);
        }
        return LanguageCachedValueUtil.getCachedValue(topOwner,
            () -> CachedValueProvider.Result.create(collectNames(topOwner), PsiModificationTracker.MODIFICATION_COUNT));
    }

    private static Set<String> collectNames(PsiElement topOwner) {
        Set<String> names = new HashSet<>();
        collectNames(topOwner, names);
        return names;
    }

    private static void collectNames(PsiElement element, Set<String> names) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() == null) {
                continue;
            }
            IElementType type = PsiUtilCore.getElementType(child);
            if (type == SwiftTypes.IDENTIFIER_PATTERN || type == SwiftTypes.PARAMETER || type == SwiftTypes.CLOSURE_PARAMETER) {
                names.add(name(child));
                continue;
            }
            if (type == SwiftTypes.CLOSURE_PARAM_CLAUSE) {
                for (PsiElement parameter = child.getFirstChild(); parameter != null; parameter = parameter.getNextSibling()) {
                    if (PsiUtilCore.getElementType(parameter) == SwiftTypes.IDENTIFIER) {
                        names.add(unquote(parameter.getText()));
                    }
                }
            }
            collectNames(child, names);
        }
    }

    // endregion

    public static @Nullable PsiElement firstChildOfType(@Nullable PsiElement element, IElementType type) {
        if (element == null) {
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == type) {
                return child;
            }
        }
        return null;
    }

    public static @Nullable PsiElement firstComposite(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getFirstChild() != null) {
                return child;
            }
        }
        return null;
    }
}
