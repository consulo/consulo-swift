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
package consulo.swift.execution.test;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

/**
 * The tests to run; a {@code null} framework means {@link SwiftTestFramework#ALL}.
 */
@CompiledJson
public class SwiftTestRunConfigurationOptions extends RunConfigurationOptions {
    private @Nullable String myFilter;
    private @Nullable String myFramework;

    @Attribute("filter")
    public @Nullable String getFilter() {
        return myFilter;
    }

    public void setFilter(@Nullable String filter) {
        myFilter = StringUtil.nullize(filter, true);
    }

    @Attribute("framework")
    public @Nullable String getFramework() {
        return myFramework;
    }

    public void setFramework(@Nullable String framework) {
        myFramework = StringUtil.nullize(framework, true);
    }
}
