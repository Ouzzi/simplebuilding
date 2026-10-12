package com.simplebuilding.modules.simpleenddragonfight;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

/** Entity type and items of Simple End Dragon Fight. Fabric registers through {@link #registerAll()}, the other loaders through their register events. */
public final class DragonFightRegistry {
    public static final String ID = "simpleenddragonfight";

    public static final ResourceKey<EntityType<?>> DRAGONMITE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "dragonmite"));
    /** Endermite size x 1.4. */
    public static final EntityType<Dragonmite> DRAGONMITE =
            EntityType.Builder.<Dragonmite>of(Dragonmite::new, MobCategory.MONSTER)
                    .sized(0.56f, 0.42f)
                    .eyeHeight(0.18f)
                    .clientTrackingRange(8)
                    .build(DRAGONMITE_KEY);

    public static final ResourceKey<Item> CHITIN_KEY = itemKey("dragon_chitin");
    public static final ResourceKey<Item> EGG_KEY = itemKey("dragonmite_spawn_egg");
    public static final Item DRAGON_CHITIN = new Item(new Item.Properties().rarity(Rarity.UNCOMMON).setId(CHITIN_KEY));
    public static final Item DRAGONMITE_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(DRAGONMITE).setId(EGG_KEY));

    private DragonFightRegistry() {}

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    /** Super-mod switch "Enable Simple End Dragon Fight" (framework SubMods) and the module's own config switch. */
    public static boolean on() {
        return DragonFightConfig.enabled && com.simplebuilding.framework.api.SubMods.enabled(ID);
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, DRAGONMITE_KEY, DRAGONMITE);
        Registry.register(BuiltInRegistries.ITEM, CHITIN_KEY, DRAGON_CHITIN);
        Registry.register(BuiltInRegistries.ITEM, EGG_KEY, DRAGONMITE_EGG);
    }
}
