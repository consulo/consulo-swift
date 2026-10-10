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

package consulo.swift.language.editor.impl.navigation;

import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.ide.navigation.ChooseByNameContributorEx;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.navigation.NavigationItem;
import consulo.swift.language.psi.SwiftDeclaration;
import consulo.swift.language.psi.stub.SwiftDeclarationKind;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * "Go to" over the Swift declaration indexes of the given kinds.
 */
public abstract class SwiftGotoContributorBase implements ChooseByNameContributorEx {
    private final List<StubIndexKey<String, SwiftDeclaration>> myIndexKeys = new ArrayList<>();

    protected SwiftGotoContributorBase(SwiftDeclarationKind... kinds) {
        for (SwiftDeclarationKind kind : kinds) {
            StubIndexKey<String, SwiftDeclaration> indexKey = kind.getIndexKey();
            if (indexKey != null) {
                myIndexKeys.add(indexKey);
            }
        }
    }

    @Override
    public void processNames(Processor<String> processor, SearchScope scope, @Nullable IdFilter filter) {
        for (StubIndexKey<String, SwiftDeclaration> indexKey : myIndexKeys) {
            ProgressManager.checkCanceled();
            StubIndex.getInstance().processAllKeys(indexKey, processor, (GlobalSearchScope) scope, filter);
        }
    }

    @Override
    public void processElementsWithName(String name, Processor<NavigationItem> processor, FindSymbolParameters parameters) {
        for (StubIndexKey<String, SwiftDeclaration> indexKey : myIndexKeys) {
            ProgressManager.checkCanceled();
            StubIndex.getInstance().processElements(
                indexKey,
                name,
                parameters.getProject(),
                (GlobalSearchScope) parameters.getSearchScope(),
                parameters.getIdFilter(),
                SwiftDeclaration.class,
                processor::process
            );
        }
    }
}
