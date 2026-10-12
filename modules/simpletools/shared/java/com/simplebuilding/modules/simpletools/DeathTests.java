package com.simplebuilding.modules.simpletools;

import com.simplebuilding.modules.simpletools.mixin.ItemEntityAgeAccessor;
import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import com.simplebuilding.modules.simpletools.vault.DeathLedger;
import com.simplebuilding.modules.simpletools.vault.DeathVault;
import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Soulbound and Death Vault (owner 2026-10-12); bodies registered in {@link ToolsTests#ALL}. */
public final class DeathTests {
    private DeathTests() {}

    private static Holder<Enchantment> soulbound(GameTestHelper h) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Soulbound.KEY);
    }

    private static ItemStack bound(GameTestHelper h, ItemStack stack) {
        stack.enchant(soulbound(h), 1);
        return stack;
    }

    private static ItemStack book(GameTestHelper h) {
        return Soulbound.book(h.getLevel().registryAccess());
    }

    private static void configured(Runnable body) {
        var old = SimpleTools.CONFIG;
        try {
            SimpleTools.CONFIG = new ToolsConfig();
            body.run();
        } finally {
            SimpleTools.CONFIG = old;
        }
    }

    private static int left(ItemStack s) {
        return s.getMaxDamage() - s.getDamageValue();
    }

    private static ServerPlayer living(GameTestHelper h, BlockPos rel) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(Vec3.atBottomCenterOf(rel));
        p.snapTo(pos.x, pos.y, pos.z, 0, 0);
        return p;
    }

    private static List<ItemEntity> itemsNear(GameTestHelper h, ServerPlayer p) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(4));
    }

    private static ItemEntity spawn(GameTestHelper h, ItemStack stack, BlockPos rel) {
        Vec3 pos = h.absoluteVec(Vec3.atBottomCenterOf(rel));
        ItemEntity e = new ItemEntity(h.getLevel(), pos.x, pos.y, pos.z, stack);
        e.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(e);
        return e;
    }

    private static DeathLedger ledger(GameTestHelper h) {
        return DeathLedger.get(h.getLevel().getServer());
    }

    private static int lost(GameTestHelper h, UUID owner) {
        return DeathVault.lost(ledger(h), owner).size();
    }

    private static int lying(GameTestHelper h, UUID owner) {
        return DeathVault.lying(ledger(h), owner).size();
    }

    /** Registered, treasure only, book only, exclusive with Curse of Vanishing, one level. */
    public static void soulboundRegistered(GameTestHelper h) {
        var reg = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> s = soulbound(h);
        h.assertTrue(s.value().getMaxLevel() == 1, "one level");
        h.assertTrue(s.is(EnchantmentTags.TREASURE), "treasure enchantment");
        h.assertTrue(!s.is(EnchantmentTags.IN_ENCHANTING_TABLE) && !s.is(EnchantmentTags.NON_TREASURE), "never from the enchanting table");
        h.assertTrue(!s.is(EnchantmentTags.TRADEABLE) && !s.is(EnchantmentTags.ON_RANDOM_LOOT) && !s.is(EnchantmentTags.ON_TRADED_EQUIPMENT),
                "no librarian trade, no random loot, no traded gear");
        h.assertTrue(!Enchantment.areCompatible(s, reg.getOrThrow(Enchantments.VANISHING_CURSE)), "exclusive with Curse of Vanishing");
        h.assertTrue(Enchantment.areCompatible(s, reg.getOrThrow(Enchantments.MENDING)) && Enchantment.areCompatible(s, reg.getOrThrow(Enchantments.BINDING_CURSE)),
                "goes with Mending and Curse of Binding");
        h.assertTrue(s.value().getWeight() < reg.getOrThrow(Enchantments.MENDING).value().getWeight(), "rarer than Mending");
        h.assertTrue(s.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD)) && s.value().canEnchant(new ItemStack(Items.ELYTRA)), "tools, weapons, elytra");
        h.succeed();
    }

    /** A real death: the Soulbound sword never becomes an item entity, stays in slot 3 and comes back there with 50 durability. */
    public static void soulboundDeath(GameTestHelper h) {
        configured(() -> {
            ServerPlayer p = living(h, new BlockPos(2, 2, 2));
            try {
                ItemStack sword = bound(h, new ItemStack(Items.DIAMOND_SWORD));
                ItemStack helmet = bound(h, new ItemStack(Items.IRON_HELMET));
                helmet.setDamageValue(helmet.getMaxDamage() - 20);
                p.getInventory().setItem(3, sword);
                p.getInventory().setItem(5, new ItemStack(Items.DIRT, 10));
                p.setItemSlot(EquipmentSlot.HEAD, helmet);
                int helmetSlot = -1;
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(Items.IRON_HELMET)) helmetSlot = i;
                h.assertTrue(helmetSlot >= 0, "helmet sits in an inventory slot");
                p.kill(h.getLevel());
                h.assertTrue(p.isDeadOrDying(), "the player died");
                List<ItemEntity> drops = itemsNear(h, p);
                h.assertTrue(drops.stream().noneMatch(e -> Soulbound.has(e.getItem())), "no Soulbound item entity exists after the death");
                h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.DIRT)), "the plain dirt dropped");
                h.assertTrue(p.getInventory().getItem(3).is(Items.DIAMOND_SWORD) && p.getInventory().getItem(5).isEmpty(),
                        "the dead player still holds the sword in slot 3, the dirt is gone");
                ServerPlayer fresh = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
                fresh.restoreFrom(p, false);
                ItemStack back = fresh.getInventory().getItem(3);
                h.assertTrue(back.is(Items.DIAMOND_SWORD) && Soulbound.has(back), "sword back in slot 3 with Soulbound: " + back);
                h.assertValueEqual(left(back), 50, "durability after death is min(1561, 50)");
                ItemStack head = fresh.getInventory().getItem(helmetSlot);
                h.assertTrue(head.is(Items.IRON_HELMET), "helmet back in its slot");
                h.assertValueEqual(left(head), 20, "durability below 50 stays");
                h.assertTrue(p.getInventory().getItem(3).isEmpty() && p.getInventory().getItem(helmetSlot).isEmpty(), "the dead player keeps no copy");
                ServerPlayer again = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
                again.restoreFrom(p, false);
                h.assertTrue(again.getInventory().getItem(3).isEmpty(), "a second respawn from the same death gives nothing (no dupe)");
                // Death Vault recorded the dirt, never the Soulbound stacks.
                List<ItemStack> recorded = DeathVault.lying(ledger(h), p.getUUID());
                h.assertTrue(recorded.size() == 1 && recorded.getFirst().is(Items.DIRT), "the vault recorded only the dirt: " + recorded);
                UUID dirt = drops.stream().filter(e -> e.getItem().is(Items.DIRT)).findFirst().orElseThrow().getUUID();
                h.assertTrue(ledger(h).tracked(dirt), "recorded with the UUID of the dropped entity");
            } finally {
                ledger(h).clear(p.getUUID());
                h.getLevel().getServer().getPlayerList().remove(p);
            }
        });
        h.succeed();
    }

    /** Switched off: the Soulbound item drops like any other; End return keeps the durability untouched. */
    public static void soulboundSwitch(GameTestHelper h) {
        configured(() -> {
            ServerPlayer p = living(h, new BlockPos(2, 2, 2));
            try {
                SimpleTools.config().enableSoulbound = false;
                p.getInventory().setItem(0, bound(h, new ItemStack(Items.IRON_SWORD)));
                p.kill(h.getLevel());
                h.assertTrue(itemsNear(h, p).stream().anyMatch(e -> Soulbound.has(e.getItem())), "switched off: the sword dropped");
                h.assertTrue(p.getInventory().getItem(0).isEmpty(), "nothing kept");
            } finally {
                ledger(h).clear(p.getUUID());
                h.getLevel().getServer().getPlayerList().remove(p);
            }
            ServerPlayer traveller = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
            traveller.getInventory().setItem(2, bound(h, new ItemStack(Items.DIAMOND_PICKAXE)));
            ServerPlayer arrived = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
            arrived.restoreFrom(traveller, true);
            h.assertValueEqual(left(arrived.getInventory().getItem(2)), Items.DIAMOND_PICKAXE.getDefaultInstance().getMaxDamage(), "End return: no durability cap");
        });
        h.succeed();
    }

    public static void durabilityCap(GameTestHelper h) {
        configured(() -> {
            ItemStack fresh = new ItemStack(Items.NETHERITE_SWORD);
            h.assertValueEqual(left(Soulbound.capDurability(fresh)), 50, "full sword capped to 50");
            ItemStack worn = new ItemStack(Items.NETHERITE_SWORD);
            worn.setDamageValue(worn.getMaxDamage() - 30);
            h.assertValueEqual(left(Soulbound.capDurability(worn)), 30, "30 left stays 30");
            ItemStack dirt = new ItemStack(Items.DIRT, 7);
            h.assertTrue(ItemStack.matches(Soulbound.capDurability(dirt.copy()), dirt), "no durability: unchanged");
            SimpleTools.config().deathDurabilityCap = 100;
            h.assertValueEqual(left(Soulbound.capDurability(new ItemStack(Items.IRON_PICKAXE))), 100, "config cap");
            ToolsConfig c = new ToolsConfig();
            c.deathDurabilityCap = 0;
            c.soulboundAnvilCost = 99;
            c.deathVaultCostPerItem = -3;
            c.soulboundTraderChance = Double.NaN;
            c.normalize();
            h.assertTrue(c.deathDurabilityCap == 1 && c.soulboundAnvilCost == 39 && c.deathVaultCostPerItem == 1 && c.soulboundTraderChance == 0.05,
                    "bounds: cap >= 1, anvil < 40 (Vanilla 'too expensive'), cost >= 1, NaN chance default");
        });
        h.succeed();
    }

    /** Anvil: combining with Soulbound always costs the full 35 levels; Curse of Vanishing refuses it. */
    public static void soulboundAnvil(GameTestHelper h) {
        configured(() -> {
            var p = h.makeMockPlayer(GameType.CREATIVE);
            var menu = new AnvilMenu(1, p.getInventory());
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
            menu.getSlot(1).set(book(h));
            ItemStack result = menu.getSlot(2).getItem();
            h.assertTrue(Soulbound.has(result), "book applies Soulbound: " + result);
            h.assertValueEqual(menu.getCost(), 35, "full 35 levels");
            ItemStack worn = new ItemStack(Items.DIAMOND_SWORD);
            worn.set(DataComponents.REPAIR_COST, 0);
            ItemStack donor = bound(h, new ItemStack(Items.DIAMOND_SWORD));
            menu.getSlot(0).set(worn);
            menu.getSlot(1).set(donor);
            h.assertValueEqual(menu.getCost(), 35, "item + item with Soulbound also 35");
            SimpleTools.config().soulboundAnvilCost = 30;
            menu.getSlot(1).set(book(h));
            h.assertValueEqual(menu.getCost(), 30, "server setting");
            ItemStack cursed = new ItemStack(Items.DIAMOND_SWORD);
            cursed.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE), 1);
            menu.getSlot(0).set(cursed);
            menu.getSlot(1).set(book(h));
            h.assertTrue(!Soulbound.has(menu.getSlot(2).getItem()), "Curse of Vanishing keeps Soulbound off");
        });
        h.succeed();
    }

    /** The grindstone removes Soulbound like any non-curse enchantment. */
    public static void soulboundGrindstone(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.CREATIVE);
        var menu = new GrindstoneMenu(1, p.getInventory());
        menu.getSlot(0).set(bound(h, new ItemStack(Items.IRON_AXE)));
        ItemStack out = menu.getSlot(2).getItem();
        h.assertTrue(out.is(Items.IRON_AXE) && !Soulbound.has(out), "grindstone strips Soulbound: " + out);
        h.succeed();
    }

    /** Sources: the wandering trader (5 %, 24-32 diamonds, once) and End City treasure only. */
    public static void soulboundSources(GameTestHelper h) {
        configured(() -> {
            MerchantOffers offers = new MerchantOffers();
            SimpleTools.config().soulboundTraderChance = 0;
            h.assertTrue(!Soulbound.maybeOffer(h.getLevel(), offers, RandomSource.create(1)) && offers.isEmpty(), "chance 0: no offer");
            SimpleTools.config().soulboundTraderChance = 1;
            for (int i = 0; i < 20; i++) Soulbound.maybeOffer(h.getLevel(), offers, RandomSource.create(i));
            h.assertValueEqual(offers.size(), 20, "chance 1: always an offer");
            for (var offer : offers) {
                h.assertTrue(offer.getBaseCostA().is(Items.DIAMOND) && offer.getBaseCostA().getCount() >= 24 && offer.getBaseCostA().getCount() <= 32,
                        "24-32 diamonds: " + offer.getBaseCostA());
                h.assertTrue(offer.getResult().is(Items.ENCHANTED_BOOK) && Soulbound.carries(offer.getResult()) && offer.getMaxUses() == 1, "one Soulbound book");
            }
            h.assertTrue(new ToolsConfig().soulboundTraderChance == 0.05, "default 5 % per trader");
            List<LootPool.Builder> pools = new ArrayList<>();
            var registries = h.getLevel().registryAccess();
            ToolsLoot.apply(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(ToolsLoot.END_CITY)), pools::add, registries);
            h.assertValueEqual(pools.size(), 1, "one pool in End City treasure");
            pools.clear();
            for (String other : List.of("minecraft:chests/simple_dungeon", "minecraft:chests/stronghold_library", "minecraft:chests/village/village_weaponsmith"))
                ToolsLoot.apply(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(other)), pools::add, registries);
            h.assertTrue(pools.isEmpty(), "no other chest");
        });
        h.succeed();
    }

    /** A recorded stack still lying on the ground is shown but never redeemable; an offering then costs nothing. */
    public static void vaultLying(GameTestHelper h) {
        configured(() -> {
            UUID owner = UUID.randomUUID();
            ItemEntity e = spawn(h, new ItemStack(Items.IRON_INGOT, 5), new BlockPos(2, 2, 2));
            ledger(h).record(owner, 0, Optional.empty(), List.of(e));
            h.assertTrue(lying(h, owner) == 1 && lost(h, owner) == 0, "lying: shown, not redeemable");
            ItemStack diamonds = new ItemStack(Items.DIAMOND, 64);
            h.assertTrue(DeathVault.offer(ledger(h), owner, diamonds, RandomSource.create(3)).isEmpty() && diamonds.getCount() == 64,
                    "nothing to return: no diamond taken");
            ledger(h).clear(owner);
            e.discard();
        });
        h.succeed();
    }

    /** Despawn, lava, fire, explosion and void make a stack redeemable; the entity is gone, so the return never duplicates. */
    public static void vaultLost(GameTestHelper h) {
        configured(() -> {
            UUID owner = UUID.randomUUID();
            ServerLevel level = h.getLevel();
            ItemEntity despawn = spawn(h, new ItemStack(Items.IRON_INGOT, 3), new BlockPos(1, 2, 1));
            ItemEntity lava = spawn(h, new ItemStack(Items.GOLD_INGOT, 2), new BlockPos(2, 2, 1));
            ItemEntity fire = spawn(h, new ItemStack(Items.COPPER_INGOT, 4), new BlockPos(3, 2, 1));
            ItemEntity boom = spawn(h, new ItemStack(Items.EMERALD, 1), new BlockPos(1, 2, 3));
            ItemEntity fall = spawn(h, new ItemStack(Items.REDSTONE, 6), new BlockPos(2, 2, 3));
            ledger(h).record(owner, 0, Optional.empty(), List.of(despawn, lava, fire, boom, fall));
            ((ItemEntityAgeAccessor) despawn).simpletools$setAge(DeathLedger.LIFETIME - 1);
            despawn.tick();
            h.assertTrue(despawn.isRemoved(), "despawned at full age");
            lava.hurtServer(level, level.damageSources().lava(), 100);
            fire.hurtServer(level, level.damageSources().inFire(), 100);
            boom.hurtServer(level, level.damageSources().explosion(null, null), 100);
            fall.setPos(fall.getX(), level.getMinY() - 100, fall.getZ());
            fall.checkBelowWorld();
            h.assertTrue(lava.isRemoved() && fire.isRemoved() && boom.isRemoved() && fall.isRemoved(), "all destroyed");
            h.assertValueEqual(lost(h, owner), 5, "despawn, lava, fire, explosion and void are redeemable");
            ItemStack diamonds = new ItemStack(Items.DIAMOND, 64);
            List<ItemStack> back = DeathVault.offer(ledger(h), owner, diamonds, RandomSource.create(5));
            h.assertValueEqual(back.size(), 3, "three returns per offering");
            h.assertValueEqual(diamonds.getCount(), 64 - 3 * 4, "four diamonds per return");
            h.assertValueEqual(lost(h, owner), 2, "returned stacks left the list");
            back = DeathVault.offer(ledger(h), owner, diamonds, RandomSource.create(6));
            h.assertTrue(back.size() == 2 && lost(h, owner) == 0 && diamonds.getCount() == 64 - 5 * 4, "enough offerings return everything, once");
            h.assertTrue(DeathVault.offer(ledger(h), owner, diamonds, RandomSource.create(7)).isEmpty() && diamonds.getCount() == 44, "empty list: nothing taken");
            ledger(h).clear(owner);
        });
        h.succeed();
    }

    /** Picked up (whole or part), by a hopper or killed by command: struck from the list. */
    public static void vaultTaken(GameTestHelper h) {
        configured(() -> {
            UUID owner = UUID.randomUUID();
            ServerPlayer taker = living(h, new BlockPos(2, 2, 2));
            try {
                ItemEntity picked = spawn(h, new ItemStack(Items.IRON_INGOT, 3), new BlockPos(2, 2, 2));
                ItemEntity part = spawn(h, new ItemStack(Items.GOLD_INGOT, 8), new BlockPos(1, 2, 1));
                ItemEntity killed = spawn(h, new ItemStack(Items.EMERALD, 2), new BlockPos(3, 2, 3));
                ledger(h).record(owner, 0, Optional.empty(), List.of(picked, part, killed));
                picked.setNoPickUpDelay();
                picked.playerTouch(taker);
                h.assertTrue(picked.isRemoved() && taker.getInventory().countItem(Items.IRON_INGOT) == 3, "picked up by another player");
                part.getItem().shrink(1);
                ((ItemEntityAgeAccessor) part).simpletools$setAge(DeathLedger.LIFETIME - 1);
                part.tick();
                killed.kill(h.getLevel());
                h.assertTrue(part.isRemoved() && killed.isRemoved(), "gone");
                h.assertTrue(lost(h, owner) == 0 && lying(h, owner) == 0, "picked up, part taken and killed stacks are struck");
            } finally {
                ledger(h).clear(owner);
                h.getLevel().getServer().getPlayerList().remove(taker);
            }
        });
        h.succeed();
    }

    /** PvP death: only a despawn or a destruction by the killer counts; plain lava or void does not. */
    public static void vaultPvp(GameTestHelper h) {
        configured(() -> {
            UUID owner = UUID.randomUUID();
            ServerLevel level = h.getLevel();
            ServerPlayer killer = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
            ItemEntity despawn = spawn(h, new ItemStack(Items.IRON_INGOT, 3), new BlockPos(1, 2, 1));
            ItemEntity lava = spawn(h, new ItemStack(Items.GOLD_INGOT, 2), new BlockPos(2, 2, 1));
            ItemEntity blown = spawn(h, new ItemStack(Items.EMERALD, 1), new BlockPos(3, 2, 1));
            ItemEntity fall = spawn(h, new ItemStack(Items.REDSTONE, 6), new BlockPos(2, 2, 3));
            ledger(h).record(owner, 0, Optional.of(killer.getUUID()), List.of(despawn, lava, blown, fall));
            ((ItemEntityAgeAccessor) despawn).simpletools$setAge(DeathLedger.LIFETIME - 1);
            despawn.tick();
            lava.hurtServer(level, level.damageSources().lava(), 100);
            blown.hurtServer(level, level.damageSources().explosion(killer, killer), 100);
            fall.setPos(fall.getX(), level.getMinY() - 100, fall.getZ());
            fall.checkBelowWorld();
            List<ItemStack> lostStacks = DeathVault.lost(ledger(h), owner).stream().map(l -> l.entry().stack).toList();
            h.assertTrue(lostStacks.size() == 2 && lostStacks.stream().anyMatch(s -> s.is(Items.IRON_INGOT)) && lostStacks.stream().anyMatch(s -> s.is(Items.EMERALD)),
                    "PvP: the despawn and the killer's explosion count, lava and void do not: " + lostStacks);
            ledger(h).clear(owner);
        });
        h.succeed();
    }

    /** Newer deaths and more valuable items are more likely; the weights are deterministic. */
    public static void vaultWeights(GameTestHelper h) {
        configured(() -> {
            h.assertTrue(DeathVault.value(new ItemStack(Items.DIAMOND_SWORD)) > DeathVault.value(new ItemStack(Items.DIRT, 64)), "sword worth more than dirt");
            ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
            enchanted.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 5);
            h.assertTrue(DeathVault.value(enchanted) > DeathVault.value(new ItemStack(Items.DIAMOND_SWORD)), "enchantments add value");
            h.assertTrue(DeathVault.recency(0) > DeathVault.recency(1) && DeathVault.recency(1) > DeathVault.recency(5), "newer deaths weigh more");
            UUID owner = UUID.randomUUID();
            ItemEntity old = spawn(h, new ItemStack(Items.IRON_INGOT), new BlockPos(1, 2, 1));
            ledger(h).record(owner, 0, Optional.empty(), List.of(old));
            ItemEntity recent = spawn(h, new ItemStack(Items.IRON_INGOT), new BlockPos(3, 2, 3));
            ledger(h).record(owner, 10, Optional.empty(), List.of(recent));
            old.hurtServer(h.getLevel(), h.getLevel().damageSources().lava(), 100);
            recent.hurtServer(h.getLevel(), h.getLevel().damageSources().lava(), 100);
            List<DeathVault.Lost> both = DeathVault.lost(ledger(h), owner);
            h.assertValueEqual(both.size(), 2, "two lost ingots");
            h.assertTrue(both.get(0).death().time == 10 && both.get(0).weight() > both.get(1).weight(), "newest first and heavier");
            RandomSource random = RandomSource.create(42);
            int newer = 0;
            for (int i = 0; i < 3000; i++) if (DeathVault.pick(both, random) == both.get(0)) newer++;
            h.assertTrue(newer > 1800 && newer < 2200, "newer picked about 2/3 of the time: " + newer);
            List<DeathVault.Lost> mixed = List.of(new DeathVault.Lost(null, null, DeathVault.weight(new ItemStack(Items.DIRT), 0)),
                    new DeathVault.Lost(null, null, DeathVault.weight(new ItemStack(Items.NETHERITE_SWORD), 0)));
            int sword = 0;
            for (int i = 0; i < 2000; i++) if (DeathVault.pick(mixed, random) == mixed.get(1)) sword++;
            h.assertTrue(sword > 1600, "valuable item clearly more likely: " + sword);
            ledger(h).clear(owner);
        });
        h.succeed();
    }

    /** Dupe attempts: display fields cannot be taken, recorded drops do not merge, a second offering cannot repeat a return. */
    public static void vaultDupe(GameTestHelper h) {
        configured(() -> {
            ServerPlayer p = living(h, new BlockPos(0, 2, 0));
            UUID owner = p.getUUID();
            try {
            ItemEntity a = spawn(h, new ItemStack(Items.IRON_INGOT, 5), new BlockPos(2, 2, 2));
            ItemEntity b = spawn(h, new ItemStack(Items.IRON_INGOT, 5), new BlockPos(2, 2, 2));
            ItemEntity c = spawn(h, new ItemStack(Items.GOLD_INGOT, 2), new BlockPos(1, 2, 1));
            ledger(h).record(owner, 0, Optional.empty(), List.of(a, b, c));
            for (int i = 0; i < 81; i++) {
                a.tick();
                b.tick();
            }
            h.assertTrue(!a.isRemoved() && !b.isRemoved() && a.getItem().getCount() == 5 && b.getItem().getCount() == 5, "recorded drops never merge");
            c.hurtServer(h.getLevel(), h.getLevel().damageSources().lava(), 100);
            var menu = new DeathVaultMenu(1, p.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(new BlockPos(0, 1, 0))));
            h.assertTrue(menu.getSlot(0).getItem().is(Items.GOLD_INGOT) && menu.data(DeathVaultMenu.D_SHOWN_LOST) == 1, "the lost gold is shown first");
            h.assertTrue(menu.getSlot(1).getItem().is(Items.IRON_INGOT), "lying iron is shown too");
            h.assertTrue(!menu.getSlot(0).mayPickup(p) && !menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "display fields cannot be taken or filled");
            h.assertTrue(menu.quickMoveStack(p, 0).isEmpty() && menu.getSlot(0).getItem().is(Items.GOLD_INGOT), "shift-click takes nothing");
            h.assertTrue(!menu.clickMenuButton(p, DeathVaultMenu.BUTTON_OFFER), "no offering: nothing happens");
            menu.getSlot(DeathVaultMenu.OFFER_SLOT).set(new ItemStack(Items.DIAMOND, 3));
            h.assertTrue(!menu.clickMenuButton(p, DeathVaultMenu.BUTTON_OFFER) && menu.offering().getCount() == 3, "too few diamonds: nothing taken");
            h.assertTrue(!menu.getSlot(DeathVaultMenu.OFFER_SLOT).mayPlace(new ItemStack(Items.EMERALD)), "only offerings fit the slot");
            menu.getSlot(DeathVaultMenu.OFFER_SLOT).set(new ItemStack(Items.DIAMOND, 8));
            h.assertTrue(menu.clickMenuButton(p, DeathVaultMenu.BUTTON_OFFER), "offering accepted");
            h.assertTrue(p.getInventory().countItem(Items.GOLD_INGOT) == 2 && menu.offering().getCount() == 4, "the gold came back for four diamonds");
            h.assertTrue(!menu.clickMenuButton(p, DeathVaultMenu.BUTTON_OFFER) && menu.offering().getCount() == 4 && p.getInventory().countItem(Items.GOLD_INGOT) == 2,
                    "a second offering cannot return it again");
            menu.removed(p);
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 4, "unused diamonds come back when closing");
            a.discard();
            b.discard();
            } finally {
                ledger(h).clear(owner);
                h.getLevel().getServer().getPlayerList().remove(p);
            }
        });
        h.succeed();
    }

    /** The ledger survives a save: states, killer and stacks round-trip, lying entries stay tracked. */
    public static void vaultPersistence(GameTestHelper h) {
        configured(() -> {
            UUID owner = UUID.randomUUID();
            DeathLedger ledger = new DeathLedger();
            ItemEntity lying = spawn(h, new ItemStack(Items.IRON_INGOT, 2), new BlockPos(1, 2, 1));
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(100);
            ItemEntity gone = spawn(h, sword, new BlockPos(3, 2, 3));
            ledger.record(owner, 77, Optional.of(owner), List.of(lying, gone));
            ledger.removed(gone, true, null, false);
            var ops = RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess());
            var tag = DeathLedger.CODEC.encodeStart(ops, ledger).getOrThrow();
            DeathLedger loaded = DeathLedger.CODEC.parse(ops, tag).getOrThrow();
            var deaths = loaded.deaths(owner);
            h.assertTrue(deaths.size() == 1 && deaths.getFirst().time == 77 && deaths.getFirst().killer.equals(Optional.of(owner)), "death and killer kept");
            h.assertTrue(loaded.tracked(lying.getUUID()) && !loaded.tracked(gone.getUUID()), "lying stays tracked, lost does not");
            var lost = DeathVault.lost(loaded, owner);
            h.assertTrue(lost.size() == 1 && lost.getFirst().entry().stack.getDamageValue() == 100, "stack snapshot with its damage kept");
            SimpleTools.config().deathVaultMaxDeaths = 2;
            for (int i = 0; i < 4; i++) loaded.record(owner, 100 + i, Optional.empty(), List.of(spawn(h, new ItemStack(Items.DIRT), new BlockPos(2, 2, 2))));
            h.assertTrue(loaded.deaths(owner).size() == 2 && loaded.deaths(owner).getFirst().time == 102, "only the newest deaths are kept");
            SimpleTools.config().enableDeathVault = false;
            ServerPlayer p = living(h, new BlockPos(2, 2, 2));
            try {
                p.getInventory().setItem(0, new ItemStack(Items.DIRT));
                p.kill(h.getLevel());
                h.assertTrue(ledger(h).deaths(p.getUUID()).isEmpty(), "switched off: nothing recorded");
            } finally {
                h.getLevel().getServer().getPlayerList().remove(p);
            }
            lying.discard();
        });
        h.succeed();
    }
}
