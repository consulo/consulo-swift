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

package consulo.swift.language.impl.psi;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.swift.language.psi.SwiftTypes;
import org.jspecify.annotations.Nullable;

/**
 * Where the name of each kind of declaration is in its tree.
 */
public final class SwiftDeclarationNames {
    private SwiftDeclarationNames() {
    }

    /**
     * @return the node naming the declaration: the identifier, the function name, the extended type's last
     * {@code type_name}; {@code null} for declarations without a name
     */
    public static @Nullable ASTNode findNameNode(ASTNode declaration) {
        IElementType type = declaration.getElementType();
        if (type == SwiftTypes.ENUM_DECLARATION) {
            ASTNode body = declaration.findChildByType(SwiftTypes.UNION_STYLE_ENUM);
            return body == null ? null : body.findChildByType(SwiftTypes.IDENTIFIER);
        }
        if (type == SwiftTypes.FUNCTION_DECLARATION || type == SwiftTypes.PROTOCOL_METHOD_DECLARATION) {
            return declaration.findChildByType(SwiftTypes.FUNCTION_NAME);
        }
        if (type == SwiftTypes.EXTENSION_DECLARATION) {
            return findExtendedTypeName(declaration.findChildByType(SwiftTypes.TYPE_IDENTIFIER));
        }
        if (type == SwiftTypes.VARIABLE_DECLARATION) {
            // `var x: T { get }` names a variable_name, `var x: T` and `var x = 1` a pattern
            ASTNode variableName = declaration.findChildByType(SwiftTypes.VARIABLE_NAME);
            return variableName != null ? variableName : findSinglePatternName(declaration);
        }
        if (type == SwiftTypes.CONSTANT_DECLARATION) {
            return findSinglePatternName(declaration);
        }
        if (type == SwiftTypes.CLASS_DECLARATION
            || type == SwiftTypes.STRUCT_DECLARATION
            || type == SwiftTypes.PROTOCOL_DECLARATION
            || type == SwiftTypes.ACTOR_DECLARATION
            || type == SwiftTypes.TYPEALIAS_DECLARATION
            || type == SwiftTypes.PROTOCOL_ASSOCIATED_TYPE_DECLARATION
            || type == SwiftTypes.PROTOCOL_PROPERTY_DECLARATION
            || type == SwiftTypes.PRECEDENCE_GROUP_DECLARATION) {
            return declaration.findChildByType(SwiftTypes.IDENTIFIER);
        }
        return null;
    }

    public static @Nullable String getName(ASTNode declaration) {
        ASTNode nameNode = findNameNode(declaration);
        return nameNode == null ? null : unquote(nameNode.getText());
    }

    /**
     * {@code extension Outer.Inner<T>} extends {@code Inner}.
     */
    private static @Nullable ASTNode findExtendedTypeName(@Nullable ASTNode typeIdentifier) {
        ASTNode current = typeIdentifier;
        while (current != null) {
            ASTNode nested = current.findChildByType(SwiftTypes.TYPE_IDENTIFIER);
            if (nested == null) {
                return current.findChildByType(SwiftTypes.TYPE_NAME);
            }
            current = nested;
        }
        return null;
    }

    /**
     * {@code let x = 1} and {@code var x: Int} are named, {@code let (a, b) = pair} and {@code let a = 1, b = 2} are not.
     */
    private static @Nullable ASTNode findSinglePatternName(ASTNode declaration) {
        ASTNode list = declaration.findChildByType(SwiftTypes.PATTERN_INITIALIZER_LIST);
        ASTNode[] initializers = list == null ? ASTNode.EMPTY_ARRAY : list.getChildren(null);
        ASTNode single = null;
        for (ASTNode initializer : initializers) {
            if (initializer.getElementType() == SwiftTypes.PATTERN_INITIALIZER) {
                if (single != null) {
                    return null;
                }
                single = initializer;
            }
        }
        ASTNode pattern = single == null ? null : single.getFirstChildNode();
        if (pattern != null && pattern.getElementType() == SwiftTypes.PATTERN) {
            // `x: Int` is a pattern of the identifier and the type annotation
            pattern = pattern.getFirstChildNode();
        }
        if (pattern != null && pattern.getElementType() == SwiftTypes.IDENTIFIER_PATTERN) {
            return pattern.findChildByType(SwiftTypes.IDENTIFIER);
        }
        return null;
    }

    /**
     * {@code `class`} is named {@code class}.
     */
    public static String unquote(String name) {
        return name.length() > 1 && name.startsWith("`") && name.endsWith("`") ? name.substring(1, name.length() - 1) : name;
    }
}
