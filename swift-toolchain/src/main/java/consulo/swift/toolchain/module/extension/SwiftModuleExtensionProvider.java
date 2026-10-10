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
package consulo.swift.toolchain.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.swift.language.icon.SwiftIconGroup;
import consulo.swift.toolchain.localize.SwiftToolchainLocalize;
import consulo.ui.image.Image;

@ExtensionImpl
public class SwiftModuleExtensionProvider implements ModuleExtensionProvider<SwiftModuleExtension> {
    public static final String ID = "swift";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return SwiftToolchainLocalize.moduleExtensionName();
    }

    @Override
    public Image getIcon() {
        return SwiftIconGroup.swift();
    }

    @Override
    public ModuleExtension<SwiftModuleExtension> createImmutableExtension(ModuleRootLayer moduleRootLayer) {
        return new SwiftModuleExtension(getId(), moduleRootLayer);
    }

    @Override
    public MutableModuleExtension<SwiftModuleExtension> createMutableExtension(ModuleRootLayer moduleRootLayer) {
        return new SwiftMutableModuleExtension(getId(), moduleRootLayer);
    }
}
