package com.simplebuilding.modules.simpletools.bucket;

import com.mojang.serialization.Codec;
import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplelib.api.IdMoves;
import com.simplelib.bucket.BucketDispensing;
import com.simplelib.bucket.Buckets;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * WH26-7 (owner 06.11.): the Enderite and ceramic buckets are Simple Tools' (were Simple Crucibles', before that
 * SimpleBuilding's). The item classes and the shared rules live in SimpleLib ({@link Buckets}); the Enderite soul lava
 * buckets come with Simple Crucibles' soul lava. Old {@code simplebuilding:} and {@code simplecrucibles:} ids move here.
 */
public final class ToolsBuckets {
    /** Every bucket id of this module (items; the soul lava ones only exist with Simple Crucibles). */
    public static final List<String> IDS = List.of("enderite_bucket", "enderite_water_bucket", "enderite_lava_bucket", "enderite_soul_lava_bucket",
            "enderite_water_bucket_full", "enderite_lava_bucket_full", "enderite_soul_lava_bucket_full", "enderite_milk_bucket",
            "enderite_milk_bucket_full", "raw_ceramic_bucket", "ceramic_bucket", "ceramic_water_bucket", "ceramic_lava_bucket",
            "chipped_ceramic_bucket", "chipped_ceramic_water_bucket", "chipped_ceramic_lava_bucket", "cracked_ceramic_bucket",
            "cracked_ceramic_water_bucket", "cracked_ceramic_lava_bucket", "brittle_ceramic_bucket", "brittle_ceramic_water_bucket",
            "brittle_ceramic_lava_bucket");
    /** Recipe ids (each with its recipe advancement under recipes/tools/). */
    public static final List<String> RECIPES = List.of("enderite_bucket", "raw_ceramic_bucket", "ceramic_bucket_from_smelting_raw_ceramic_bucket");
    /** The previous owners: SimpleBuilding (before split stage 5b) and Simple Crucibles (until WH26-7). */
    public static final List<String> OLD_NAMESPACES = List.of("simplebuilding", "simplecrucibles");

    private ToolsBuckets() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SimpleTools.MOD_ID, path);
    }

    /** Old ids resolve to the module's (SimpleLib IdMoves; the next save writes the new id). Called before registration. */
    public static void registerMoves() {
        for (String ns : OLD_NAMESPACES) {
            for (String path : IDS) IdMoves.move(Registries.ITEM, Identifier.fromNamespaceAndPath(ns, path), id(path));
            IdMoves.move(Registries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(ns, "ceramic_uses"), id("ceramic_uses"));
            for (String path : RECIPES) {
                IdMoves.move(Registries.RECIPE, Identifier.fromNamespaceAndPath(ns, path), id(path));
                IdMoves.move(Registries.ADVANCEMENT, Identifier.fromNamespaceAndPath(ns, "recipes/tools/" + path), id("recipes/tools/" + path));
            }
        }
    }

    /** The ceramic bucket's pour counter (DATA_COMPONENT_TYPE phase; Fabric directly). */
    public static void registerComponents() {
        if (Buckets.CERAMIC_USES != null) return;
        DataComponentType<Integer> uses = DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 31)).networkSynchronized(ByteBufCodecs.VAR_INT).build();
        Buckets.CERAMIC_USES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("ceramic_uses"), uses);
    }

    /** The bucket items and their dispenser behaviour (ITEM phase). */
    public static void registerItems() {
        registerComponents();
        Buckets.registerEnderiteAndCeramic(SimpleTools.MOD_ID);
        BucketDispensing.register(Buckets.toolsBuckets());
    }

    /** Creative tab entries after Vanilla's powder snow bucket (the module tab collects them); hidden with the sub-mod off. */
    public static List<ItemStack> tabStacks() {
        if (!SimpleTools.active()) return List.of();
        return Buckets.creativeToolsBuckets().stream().map(ItemStack::new).toList();
    }
}
