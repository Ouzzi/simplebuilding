package com.simplebuilding.modules.simplefun;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ModuleGameTest {
  @GameTest
  public void launch(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("launch").accept(h);
  }

  @GameTest
  public void recipe(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("recipe").accept(h);
  }

  @GameTest
  public void configBounds(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("config_bounds").accept(h);
  }

  @GameTest
  public void yeet(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("yeet").accept(h);
  }

  @GameTest
  public void projectiles(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("projectiles").accept(h);
  }

  @GameTest
  public void noDamage(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("no_damage").accept(h);
  }

  @GameTest
  public void knockbackCap(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("knockback_cap").accept(h);
  }

  @GameTest
  public void piggy(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("piggy").accept(h);
  }

  @GameTest
  public void playerHeads(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("player_heads").accept(h);
  }

  @GameTest
  public void crossMod(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("cross_mod").accept(h);
  }

  @GameTest
  public void langConfig(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("lang_config").accept(h);
  }

  @GameTest
  public void trades(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("trades").accept(h);
  }

  @GameTest
  public void headBlocks(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("head_blocks").accept(h);
  }

  @GameTest
  public void pigHead(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("pig_head").accept(h);
  }

  @GameTest
  public void cowHead(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("cow_head").accept(h);
  }

  @GameTest
  public void chickenHead(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("chicken_head").accept(h);
  }

  @GameTest
  public void sheepHead(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("sheep_head").accept(h);
  }

  @GameTest
  public void flowerSniff(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("flower_sniff").accept(h);
  }

  @GameTest
  public void cookieCrumbs(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("cookie_crumbs").accept(h);
  }

  @GameTest
  public void appleSparkle(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("apple_sparkle").accept(h);
  }

  @GameTest
  public void carrotCrunch(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("carrot_crunch").accept(h);
  }

  @GameTest
  public void melonSplash(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("melon_splash").accept(h);
  }

  @GameTest
  public void honeyBubbles(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("honey_bubbles").accept(h);
  }

  @GameTest
  public void breadCrumbs(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("bread_crumbs").accept(h);
  }

  @GameTest
  public void berryBlush(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("berry_blush").accept(h);
  }

  @GameTest
  public void commands(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("commands").accept(h);
  }

  @GameTest
  public void oldConfig(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("old_config").accept(h);
  }

  @GameTest
  public void noDamageIndirect(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("no_damage_indirect").accept(h);
  }

  @GameTest
  public void projectileDamageAndGlass(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("projectile_damage_and_glass").accept(h);
  }

  @GameTest
  public void headsSwitchAndSources(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("heads_switch_and_sources").accept(h);
  }

  @GameTest
  public void tradeOffer(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("trade_offer").accept(h);
  }

  @GameTest
  public void delightTriggers(GameTestHelper h) {
    com.simplefun.test.FunTests.ALL.get("delight_triggers").accept(h);
  }
}
