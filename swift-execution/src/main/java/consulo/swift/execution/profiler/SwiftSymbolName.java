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
package consulo.swift.execution.profiler;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a demangled Swift symbol ({@code swift-demangle --simplified}) refers to:
 * {@code Point.init(x:y:)}, {@code Point.x.getter}, {@code static Main.main()}, {@code closure #1 in greet()}, ...
 *
 * @param typeName   the type declaring the member, {@code null} for a top-level function
 * @param memberName the function, or the property of an accessor; {@code null} when only the type is known
 * @param function   whether {@code memberName} is a function (otherwise an initializer, accessor, ...)
 */
public record SwiftSymbolName(@Nullable String typeName, @Nullable String memberName, boolean function) {
    private static final Pattern PREFIXES = Pattern.compile(
        "^(?:(?:static|class|specialized|merged|@objc|partial apply for|dispatch thunk of|protocol witness for"
            + "|(?:implicit )?closure #\\d+ in|generic specialization <[^>]*> of) )+"
    );
    private static final Pattern GENERIC_ARGUMENTS = Pattern.compile("<[^<>]*>");
    private static final Set<String> ACCESSORS = Set.of(
        "getter", "setter", "modify", "read", "_modify", "_read", "didset", "willset", "unsafeAddressor", "unsafeMutableAddressor"
    );
    private static final Set<String> NOT_FUNCTIONS = Set.of("init", "deinit", "subscript", "__allocating_init", "__deallocating_deinit");

    public static @Nullable SwiftSymbolName parse(String symbol) {
        String text = symbol.trim();
        Matcher prefixes = PREFIXES.matcher(text);
        if (prefixes.lookingAt()) {
            text = text.substring(prefixes.end());
        }
        int parameters = text.indexOf('(');
        if (parameters >= 0) {
            text = text.substring(0, parameters);
        }
        String previous;
        do {
            previous = text;
            text = GENERIC_ARGUMENTS.matcher(text).replaceAll("");
        }
        while (!previous.equals(text));
        int space = text.lastIndexOf(' ');
        if (space >= 0) {
            text = text.substring(space + 1);
        }

        String[] parts = text.split("\\.");
        int last = parts.length - 1;
        if (last < 0 || parts[last].isEmpty()) {
            return null;
        }
        if (ACCESSORS.contains(parts[last])) {
            // Point.x.getter
            String property = last >= 1 ? parts[last - 1] : null;
            String type = last >= 2 ? parts[last - 2] : null;
            return new SwiftSymbolName(type, property, false);
        }
        String member = parts[last];
        String type = last >= 1 ? parts[last - 1] : null;
        if (NOT_FUNCTIONS.contains(member)) {
            return type == null ? null : new SwiftSymbolName(type, null, false);
        }
        return new SwiftSymbolName(type, member, true);
    }
}
