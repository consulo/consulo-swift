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

import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.psi.stub.PsiFileStub;
import consulo.swift.language.SwiftLanguage;
import consulo.swift.language.psi.SwiftFile;

public class SwiftFileStubElementType extends IStubFileElementType<PsiFileStub<SwiftFile>> {
    /**
     * Bump when the stubs or what is indexed change.
     */
    public static final int VERSION = 1;

    public SwiftFileStubElementType() {
        // the debug name of the plain file element type it replaced - PSI dumps print it
        super("FILE", SwiftLanguage.INSTANCE);
    }

    @Override
    public int getStubVersion() {
        return VERSION;
    }

    @Override
    public String getExternalId() {
        return "swift.FILE";
    }
}
