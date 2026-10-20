package com.simplemobheads.drop;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.config.MobHeadsConfig;
import com.simplemobheads.data.HeadDef;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.jspecify.annotations.Nullable;

/**
 * How heads drop (concept §3.3, owner 2026-09-29 and 13.10.):
 * <ul>
 *   <li>Only a charged creeper's explosion: one pool per head in {@code minecraft:charged_creeper} ("victim is the mob"),
 *   like Vanilla's creeper/zombie/skeleton/piglin heads - {@code Creeper#killedEntity} rolls the table once per explosion,
 *   so one head per explosion together with the Vanilla heads. Babies drop the normal head (the creeper's rule only asks
 *   the creeper). The head shows the victim's variant ({@link CopyVariantFunction}). Pools only for registered mobs.</li>
 *   <li>Optional player kills ({@code playerKillDrops}, default off): base chance + bonus per looting level.</li>
 *   <li>PvP: a player killed by another player drops their own head with their skin ({@code playerHeadDrops}).</li>
 * </ul>
 */
public final class HeadDrops {
    private HeadDrops() {
    }

    /** Adds the head pools to {@code charged_creeper} (and the Herobrine hint book to mineshaft chests); every loader calls this from its loot table event. */
    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(key) && SimpleMobHeads.config().hintBook) {
            // The Herobrine ritual's hint (concept §4, rule A6): a rare book in mineshaft chests.
            add.accept(com.simplemobheads.ritual.HerobrineRitual.hintBookPool());
            return;
        }
        if (!BuiltInLootTables.CHARGED_CREEPER.equals(key)) {
            return;
        }
        HolderGetter<EntityType<?>> types = registries.lookupOrThrow(Registries.ENTITY_TYPE);
        for (HeadDef head : MobHeads.heads()) {
            Optional<Holder.Reference<EntityType<?>>> mob = types.get(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(head.mob())));
            if (mob.isEmpty() || MobHeads.item(head.id()) == null) {
                continue;
            }
            add.accept(pool(types, head, mob.get().value()));
        }
    }

    public static LootPool.Builder pool(HolderGetter<EntityType<?>> types, HeadDef head, EntityType<?> mob) {
        LootItemFunction.Builder copy = CopyVariantFunction::new;
        LootItemCondition.Builder allowed = () -> new HeadDropCondition(head.id());
        return LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(MobHeads.item(head.id())).apply(copy))
                .when(allowed)
                .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(types, mob))));
    }

    private static Map<EntityType<?>, HeadDef> byMob;

    /** The head of a mob type, or null (Vanilla heads and excluded mobs have none). */
    public static @Nullable HeadDef headOf(EntityType<?> type) {
        if (byMob == null) {
            Map<EntityType<?>, HeadDef> map = new HashMap<>();
            for (HeadDef head : MobHeads.heads()) {
                BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(head.mob())).ifPresent(t -> map.putIfAbsent(t, head));
            }
            byMob = map;
        }
        return byMob.get(type);
    }

    /** Every loader's "living entity died" event: the PvP head and the optional player-kill head. */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !SimpleMobHeads.on()) {
            return;
        }
        MobHeadsConfig config = SimpleMobHeads.config();
        if (config.playerHeadDrops && entity instanceof ServerPlayer victim && source.getEntity() instanceof Player killer && killer != victim) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(victim.getGameProfile()));
            victim.spawnAtLocation(level, head);
            return;
        }
        if (config.playerKillDrops && !(entity instanceof Player) && source.getEntity() instanceof Player killer
                && level.getGameRules().get(GameRules.MOB_DROPS)) {
            HeadDef head = headOf(entity.getType());
            if (head == null || !HeadDropCondition.allowed(head)) {
                return;
            }
            int looting = EnchantmentHelper.getEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), killer);
            if (level.getRandom().nextFloat() < playerKillChance(config, looting)) {
                entity.spawnAtLocation(level, MobHeads.stack(head, Variants.of(head, entity)));
            }
        }
    }

    /** Chance of a head from a player kill: base chance plus the bonus per looting level (both bounded by the config). */
    public static float playerKillChance(MobHeadsConfig config, int looting) {
        return config.playerKillChance + Math.max(0, looting) * config.playerKillLootingBonus;
    }
}
