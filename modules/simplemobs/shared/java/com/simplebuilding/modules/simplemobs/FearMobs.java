package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import org.jspecify.annotations.Nullable;

/**
 * Fear mobs (owner 12.10.): the Dread and the Hollow Watcher, their drops, the Calm Amulet, the Focus effect and potion,
 * and Shivering. Shivering belongs to Simple Building ({@code simplebuilding:shivering}); without it Simple Mobs
 * registers its own {@code simplemobs:shivering} so it stays playable alone - never both.
 */
public final class FearMobs {
    public static final String ID = MobsRegistry.ID;
    public static final Identifier SB_SHIVERING = Identifier.fromNamespaceAndPath("simplebuilding", "shivering");

    public static final ResourceKey<EntityType<?>> DREAD_KEY = entityKey("dread");
    public static final EntityType<DreadEntity> DREAD =
            EntityType.Builder.<DreadEntity>of(DreadEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 2.6f).eyeHeight(2.25f).clientTrackingRange(10).notInPeaceful()
                    .build(DREAD_KEY);
    public static final ResourceKey<EntityType<?>> WATCHER_KEY = entityKey("hollow_watcher");
    public static final EntityType<HollowWatcherEntity> WATCHER =
            EntityType.Builder.<HollowWatcherEntity>of(HollowWatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 4.0f).eyeHeight(3.55f).clientTrackingRange(12).notInPeaceful()
                    .build(WATCHER_KEY);

    public static final ResourceKey<Item> LINK_KEY = itemKey("dread_link");
    public static final ResourceKey<Item> AMULET_KEY = itemKey("calm_amulet");
    public static final ResourceKey<Item> EYE_KEY = itemKey("watcher_eye");
    public static final ResourceKey<Item> DREAD_EGG_KEY = itemKey("dread_spawn_egg");
    public static final ResourceKey<Item> WATCHER_EGG_KEY = itemKey("hollow_watcher_spawn_egg");
    public static final Item LINK = new Item(new Item.Properties().rarity(Rarity.UNCOMMON).setId(LINK_KEY));
    public static final Item AMULET = new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).setId(AMULET_KEY));
    public static final Item EYE = new Item(new Item.Properties().rarity(Rarity.UNCOMMON).setId(EYE_KEY));
    public static final Item DREAD_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(DREAD).setId(DREAD_EGG_KEY));
    public static final Item WATCHER_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(WATCHER).setId(WATCHER_EGG_KEY));

    public static final ResourceKey<MobEffect> SHIVERING_KEY = ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ID, "shivering"));
    public static final ResourceKey<MobEffect> FOCUS_KEY = ResourceKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(ID, "focus"));
    public static final MobEffect SHIVERING = new FearEffect(MobEffectCategory.HARMFUL, 0x9FD3EA);
    public static final MobEffect FOCUS = new FearEffect(MobEffectCategory.BENEFICIAL, 0xC9A2F2);
    public static final ResourceKey<Potion> FOCUS_POTION_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(ID, "focus"));
    public static final ResourceKey<Potion> LONG_FOCUS_POTION_KEY = ResourceKey.create(Registries.POTION, Identifier.fromNamespaceAndPath(ID, "long_focus"));
    private static @Nullable Potion focusPotion, longFocusPotion;
    private static @Nullable Holder<MobEffect> focusHolder, shiveringHolder;

    /** Set by the loader before registration: Simple Building owns Shivering when it is present. */
    public static boolean simplebuildingPresent;

    /** Plain effect; Shivering and Focus act through the aura, the immunity check and the client sway. */
    static final class FearEffect extends MobEffect {
        FearEffect(MobEffectCategory category, int color) { super(category, color); }
    }

    private FearMobs() {}

    private static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, path));
    }

    public static Holder<MobEffect> focus() {
        if (focusHolder == null) focusHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FOCUS);
        return focusHolder;
    }

    /** Shivering: Simple Building's if loaded, else this module's own. Null only before registration. */
    public static @Nullable Holder<MobEffect> shivering() {
        if (shiveringHolder == null) {
            shiveringHolder = BuiltInRegistries.MOB_EFFECT.get(SB_SHIVERING).map(h -> (Holder<MobEffect>) h)
                    .or(() -> BuiltInRegistries.MOB_EFFECT.get(SHIVERING_KEY).map(h -> (Holder<MobEffect>) h)).orElse(null);
        }
        return shiveringHolder;
    }

    public static Potion focusPotion() {
        if (focusPotion == null) focusPotion = new Potion("focus", new MobEffectInstance(focus(), FearLogic.FOCUS_TICKS));
        return focusPotion;
    }

    public static Potion longFocusPotion() {
        if (longFocusPotion == null) longFocusPotion = new Potion("focus", new MobEffectInstance(focus(), FearLogic.LONG_FOCUS_TICKS));
        return longFocusPotion;
    }

    /** Immune to Shivering: Focus, or a Calm Amulet anywhere in the inventory. */
    public static boolean immune(LivingEntity living) {
        if (living.hasEffect(focus())) return true;
        return living instanceof Player p && p.getInventory().contains(s -> s.is(AMULET));
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(DREAD_KEY, DREAD);
        sink.accept(WATCHER_KEY, WATCHER);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(LINK_KEY, LINK);
        sink.accept(AMULET_KEY, AMULET);
        sink.accept(EYE_KEY, EYE);
        sink.accept(DREAD_EGG_KEY, DREAD_EGG);
        sink.accept(WATCHER_EGG_KEY, WATCHER_EGG);
    }

    public static void effects(BiConsumer<ResourceKey<MobEffect>, MobEffect> sink) {
        if (!simplebuildingPresent) sink.accept(SHIVERING_KEY, SHIVERING);
        sink.accept(FOCUS_KEY, FOCUS);
    }

    public static void potions(BiConsumer<ResourceKey<Potion>, Potion> sink) {
        sink.accept(FOCUS_POTION_KEY, focusPotion());
        sink.accept(LONG_FOCUS_POTION_KEY, longFocusPotion());
    }

    /** Fabric: direct registration. */
    public static void registerAll() {
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
        effects((k, v) -> Registry.register(BuiltInRegistries.MOB_EFFECT, k, v));
        potions((k, v) -> Registry.register(BuiltInRegistries.POTION, k, v));
    }
}
