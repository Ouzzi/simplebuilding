package com.simplefun.test;

import com.simplefun.SimplefunCommon;
import com.simplefun.block.RubberDuckBlock;
import com.simplefun.config.SimplefunConfig;
import com.simplefun.entity.PaperPlaneEntity;
import com.simplefun.entity.SkippingStoneEntity;
import com.simplefun.entity.WaterBalloonEntity;
import com.simplefun.item.RollItem;
import com.simplefun.registry.FunContent;
import com.simplefun.registry.ModToys;
import com.simplefun.toys.FunToys;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests of FUN11-1..10 (b-fun14, docs/ai/PLAN-B-FUN14-2026-11-09.md): each toy is registered, has its recipe and
 * shows its core effect with its limits (cooldown, caps, switch).
 */
public final class FunToyTests {
  private FunToyTests() {}

  static void register(java.util.Map<String, java.util.function.Consumer<GameTestHelper>> all) {
    all.put("toy_party_popper", FunToyTests::partyPopper);
    all.put("toy_dice_coin", FunToyTests::diceCoin);
    all.put("toy_high_five", FunToyTests::highFive);
    all.put("toy_petting", FunToyTests::petting);
    all.put("toy_skip_stone", FunToyTests::skipStone);
    all.put("toy_water_balloon", FunToyTests::waterBalloon);
    all.put("toy_bubble_wand", FunToyTests::bubbleWand);
    all.put("toy_paper_plane", FunToyTests::paperPlane);
    all.put("toy_rubber_duck", FunToyTests::rubberDuck);
    all.put("toy_snow_angel", FunToyTests::snowAngel);
    all.put("toy_switches", FunToyTests::switches);
  }

  private static ItemStack craft(GameTestHelper h, net.minecraft.world.level.ItemLike... items) {
    var list = new ArrayList<ItemStack>();
    for (var item : items) list.add(new ItemStack(item));
    var input = CraftingInput.of(items.length, 1, list);
    return h.getLevel()
        .getServer()
        .getRecipeManager()
        .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
        .map(r -> r.value().assemble(input))
        .orElse(ItemStack.EMPTY);
  }

  private static net.minecraft.world.item.Item dye(String color) {
    return BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(color + "_dye"));
  }

  private static void registered(GameTestHelper h, String path, net.minecraft.world.item.Item item) {
    h.assertTrue(BuiltInRegistries.ITEM.getValue(FunContent.id(path)) == item, "item registered: " + path);
  }

  private static ServerPlayer player(GameTestHelper h) {
    return FunTests.player(h);
  }

  // FUN11-1
  static void partyPopper(GameTestHelper h) {
    registered(h, "party_popper", ModToys.PARTY_POPPER);
    ItemStack crafted = craft(h, Items.PAPER, Items.STRING, dye("red"));
    h.assertTrue(crafted.is(ModToys.PARTY_POPPER) && crafted.getCount() == 2, "paper + string + red dye make two poppers");
    DyedItemColor color = crafted.get(DataComponents.DYED_COLOR);
    h.assertTrue(color != null && color.rgb() == 0xB02E26, "the popper keeps the dye's color: " + color);
    var p = player(h);
    ItemStack stack = crafted.copy();
    p.setItemSlot(EquipmentSlot.MAINHAND, stack);
    h.assertTrue(FunToys.pop(p, stack), "first pop");
    h.assertTrue(stack.getCount() == 1, "a pop uses the popper up");
    h.assertTrue(!FunToys.pop(p, stack) && stack.getCount() == 1, "3 s cooldown blocks the next pop");
    h.succeed();
  }

  // FUN11-2
  static void diceCoin(GameTestHelper h) {
    registered(h, "dice", ModToys.DICE);
    registered(h, "lucky_coin", ModToys.LUCKY_COIN);
    h.assertTrue(craft(h, Items.BONE_MEAL, Items.BONE_MEAL, dye("black")).is(ModToys.DICE), "dice recipe");
    h.assertTrue(craft(h, Items.COPPER_INGOT, Items.GOLD_NUGGET).is(ModToys.LUCKY_COIN), "coin recipe");
    var random = h.getLevel().getRandom();
    Set<Integer> faces = new HashSet<>(), sides = new HashSet<>();
    for (int i = 0; i < 400; i++) {
      int[] one = FunToys.rollValues(random, RollItem.Kind.DICE, false);
      h.assertTrue(one.length == 1 && one[0] >= 1 && one[0] <= 6, "die shows 1-6");
      faces.add(one[0]);
      int[] two = FunToys.rollValues(random, RollItem.Kind.DICE, true);
      h.assertTrue(two.length == 2 && two[0] >= 1 && two[1] <= 6 && two[1] >= 1 && two[0] <= 6, "two dice when sneaking");
      sides.add(FunToys.rollValues(random, RollItem.Kind.COIN, false)[0]);
    }
    h.assertTrue(faces.size() == 6 && sides.equals(Set.of(0, 1)), "every face and both coin sides come up");
    var p = player(h);
    ItemStack dice = new ItemStack(ModToys.DICE);
    p.setItemSlot(EquipmentSlot.MAINHAND, dice);
    h.assertTrue(FunToys.roll(p, dice, RollItem.Kind.DICE) != null, "roll");
    h.assertTrue(FunToys.roll(p, dice, RollItem.Kind.DICE) == null, "1 s cooldown");
    var shown =
        h.getLevel().getEntitiesOfClass(Display.TextDisplay.class, p.getBoundingBox().inflate(3));
    h.assertTrue(!shown.isEmpty() && shown.stream().noneMatch(d -> d.shouldBeSaved()), "result floats above the head and is never saved");
    shown.forEach(d -> d.discard());
    h.succeed();
  }

  // FUN11-3
  static void highFive(GameTestHelper h) {
    var a = player(h);
    var b = player(h);
    a.setShiftKeyDown(true);
    h.assertTrue(!FunToys.interact(a, b, InteractionHand.OFF_HAND), "only the main hand");
    a.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
    h.assertTrue(!FunToys.interact(a, b, InteractionHand.MAIN_HAND), "only with an empty hand");
    a.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    h.assertTrue(FunToys.highFive(a, b) == FunToys.HighFive.RAISED, "first click raises the hand");
    h.assertTrue(FunToys.highFive(b, a) == FunToys.HighFive.CLAPPED, "the click back claps");
    h.assertTrue(
        FunToys.stat(a, ModToys.STAT_HIGH_FIVES) == 1 && FunToys.stat(b, ModToys.STAT_HIGH_FIVES) == 1,
        "both count the high five");
    h.assertTrue(FunToys.highFive(b, a) != FunToys.HighFive.CLAPPED, "a clap closes the window");
    h.assertTrue(FunToys.highFive(a, a) == FunToys.HighFive.NONE, "no high five with oneself");
    h.succeed();
  }

  // FUN11-4
  static void petting(GameTestHelper h) {
    var owner = player(h);
    var stranger = player(h);
    var wolf = h.spawn(EntityTypes.WOLF, new BlockPos(3, 2, 3));
    h.assertTrue(!FunToys.pet(owner, wolf), "a wild wolf is not petted");
    wolf.tame(owner);
    h.assertTrue(FunToys.pet(owner, wolf), "the owner pets the wolf");
    h.assertTrue(FunToys.stat(owner, ModToys.STAT_PETS_GIVEN) == 1, "pets given counted");
    FunToys.pet(owner, wolf);
    h.assertTrue(FunToys.stat(owner, ModToys.STAT_PETS_GIVEN) == 1, "10 s per animal");
    h.assertTrue(!FunToys.pet(stranger, wolf), "only the owner");
    owner.setShiftKeyDown(true);
    boolean sitting = wolf.isOrderedToSit();
    h.assertTrue(FunToys.interact(owner, wolf, InteractionHand.MAIN_HAND), "the sneak click on an own wolf is consumed");
    h.assertTrue(wolf.isOrderedToSit() == sitting, "petting does not toggle sitting");
    wolf.discard();
    h.succeed();
  }

  // FUN11-5
  static void skipStone(GameTestHelper h) {
    registered(h, "skipping_stone", ModToys.SKIPPING_STONE);
    ItemStack crafted = craft(h, Items.COBBLESTONE);
    h.assertTrue(crafted.is(ModToys.SKIPPING_STONE) && crafted.getCount() == 4, "one cobblestone makes four flat stones");
    h.assertTrue(craft(h, Items.TUFF).is(ModToys.SKIPPING_STONE), "tuff works too");
    Vec3 hop = SkippingStoneEntity.bounce(new Vec3(1.2, -0.3, 0), 0);
    h.assertTrue(hop != null && hop.y > 0 && hop.x < 1.2, "a flat fast stone hops and slows down");
    h.assertTrue(SkippingStoneEntity.bounce(new Vec3(1.2, -0.3, 0), SkippingStoneEntity.MAX_SKIPS) == null, "at most five hops");
    h.assertTrue(SkippingStoneEntity.bounce(new Vec3(0.1, -0.1, 0), 0) == null, "too slow: sinks");
    h.assertTrue(SkippingStoneEntity.bounce(new Vec3(0.4, -1.5, 0), 0) == null, "too steep: sinks");
    // On real water: one tick over a pool turns the downward stone into a hop.
    for (int x = 1; x <= 6; x++)
      for (int z = 1; z <= 3; z++) {
        h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
      }
    var p = player(h);
    var stone = new SkippingStoneEntity(h.getLevel(), p, new ItemStack(ModToys.SKIPPING_STONE));
    Vec3 at = h.absoluteVec(new Vec3(1.5, 2.5, 2.5));
    stone.setPos(at.x, at.y, at.z);
    stone.setDeltaMovement(0.8, -0.2, 0);
    h.getLevel().addFreshEntity(stone);
    stone.tick();
    h.assertTrue(stone.skips() == 1 && stone.getDeltaMovement().y > 0, "the stone hopped on the water: " + stone.skips());
    stone.discard();
    h.succeed();
  }

  // FUN11-6
  static void waterBalloon(GameTestHelper h) {
    registered(h, "water_balloon", ModToys.WATER_BALLOON);
    ItemStack crafted = craft(h, Items.LEATHER, Items.STRING, Items.WATER_BUCKET);
    h.assertTrue(crafted.is(ModToys.WATER_BALLOON) && crafted.getCount() == 4, "leather + string + water bucket make four");
    var level = h.getLevel();
    h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
    h.setBlock(new BlockPos(2, 2, 2), Blocks.FIRE);
    h.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
    h.setBlock(new BlockPos(5, 2, 5), Blocks.FIRE);
    BlockPos fire = h.absolutePos(new BlockPos(2, 2, 2));
    BlockPos netherFire = h.absolutePos(new BlockPos(5, 2, 5));
    h.assertTrue(level.getBlockState(fire).is(Blocks.FIRE), "fire set up");
    WaterBalloonEntity.splash(level, Vec3.atCenterOf(netherFire), null, null, null, true);
    h.assertTrue(level.getBlockState(netherFire).is(Blocks.FIRE), "in the Nether it only steams");
    int changed = WaterBalloonEntity.splash(level, Vec3.atCenterOf(fire), null, null, null, false);
    h.assertTrue(changed >= 1 && !level.getBlockState(fire).is(Blocks.FIRE), "the splash douses the fire");
    h.assertTrue(level.getBlockState(fire.below()).is(Blocks.STONE), "nothing else breaks");
    h.setBlock(new BlockPos(4, 2, 2), Blocks.CAULDRON);
    BlockPos cauldron = h.absolutePos(new BlockPos(4, 2, 2));
    var hit = new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false);
    WaterBalloonEntity.splash(level, Vec3.atCenterOf(cauldron), hit, null, null, false);
    BlockState filled = level.getBlockState(cauldron);
    h.assertTrue(filled.is(Blocks.WATER_CAULDRON) && filled.getValue(LayeredCauldronBlock.LEVEL) == 1, "empty cauldron gets one level");
    WaterBalloonEntity.splash(level, Vec3.atCenterOf(cauldron), hit, null, null, false);
    h.assertTrue(level.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL) == 2, "one more level per balloon");
    var p = player(h);
    p.igniteForSeconds(5);
    WaterBalloonEntity.splash(level, p.position(), null, p, null, false);
    h.assertTrue(!p.isOnFire(), "a hit player is wet: fire out");
    var pig = h.spawn(EntityTypes.PIG, new BlockPos(6, 2, 2));
    h.assertTrue(WaterBalloonEntity.hits(p) && !WaterBalloonEntity.hits(pig), "players only, never mobs");
    pig.discard();
    var balloon = new ItemStack(ModToys.WATER_BALLOON, 2);
    p.setItemSlot(EquipmentSlot.MAINHAND, balloon);
    h.assertTrue(FunToys.throwToy(p, balloon, com.simplefun.item.ToyThrowItem.Kind.WATER_BALLOON) != null, "throw");
    h.assertTrue(FunToys.throwToy(p, balloon, com.simplefun.item.ToyThrowItem.Kind.WATER_BALLOON) == null, "5 s cooldown");
    h.succeed();
  }

  // FUN11-7
  static void bubbleWand(GameTestHelper h) {
    registered(h, "bubble_wand", ModToys.BUBBLE_WAND);
    h.assertTrue(craft(h, Items.STICK, Items.STRING, Items.WATER_BUCKET).is(ModToys.BUBBLE_WAND), "wand recipe");
    h.assertTrue(BuiltInRegistries.PARTICLE_TYPE.getValue(FunContent.id("soap_bubble")) == ModToys.SOAP_BUBBLE, "bubble particle registered");
    var p = player(h);
    ItemStack wand = new ItemStack(ModToys.BUBBLE_WAND);
    p.setItemSlot(EquipmentSlot.MAINHAND, wand);
    h.assertTrue(wand.getMaxDamage() == 64, "64 uses");
    for (int i = 0; i < FunToys.MAX_BUBBLES; i++) h.assertTrue(FunToys.blowBubble(p, wand), "bubble " + i);
    h.assertTrue(!FunToys.blowBubble(p, wand), "at most 12 bubbles per player");
    h.assertTrue(wand.getDamageValue() == FunToys.MAX_BUBBLES, "one use per bubble");
    h.succeed();
  }

  // FUN11-8
  static void paperPlane(GameTestHelper h) {
    registered(h, "paper_plane", ModToys.PAPER_PLANE);
    h.assertTrue(craft(h, Items.PAPER).is(ModToys.PAPER_PLANE), "one paper folds a plane");
    ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
    book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough("Meet at the tower"))));
    h.assertTrue("Meet at the tower".equals(FunToys.noteFrom(book)), "the first page is the note");
    h.assertTrue(FunToys.noteFrom(new ItemStack(Items.BOOK)) == null, "no note without writing");
    var p = player(h);
    p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModToys.PAPER_PLANE, 2));
    p.setItemSlot(EquipmentSlot.OFFHAND, book);
    PaperPlaneEntity plane = FunToys.throwPlane(p, InteractionHand.MAIN_HAND);
    h.assertTrue(plane != null && p.getMainHandItem().getCount() == 1, "thrown, one plane used");
    h.assertTrue(p.getOffhandItem().is(Items.WRITABLE_BOOK), "the book stays");
    var lore = plane.getItem().get(DataComponents.LORE);
    h.assertTrue(lore != null && lore.lines().getFirst().getString().equals("Meet at the tower"), "the note is folded in");
    ItemEntity landed = plane.land();
    h.assertTrue(landed != null && landed.getItem().is(ModToys.PAPER_PLANE) && plane.isRemoved(), "it lands as an item");
    h.assertTrue(FunToys.readNote(p, landed.getItem()), "sneak + use reads the note");
    landed.discard();
    h.succeed();
  }

  // FUN11-9
  static void rubberDuck(GameTestHelper h) {
    registered(h, "rubber_duck", ModToys.RUBBER_DUCK);
    h.assertTrue(craft(h, dye("yellow"), Items.SLIME_BALL, Items.GOLD_NUGGET).is(ModToys.RUBBER_DUCK), "duck recipe");
    var level = h.getLevel();
    var duck = ModToys.RUBBER_DUCK_BLOCK.defaultBlockState();
    h.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
    h.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
    h.setBlock(new BlockPos(6, 1, 2), Blocks.SOUL_SAND);
    h.assertTrue(duck.canSurvive(level, h.absolutePos(new BlockPos(2, 2, 2))), "floats on water");
    h.assertTrue(duck.canSurvive(level, h.absolutePos(new BlockPos(4, 2, 2))), "stands on the ground");
    h.assertTrue(!duck.canSurvive(level, h.absolutePos(new BlockPos(4, 4, 2))), "not in the air");
    var p = player(h);
    BlockPos soul = h.absolutePos(new BlockPos(6, 1, 2));
    var ctx = new BlockPlaceContext(p, InteractionHand.MAIN_HAND, new ItemStack(ModToys.RUBBER_DUCK),
        new BlockHitResult(Vec3.atCenterOf(soul).add(0, 0.5, 0), Direction.UP, soul, false));
    BlockState placed = ModToys.RUBBER_DUCK_BLOCK.getStateForPlacement(ctx);
    h.assertTrue(placed != null && placed.getValue(RubberDuckBlock.PIRATE), "on soul sand it is the pirate duck");
    h.setBlock(new BlockPos(2, 2, 2), duck);
    BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
    h.assertTrue(RubberDuckBlock.squeak(level, pos, level.getBlockState(pos)), "click squeaks");
    h.assertTrue(level.getBlockState(pos).getValue(RubberDuckBlock.SQUEAK), "and hops");
    h.assertTrue(!RubberDuckBlock.squeak(level, pos, level.getBlockState(pos)), "no squeak spam while it hops");
    h.assertTrue(
        level.getServer().reloadableRegistries().getLootTable(ModToys.RUBBER_DUCK_BLOCK.getLootTable().orElseThrow()) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
        "drops itself");
    h.succeed();
  }

  // FUN11-10
  static void snowAngel(GameTestHelper h) {
    h.setBlock(new BlockPos(2, 1, 2), Blocks.SNOW_BLOCK);
    var p = player(h);
    h.assertTrue(FunToys.onSnow(p), "on snow");
    h.assertTrue(!FunToys.trySnowAngel(p, 30), "3 s still are needed");
    h.assertTrue(FunToys.trySnowAngel(p, FunToys.ANGEL_STILL_TICKS) && FunToys.hasAngel(p), "the angel appears");
    h.assertTrue(FunToys.stat(p, ModToys.STAT_SNOW_ANGELS) == 1, "snow angels counted");
    h.assertTrue(!FunToys.trySnowAngel(p, FunToys.ANGEL_STILL_TICKS), "one at a time");
    h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
    var q = player(h);
    h.assertTrue(!FunToys.onSnow(q) && !FunToys.trySnowAngel(q, FunToys.ANGEL_STILL_TICKS), "no snow, no angel");
    h.assertTrue(FunToys.angelShape().size() >= 30, "angel outline");
    h.succeed();
  }

  /** Every toy switch off: nothing happens. */
  static void switches(GameTestHelper h) {
    var fun = SimplefunCommon.getConfig().fun;
    for (String key : SimplefunConfig.TOYS) {
      try {
        fun.getClass().getField(key).setBoolean(fun, false);
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(e);
      }
      h.assertTrue(!FunToys.on(key), "switch " + key);
    }
    var p = player(h);
    ItemStack popper = new ItemStack(ModToys.PARTY_POPPER);
    h.assertTrue(!FunToys.pop(p, popper) && popper.getCount() == 1, "popper inert");
    h.assertTrue(FunToys.roll(p, new ItemStack(ModToys.DICE), RollItem.Kind.DICE) == null, "dice inert");
    h.assertTrue(!FunToys.blowBubble(p, new ItemStack(ModToys.BUBBLE_WAND)), "wand inert");
    h.setBlock(new BlockPos(2, 1, 2), Blocks.SNOW_BLOCK);
    h.assertTrue(!FunToys.trySnowAngel(p, FunToys.ANGEL_STILL_TICKS), "no angel");
    h.assertTrue(FunToys.highFive(p, player(h)) == FunToys.HighFive.NONE, "no high five");
    h.succeed();
  }
}
