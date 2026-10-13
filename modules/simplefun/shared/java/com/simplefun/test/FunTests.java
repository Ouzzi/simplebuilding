package com.simplefun.test;

import com.simplefun.*;
import com.simplefun.config.*;
import com.simplefun.entity.*;
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
 /** Principle 8 (standalone): a content mod is loaded when it owns registry ids; loader-neutral for shared tests. */
 public static boolean isModLoaded(String mod){return BuiltInRegistries.ITEM.keySet().stream().anyMatch(i->i.getNamespace().equals(mod))||BuiltInRegistries.BLOCK.keySet().stream().anyMatch(i->i.getNamespace().equals(mod));}
 /** Without the partner the coupling cannot be observed: pass with a log note instead of failing. */
 public static boolean partnerMissing(net.minecraft.gametest.framework.GameTestHelper h,String mod,String what){if(isModLoaded(mod))return false;com.mojang.logging.LogUtils.getLogger().info("[standalone] {} not loaded - skipping {}",mod,what);h.succeed();return true;}
  public static final Map<String, java.util.function.Consumer<GameTestHelper>> ALL =
      new LinkedHashMap<>();

  static {
    ALL.put("launch", FunTests::launch);
    ALL.put("guide_book", com.simplefun.guide.FunGuide::gameTest);
    ALL.put("recipe", FunTests::recipe);
    ALL.put("config_bounds", FunTests::configBounds);
    ALL.put("yeet", FunTests::yeet);
    ALL.put("projectiles", FunTests::projectiles);
    ALL.put("no_damage", FunTests::noDamage);
    ALL.put("knockback_cap", FunTests::knockbackCap);
    ALL.put("piggy", FunTests::piggy);
    ALL.put("cross_mod", FunTests::crossMod);
    ALL.put("lang_config", FunTests::langConfig);
    ALL.put("trades", FunTests::trades);
    ALL.put("creative_tabs", FunTests::creativeTabs);
    ALL.put("sub_mod_switch", FunTests::subModSwitch);
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

  /** Super-mod switch "Enable Simple Fun" off: registries unchanged, empty tab, no vanilla tab entries, no loot. */
  private static net.minecraft.world.item.CreativeModeTab funTab() {
    return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, FunContent.id("fun")));
  }

  private static net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters displayParameters(GameTestHelper h) {
    return new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
        h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
  }

  public static void subModSwitch(GameTestHelper h) {
    var sw = com.simplebuilding.framework.api.SubMods.enabled("simplefun");
    try {
      com.simplebuilding.framework.api.SubMods.set("simplefun", false);
      h.assertTrue(!SimplefunCommon.on(), "Switch off reaches Simple Fun");
      h.assertTrue(
          BuiltInRegistries.CREATIVE_MODE_TAB.getValue(FunContent.id("fun")) != null
              && BuiltInRegistries.ITEM.getValue(FunContent.id("brick_snowball")) != null,
          "Registries stay");
      // buildContents directly: tryRebuildTabContents is a no-op while its parameters stay the same.
      funTab().buildContents(displayParameters(h));
      h.assertTrue(funTab().getDisplayItems().isEmpty(), "Own tab empty while off");
    } finally {
      com.simplebuilding.framework.api.SubMods.set("simplefun", sw);
      funTab().buildContents(displayParameters(h));
    }
    h.assertTrue(SimplefunCommon.on() == sw, "Switch restored");
    h.succeed();
  }

  /**
   * The fun tab holds only the mod's items (guide, brick snowball, no vanilla ingredients); the brick
   * snowball follows the snowball in Combat, and in the search tab it stands there exactly once.
   */
  public static void creativeTabs(GameTestHelper h) {
    CreativeModeTabs.tryRebuildTabContents(
        h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
    var own =
        BuiltInRegistries.CREATIVE_MODE_TAB
            .getValueOrThrow(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FunContent.id("fun")))
            .getDisplayItems()
            .stream()
            .map(ItemStack::getItem)
            .toList();
    var expected = FunContent.tabStacks().stream().map(ItemStack::getItem).toList();
    h.assertTrue(own.equals(expected), "fun tab lists only own items in order: " + own);
    var search =
        CreativeModeTabs.searchTab().getDisplayItems().stream().map(ItemStack::getItem).toList();
    var combat =
        BuiltInRegistries.CREATIVE_MODE_TAB
            .getValueOrThrow(CreativeModeTabs.COMBAT)
            .getDisplayItems()
            .stream()
            .map(ItemStack::getItem)
            .toList();
    for (var list : List.of(combat, search))
      follows(h, list, FunContent.SNOWBALL_ANCHOR, List.of(ModItems.BRICK_SNOWBALL));
    for (var item : expected)
      h.assertTrue(
          search.stream().filter(item::equals).count() == 1, "search tab lists " + item + " once");
    h.succeed();
  }

  private static void follows(GameTestHelper h, List<Item> list, Item anchor, List<Item> items) {
    int at = list.indexOf(anchor);
    h.assertTrue(
        at >= 0
            && at + items.size() < list.size()
            && list.subList(at + 1, at + 1 + items.size()).equals(items),
        items + " follow " + anchor);
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
    // Simple Fun 1.4.0: the animal heads moved to Simple Mobheads.
    h.assertTrue(!BuiltInRegistries.ITEM.containsKey(FunContent.id("pig_head")), "Fun registers no heads any more");
    h.assertTrue(
        BuiltInRegistries.ENTITY_TYPE.getValue(FunContent.id("brick_projectile"))
            == ModEntities.BRICK_PROJECTILE,
        "entity");
    h.assertTrue(
        BuiltInRegistries.MOB_EFFECT.getValue(FunContent.id("piggy_effect"))
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
                .get(FunContent.id("recipes/misc/brick_snowball"))
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
    if (partnerMissing(h, "simplebuilding", "Fun items in SimpleBuilding storage")) return;
    var id = Identifier.parse("simplebuilding:reinforced_hopper");
    h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "SimpleBuilding loaded");
    h.setBlock(new BlockPos(2, 2, 2), BuiltInRegistries.BLOCK.getValue(id));
    var inv =
        (net.minecraft.world.Container)
            h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2, 2, 2)));
    var item = new ItemStack(ModItems.BRICK_SNOWBALL);
    inv.setItem(0, item);
    h.assertTrue(
        inv.removeItem(0, 1).is(ModItems.BRICK_SNOWBALL) && inv.getItem(0).isEmpty(),
        "foreign storage no duplication");
    h.succeed();
  }

  public static void trades(GameTestHelper h) {
    var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
    h.assertTrue(registry.get(FunContent.id("librarian/1/no_damage")).isPresent(), "trade parses");
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
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
    h.succeed();
  }

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplefun.discover.FunDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplefun.discover.FunDiscover::behaviourHints);
    }
}
