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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * What an initializer of a struct, a class or an actor must initialize - the stored properties of the type without an
 * initial value (not static, not lazy, not a var of an optional type, not wrapped by an attribute), in the order of the
 * declarations - and the members using self: the methods and the computed properties. None for an initializer
 * delegating to another one ({@code self.init(...)}), of an enum or of an extension.
 *
 * @author VISTALL
 */
public record SwiftInitializerInfo(boolean isClass, Map<String, PsiElement> properties, Set<String> methods, Set<String> computed) {
    public static @Nullable SwiftInitializerInfo of(PsiElement owner) {
        if (PsiUtilCore.getElementType(owner) != SwiftTypes.INITIALIZER_DECLARATION) {
            return null;
        }
        PsiElement member = owner.getParent();
        PsiElement body = member == null ? null : member.getParent();
        IElementType bodyType = PsiUtilCore.getElementType(body);
        if (bodyType != SwiftTypes.STRUCT_BODY && bodyType != SwiftTypes.CLASS_BODY) {
            return null;
        }
        for (PsiElement self : PsiTreeUtil.findChildrenOfType(owner, PsiElement.class)) {
            if (PsiUtilCore.getElementType(self) == SwiftTypes.SELF_EXPRESSION && SwiftFlowVariables.firstChildOfType(self, SwiftTypes.INIT_KEYWORD) != null) {
                // self.init(...) - initializes all of them
                return null;
            }
        }

        Map<String, PsiElement> properties = new LinkedHashMap<>();
        Set<String> methods = new HashSet<>();
        Set<String> computed = new HashSet<>();
        for (PsiElement child = body.getFirstChild(); child != null; child = child.getNextSibling()) {
            PsiElement declaration = SwiftFlowVariables.firstComposite(child);
            IElementType type = PsiUtilCore.getElementType(declaration);
            if (declaration == null || isStatic(declaration)) {
                continue;
            }
            if (type == SwiftTypes.FUNCTION_DECLARATION) {
                PsiElement name = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.FUNCTION_NAME);
                if (name != null) {
                    methods.add(SwiftFlowVariables.unquote(name.getText()));
                }
            }
            else if (type == SwiftTypes.CONSTANT_DECLARATION || type == SwiftTypes.VARIABLE_DECLARATION) {
                property(declaration, properties, computed);
            }
        }
        return new SwiftInitializerInfo(bodyType == SwiftTypes.CLASS_BODY, properties, methods, computed);
    }

    private static void property(PsiElement declaration, Map<String, PsiElement> properties, Set<String> computed) {
        boolean wrapped = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.ATTRIBUTES) != null;
        PsiElement name = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.VARIABLE_NAME);
        if (name != null) {
            // var x: Int { get } - computed; var x: Int { didSet { } } - stored, without a value
            boolean observed = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.WILL_SET_DID_SET_BLOCK) != null;
            if (!observed) {
                computed.add(SwiftFlowVariables.unquote(name.getText()));
            }
            else if (!wrapped && SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.EQ) == null) {
                properties.put(SwiftFlowVariables.unquote(name.getText()), name);
            }
            return;
        }
        PsiElement list = SwiftFlowVariables.firstChildOfType(declaration, SwiftTypes.PATTERN_INITIALIZER_LIST);
        for (PsiElement initializer = list == null ? null : list.getFirstChild(); initializer != null; initializer = initializer.getNextSibling()) {
            if (PsiUtilCore.getElementType(initializer) != SwiftTypes.PATTERN_INITIALIZER
                || SwiftFlowVariables.firstChildOfType(initializer, SwiftTypes.INITIALIZER) != null) {
                continue;
            }
            List<PsiElement> variables = new ArrayList<>();
            SwiftFlowVariables.bindings(SwiftFlowVariables.firstComposite(initializer), true, variables);
            for (PsiElement variable : variables) {
                if (!wrapped && !SwiftFlowVariables.isImplicitlyInitialized(variable)) {
                    properties.put(SwiftFlowVariables.name(variable), variable);
                }
            }
        }
    }

    private static boolean isStatic(PsiElement declaration) {
        for (PsiElement child = declaration.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == SwiftTypes.DECLARATION_MODIFIER) {
                IElementType modifier = PsiUtilCore.getElementType(child.getFirstChild());
                if (modifier == SwiftTypes.STATIC_KEYWORD || modifier == SwiftTypes.CLASS_KEYWORD || modifier == SwiftTypes.LAZY_KEYWORD) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isProperty(PsiElement variable) {
        return properties.containsValue(variable);
    }
}
