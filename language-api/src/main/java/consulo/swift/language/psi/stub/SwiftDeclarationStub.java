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

package consulo.swift.language.psi.stub;

import consulo.index.io.StringRef;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.NamedStubBase;
import consulo.language.psi.stub.StubElement;
import consulo.swift.language.psi.SwiftDeclaration;
import org.jspecify.annotations.Nullable;

/**
 * Stub of every declaration - one class for all of them, since all declaration PSI shares the {@link SwiftDeclaration}
 * super type (whose {@code StubBasedPsiElement} type argument cannot differ between subtypes).
 */
public class SwiftDeclarationStub extends NamedStubBase<SwiftDeclaration> {
    public SwiftDeclarationStub(@Nullable StubElement parent, IStubElementType elementType, @Nullable StringRef name) {
        super(parent, elementType, name);
    }

    public SwiftDeclarationStub(@Nullable StubElement parent, IStubElementType elementType, @Nullable String name) {
        super(parent, elementType, name);
    }
}
