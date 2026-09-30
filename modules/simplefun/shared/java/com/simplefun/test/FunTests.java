package com.simplefun.test;

import com.simplefun.*;
import com.simplefun.config.*;
import com.simplefun.entity.*;
import com.simplefun.heads.*;
import com.simplefun.registry.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.phys.*;

public final class FunTests {
  public static final Map<String, java.util.function.Consumer<GameTestHelper>> ALL =
      new LinkedHashMap<>();

  static {
    ALL.put("launch", FunTests::launch);
    ALL.put("recipe", FunTests::recipe);
    ALL.put("config_bounds", FunTests::configBounds);
    ALL.put("yeet", FunTests::yeet);
    ALL.put("projectiles", FunTests::projectiles);
    ALL.put("no_damage", FunTests::noDamage);
    ALL.put("knockback_cap", FunTests::knockbackCap);
    ALL.put("piggy", FunTests::piggy);
    ALL.put("player_heads", FunTests::playerHeads);
    ALL.put("cross_mod", FunTests::crossMod);
    ALL.put("lang_config", FunTests::langConfig);
    ALL.put("trades", FunTests::trades);
    ALL.put("head_blocks", FunTests::headBlocks);
    for (var t : AnimalHead.values()) ALL.put(t.path(), h -> head(h, t));
    for (var key :
        List.of(
            "flowerSniff",
            "cookieCrumbs",
            "appleSparkle",
            "carrotCrunch",
            "melonSplash",
            "honeyBubbles",
            "breadCrumbs",
            "berryBlush")) ALL.put(snake(key), h -> delight(h, key));
  }

  static {
    ALL.put("commands", FunSecurityTests::commands);
    ALL.put("old_config", FunSecurityTests::oldConfig);
    ALL.put("no_damage_indirect", FunSecurityTests::noDamageIndirect);
    ALL.put("projectile_damage_and_glass", FunSecurityTests::projectileDamageAndGlass);
    ALL.put("heads_switch_and_sources", FunSecurityTests::headsSwitchAndSources);
    ALL.put("trade_offer", FunSecurityTests::tradeOffer);
    ALL.put("delight_triggers", FunSecurityTests::delightTriggers);
  }

  static {
    ALL.replaceAll(
        (name, body) ->
            helper -> {
              var gson = new com.google.gson.Gson();
              var saved =
                  gson.fromJson(
                      gson.toJson(SimplefunCommon.getConfig().fun), SimplefunConfig.Fun.class);
              try {
                body.accept(helper);
              } finally {
                SimplefunCommon.getConfig().fun = saved;
              }
            });
  }

  private static String snake(String k) {
    return k.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }

  public static ServerPlayer player(GameTestHelper h) {
    var p = h.makeMockServerPlayerInLevel();
    var pos = h.absoluteVec(new Vec3(2, 2, 2));
    p.snapTo(pos.x, pos.y, pos.z, 0, 0);
    p.getAbilities().instabuild = false;
    h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(p));
    return p;
  }

  private static void clean(GameTestHelper h) {
    for (var e : h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds())) e.discard();
  }

  private static int count(GameTestHelper h, Item i) {
    return h
        .getLevel()
        .getEntitiesOfClass(ItemEntity.class, h.getBounds(), e -> e.getItem().is(i))
        .stream()
        .mapToInt(e -> e.getItem().getCount())
        .sum();
  }

  private static void restore(GameTestHelper h) {}

  public static void launch(GameTestHelper h) {
    RegistryExport.writeIfRequested();
    for (var t : AnimalHead.values()) {
      h.assertTrue(
          BuiltInRegistries.ITEM.getValue(AnimalHeads.id(t.path())) == AnimalHeads.ITEMS.get(t),
          "head item registered");
      h.assertTrue(
          BuiltInRegistries.BLOCK.getValue(AnimalHeads.id(t.wall())) == AnimalHeads.WALL.get(t),
          "wall registered");
    }
    h.assertTrue(
        BuiltInRegistries.ENTITY_TYPE.getValue(AnimalHeads.id("brick_projectile"))
            == ModEntities.BRICK_PROJECTILE,
        "entity");
    h.assertTrue(
        BuiltInRegistries.MOB_EFFECT.getValue(AnimalHeads.id("piggy_effect"))
            == ModEffects.PIGGY_EFFECT,
        "effect");
    h.assertTrue(
        h.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModEnchantments.NO_DAMAGE)
                .value()
                .getMaxLevel()
            == 1,
        "enchantment data");
    h.assertTrue(
        h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("simplefun")
            != null,
        "command");
    h.assertTrue(
        h.getLevel()
                .getServer()
                .getAdvancements()
                .get(AnimalHeads.id("recipes/misc/brick_snowball"))
            != null,
        "advancement parses");
    h.assertTrue(
        new ItemStack(Items.FEATHER).has(DataComponents.ENCHANTABLE), "feather enchantability");
    h.succeed();
  }

  public static void recipe(GameTestHelper h) {
    var list = new ArrayList<ItemStack>();
    for (int n = 0; n < 9; n++)
      list.add(
          n == 4
              ? new ItemStack(Items.BRICK)
              : Set.of(1, 3, 5, 7).contains(n) ? new ItemStack(Items.SNOWBALL) : ItemStack.EMPTY);
    var input = CraftingInput.of(3, 3, list);
    var match =
        h.getLevel()
            .getServer()
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
    h.assertTrue(match.isPresent(), "recipe matches");
    var out = match.get().value().assemble(input);
    h.assertTrue(out.is(ModItems.BRICK_SNOWBALL) && out.getCount() == 1, "recipe yield");
    h.succeed();
  }

  public static void configBounds(GameTestHelper h) {
    var c = new SimplefunConfig();
    for (float n : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -999, 999}) {
      c.fun.yeetStrength = n;
      c.fun.brickDamage = n;
      c.fun.brickSnowballDamage = n;
      c.fun.maxKnockback = n;
      c.normalize();
      h.assertTrue(
          c.fun.yeetStrength >= .1
              && c.fun.yeetStrength <= 3
              && c.fun.brickDamage >= 0
              && c.fun.brickDamage <= 4
              && c.fun.maxKnockback <= 4,
          "unsafe floats bounded");
    }
    c.fun = null;
    c.normalize();
    h.assertTrue(c.fun != null, "null section repaired");
    h.succeed();
  }

  public static void yeet(GameTestHelper h) {
    restore(h);
    var p = player(h);
    p.setShiftKeyDown(true);
    var c = SimplefunCommon.getConfig().fun;
    c.enableYeet = true;
    c.yeetStrength = 999;
    var e = p.drop(new ItemStack(Items.STONE), false, net.minecraft.util.Prediction.SERVER_ONLY);
    h.assertTrue(
        e != null && e.getDeltaMovement().length() <= 1.50001 && e.getItem().getCount() == 1,
        "capped drop, no duplication");
    c.enableYeet = false;
    var normal =
        p.drop(new ItemStack(Items.STONE), false, net.minecraft.util.Prediction.SERVER_ONLY);
    h.assertTrue(
        normal != null && normal.getDeltaMovement().length() < e.getDeltaMovement().length(),
        "off switch");
    h.succeed();
  }

  public static void projectiles(GameTestHelper h) {
    restore(h);
    var p = player(h);
    var c = SimplefunCommon.getConfig().fun;
    c.enableThrowableBricks = true;
    var s = new ItemStack(Items.BRICK, 8);
    BrickProjectileEntity.throwFrom(h.getLevel(), p, s);
    h.assertTrue(s.getCount() == 7, "exactly one consumed");
    BrickProjectileEntity.throwFrom(h.getLevel(), p, s);
    h.assertTrue(s.getCount() == 7, "spam cooldown");
    var es = h.getLevel().getEntitiesOfClass(BrickProjectileEntity.class, h.getBounds());
    h.assertTrue(
        es.size() == 1 && es.get(0).getItem().getCount() == 1, "one projectile, one render item");
    var e = es.get(0);
    e.tickCount = 201;
    e.tick();
    h.assertTrue(e.isRemoved(), "finite lifetime");
    p.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(s.getItem()));
    c.enableThrowableBricks = false;
    BrickProjectileEntity.throwFrom(h.getLevel(), p, s);
    h.assertTrue(s.getCount() == 7, "disabled prevents consumption");
    h.succeed();
  }

  public static void noDamage(GameTestHelper h) {
    restore(h);
    var p = player(h);
    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FEATHER));
    var pig = h.spawn(EntityTypes.PIG, new BlockPos(4, 2, 4));
    float before = pig.getHealth();
    pig.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(p), 3);
    h.assertTrue(pig.getHealth() == before, "feather no health loss");
    h.assertTrue(pig.getDeltaMovement().length() > 0, "harmless knockback");
    pig.setInvulnerableTime(0);
    SimplefunCommon.getConfig().fun.enableNoDamage = false;
    pig.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(p), 3);
    h.assertTrue(pig.getHealth() < before, "off switch");
    h.succeed();
  }

  public static void knockbackCap(GameTestHelper h) {
    restore(h);
    var c = SimplefunCommon.getConfig().fun;
    c.enableHigherKnockback = true;
    c.maxKnockback = 999;
    var p = player(h);
    var s = new ItemStack(Items.STICK);
    s.enchant(
        h.getLevel()
            .registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.KNOCKBACK),
        255);
    float v =
        EnchantmentHelper.modifyKnockback(
            h.getLevel(), s, p, h.getLevel().damageSources().playerAttack(p), 0);
    h.assertTrue(v <= 4, "forged enchantment capped");
    c.enableHigherKnockback = false;
    h.assertTrue(
        EnchantmentHelper.modifyKnockback(
                h.getLevel(), s, p, h.getLevel().damageSources().playerAttack(p), 0)
            <= 2,
        "vanilla cap when off");
    h.succeed();
  }

  public static void piggy(GameTestHelper h) {
    restore(h);
    var p = player(h);
    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PORKCHOP));
    p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
    for (int i = 0; i < 40; i++) p.doTick();
    h.assertTrue(p.hasEffect(ModEffects.holder()), "food grants cosmetic effect");
    h.assertTrue(p.getEffect(ModEffects.holder()).getDuration() <= 6000, "five minute cap");
    h.succeed();
  }

  public static void playerHeads(GameTestHelper h) {
    restore(h);
    var victim = player(h);
    var killer = player(h);
    clean(h);
    com.simplefun.event.PlayerHeadDrop.onDeath(
        victim, h.getLevel().damageSources().playerAttack(killer));
    h.assertTrue(count(h, Items.PLAYER_HEAD) == 1, "PvP head");
    var head =
        h.getLevel()
            .getEntitiesOfClass(
                ItemEntity.class, h.getBounds(), e -> e.getItem().is(Items.PLAYER_HEAD))
            .getFirst()
            .getItem();
    h.assertTrue(head.has(DataComponents.PROFILE), "skin profile");
    clean(h);
    SimplefunCommon.getConfig().fun.playerHeadDrops = false;
    com.simplefun.event.PlayerHeadDrop.onDeath(
        victim, h.getLevel().damageSources().playerAttack(killer));
    h.assertTrue(count(h, Items.PLAYER_HEAD) == 0, "switch");
    h.succeed();
  }

  public static void head(GameTestHelper h, AnimalHead t) {
    restore(h);
    clean(h);
    var creeper = h.spawn(EntityTypes.CREEPER, new BlockPos(1, 2, 1));
    var bolt = EntityTypes.LIGHTNING_BOLT.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
    creeper.thunderHit(h.getLevel(), bolt);
    creeper.clearFire();
    var victim = (LivingEntity) h.spawn(t.source, new BlockPos(4, 2, 4));
    victim.hurtServer(h.getLevel(), h.getLevel().damageSources().explosion(creeper, creeper), 1000);
    h.assertTrue(count(h, AnimalHeads.ITEMS.get(t)) == 1, "charged creeper drops " + t);
    var second = (LivingEntity) h.spawn(t.source, new BlockPos(5, 2, 5));
    second.hurtServer(h.getLevel(), h.getLevel().damageSources().explosion(creeper, creeper), 1000);
    h.assertTrue(count(h, AnimalHeads.ITEMS.get(t)) == 1, "one head per creeper");
    clean(h);
    var plain = h.spawn(EntityTypes.CREEPER, new BlockPos(1, 2, 1));
    var third = (LivingEntity) h.spawn(t.source, new BlockPos(4, 2, 4));
    third.hurtServer(h.getLevel(), h.getLevel().damageSources().explosion(plain, plain), 1000);
    h.assertTrue(count(h, AnimalHeads.ITEMS.get(t)) == 0, "uncharged drops none");
    var p = player(h);
    p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AnimalHeads.ITEMS.get(t)));
    p.setShiftKeyDown(true);
    String key = t.name().toLowerCase(Locale.ROOT) + "HeadGreeting";
    h.assertTrue(FunDelights.emit(p, key, t.sound), "secret greeting");
    h.assertTrue(!FunDelights.emit(p, key, t.sound), "greeting spam capped");
    h.succeed();
  }

  public static void headBlocks(GameTestHelper h) {
    for (var t : AnimalHead.values()) {
      h.setBlock(new BlockPos(2, 2, 2), AnimalHeads.STANDING.get(t));
      h.assertTrue(
          h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)))
              instanceof net.minecraft.world.level.block.entity.SkullBlockEntity,
          "standing skull entity");
      h.setBlock(new BlockPos(2, 2, 2), AnimalHeads.WALL.get(t));
      h.assertTrue(
          h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)))
              instanceof net.minecraft.world.level.block.entity.SkullBlockEntity,
          "wall skull entity");
      h.assertTrue(
          new ItemStack(AnimalHeads.ITEMS.get(t)).has(DataComponents.EQUIPPABLE), "wearable");
    }
    h.succeed();
  }

  public static void delight(GameTestHelper h, String key) {
    restore(h);
    var p = player(h);
    h.assertTrue(
        FunDelights.emit(p, key, net.minecraft.sounds.SoundEvents.FOX_SNIFF), "enabled delight");
    h.assertTrue(
        !FunDelights.emit(p, key, net.minecraft.sounds.SoundEvents.FOX_SNIFF),
        "five second spam cap");
    try {
      SimplefunCommon.getConfig()
          .fun
          .getClass()
          .getField(key)
          .setBoolean(SimplefunCommon.getConfig().fun, false);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    h.assertTrue(
        !FunDelights.emit(player(h), key, net.minecraft.sounds.SoundEvents.FOX_SNIFF), "switch");
    h.assertTrue(!FunDelights.enabled("not_a_feature"), "unknown denied");
    h.succeed();
  }

  public static void crossMod(GameTestHelper h) {
    var id = Identifier.parse("simplebuilding:reinforced_hopper");
    h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "SimpleBuilding loaded");
    h.setBlock(new BlockPos(2, 2, 2), BuiltInRegistries.BLOCK.getValue(id));
    var inv =
        (net.minecraft.world.Container)
            h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)));
    var item = new ItemStack(AnimalHeads.ITEMS.get(AnimalHead.PIG));
    inv.setItem(0, item);
    h.assertTrue(
        inv.removeItem(0, 1).is(AnimalHeads.ITEMS.get(AnimalHead.PIG)) && inv.getItem(0).isEmpty(),
        "foreign head storage no duplication");
    h.succeed();
  }

  public static void trades(GameTestHelper h) {
    var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
    h.assertTrue(registry.get(AnimalHeads.id("librarian/1/no_damage")).isPresent(), "trade parses");
    h.succeed();
  }

  public static void langConfig(GameTestHelper h) {
    for (String locale : List.of("en_us", "de_de")) {
      try (var stream =
          FunTests.class.getResourceAsStream("/assets/simplefun/lang/" + locale + ".json")) {
        var data =
            com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                .getAsJsonObject();
        for (var f : SimplefunConfig.Fun.class.getFields()) {
          String key = "text.autoconfig.simplefun.option.fun." + f.getName();
          h.assertTrue(data.has(key) && data.has(key + ".@Tooltip"), "option localization " + key);
          h.assertTrue(
              data.get(key + ".@Tooltip")
                  .getAsString()
                  .contains(String.valueOf(f.get(new SimplefunConfig.Fun()))),
              "default tooltip " + key);
        }
        for (var t : AnimalHead.values())
          h.assertTrue(data.has("block.simplefun." + t.path()), "head lang");
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
    h.succeed();
  }
}
