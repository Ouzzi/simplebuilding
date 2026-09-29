package com.simplebuilding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jade (since Minecraft 1.21.6) throws {@code IllegalArgumentException: Data providers cannot
 * implement IComponentProvider} from {@code WailaCommonRegistration.checkDataProvider} on a physical
 * client, so one class that is both an {@code IServerDataProvider} and an {@code IComponentProvider}
 * keeps every client with Jade from starting. No gametest ever loads Jade (it is a dev-only runtime
 * mod of runClient/runServer), so this check is the only automated one: it loads every class of the
 * Jade plugin package (against the real Jade jar, without initializing it) and refuses the mix.
 */
class JadeProviderSplitTest {

    private static final String PACKAGE = "com/simplebuilding/compat/jade";

    @Test
    void noJadeProviderIsBothServerDataAndComponentProvider() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> dataProvider = Class.forName("snownee.jade.api.IServerDataProvider", false, loader);
        Class<?> componentProvider = Class.forName("snownee.jade.api.IComponentProvider", false, loader);

        List<String> classes = pluginClasses(loader);
        assertTrue(classes.contains("com.simplebuilding.compat.jade.SimplebuildingJadePlugin"),
                "Jade plugin classes not found on the test classpath: " + classes);

        List<String> mixed = new ArrayList<>();
        int data = 0;
        int components = 0;
        for (String name : classes) {
            Class<?> type = Class.forName(name, false, loader);
            boolean isData = dataProvider.isAssignableFrom(type);
            boolean isComponent = componentProvider.isAssignableFrom(type);
            if (isData) data++;
            if (isComponent) components++;
            if (isData && isComponent) mixed.add(name);
        }
        assertFalse(data == 0 || components == 0,
                "expected both server data and component providers, found " + data + " / " + components);
        assertTrue(mixed.isEmpty(), "Jade providers that are both IServerDataProvider and IComponentProvider "
                + "(Jade refuses them on a physical client, the client crashes at startup): " + mixed);
    }

    private static List<String> pluginClasses(ClassLoader loader) throws IOException, URISyntaxException {
        URL dir = loader.getResource(PACKAGE);
        assertNotNull(dir, "package " + PACKAGE + " not on the test classpath");
        Path root = Path.of(dir.toURI());
        try (Stream<Path> files = Files.list(root)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".class"))
                    .map(n -> PACKAGE.replace('/', '.') + "." + n.substring(0, n.length() - ".class".length()))
                    .sorted()
                    .toList();
        }
    }
}
