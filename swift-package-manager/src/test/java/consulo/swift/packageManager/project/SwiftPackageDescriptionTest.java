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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixtures are the output of Swift 6.4 for a package with a library, an executable, a test target and a local dependency.
 * Building the {@code DataNode} tree needs the platform ({@code ProjectSystemId} loads icons), so only parsing is covered here.
 */
public class SwiftPackageDescriptionTest {
    @Test
    public void testParsePackage() throws IOException {
        SwiftPackageDescription.Package swiftPackage = SwiftPackageDescription.parsePackage(fixture("describe.json"));

        assertEquals("Demo", swiftPackage.name());
        List<String> targets = swiftPackage.targets().stream().map(SwiftPackageDescription.Target::name).toList();
        assertEquals(List.of("DemoTests", "DemoLib", "Demo"), targets);
        assertEquals("Tests/DemoTests", swiftPackage.targets().get(0).path());
        assertTrue(swiftPackage.targets().get(0).isTest());
        assertEquals("executable", swiftPackage.targets().get(2).type());
        assertEquals(List.of("Demo"), swiftPackage.executableProducts().stream().map(SwiftPackageDescription.Product::name).toList());
    }

    @Test
    public void testParseDependencies() throws IOException {
        List<SwiftPackageDescription.Dependency> dependencies =
            SwiftPackageDescription.parseDependencies(fixture("show-dependencies.json"));

        assertEquals(1, dependencies.size());
        SwiftPackageDescription.Dependency helper = dependencies.get(0);
        assertEquals("helper", helper.identity());
        assertEquals("Helper", helper.name());
        assertEquals("@HELPER@", helper.path());
    }

    @Test
    public void testOutputBeforeJson() {
        String output = "Fetching https://example.com/x.git\n{\"name\": \"X\"}";
        SwiftPackageDescription.Package swiftPackage = SwiftPackageDescription.parsePackage(output);
        assertEquals("X", swiftPackage.name());
    }

    private static String fixture(String name) throws IOException {
        try (InputStream stream = SwiftPackageDescriptionTest.class.getResourceAsStream("/swiftpm/" + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
