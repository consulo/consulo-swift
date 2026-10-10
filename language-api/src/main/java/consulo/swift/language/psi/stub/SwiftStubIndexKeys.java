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

import consulo.language.psi.stub.StubIndexKey;
import consulo.swift.language.psi.SwiftDeclaration;

public final class SwiftStubIndexKeys {
    public static final StubIndexKey<String, SwiftDeclaration> TYPE_NAME = StubIndexKey.createIndexKey("swift.type.name");
    public static final StubIndexKey<String, SwiftDeclaration> EXTENSION_TYPE_NAME = StubIndexKey.createIndexKey("swift.extension.type.name");
    public static final StubIndexKey<String, SwiftDeclaration> FUNCTION_NAME = StubIndexKey.createIndexKey("swift.function.name");

    private SwiftStubIndexKeys() {
    }
}
