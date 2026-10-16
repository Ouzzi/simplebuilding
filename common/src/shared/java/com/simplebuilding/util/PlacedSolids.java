package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Placed smithing templates, pottery sherds and music discs as small 3D models instead of the punched item plate (owner
 * 2026-10-15, docs/ai/VORSCHLAG-ITEM-DISPLAY-3D-2026-10-15.md, answers D1-D7):
 *
 * <ul>
 *   <li>{@link Shape#SLAB}: the silhouette of the item sprite as stepped cuboids, the sprite itself on the front, the
 *       border pixels on the sides; {@link #MODEL_THICKNESS} model px thick, which is 2 px at the single-part scale of
 *       14/16 and about 1.1 px at the 8/16 scale inside a pile;</li>
 *   <li>{@link Shape#DISC}: a round disc of 12 px (rows {@link #DISC_ROWS}), 1 px thick, with a top view texture that is
 *       built from the item sprite at build time, never checked in; discs lying alone on a spot form a real stack.</li>
 * </ul>
 *
 * Geometry and models come from {@code tools/textures/placed_display_3d_2026_10_16.py} (models
 * {@code simplebuilding:block/placed_<item path>}, item definitions {@code simplebuilding:placed_<item path>}; the renderer
 * puts them on a stand-in stack). Only the items listed here have a model; every other item of the tags keeps the flat plate
 * (mods' sherds and discs). The hammer switches a spot back to the flat plates ({@code FlatToggles}). 26.3 only
 * ({@link McVersion#SMALL_PLACEABLES}).
 */
public final class PlacedSolids {
    /** Which model family an item has. */
    public enum Shape {
        SLAB, DISC
    }

    /** Slab thickness in model px (16/7: exactly 2 px at the single scale 14/16). */
    public static final float MODEL_THICKNESS = 16.0F / 7.0F;
    /** Disc thickness in px; a stack adds this much per disc. */
    public static final float DISC_HEIGHT = 1.0F;
    /** Width of each pixel row of the 12 px disc (top to bottom); the generator asserts the same table. */
    public static final int[] DISC_ROWS = {4, 8, 10, 10, 12, 12, 12, 12, 10, 10, 8, 4};
    /** Radius of a disc lying alone (px). */
    public static final float DISC_RADIUS = 6.0F;
    /** x/z scale of a disc that shares the spot with other parts (radius 4 px, fits every cell of a pile). */
    public static final float DISC_PILE_SCALE = 8.0F / 12.0F;

    private static final List<String> TRIMS = List.of("bolt", "coast", "dune", "eye", "flow", "host", "raiser", "rib", "sentry",
            "shaper", "silence", "snout", "spire", "tide", "vex", "ward", "wayfinder", "wild");
    private static final List<String> SHERDS = List.of("angler", "archer", "arms_up", "blade", "brewer", "burn", "danger", "explorer",
            "flow", "friend", "guster", "heart", "heartbreak", "howl", "miner", "mourner", "plenty", "prize", "scrape", "sheaf",
            "shelter", "skull", "snort");
    private static final List<String> VANILLA_DISCS = List.of("11", "13", "5", "blocks", "bounce", "cat", "chirp", "creator",
            "creator_music_box", "far", "lava_chicken", "mall", "mellohi", "otherside", "pigstep", "precipice", "relic", "stal",
            "strad", "tears", "wait", "ward");
    private static final List<String> SONGS = List.of("voidline", "driftwood", "daybreak", "brimstone");
    private static final List<String> TRACKS = List.of("", "_b_side", "_track_3", "_track_4");

    /** Item ids with a slab model, and with a disc model. */
    public static final Set<String> SLABS;
    public static final Set<String> DISCS;

    static {
        List<String> slabs = new ArrayList<>();
        for (String trim : TRIMS) {
            slabs.add("minecraft:" + trim + "_armor_trim_smithing_template");
        }
        slabs.add("minecraft:netherite_upgrade_smithing_template");
        slabs.add("simplelib:enderite_upgrade_template");
        slabs.add("simplelib:basic_upgrade_template");
        for (String sherd : SHERDS) {
            slabs.add("minecraft:" + sherd + "_pottery_sherd");
        }
        SLABS = Set.copyOf(slabs);
        List<String> discs = new ArrayList<>();
        for (String disc : VANILLA_DISCS) {
            discs.add("minecraft:music_disc_" + disc);
        }
        for (String song : SONGS) {
            for (String track : TRACKS) {
                discs.add("simplebuilding:music_disc_" + song + track);
            }
        }
        DISCS = Set.copyOf(discs);
    }

    private PlacedSolids() {
    }

    /** The model family of {@code item}, or null for the flat plate (also on lines without the feature). */
    public static @Nullable Shape shape(Item item) {
        if (!McVersion.SMALL_PLACEABLES) {
            return null;
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        return SLABS.contains(id) ? Shape.SLAB : DISCS.contains(id) ? Shape.DISC : null;
    }

    /** Same, for a stack (empty = null). */
    public static @Nullable Shape shape(ItemStack stack) {
        return stack.isEmpty() ? null : shape(stack.getItem());
    }

    /** The item definition that draws {@code item} in 3D: {@code <namespace>:placed_<item path>}. */
    public static Identifier modelId(String namespace, Item item) {
        return Identifier.fromNamespaceAndPath(namespace, "placed_" + BuiltInRegistries.ITEM.getKey(item).getPath());
    }

    /**
     * The disc as cuboids: {y0, y1, half length in x, half length in z, offset x, offset z} in px around x = z = 8, rows of
     * equal width merged. The strip rows of the model are the texture rows 2..13, so row r lies at z = 2 + r.
     */
    public static float[][] discBoxes() {
        List<float[]> boxes = new ArrayList<>();
        int a = 0;
        while (a < DISC_ROWS.length) {
            int b = a;
            while (b + 1 < DISC_ROWS.length && DISC_ROWS[b + 1] == DISC_ROWS[a]) {
                b++;
            }
            float z0 = 2.0F + a;
            float z1 = 3.0F + b;
            boxes.add(new float[]{0.0F, DISC_HEIGHT, DISC_ROWS[a] / 2.0F, (z1 - z0) / 2.0F, 0.0F, (z0 + z1) / 2.0F - 8.0F});
            a = b + 1;
        }
        return boxes.toArray(new float[0][]);
    }
}
