package com.simpleanimals;

import com.simpleanimals.entity.Boar;
import com.simpleanimals.entity.Crab;
import com.simpleanimals.entity.Deer;
import com.simpleanimals.entity.Hedgehog;
import com.simpleanimals.entity.Zebra;
import com.simpleanimals.entity.Zorse;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.food.Foods;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Everything Simple Animals registers: six entity types (the zorse has no spawn egg and no natural spawn), five spawn
 * eggs, raw and cooked crab, truffle and truffle soup and the guide book. Loaders call {@link #entityTypes} / {@link #items} inside their matching
 * registry event (Fabric: {@link #registerAll}).
 */
public final class AnimalsRegistry {
    public static final ResourceKey<EntityType<?>> DEER_KEY = entityKey("deer");
    public static final ResourceKey<EntityType<?>> BOAR_KEY = entityKey("boar");
    public static final ResourceKey<EntityType<?>> HEDGEHOG_KEY = entityKey("hedgehog");
    public static final ResourceKey<EntityType<?>> CRAB_KEY = entityKey("crab");
    public static final ResourceKey<EntityType<?>> ZEBRA_KEY = entityKey("zebra");
    public static final ResourceKey<EntityType<?>> ZORSE_KEY = entityKey("zorse");

    /** Cow-sized, with room for the antlers; eye height at the neck. */
    public static final EntityType<Deer> DEER = EntityType.Builder.<Deer>of(Deer::new, MobCategory.CREATURE)
            .sized(0.9F, 1.5F).eyeHeight(1.35F).clientTrackingRange(10).build(DEER_KEY);
    /** A little smaller than a Vanilla cow, stocky. */
    public static final EntityType<Boar> BOAR = EntityType.Builder.<Boar>of(Boar::new, MobCategory.CREATURE)
            .sized(0.9F, 0.9F).eyeHeight(0.7F).clientTrackingRange(10).build(BOAR_KEY);
    public static final EntityType<Hedgehog> HEDGEHOG = EntityType.Builder.<Hedgehog>of(Hedgehog::new, MobCategory.CREATURE)
            .sized(0.5F, 0.4F).eyeHeight(0.25F).clientTrackingRange(8).build(HEDGEHOG_KEY);
    public static final EntityType<Crab> CRAB = EntityType.Builder.<Crab>of(Crab::new, MobCategory.CREATURE)
            .sized(0.65F, 0.35F).eyeHeight(0.25F).clientTrackingRange(8).build(CRAB_KEY);
    /** Same size, eye height and passenger point as the Vanilla Horse. */
    public static final EntityType<Zebra> ZEBRA = EntityType.Builder.<Zebra>of(Zebra::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.44375F).clientTrackingRange(10).build(ZEBRA_KEY);
    public static final EntityType<Zorse> ZORSE = EntityType.Builder.<Zorse>of(Zorse::new, MobCategory.CREATURE)
            .sized(1.3964844F, 1.6F).eyeHeight(1.52F).passengerAttachments(1.44375F).clientTrackingRange(10).build(ZORSE_KEY);

    public static final ResourceKey<Item> DEER_EGG_KEY = itemKey("deer_spawn_egg");
    public static final ResourceKey<Item> BOAR_EGG_KEY = itemKey("boar_spawn_egg");
    public static final ResourceKey<Item> HEDGEHOG_EGG_KEY = itemKey("hedgehog_spawn_egg");
    public static final ResourceKey<Item> CRAB_EGG_KEY = itemKey("crab_spawn_egg");
    public static final ResourceKey<Item> ZEBRA_EGG_KEY = itemKey("zebra_spawn_egg");
    public static final ResourceKey<Item> RAW_CRAB_KEY = itemKey("raw_crab");
    public static final ResourceKey<Item> COOKED_CRAB_KEY = itemKey("cooked_crab");

    public static final ResourceKey<Item> TRUFFLE_KEY = itemKey("truffle");
    public static final ResourceKey<Item> TRUFFLE_SOUP_KEY = itemKey("truffle_soup");

    public static final Item DEER_EGG = egg(DEER, DEER_EGG_KEY);
    public static final Item BOAR_EGG = egg(BOAR, BOAR_EGG_KEY);
    public static final Item HEDGEHOG_EGG = egg(HEDGEHOG, HEDGEHOG_EGG_KEY);
    public static final Item CRAB_EGG = egg(CRAB, CRAB_EGG_KEY);
    public static final Item ZEBRA_EGG = egg(ZEBRA, ZEBRA_EGG_KEY);
    /** Eats like Vanilla raw cod; cooks to the value of cooked cod, so crabs add no new food level (A12). */
    public static final Item RAW_CRAB = new Item(new Item.Properties().food(Foods.COD).setId(RAW_CRAB_KEY));
    public static final Item COOKED_CRAB = new Item(new Item.Properties().food(Foods.COOKED_COD).setId(COOKED_CRAB_KEY));

    /** 20 seconds. */
    public static final int TRUFFLE_SOUP_NIGHT_VISION_TICKS = 400;
    /** Only wild boars root it up (IG31-20); not edible raw, like the Vanilla mushrooms. */
    public static final Item TRUFFLE = new Item(new Item.Properties().setId(TRUFFLE_KEY));
    /** Eats like Vanilla mushroom stew (same food value, A12) plus a short, light Night Vision (underground theme); gives the bowl back. */
    public static final Item TRUFFLE_SOUP = new Item(new Item.Properties().stacksTo(1).food(Foods.MUSHROOM_STEW, Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, TRUFFLE_SOUP_NIGHT_VISION_TICKS, 0))).build())
            .usingConvertsTo(Items.BOWL).setId(TRUFFLE_SOUP_KEY));

    private static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, SimpleAnimals.id(path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, SimpleAnimals.id(path));
    }

    private static Item egg(EntityType<?> type, ResourceKey<Item> key) {
        return new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key));
    }

    public static AttributeSupplier.Builder deerAttributes() {
        return Deer.createAttributes();
    }

    public static AttributeSupplier.Builder boarAttributes() {
        return Boar.createAttributes();
    }

    public static AttributeSupplier.Builder hedgehogAttributes() {
        return Hedgehog.createAttributes();
    }

    public static AttributeSupplier.Builder crabAttributes() {
        return Crab.createAttributes();
    }

    public static AttributeSupplier.Builder horseAttributes() {
        return net.minecraft.world.entity.animal.equine.AbstractHorse.createBaseHorseAttributes();
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(DEER_KEY, DEER);
        sink.accept(BOAR_KEY, BOAR);
        sink.accept(HEDGEHOG_KEY, HEDGEHOG);
        sink.accept(CRAB_KEY, CRAB);
        sink.accept(ZEBRA_KEY, ZEBRA);
        sink.accept(ZORSE_KEY, ZORSE);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(DEER_EGG_KEY, DEER_EGG);
        sink.accept(BOAR_EGG_KEY, BOAR_EGG);
        sink.accept(HEDGEHOG_EGG_KEY, HEDGEHOG_EGG);
        sink.accept(CRAB_EGG_KEY, CRAB_EGG);
        sink.accept(ZEBRA_EGG_KEY, ZEBRA_EGG);
        sink.accept(RAW_CRAB_KEY, RAW_CRAB);
        sink.accept(COOKED_CRAB_KEY, COOKED_CRAB);
        sink.accept(TRUFFLE_KEY, TRUFFLE);
        sink.accept(TRUFFLE_SOUP_KEY, TRUFFLE_SOUP);
        com.simpleanimals.guide.AnimalsGuide.register();
    }

    /** Fabric: registries are open during init. */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }

    public static List<ItemStack> eggStacks() {
        return List.of(new ItemStack(DEER_EGG), new ItemStack(BOAR_EGG), new ItemStack(HEDGEHOG_EGG), new ItemStack(CRAB_EGG),
                new ItemStack(ZEBRA_EGG));
    }

    private AnimalsRegistry() {}
}
