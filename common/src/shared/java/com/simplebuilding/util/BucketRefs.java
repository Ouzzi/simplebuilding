package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The crucible buckets and soul lava by id (split stage 5b). They belong to the required, bundled sub-mod Simple
 * Crucibles ({@code simplecrucibles:*}) and SimpleBuilding imports none of its classes in shared code (principle 3):
 * tabs, the search tab, the test centre, the guide and the tests find them in the registry. On 26.2
 * ({@link McVersion#CRUCIBLE} false) there are none and every list is empty. Call after registration.
 */
public final class BucketRefs {
    public static final String NAMESPACE = "simplecrucibles";

    /** Every bucket item in creative-tab order (the raw ceramic bucket included), as Simple Crucibles lists them. */
    public static final List<String> ALL = List.of("copper_bucket", "copper_water_bucket", "copper_lava_bucket", "soul_lava_bucket",
            "enderite_bucket", "enderite_water_bucket", "enderite_lava_bucket", "enderite_soul_lava_bucket", "enderite_milk_bucket",
            "raw_ceramic_bucket", "ceramic_bucket", "ceramic_water_bucket", "ceramic_lava_bucket",
            "chipped_ceramic_bucket", "chipped_ceramic_water_bucket", "chipped_ceramic_lava_bucket",
            "cracked_ceramic_bucket", "cracked_ceramic_water_bucket", "cracked_ceramic_lava_bucket",
            "brittle_ceramic_bucket", "brittle_ceramic_water_bucket", "brittle_ceramic_lava_bucket",
            "enderite_water_bucket_full", "enderite_lava_bucket_full", "enderite_soul_lava_bucket_full", "enderite_milk_bucket_full");
    /** Only reached by scooping twice (two buckets inside): not in the creative tab. */
    public static final List<String> FULL_ENDERITE = List.of("enderite_water_bucket_full", "enderite_lava_bucket_full",
            "enderite_soul_lava_bucket_full", "enderite_milk_bucket_full");
    /** Only reached by pouring (wear stages 1..3): not in the creative tab. */
    public static final List<String> WORN_CERAMIC = List.of("chipped_ceramic_bucket", "chipped_ceramic_water_bucket",
            "chipped_ceramic_lava_bucket", "cracked_ceramic_bucket", "cracked_ceramic_water_bucket", "cracked_ceramic_lava_bucket",
            "brittle_ceramic_bucket", "brittle_ceramic_water_bucket", "brittle_ceramic_lava_bucket");

    private BucketRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    /** A bucket item by path; {@link Items#AIR} when it is not registered (26.2, module missing). */
    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** Whether the bucket items are there (26.3 with Simple Crucibles). */
    public static boolean present() {
        return McVersion.CRUCIBLE && item("copper_bucket") != Items.AIR;
    }

    /** All registered bucket items in creative-tab order; empty without them. */
    public static List<Item> buckets() {
        return items(ALL);
    }

    public static List<Item> fullEnderiteBuckets() {
        return items(FULL_ENDERITE);
    }

    public static List<Item> wornCeramicBuckets() {
        return items(WORN_CERAMIC);
    }

    /** {@link #buckets()} without the worn ceramic stages and the full Enderite buckets: what the creative and search tabs offer. */
    public static List<Item> creativeBuckets() {
        List<String> paths = new ArrayList<>(ALL);
        paths.removeAll(FULL_ENDERITE);
        paths.removeAll(WORN_CERAMIC);
        return items(paths);
    }

    private static List<Item> items(List<String> paths) {
        if (!present()) return List.of();
        List<Item> out = new ArrayList<>();
        for (String path : paths) {
            Item item = item(path);
            if (item != Items.AIR) out.add(item);
        }
        return out;
    }
}
