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
package consulo.swift.execution.compiler;

import consulo.execution.configuration.RunConfiguration;
import consulo.module.Module;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * A run configuration that {@link SwiftCompilerRunner} builds before launch, publishing where the binaries are as
 * {@link SwiftCompilerRunner#BIN_PATH}.
 */
public interface SwiftBuildableConfiguration extends RunConfiguration {
    @Nullable Module getBuildModule();

    /**
     * @return {@code swift build ...} arguments, also used to ask for the binary directory with {@code --show-bin-path}
     */
    List<String> getBuildArguments(Path packageDirectory);
}
