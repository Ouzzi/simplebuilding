package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import org.jspecify.annotations.Nullable;

/** Entity types, items, effect and potions of Simple Mobs. Fabric registers through {@link #registerAll()}, the other loaders through their register events. */
public final class MobsRegistry {
    public static final String ID = "simplemobs";

    public static final ResourceKey<EntityType<?>> DECEIVER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "deceiver"));
    public static final EntityType<DeceiverEntity> DECEIVER =
            EntityType.Builder.<DeceiverEntity>of(DeceiverEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.95f)
                    .eyeHeight(0.8f)
                    .clientTrackingRange(10)
                    .build(DECEIVER_KEY);

    public static final ResourceKey<EntityType<?>> HOWLER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "hollow_howler"));
    public static final EntityType<HowlerEntity> HOLLOW_HOWLER =
            EntityType.Builder.<HowlerEntity>of(HowlerEntity::new, MobCategory.MONSTER)
                    .sized(1.1f, 2.5f)
                    .eyeHeight(2.1f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build(HOWLER_KEY);

    public static final ResourceKey<EntityType<?>> RITUAL_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "howler_manifestation"));
    public static final EntityType<HowlerRitualEntity> HOWLER_RITUAL =
            EntityType.Builder.<HowlerRitualEntity>of(HowlerRitualEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .noSummon()
                    .clientTrackingRange(4)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(RITUAL_KEY);

    public static final ResourceKey<Item> CLOTH_KEY = itemKey("deceiver_cloth");
    public static final ResourceKey<Item> EGG_KEY = itemKey("deceiver_spawn_egg");
    public static final ResourceKey<Item> ECHO_CORE_KEY = itemKey("echo_core");
    public static final ResourceKey<Item> HOWLER_EGG_KEY = itemKey("hollow_howler_spawn_egg");
    public static final Item CLOTH = new Item(new Item.Properties().setId(CLOTH_KEY));
    public static final Item EGG = new SpawnEggItem(new Item.Properties().spawnEgg(DECEIVER).setId(EGG_KEY));
    public static final Item ECHO_CORE = new Item(new Item.Properties().rarity(Rarity.RARE).setId(ECHO_CORE_KEY));
    public static final Item HOWLER_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(HOLLOW_HOWLER).setId(HOWLER_EGG_KEY));

    public static final ResourceKey<MobEffect> SILENCE_KEY = ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ID, "silence"));
    public static final MobEffect SILENCE = new SilenceEffect();
    public static final ResourceKey<Potion> SILENCE_POTION_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(ID, "silence"));
    public static final ResourceKey<Potion> LONG_SILENCE_POTION_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(ID, "long_silence"));
    /** Potion of Silence 3:00 / 8:00. Built at registration, after the effect, because a potion holds the effect's holder. */
    private static @Nullable Potion silencePotion, longSilencePotion;
    private static @Nullable Holder<MobEffect> silenceHolder;

    private MobsRegistry() {}

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    /** The registered Silence effect (call after the effect registry was filled). */
    public static Holder<MobEffect> silence() {
        if (silenceHolder == null) silenceHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(SILENCE);
        return silenceHolder;
    }

    public static Potion silencePotion() {
        if (silencePotion == null) silencePotion = new Potion("silence", new MobEffectInstance(silence(), 3600));
        return silencePotion;
    }

    public static Potion longSilencePotion() {
        if (longSilencePotion == null) longSilencePotion = new Potion("silence", new MobEffectInstance(silence(), 9600));
        return longSilencePotion;
    }

    /** Super-mod switch "Enable Simple Mobs" (framework SubMods): off = mobs/items stay registered but inert (no natural spawns, ritual, silence, tab entries). */
    public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled(ID); }

    /** Whether a living entity carries Silence (used by the dampening mixin; false before registration). */
    public static boolean silenced(LivingEntity living) {
        return on() && silenceHolder != null && living.hasEffect(silenceHolder);
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        MobsSounds.registerAll();
        Registry.register(BuiltInRegistries.ENTITY_TYPE, DECEIVER_KEY, DECEIVER);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, HOWLER_KEY, HOLLOW_HOWLER);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, RITUAL_KEY, HOWLER_RITUAL);
        Registry.register(BuiltInRegistries.ITEM, CLOTH_KEY, CLOTH);
        Registry.register(BuiltInRegistries.ITEM, EGG_KEY, EGG);
        Registry.register(BuiltInRegistries.ITEM, ECHO_CORE_KEY, ECHO_CORE);
        Registry.register(BuiltInRegistries.ITEM, HOWLER_EGG_KEY, HOWLER_EGG);
        silenceHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, SILENCE_KEY, SILENCE);
        Registry.register(BuiltInRegistries.POTION, SILENCE_POTION_KEY, silencePotion());
        Registry.register(BuiltInRegistries.POTION, LONG_SILENCE_POTION_KEY, longSilencePotion());
    }

    /**
     * The module's own creative tab: guide, tools and gear, ingredients, then the spawn eggs. Items are looked up by id so
     * an item a config or companion mod leaves unregistered is simply skipped (the Vanilla tab hooks stay as they are).
     */
    public static void registerTab() {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(ID, "mobs"),
                net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                        .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplemobs.mobs"))
                        .icon(() -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ECHO_SHARD))
                        .displayItems((params, out) -> {
                            if (!on()) return;
                            out.accept(com.simplebuilding.modules.simplemobs.guide.MobsGuide.stack());
                            for (String path : TAB_ITEMS) {
                                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                                        net.minecraft.resources.Identifier.fromNamespaceAndPath(ID, path));
                                item.ifPresent(i -> out.accept(new net.minecraft.world.item.ItemStack(i)));
                            }
                        }).build());
    }

    /** Tab order: tools and gear, ingredients, spawn eggs. */
    public static final java.util.List<String> TAB_ITEMS = java.util.List.of(
            "biome_brush", "calm_amulet", "deceiver_cloak", "shellker_helmet", "soul_fire_charge",
            "deceiver_cloth", "dread_link", "echo_core", "illusion_shard", "shellker_shell", "soulfire_powder",
            "soulfire_rod", "watcher_eye", "wither_heart",
            "deceiver_spawn_egg", "dread_spawn_egg", "hollow_howler_spawn_egg", "hollow_watcher_spawn_egg",
            "shellker_spawn_egg", "soulfire_blaze_spawn_egg");
}
