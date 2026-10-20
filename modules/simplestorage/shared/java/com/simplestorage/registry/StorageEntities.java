package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.vehicle.TieredChestBoat;
import com.simplestorage.vehicle.TieredChestMinecart;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity types (split stage 8b-2): the chest carts ({@code <tier>_chest_minecart}) and chest boats ({@code <tier>_chest_boat})
 * moved from SimpleBuilding (id moves), sizes as Vanilla's chest cart and chest boat. Registered in the ENTITY_TYPE phase
 * (Fabric directly, after the items).
 */
public final class StorageEntities {
    public static final Map<ChestTier, EntityType<TieredChestMinecart>> CHEST_MINECARTS = new EnumMap<>(ChestTier.class);
    public static final Map<ChestTier, EntityType<TieredChestBoat>> CHEST_BOATS = new EnumMap<>(ChestTier.class);

    public static void register() {
        for (ChestTier tier : ChestTier.values()) {
            ResourceKey<EntityType<?>> cart = key(tier.textureName() + "_chest_minecart");
            CHEST_MINECARTS.put(tier, Registry.register(BuiltInRegistries.ENTITY_TYPE, cart, EntityType.Builder
                    .<TieredChestMinecart>of((t, level) -> new TieredChestMinecart(t, level, tier), MobCategory.MISC)
                    .noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8).build(cart)));
            ResourceKey<EntityType<?>> boat = key(tier.textureName() + "_chest_boat");
            CHEST_BOATS.put(tier, Registry.register(BuiltInRegistries.ENTITY_TYPE, boat, EntityType.Builder
                    .<TieredChestBoat>of((t, level) -> new TieredChestBoat(t, level, tier), MobCategory.MISC)
                    .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10).build(boat)));
        }
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, SimpleStorage.id(path));
    }

    private StorageEntities() {}
}
