package com.simplewanderers;

import com.simplewanderers.entity.VillageWanderer;
import com.simplewanderers.entity.WandererHorse;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Everything Simple Wanderers registers: two entity types, the wanderer's spawn egg and the guide book. Loaders call
 * {@link #entityTypes} / {@link #items} inside their matching registry event (Fabric: {@link #registerAll}).
 */
public final class WandererRegistry {
    public static final ResourceKey<EntityType<?>> WANDERER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleWanderers.id("village_wanderer"));
    public static final ResourceKey<EntityType<?>> HORSE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleWanderers.id("wanderer_horse"));
    public static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, SimpleWanderers.id("village_wanderer_spawn_egg"));

    /** Same size, eye height and tracking range as the Wandering Trader. */
    public static final EntityType<VillageWanderer> WANDERER = EntityType.Builder.<VillageWanderer>of(VillageWanderer::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build(WANDERER_KEY);
    /** Same size as the Vanilla Horse. */
    public static final EntityType<WandererHorse> HORSE = EntityType.Builder.<WandererHorse>of(WandererHorse::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.44375F).clientTrackingRange(10).build(HORSE_KEY);
    public static final Item WANDERER_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(WANDERER).setId(EGG_KEY));

    public static AttributeSupplier.Builder wandererAttributes() {
        return Mob.createMobAttributes();
    }

    public static AttributeSupplier.Builder horseAttributes() {
        return AbstractHorse.createBaseHorseAttributes();
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(WANDERER_KEY, WANDERER);
        sink.accept(HORSE_KEY, HORSE);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(EGG_KEY, WANDERER_EGG);
        com.simplewanderers.guide.WanderersGuide.register();
    }

    /** Fabric: registries are open during init. */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }

    public static List<ItemStack> eggStacks() {
        return List.of(new ItemStack(WANDERER_EGG));
    }

    private WandererRegistry() {}
}
