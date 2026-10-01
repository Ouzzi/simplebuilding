package dev.simpledimension.common.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionPortalConfigTest {

    @Test
    void defaultConfigUsesArchesAndSeparateLight() {
        DimensionPortalConfig cfg = DimensionPortalConfig.defaultSkyblock();
        assertNotNull(cfg.worldGeneration);
        assertTrue(cfg.requireSeparateLight);
        assertEquals(6, cfg.portalRecipes.size());
        assertTrue(cfg.portalRecipes.stream().allMatch(r -> r.legend.values().stream().allMatch(b -> b.equals("minecraft:glowstone") || b.equals("minecraft:air"))));
        assertNotNull(cfg.frameBlock);
        // Default frame is not obsidian, so vanilla nether portals are never hijacked.
        assertFalse(cfg.frameBlock.equals("minecraft:obsidian"));
        assertEquals(2, cfg.minPortalWidth);
        assertEquals(3, cfg.minPortalHeight);
    }

    @Test
    void presetsUseDistinctFrameBlocksForDisambiguation() {
        String sky = DimensionPortalConfig.defaultSkyblock().frameBlock;
        String mining = DimensionPortalConfig.miningDimensionPreset().frameBlock;
        String travel = DimensionPortalConfig.travelDimensionPreset().frameBlock;
        assertEquals(3, java.util.Set.of(sky, mining, travel).size());
    }

    @Test
    void presetsAreOnlyOpenableFromTheOverworld() {
        for (DimensionPortalConfig cfg : java.util.List.of(
                DimensionPortalConfig.defaultSkyblock(),
                DimensionPortalConfig.miningDimensionPreset(),
                DimensionPortalConfig.travelDimensionPreset())) {
            assertEquals("minecraft:overworld", cfg.sourceDimensionId);
            assertTrue(cfg.allowIgniteFromSource);
            assertFalse(cfg.allowIgniteFromTarget);
            assertTrue(cfg.openFromDimensions.isEmpty());
        }
    }

    @Test
    void everyPresetDefinesValidRecipes() {
        for (DimensionPortalConfig cfg : java.util.List.of(
                DimensionPortalConfig.defaultSkyblock(),
                DimensionPortalConfig.miningDimensionPreset(),
                DimensionPortalConfig.travelDimensionPreset())) {
            java.util.List<PortalRecipe> recipes = cfg.buildRecipes();
            assertEquals(cfg.id.equals("skyblock") ? 6 : 1, recipes.size());
            assertTrue(recipes.stream().allMatch(PortalRecipe::hasInterior));
        }
    }

    @Test
    void travelRatiosMatchTheRequestedSpec() {
        assertEquals(1.0, DimensionPortalConfig.defaultSkyblock().travelCoordinateScale);
        assertEquals(0.5, DimensionPortalConfig.miningDimensionPreset().travelCoordinateScale); // 1 overworld : 2 mining
        assertEquals(10.0, DimensionPortalConfig.travelDimensionPreset().travelCoordinateScale); // 10 overworld : 1 travel
    }

    @Test
    void colorHexParsingIsTolerant() {
        assertEquals(0x66D9FF, DimensionPortalConfig.parseHexColor("#66D9FF", 0));
        assertEquals(0x66D9FF, DimensionPortalConfig.parseHexColor("66D9FF", 0));
        assertEquals(0xFFAA00, DimensionPortalConfig.parseHexColor("#FA0", 0)); // shorthand
        assertEquals(0x123456, DimensionPortalConfig.parseHexColor("not-a-color", 0x123456)); // fallback
    }

    @Test
    void buildScannerClampsRangeToSelfConsistentWindow() {
        DimensionPortalConfig cfg = DimensionPortalConfig.defaultSkyblock();
        cfg.minPortalWidth = 5;
        cfg.maxPortalWidth = 2; // inverted on purpose
        PortalFrameScanner scanner = cfg.buildScanner();
        assertTrue(scanner.maxWidth() >= scanner.minWidth());
    }

    @Test
    void miningPresetEnablesOreGenerationAndHighWorldHeight() {
        DimensionPortalConfig cfg = DimensionPortalConfig.miningDimensionPreset();
        assertTrue(cfg.worldGeneration.generateOres);
        assertTrue(cfg.worldGeneration.height >= 384);
        assertTrue(cfg.worldGeneration.flatLayers.size() >= 3);
    }
}
