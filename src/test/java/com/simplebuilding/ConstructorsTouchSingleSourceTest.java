package com.simplebuilding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Constructor's Touch stick logic exists once per Minecraft line, in the shared
 * {@code ConstructorsTouchInteraction}, and every loader's use-block hook delegates to it.
 *
 * <p>A source check, because no game test can see Forge code: there is no Forge game test target, so
 * a Forge branch in a shared test would never run and stay silently green. Until 2026-09 Forge called
 * its own copy ({@code ModRegistriesForge}), which had drifted - its value readout went to the chat
 * instead of the action bar - and NeoForge carried a third copy nobody called.
 */
class ConstructorsTouchSingleSourceTest {

    private static final String SHARED = "common/src/shared/java/com/simplebuilding/util/ConstructorsTouchInteraction.java";
    private static final String SHARED_1_21_11 = "mc1_21_11/shared/java/com/simplebuilding/util/ConstructorsTouchInteraction.java";

    @Test
    void theStickLogicLivesOnlyInTheSharedClass() throws IOException {
        assertEquals(List.of(SHARED), stickLogicIn("common/src/shared/java", "src/main/java",
                "neoforge/src/main/java", "forge/src/main/java"),
                "Constructor's Touch stick logic exists outside the shared class on the 26.2 line");
        // The 1.21.11 line had it twice until 2026-09 (Fabric ModRegistries, NeoForge
        // ModRegistriesNeoForge), and the NeoForge copy wrote the readout to the chat.
        assertEquals(List.of(SHARED_1_21_11), stickLogicIn("mc1_21_11/shared/java",
                "mc1_21_11/fabric/src/main/java", "mc1_21_11/neoforge/src/main/java"),
                "Constructor's Touch stick logic exists outside the shared class on the 1.21.11 line");
    }

    private static List<String> stickLogicIn(String... roots) throws IOException {
        List<String> hits = new ArrayList<>();
        for (String root : roots) {
            try (Stream<Path> files = Files.walk(Path.of(root))) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                    if (Files.readString(file, StandardCharsets.UTF_8).contains("BlockState cycleState(")) {
                        hits.add(file.toString().replace(java.io.File.separatorChar, '/'));
                    }
                }
            }
        }
        return hits;
    }

    @Test
    void everyLoaderHookDelegatesToTheSharedClass() throws IOException {
        assertTrue(read("forge/src/main/java/com/simplebuilding/forge/ForgeGameplayEvents.java")
                        .contains("ConstructorsTouchInteraction.handleUseBlock("),
                "Forge's RightClickBlock handler no longer calls ConstructorsTouchInteraction.handleUseBlock");
        assertTrue(read("neoforge/src/main/java/com/simplebuilding/neoforge/NeoForgeGameplayEvents.java")
                        .contains("ConstructorsTouchInteraction.handleUseBlock("),
                "NeoForge's RightClickBlock handler no longer calls ConstructorsTouchInteraction.handleUseBlock");
        assertTrue(read("src/main/java/com/simplebuilding/util/ModRegistries.java")
                        .contains("ConstructorsTouchInteraction::handleUseBlock"),
                "Fabric's UseBlockCallback no longer points at ConstructorsTouchInteraction::handleUseBlock");
        assertTrue(read("mc1_21_11/neoforge/src/main/java/com/simplebuilding/neoforge/NeoForgeGameplayEvents.java")
                        .contains("ConstructorsTouchInteraction.handleUseBlock("),
                "1.21.11 NeoForge's RightClickBlock handler no longer calls ConstructorsTouchInteraction.handleUseBlock");
        assertTrue(read("mc1_21_11/fabric/src/main/java/com/simplebuilding/util/ModRegistries.java")
                        .contains("ConstructorsTouchInteraction::handleUseBlock"),
                "1.21.11 Fabric's UseBlockCallback no longer points at ConstructorsTouchInteraction::handleUseBlock");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
