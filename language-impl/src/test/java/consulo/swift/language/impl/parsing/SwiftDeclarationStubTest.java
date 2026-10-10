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

package consulo.swift.language.impl.parsing;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.stub.StubElement;
import consulo.swift.language.SwiftFileElementTypes;
import consulo.swift.language.SwiftFileType;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationStub;
import consulo.test.junit.impl.language.SimpleParsingTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which declarations get stubs - and so go to the indexes - and what they are named.
 */
public class SwiftDeclarationStubTest extends SimpleParsingTest<Object> {
    private static final String SOURCE = """
        import Foundation

        public protocol Shape {
            associatedtype Unit
            func area() -> Double
            var name: String { get }
        }

        struct Point {
            var x: Int
            let y = 0
            func moved(by dx: Int) -> Point {
                struct Local {}
                func helper() {}
                return self
            }
        }

        final class `Box`<T>: Shape {
            func area() -> Double { 0 }
        }

        enum Direction { case north, south }

        actor Counter {}

        typealias Coordinates = (Int, Int)

        extension Swift.Array where Element == Int {
            func total() -> Int { 0 }
        }

        func topLevel() {
            let closure = { () -> Void in
                func inClosure() {}
            }
        }

        let a = 1, b = 2
        """;

    public SwiftDeclarationStubTest() {
        super("parsing", "swift");
    }

    @RequiredReadAction
    @Test
    public void testStubs(Context context) {
        PsiFile file = createFile(context, null, "Stubs.swift", SwiftFileType.INSTANCE, SOURCE);
        StubElement<?> root = SwiftFileElementTypes.SWIFT_FILE.getBuilder().buildStubTree(file);

        List<String> stubs = new ArrayList<>();
        collect(root, "", stubs);
        assertEquals(List.of(
            "PROTOCOL_DECLARATION Shape",
            "  PROTOCOL_ASSOCIATED_TYPE_DECLARATION Unit",
            "  PROTOCOL_METHOD_DECLARATION area",
            "STRUCT_DECLARATION Point",
            "  FUNCTION_DECLARATION moved",
            "CLASS_DECLARATION Box",
            "  FUNCTION_DECLARATION area",
            "ENUM_DECLARATION Direction",
            "ACTOR_DECLARATION Counter",
            "TYPEALIAS_DECLARATION Coordinates",
            "EXTENSION_DECLARATION Array",
            "  FUNCTION_DECLARATION total",
            "FUNCTION_DECLARATION topLevel"
        ), stubs);
    }

    @RequiredReadAction
    @Test
    public void testNames(Context context) {
        PsiFile file = createFile(context, null, "Names.swift", SwiftFileType.INSTANCE, SOURCE);
        List<String> names = new ArrayList<>();
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof SwiftDeclaration declaration) {
                    PsiElement nameIdentifier = declaration.getNameIdentifier();
                    String name = declaration.getName();
                    names.add(declaration.getNode().getElementType() + " " + name
                        + (nameIdentifier == null ? "" : " @" + nameIdentifier.getText()));
                }
                super.visitElement(element);
            }
        });

        assertEquals(List.of(
            "IMPORT_DECLARATION null",
            "PROTOCOL_DECLARATION Shape @Shape",
            "PROTOCOL_ASSOCIATED_TYPE_DECLARATION Unit @Unit",
            "PROTOCOL_METHOD_DECLARATION area @area",
            "PROTOCOL_PROPERTY_DECLARATION name @name",
            "STRUCT_DECLARATION Point @Point",
            "VARIABLE_DECLARATION x @x",
            "CONSTANT_DECLARATION y @y",
            "FUNCTION_DECLARATION moved @moved",
            "STRUCT_DECLARATION Local @Local",
            "FUNCTION_DECLARATION helper @helper",
            "CLASS_DECLARATION Box @`Box`",
            "FUNCTION_DECLARATION area @area",
            "ENUM_DECLARATION Direction @Direction",
            "ACTOR_DECLARATION Counter @Counter",
            "TYPEALIAS_DECLARATION Coordinates @Coordinates",
            "EXTENSION_DECLARATION Array @Array",
            "FUNCTION_DECLARATION total @total",
            "FUNCTION_DECLARATION topLevel @topLevel",
            "CONSTANT_DECLARATION closure @closure",
            "FUNCTION_DECLARATION inClosure @inClosure",
            "CONSTANT_DECLARATION null"
        ), names);
    }

    private static void collect(StubElement<?> stub, String indent, List<String> result) {
        for (StubElement<?> child : stub.getChildrenStubs()) {
            if (child instanceof SwiftDeclarationStub declaration) {
                result.add(indent + declaration.getStubType() + " " + declaration.getName());
                collect(child, indent + "  ", result);
            }
            else {
                collect(child, indent, result);
            }
        }
    }

    @Override
    protected LanguageFileType getFileType(Context context, @Nullable Object testContext) {
        return SwiftFileType.INSTANCE;
    }

    @Override
    protected void checkResult(Context context, @Nullable Object testContext, PsiFile file) {
    }
}
