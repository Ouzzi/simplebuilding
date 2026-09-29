package com.simplebuilding.clientgametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Armor trim item sprites exist in the items atlas for every trim material of the mod.
 *
 * <p>The trim overlays on armour items are paletted permutations: the items atlas pairs every trim
 * mask ({@code minecraft:trims/items/helmet_trim}, the mod's 72 {@code simplebuilding:trims/items/*}
 * pattern masks) with every material palette. A palette the atlas cannot load does not stop
 * anything - the loader logs "Failed to load palette image" and the sprite is simply missing, so the
 * trimmed item shows the pink missing texture. Every server test is blind to that.
 *
 * <p>It happened on NeoForge 26.3 (2026-09-29): NeoForge's own items atlas source
 * ({@code neoforge:directory_paletted_permutations}) paired the mod's leftover 26.2 palettes in
 * {@code textures/trims/color_palettes} with every trim mask, keyed with a palette 26.3 no longer
 * has, and replaced the mod's correct Astralit, Enderite and Nihilith sprites with broken ones.
 * {@code gradle/check-atlases.gradle} guards the atlas definitions at build time; this test
 * observes the stitched atlas itself.
 *
 * <p>The second half checks that the icons use those sprites at all. NeoForge 26.x ships its own
 * {@code assets/minecraft/items/<armour>.json} ({@code neoforge:trimmed_armor}) for every vanilla
 * armour piece; while NeoForge's pack sat above the mod's, those hid the mod's pattern icons
 * ({@code simplebuilding:visible_trim_icons}) - a plain material spot on 26.2, no trim at all on
 * 26.3, where NeoForge's trim layer is still a TODO. The mod now loads after NeoForge
 * ({@code ordering="AFTER"} in {@code neoforge.mods.toml}); the test reads the particle sprites of
 * every layer of the resolved item render state and expects the mod's pattern overlay among them.
 *
 * <p><b>Observed:</b> the items atlas by sprite name. A sprite that failed to load resolves to
 * {@code minecraft:missingno}; a sprite that exists but came out empty (a palette that mapped
 * nothing) has no opaque pixel. Vanilla's iron is the control - if it fails too, the mask itself is
 * gone, not a palette. The screenshot is documentary: the player's inventory with trimmed vanilla
 * and mod armour in the Astralit, Enderite and Nihilith materials.
 *
 * <p><b>What breaks this test:</b> a mod palette missing or in the wrong place for the version
 * ({@code textures/trims/color_palettes} on 26.2 and 1.21.11, {@code textures/palettes/trim} from
 * 26.3 on), a wrong palette id in {@code assets/minecraft/atlases/items.json} or in its 26.3 rewrite
 * ({@code mc26_3/resources.gradle}), and any other atlas source that overwrites these sprites with
 * broken ones.
 */
public final class TrimTextureClientTest {

    private TrimTextureClientTest() {
    }

    /** Trim masks: vanilla's (paired with the mod's materials by the mod's atlas) and two of the mod's. */
    private static final List<String> MASKS = List.of(
            "minecraft:trims/items/helmet_trim",
            "minecraft:trims/items/boots_trim",
            "simplebuilding:trims/items/helmet_sentry",
            "simplebuilding:trims/items/chestplate_dune",
            "simplebuilding:trims/items/boots_wild");

    /** The mod's six palettes, and vanilla iron as the control. */
    private static final List<String> MATERIALS = List.of(
            "astralit", "astralit_darker", "enderite", "enderite_darker", "nihilith", "nihilith_darker", "iron");

    /**
     * Inventory slot (command syntax), the item, its trim material and pattern, and the pattern
     * overlay sprite its icon must draw (null: not asserted, only in the screenshot).
     */
    private static final String[][] SHOWCASE = {
            {"hotbar.0", "minecraft:diamond_helmet", "astralit", "sentry", "simplebuilding:trims/items/helmet_sentry_astralit"},
            {"hotbar.1", "minecraft:diamond_chestplate", "enderite", "dune", "simplebuilding:trims/items/chestplate_dune_enderite"},
            {"hotbar.2", "minecraft:iron_leggings", "nihilith", "raiser", "simplebuilding:trims/items/leggings_raiser_nihilith"},
            {"hotbar.3", "minecraft:netherite_boots", "astralit", "wild", "simplebuilding:trims/items/boots_wild_astralit"},
            {"hotbar.4", "simplebuilding:enderite_helmet", "enderite", "host", null},
            {"hotbar.5", "simplebuilding:enderite_chestplate", "nihilith", "sentry", null},
            {"hotbar.6", "minecraft:golden_helmet", "nihilith", "eye", "simplebuilding:trims/items/helmet_eye_nihilith"},
            {"hotbar.7", "minecraft:iron_chestplate", "iron", "sentry", null},
    };

    public static void inWorld(Script script) {
        script.act("every trim mask is stitched with every mod trim palette", client -> {
            var atlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS);
            Identifier missing = MissingTextureAtlasSprite.getLocation();
            List<String> found = new ArrayList<>();
            int checked = 0;

            for (String mask : MASKS) {
                for (String material : MATERIALS) {
                    Identifier id = Identifier.parse(mask + "_" + material);
                    TextureAtlasSprite sprite = atlas.getSprite(id);
                    checked++;

                    if (sprite.contents().name().equals(missing)) {
                        found.add(id + " is missing (" + missing + ")");
                    } else if (!hasOpaquePixel(sprite)) {
                        found.add(id + " is fully transparent");
                    }
                }
            }

            if (!found.isEmpty()) {
                throw new AssertionError(found.size() + " of " + checked
                        + " trim item sprites are broken in the items atlas: " + found);
            }
        });

        for (String[] piece : SHOWCASE) {
            script.command("item replace entity @a " + piece[0] + " with " + piece[1]
                    + "[minecraft:trim={material:\"" + material(piece[2]) + "\",pattern:\"minecraft:" + piece[3] + "\"}]");
        }
        script.act("trimmed vanilla armour resolves to the mod's pattern icons", client -> {
            List<String> found = new ArrayList<>();

            for (String[] piece : SHOWCASE) {
                String expected = piece[4];
                if (expected == null) {
                    continue;
                }
                int slot = Integer.parseInt(piece[0].substring("hotbar.".length()));
                ItemStackRenderState state = new ItemStackRenderState();
                client.getItemModelResolver().updateForTopItem(state, client.player.getInventory().getItem(slot),
                        ItemDisplayContext.GUI, client.level, client.player, 0);
                // Every layer's particle sprite: the state picks one layer at random per call.
                Set<String> sprites = new TreeSet<>();
                for (int i = 0; i < 64; i++) {
                    sprites.add(String.valueOf(state.pickParticleIcon(RandomSource.create()).contents().name()));
                }
                if (!sprites.contains(expected)) {
                    found.add(piece[1] + " with " + piece[3] + "/" + piece[2] + " draws " + sprites + ", expected " + expected);
                }
            }

            if (!found.isEmpty()) {
                throw new AssertionError("Trimmed armour icons do not use the mod's item definitions (another pack's "
                        + "assets/minecraft/items/*.json wins, e.g. NeoForge's neoforge:trimmed_armor): " + found);
            }
        });
        // Survival for the shot: in creative, InventoryScreen hands over to the creative inventory
        // at once, which has no armour slots and opens on whatever tab was last used.
        script.command("gamemode survival @a");
        script.act("open the inventory", client -> client.setScreen(new InventoryScreen(client.player)));
        script.await("wait for the inventory", 100,
                client -> client.screen instanceof InventoryScreen,
                client -> "the inventory never opened; the current screen is " + client.screen);
        script.idle("let the item icons settle", 10);
        script.shot("trim-items-mod-materials");
        script.act("close the inventory", client -> client.setScreen(null));
        script.await("wait until the inventory is closed", 100,
                client -> client.screen == null,
                client -> "a screen is still open: " + client.screen);
        script.command("gamemode creative @a");
        for (String[] piece : SHOWCASE) {
            script.command("item replace entity @a " + piece[0] + " with minecraft:air");
        }
    }

    private static String material(String name) {
        return name.equals("iron") ? "minecraft:iron" : "simplebuilding:" + name;
    }

    private static boolean hasOpaquePixel(TextureAtlasSprite sprite) {
        int width = sprite.contents().width();
        int height = sprite.contents().height();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!sprite.contents().isTransparent(0, x, y)) {
                    return true;
                }
            }
        }

        return false;
    }
}
