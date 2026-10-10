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
package consulo.swift.packageManager.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What {@code swift package describe --type json} and {@code swift package show-dependencies --format json} report.
 */
public final class SwiftPackageDescription {
    public record Package(String name, List<Target> targets, List<Product> products) {
        public List<Product> executableProducts() {
            return products.stream().filter(Product::executable).toList();
        }
    }

    /**
     * @param path relative to the package directory
     * @param type {@code executable}, {@code library}, {@code test}, {@code macro}, {@code plugin}, {@code system-target},
     *             {@code binary}, ...
     */
    public record Target(String name, String path, String type) {
        public boolean isTest() {
            return "test".equals(type);
        }

        public boolean hasSources() {
            return !"binary".equals(type);
        }
    }

    public record Product(String name, boolean executable) {
    }

    /**
     * @param path where the dependency is checked out, or the local path of a local dependency
     */
    public record Dependency(String identity, String name, @Nullable String version, @Nullable String path) {
    }

    private SwiftPackageDescription() {
    }

    public static Package parsePackage(String output) throws JsonParseException {
        JsonObject root = parseObject(output);

        List<Target> targets = new ArrayList<>();
        for (JsonElement element : array(root, "targets")) {
            JsonObject target = element.getAsJsonObject();
            String name = string(target, "name");
            String path = string(target, "path");
            if (name != null && path != null) {
                String type = string(target, "type");
                targets.add(new Target(name, path, type == null ? "" : type));
            }
        }

        List<Product> products = new ArrayList<>();
        for (JsonElement element : array(root, "products")) {
            JsonObject product = element.getAsJsonObject();
            String name = string(product, "name");
            JsonElement type = product.get("type");
            if (name != null) {
                boolean executable = type != null && type.isJsonObject() && type.getAsJsonObject().has("executable");
                products.add(new Product(name, executable));
            }
        }

        String name = string(root, "name");
        return new Package(name == null ? "" : name, targets, products);
    }

    /**
     * @return all packages the root package depends on, directly or not - the root itself excluded
     */
    public static List<Dependency> parseDependencies(String output) throws JsonParseException {
        Map<String, Dependency> result = new LinkedHashMap<>();
        collectDependencies(parseObject(output), result);
        return new ArrayList<>(result.values());
    }

    private static void collectDependencies(JsonObject node, Map<String, Dependency> result) {
        for (JsonElement element : array(node, "dependencies")) {
            JsonObject dependency = element.getAsJsonObject();
            String identity = string(dependency, "identity");
            if (identity == null || result.containsKey(identity)) {
                continue;
            }
            String name = string(dependency, "name");
            String version = string(dependency, "version");
            result.put(identity, new Dependency(identity, name == null ? identity : name, version, string(dependency, "path")));
            collectDependencies(dependency, result);
        }
    }

    /**
     * SwiftPM may print progress (fetching, resolving) before the JSON document.
     */
    private static JsonObject parseObject(String output) throws JsonParseException {
        int start = output.indexOf('{');
        if (start < 0) {
            throw new JsonParseException("No JSON object in output");
        }
        return JsonParser.parseString(output.substring(start)).getAsJsonObject();
    }

    private static JsonArray array(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
