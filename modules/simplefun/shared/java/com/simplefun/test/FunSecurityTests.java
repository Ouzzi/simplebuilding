package com.simplefun.test;

import com.simplefun.*;
import com.simplefun.config.*;
import com.simplefun.entity.*;
import com.simplefun.heads.*;
import com.simplefun.registry.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;

public final class FunSecurityTests {
  public static void commands(GameTestHelper h) {
    var d = h.getLevel().getServer().getCommands().getDispatcher();
    var admin = h.getLevel().getServer().createCommandSourceStack();
    for (String command :
        List.of(
            "simplefun tweaks yeet strength 999",
            "simplefun tweaks bricks damage -1",
            "simplefun tweaks bricks snowballDamage 999")) {
      boolean denied = false;
      try {
        d.execute(command, admin);
      } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
        denied = true;
      }
      h.assertTrue(denied, "out-of-range command denied " + command);
    }
    var p = FunTests.player(h);
    boolean denied = false;
    try {
      d.execute("simplefun pvp headDrops false", p.createCommandSourceStack());
    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
      denied = true;
    }
    h.assertTrue(denied, "non-op cannot change gameplay settings");
    h.succeed();
  }

  public static void oldConfig(GameTestHelper h) {
    var c =
        new com.google.gson.Gson()
            .fromJson(
                "{\"fun\":{\"enableYeet\":false,\"yeetStrength\":999,\"brickDamage\":-3}}",
                SimplefunConfig.class);
    c.normalize();
    h.assertTrue(
        !c.fun.enableYeet
            && c.fun.yeetStrength == 3
            && c.fun.brickDamage == 0
            && c.fun.enableAnimalHeads,
        "legacy config keys, new defaults, bounded numbers");
    h.succeed();
  }

  public static void noDamageIndirect(GameTestHelper h) {
    var p = FunTests.player(h);
    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FEATHER));
    var pig = h.spawn(EntityTypes.PIG, new BlockPos(4, 2, 4));
    float hp = pig.getHealth();
    var projectile = new BrickProjectileEntity(h.getLevel(), p);
    pig.hurtServer(h.getLevel(), h.getLevel().damageSources().thrown(projectile, p), 2);
    h.assertTrue(pig.getHealth() < hp, "feather does not disable indirect projectile damage");
    var s = new ItemStack(Items.STICK);
    var ench =
        h.getLevel()
            .registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(ModEnchantments.NO_DAMAGE);
    s.enchant(ench, 1);
    p.setItemSlot(EquipmentSlot.MAINHAND, s);
    var cow = h.spawn(EntityTypes.COW, new BlockPos(5, 2, 5));
    hp = cow.getHealth();
    cow.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(p), 4);
    h.assertTrue(cow.getHealth() == hp, "Damageless enchantment works independently of feather");
    h.succeed();
  }

  private static final class Probe extends BrickProjectileEntity {
    Probe(net.minecraft.world.level.Level l, LivingEntity p) {
      super(l, p);
    }

    void hit(BlockPos p) {
      onHitBlock(new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
    }

    void hit(LivingEntity e) {
      onHitEntity(new EntityHitResult(e));
    }
  }

  public static void projectileDamageAndGlass(GameTestHelper h) {
    h.assertTrue(BrickProjectileEntity.canBreakGlass(true, false), "private single-player allows glass option");
    h.assertTrue(!BrickProjectileEntity.canBreakGlass(true, true), "LAN publication prevents claim bypass");
    h.assertTrue(!BrickProjectileEntity.canBreakGlass(false, false), "dedicated multiplayer denies glass destruction");
    var saved = new com.google.gson.Gson().toJson(SimplefunCommon.getConfig().fun);
    h.runBeforeTestEnd(
        () ->
            SimplefunCommon.getConfig().fun =
                new com.google.gson.Gson().fromJson(saved, SimplefunConfig.Fun.class));
    var p = FunTests.player(h);
    var probe = new Probe(h.getLevel(), p);
    var pig = h.spawn(EntityTypes.PIG, new BlockPos(4, 2, 4));
    float hp = pig.getHealth();
    SimplefunCommon.getConfig().fun.brickDamage = 999;
    probe.hit(pig);
    h.assertTrue(pig.getHealth() == hp - 4, "actual projectile damage capped");
    var pos = new BlockPos(3, 2, 3);
    h.setBlock(pos, Blocks.GLASS);
    SimplefunCommon.getConfig().fun.throwableBricksBreakBlocks = true;
    probe.hit(h.absolutePos(pos));
    h.assertTrue(
        h.getBlockState(pos).is(Blocks.GLASS),
        "dedicated server refuses terrain destruction even if config enables it");
    h.setBlock(pos, Blocks.TINTED_GLASS);
    probe.hit(h.absolutePos(pos));
    h.assertTrue(h.getBlockState(pos).is(Blocks.TINTED_GLASS), "tinted glass retained");
    h.setBlock(pos, Blocks.ICE);
    probe.hit(h.absolutePos(pos));
    h.assertTrue(h.getBlockState(pos).is(Blocks.ICE), "glass sound does not imply destructibility");
    h.succeed();
  }

  public static void headsSwitchAndSources(GameTestHelper h) {
    var c = SimplefunCommon.getConfig().fun;
    boolean saved = c.enableAnimalHeads;
    try {
      c.enableAnimalHeads = false;
      var pools = new ArrayList<LootPool.Builder>();
      FunLoot.apply(BuiltInLootTables.CHARGED_CREEPER, pools::add, h.getLevel().registryAccess());
      h.assertTrue(pools.isEmpty(), "head loot disabled before reload");
      c.enableAnimalHeads = true;
      FunLoot.apply(BuiltInLootTables.CHARGED_CREEPER, pools::add, h.getLevel().registryAccess());
      h.assertTrue(pools.size() == 4, "four independent animal pools");
      for (var t : AnimalHead.values()) {
        h.assertTrue(
            BuiltInRegistries.SOUND_EVENT.containsKey(t.sound.location()),
            "Vanilla note-block sound registered " + t);
        h.assertTrue(
            new ItemStack(AnimalHeads.ITEMS.get(t))
                .get(DataComponents.NOTE_BLOCK_SOUND)
                .equals(t.sound.location()),
            "correct note-block sound");
      }
    } finally {
      c.enableAnimalHeads = saved;
    }
    h.succeed();
  }

  public static void tradeOffer(GameTestHelper h) {
    var reg = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
    var trade =
        reg.getOrThrow(
            ResourceKey.create(Registries.VILLAGER_TRADE, AnimalHeads.id("librarian/1/no_damage")));
    var entity = h.spawn(EntityTypes.VILLAGER, new BlockPos(2, 2, 2));
    var params =
        new LootParams.Builder(h.getLevel())
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, entity.position())
            .withParameter(
                LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,
                net.minecraft.util.Unit.INSTANCE)
            .create(LootContextParamSets.VILLAGER_TRADE);
    var offer = trade.value().getOffer(new LootContext.Builder(params).create(Optional.empty()));
    h.assertTrue(
        offer != null
            && offer.getCostA().is(Items.EMERALD)
            && offer.getCostA().getCount() == 25
            && offer.getMaxUses() == 3,
        "source trade price and uses");
    var ench =
        h.getLevel()
            .registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(ModEnchantments.NO_DAMAGE);
    h.assertTrue(
        EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult()).getLevel(ench) == 1,
        "trade returns Damageless I book");
    h.succeed();
  }

  public static void delightTriggers(GameTestHelper h) {
    for (var row :
        List.of(
            Map.entry(Items.COOKIE, "cookieCrumbs"),
            Map.entry(Items.APPLE, "appleSparkle"),
            Map.entry(Items.CARROT, "carrotCrunch"),
            Map.entry(Items.MELON_SLICE, "melonSplash"),
            Map.entry(Items.HONEY_BOTTLE, "honeyBubbles"),
            Map.entry(Items.BREAD, "breadCrumbs"),
            Map.entry(Items.SWEET_BERRIES, "berryBlush"),
            Map.entry(Items.GLOW_BERRIES, "berryBlush")))
      h.assertTrue(
          row.getValue().equals(FunDelights.food(new ItemStack(row.getKey()))),
          "food mapping " + row);
    h.assertTrue(
        FunDelights.food(new ItemStack(Items.ROTTEN_FLESH)) == null, "unrelated food unchanged");
    var p = FunTests.player(h);
    p.setShiftKeyDown(true);
    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DANDELION));
    FunDelights.tick(p);
    h.assertTrue(
        !FunDelights.emit(p, "flowerSniff", net.minecraft.sounds.SoundEvents.FOX_SNIFF),
        "real flower trigger used bounded cooldown");
    h.succeed();
  }
}
