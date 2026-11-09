package com.simplefun.registry;

import com.simplefun.block.RubberDuckBlock;
import com.simplefun.entity.PaperPlaneEntity;
import com.simplefun.entity.SkippingStoneEntity;
import com.simplefun.entity.WaterBalloonEntity;
import com.simplefun.item.BubbleWandItem;
import com.simplefun.item.PaperPlaneItem;
import com.simplefun.item.PartyPopperItem;
import com.simplefun.item.RollItem;
import com.simplefun.item.RubberDuckItem;
import com.simplefun.item.ToyThrowItem;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * FUN11-1..10 (b-fun14, docs/ai/PLAN-B-FUN14-2026-11-09.md): the toys' items, the rubber duck block, the three thrown
 * toys, the soap bubble particle, two sounds and four statistics. Registered by {@link SimplefunRegistry#registerToys}.
 */
public final class ModToys {
  private ModToys() {}

  private static Identifier id(String path) {
    return FunContent.id(path);
  }

  private static ResourceKey<Item> itemKey(String path) {
    return ResourceKey.create(Registries.ITEM, id(path));
  }

  private static Item.Properties props(String path) {
    return new Item.Properties().setId(itemKey(path));
  }

  // FUN11-9: the block comes first, its item below.
  public static final ResourceKey<Block> RUBBER_DUCK_BLOCK_KEY =
      ResourceKey.create(Registries.BLOCK, id("rubber_duck"));
  public static final Block RUBBER_DUCK_BLOCK =
      new RubberDuckBlock(
          BlockBehaviour.Properties.of()
              .setId(RUBBER_DUCK_BLOCK_KEY)
              .mapColor(MapColor.COLOR_YELLOW)
              .instabreak()
              .sound(SoundType.WOOL)
              .noOcclusion()
              .pushReaction(PushReaction.POPPED));

  /** Path → item, in tab order (after the brick snowball). */
  public static final java.util.Map<String, Item> BY_PATH = new java.util.LinkedHashMap<>();

  private static Item item(String path, Item item) {
    BY_PATH.put(path, item);
    return item;
  }

  public static final Item PARTY_POPPER =
      item("party_popper", new PartyPopperItem(props("party_popper").stacksTo(16)));
  public static final Item DICE = item("dice", new RollItem(props("dice").stacksTo(16), RollItem.Kind.DICE));
  public static final Item LUCKY_COIN =
      item("lucky_coin", new RollItem(props("lucky_coin").stacksTo(16), RollItem.Kind.COIN));
  public static final Item SKIPPING_STONE =
      item("skipping_stone", new ToyThrowItem(props("skipping_stone").stacksTo(16), ToyThrowItem.Kind.SKIPPING_STONE));
  public static final Item WATER_BALLOON =
      item("water_balloon", new ToyThrowItem(props("water_balloon").stacksTo(16), ToyThrowItem.Kind.WATER_BALLOON));
  public static final Item BUBBLE_WAND = item("bubble_wand", new BubbleWandItem(props("bubble_wand").durability(64)));
  public static final Item PAPER_PLANE = item("paper_plane", new PaperPlaneItem(props("paper_plane").stacksTo(16)));
  public static final Item RUBBER_DUCK =
      item(
          "rubber_duck",
          new RubberDuckItem(RUBBER_DUCK_BLOCK, props("rubber_duck").useBlockDescriptionPrefix().stacksTo(16)));

  public static final List<Item> ITEMS = List.copyOf(BY_PATH.values());

  public static final ResourceKey<EntityType<?>> SKIPPING_STONE_ENTITY_KEY = entityKey("skipping_stone");
  public static final EntityType<SkippingStoneEntity> SKIPPING_STONE_ENTITY =
      EntityType.Builder.<SkippingStoneEntity>of(SkippingStoneEntity::new, MobCategory.MISC)
          .sized(0.25f, 0.25f)
          .clientTrackingRange(4)
          .updateInterval(5)
          .build(SKIPPING_STONE_ENTITY_KEY);
  public static final ResourceKey<EntityType<?>> WATER_BALLOON_ENTITY_KEY = entityKey("water_balloon");
  public static final EntityType<WaterBalloonEntity> WATER_BALLOON_ENTITY =
      EntityType.Builder.<WaterBalloonEntity>of(WaterBalloonEntity::new, MobCategory.MISC)
          .sized(0.25f, 0.25f)
          .clientTrackingRange(4)
          .updateInterval(10)
          .build(WATER_BALLOON_ENTITY_KEY);
  public static final ResourceKey<EntityType<?>> PAPER_PLANE_ENTITY_KEY = entityKey("paper_plane");
  public static final EntityType<PaperPlaneEntity> PAPER_PLANE_ENTITY =
      EntityType.Builder.<PaperPlaneEntity>of(PaperPlaneEntity::new, MobCategory.MISC)
          .sized(0.3f, 0.2f)
          .clientTrackingRange(6)
          .updateInterval(3)
          .build(PAPER_PLANE_ENTITY_KEY);

  private static ResourceKey<EntityType<?>> entityKey(String path) {
    return ResourceKey.create(Registries.ENTITY_TYPE, id(path));
  }

  /** FUN11-7: a slowly rising soap bubble (client particle, {@code SoapBubbleParticle}). */
  public static final SimpleParticleType SOAP_BUBBLE = new SimpleParticleType(false) {};

  public static final SoundEvent DUCK_SQUEAK = SoundEvent.createVariableRangeEvent(id("rubber_duck.squeak"));
  public static final SoundEvent HIGH_FIVE_CLAP = SoundEvent.createVariableRangeEvent(id("high_five.clap"));

  public static final Identifier STAT_HIGH_FIVES = id("high_fives");
  public static final Identifier STAT_PETS_GIVEN = id("pets_given");
  public static final Identifier STAT_BEST_STONE_SKIPS = id("best_stone_skips");
  public static final Identifier STAT_SNOW_ANGELS = id("snow_angels");
  public static final List<Identifier> STATS =
      List.of(STAT_HIGH_FIVES, STAT_PETS_GIVEN, STAT_BEST_STONE_SKIPS, STAT_SNOW_ANGELS);

  public static void registerBlocks() {
    Registry.register(BuiltInRegistries.BLOCK, RUBBER_DUCK_BLOCK_KEY, RUBBER_DUCK_BLOCK);
  }

  public static void registerItems() {
    BY_PATH.forEach((path, item) -> Registry.register(BuiltInRegistries.ITEM, itemKey(path), item));
  }

  public static void registerEntities() {
    Registry.register(BuiltInRegistries.ENTITY_TYPE, SKIPPING_STONE_ENTITY_KEY, SKIPPING_STONE_ENTITY);
    Registry.register(BuiltInRegistries.ENTITY_TYPE, WATER_BALLOON_ENTITY_KEY, WATER_BALLOON_ENTITY);
    Registry.register(BuiltInRegistries.ENTITY_TYPE, PAPER_PLANE_ENTITY_KEY, PAPER_PLANE_ENTITY);
  }

  public static void registerMisc() {
    Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("soap_bubble"), SOAP_BUBBLE);
    Registry.register(BuiltInRegistries.SOUND_EVENT, DUCK_SQUEAK.location(), DUCK_SQUEAK);
    Registry.register(BuiltInRegistries.SOUND_EVENT, HIGH_FIVE_CLAP.location(), HIGH_FIVE_CLAP);
    for (Identifier stat : STATS) {
      Registry.register(BuiltInRegistries.CUSTOM_STAT, stat, stat);
      Stats.CUSTOM.get(stat, StatFormatter.DEFAULT);
    }
  }
}
