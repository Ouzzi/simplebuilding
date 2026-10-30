package com.simplebuilding.modules.simplemobs.trials;

import com.simplebuilding.modules.simplemobs.MobsConfig;
import com.simplebuilding.modules.simplemobs.MobsRegistry;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Weathered Trials, Evil Omen and the Void chain of Simple Mobs (BS27-2/3/5/6, concept docs/ai/KONZEPT-A-WP20-2026-10-29.md):
 * the blocks (Weathered Trial Spawner, Weathered / Evil / Void Vault), the ten keys, the ten potions and the two omen effects.
 * Blocks and items are static (loader sinks, like {@code StrongExplosives}); the effects register through {@link #registerEffects()}.
 *
 * <p>The chain: Ominous Bottle + Nether Heart in a soul-fuelled Brewing Stand = Evil Potion I-V (same stage as the bottle) -&gt; Evil Omen
 * -&gt; walking into a Weathered Trial makes it an Evil Trial (more life, effects, the Evil Trial Key of that stage) -&gt; Evil Vault
 * (a Weathered Vault, a diamond block in the off hand, 5 hammer / axe strikes) opens with the key. The Void chain is the same one tier up
 * (Ender Heart, netherite block) and is off by default ({@link MobsConfig#voidChain}) until the Ender Heart has a source.
 */
public final class Trials {
    public static final String ID = MobsRegistry.ID;
    public static final int STAGES = 5;
    /** Evil / Void Omen last as long as Bad Omen (100 minutes). */
    public static final int OMEN_TICKS = 120000;

    // --- blocks
    public static final ResourceKey<Block> SPAWNER_BLOCK_KEY = blockKey("weathered_trial_spawner");
    public static final ResourceKey<Block> WEATHERED_VAULT_KEY = blockKey("weathered_vault");
    public static final ResourceKey<Block> EVIL_VAULT_KEY = blockKey("evil_vault");
    public static final ResourceKey<Block> VOID_VAULT_KEY = blockKey("void_vault");
    public static final Block WEATHERED_TRIAL_SPAWNER = new WeatheredTrialSpawnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TRIAL_SPAWNER).setId(SPAWNER_BLOCK_KEY));
    public static final Block WEATHERED_VAULT = new TrialVaultBlock(TrialKind.WEATHERED, BlockBehaviour.Properties.ofFullCopy(Blocks.VAULT).setId(WEATHERED_VAULT_KEY));
    public static final Block EVIL_VAULT = new TrialVaultBlock(TrialKind.EVIL, BlockBehaviour.Properties.ofFullCopy(Blocks.VAULT).setId(EVIL_VAULT_KEY));
    public static final Block VOID_VAULT = new TrialVaultBlock(TrialKind.VOID, BlockBehaviour.Properties.ofFullCopy(Blocks.VAULT).setId(VOID_VAULT_KEY));

    // --- items
    public static final ResourceKey<Item> SPAWNER_ITEM_KEY = itemKey("weathered_trial_spawner");
    public static final ResourceKey<Item> WEATHERED_VAULT_ITEM_KEY = itemKey("weathered_vault");
    public static final ResourceKey<Item> EVIL_VAULT_ITEM_KEY = itemKey("evil_vault");
    public static final ResourceKey<Item> VOID_VAULT_ITEM_KEY = itemKey("void_vault");
    public static final Item WEATHERED_TRIAL_SPAWNER_ITEM = blockItem(WEATHERED_TRIAL_SPAWNER, SPAWNER_ITEM_KEY, Rarity.EPIC);
    public static final Item WEATHERED_VAULT_ITEM = blockItem(WEATHERED_VAULT, WEATHERED_VAULT_ITEM_KEY, Rarity.EPIC);
    public static final Item EVIL_VAULT_ITEM = blockItem(EVIL_VAULT, EVIL_VAULT_ITEM_KEY, Rarity.EPIC);
    public static final Item VOID_VAULT_ITEM = blockItem(VOID_VAULT, VOID_VAULT_ITEM_KEY, Rarity.EPIC);
    public static final Item[] EVIL_KEYS = new Item[STAGES], VOID_KEYS = new Item[STAGES];
    public static final Item[] EVIL_POTIONS = new Item[STAGES], VOID_POTIONS = new Item[STAGES];

    static {
        for (int i = 0; i < STAGES; i++) {
            EVIL_KEYS[i] = key(TrialKind.EVIL, i + 1);
            VOID_KEYS[i] = key(TrialKind.VOID, i + 1);
            EVIL_POTIONS[i] = potion(TrialKind.EVIL, i + 1);
            VOID_POTIONS[i] = potion(TrialKind.VOID, i + 1);
        }
    }

    // --- effects (registered with the rest; holders filled by registerEffects)
    public static final ResourceKey<MobEffect> EVIL_OMEN_KEY = ResourceKey.create(Registries.MOB_EFFECT, id("evil_omen"));
    public static final ResourceKey<MobEffect> VOID_OMEN_KEY = ResourceKey.create(Registries.MOB_EFFECT, id("void_omen"));
    public static final MobEffect EVIL_OMEN = new OmenEffect(0xB02A24);
    public static final MobEffect VOID_OMEN = new OmenEffect(0x6A3FC8);
    private static @Nullable Holder<MobEffect> evilOmenHolder, voidOmenHolder;

    private Trials() {}

    private static final class OmenEffect extends MobEffect {
        OmenEffect(int color) {
            super(MobEffectCategory.NEUTRAL, color);
        }
    }

    /** An omen potion: drinking replaces Bad Omen (and the other tier) by the Evil / Void Omen of its stage. */
    private static final class OmenPotionItem extends Item {
        private final TrialKind kind;
        private final int stage;

        OmenPotionItem(TrialKind kind, int stage, Properties properties) {
            super(properties);
            this.kind = kind;
            this.stage = stage;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide()) applyOmen(entity, kind, stage);
            return super.finishUsingItem(stack, level, entity);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    private static Item blockItem(Block block, ResourceKey<Item> key, Rarity rarity) {
        return new BlockItem(block, new Item.Properties().rarity(rarity).useBlockDescriptionPrefix().setId(key));
    }

    public static ResourceKey<Item> keyKey(TrialKind kind, int stage) {
        return itemKey(kind.id() + "_trial_key_" + stage);
    }

    public static ResourceKey<Item> potionKey(TrialKind kind, int stage) {
        return itemKey(kind.id() + "_potion_" + stage);
    }

    private static Item key(TrialKind kind, int stage) {
        return new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).setId(keyKey(kind, stage)));
    }

    private static Item potion(TrialKind kind, int stage) {
        Consumable drink = Consumable.builder().animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false).build();
        return new OmenPotionItem(kind, stage, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
                .component(DataComponents.CONSUMABLE, drink).setId(potionKey(kind, stage)));
    }

    // --- registration
    public static void blocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        sink.accept(SPAWNER_BLOCK_KEY, WEATHERED_TRIAL_SPAWNER);
        sink.accept(WEATHERED_VAULT_KEY, WEATHERED_VAULT);
        sink.accept(EVIL_VAULT_KEY, EVIL_VAULT);
        sink.accept(VOID_VAULT_KEY, VOID_VAULT);
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(SPAWNER_ITEM_KEY, WEATHERED_TRIAL_SPAWNER_ITEM);
        sink.accept(WEATHERED_VAULT_ITEM_KEY, WEATHERED_VAULT_ITEM);
        sink.accept(EVIL_VAULT_ITEM_KEY, EVIL_VAULT_ITEM);
        sink.accept(VOID_VAULT_ITEM_KEY, VOID_VAULT_ITEM);
        for (int i = 0; i < STAGES; i++) {
            sink.accept(keyKey(TrialKind.EVIL, i + 1), EVIL_KEYS[i]);
            sink.accept(keyKey(TrialKind.VOID, i + 1), VOID_KEYS[i]);
            sink.accept(potionKey(TrialKind.EVIL, i + 1), EVIL_POTIONS[i]);
            sink.accept(potionKey(TrialKind.VOID, i + 1), VOID_POTIONS[i]);
        }
    }

    /** Fabric: the effects. Other loaders call this from their effect register event. */
    public static void registerEffects() {
        evilOmenHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, EVIL_OMEN_KEY, EVIL_OMEN);
        voidOmenHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, VOID_OMEN_KEY, VOID_OMEN);
    }

    public static Holder<MobEffect> evilOmen() {
        if (evilOmenHolder == null) evilOmenHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EVIL_OMEN);
        return evilOmenHolder;
    }

    public static Holder<MobEffect> voidOmen() {
        if (voidOmenHolder == null) voidOmenHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VOID_OMEN);
        return voidOmenHolder;
    }

    public static Holder<MobEffect> omen(TrialKind kind) {
        return kind == TrialKind.VOID ? voidOmen() : evilOmen();
    }

    // --- switches
    public static boolean evilOn() {
        return MobsRegistry.on() && MobsConfig.evilOmen;
    }

    public static boolean voidOn() {
        return evilOn() && MobsConfig.voidChain;
    }

    public static boolean chainOn(TrialKind kind) {
        return kind == TrialKind.VOID ? voidOn() : kind == TrialKind.EVIL ? evilOn() : MobsRegistry.on();
    }

    // --- omens
    /** Drinks an omen potion: Bad Omen, Raid Omen and the other tier go, the Evil / Void Omen of that stage (amplifier = stage - 1) comes. */
    public static void applyOmen(LivingEntity entity, TrialKind kind, int stage) {
        entity.removeEffect(MobEffects.BAD_OMEN);
        entity.removeEffect(omen(kind == TrialKind.EVIL ? TrialKind.VOID : TrialKind.EVIL));
        entity.addEffect(new MobEffectInstance(omen(kind), OMEN_TICKS, Math.max(0, Math.min(STAGES - 1, stage - 1)), false, true, true));
    }

    /** The Evil / Void Omen stage (1-5) of an entity, 0 for none. Void wins; it counts only while its chain is on. */
    public static int omenStage(LivingEntity entity, TrialKind kind) {
        MobEffectInstance e = entity.getEffect(omen(kind));
        return e == null || !chainOn(kind) ? 0 : e.getAmplifier() + 1;
    }

    // --- keys
    /** Evil / Void for one of the ten keys, {@code null} for everything else. */
    public static @Nullable TrialKind keyKind(ItemStack stack) {
        for (int i = 0; i < STAGES; i++) {
            if (stack.is(EVIL_KEYS[i])) return TrialKind.EVIL;
            if (stack.is(VOID_KEYS[i])) return TrialKind.VOID;
        }
        return null;
    }

    public static int keyStage(ItemStack stack) {
        for (int i = 0; i < STAGES; i++) {
            if (stack.is(EVIL_KEYS[i]) || stack.is(VOID_KEYS[i])) return i + 1;
        }
        return 0;
    }

    /** The two Vanilla keys and our ten. */
    public static boolean isAnyKey(ItemStack stack) {
        return stack.is(Items.TRIAL_KEY) || stack.is(Items.OMINOUS_TRIAL_KEY) || keyKind(stack) != null;
    }

    public static ResourceKey<LootTable> vaultLoot(TrialKind kind, int stage, boolean ominous) {
        String path = kind == TrialKind.WEATHERED ? (ominous ? "vault/weathered_ominous" : "vault/weathered") : "vault/" + kind.id() + "_" + Math.max(1, Math.min(STAGES, stage));
        return ResourceKey.create(Registries.LOOT_TABLE, id(path));
    }

    public static ResourceKey<LootTable> spawnerLoot(TrialKind kind, int stage) {
        return ResourceKey.create(Registries.LOOT_TABLE, id("spawners/" + kind.id() + "_" + Math.max(1, Math.min(STAGES, stage))));
    }

    /** The Looter Chest of an Evil / Void Raid (TR31-R). */
    public static ResourceKey<LootTable> raidLoot(TrialKind kind, int stage) {
        return ResourceKey.create(Registries.LOOT_TABLE, id("raid/" + kind.id() + "_" + Math.max(1, Math.min(STAGES, stage))));
    }

    /** The block entity config of a vault tier: key item, loot table and the Vanilla ranges (4 / 4.5 blocks). */
    public static VaultConfig vaultConfig(TrialKind kind, boolean ominous) {
        ItemStack key = switch (kind) {
            case WEATHERED -> new ItemStack(ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY);
            case EVIL -> new ItemStack(EVIL_KEYS[0]);
            case VOID -> new ItemStack(VOID_KEYS[0]);
        };
        return new VaultConfig(vaultLoot(kind, 1, ominous), 4.0, 4.5, key, Optional.empty());
    }
}
