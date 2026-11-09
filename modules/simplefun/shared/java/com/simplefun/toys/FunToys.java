package com.simplefun.toys;

import com.simplefun.FunDelights;
import com.simplefun.SimplefunCommon;
import com.simplefun.entity.PaperPlaneEntity;
import com.simplefun.entity.SkippingStoneEntity;
import com.simplefun.entity.WaterBalloonEntity;
import com.simplefun.item.RollItem;
import com.simplefun.item.ToyThrowItem;
import com.simplefun.registry.ModToys;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/**
 * FUN11-1..10 (b-fun14, docs/ai/PLAN-B-FUN14-2026-11-09.md): the server side of the ten toys. Every action checks the
 * Simple Fun switch and the toy's own switch, has a cooldown and a particle cap, deals no damage and changes no block
 * except what the concept allows (the water balloon douses fire and fills a cauldron by one level). Randomness (dice,
 * coin) is the server's. Called from the items, {@code PlayerInteractMixin} (gestures) and the player tick.
 */
public final class FunToys {
  private FunToys() {}

  public static final int POPPER_COOLDOWN = 60, POPPER_PARTICLES = 40;
  public static final int ROLL_COOLDOWN = 20, ROLL_RANGE = 16, ROLL_TEXT_TICKS = 40;
  public static final int HIGH_FIVE_WINDOW = 60, HIGH_FIVE_RAISE_COOLDOWN = 20;
  public static final int PET_COOLDOWN = 200;
  public static final int BUBBLE_INTERVAL = 14, MAX_BUBBLES = 12, BUBBLE_LIFETIME = 160;
  public static final int PLANE_NOTE_MAX = 200;
  public static final int ANGEL_STILL_TICKS = 60, ANGEL_TICKS = 600, ANGEL_EMIT_EVERY = 10, ANGEL_COOLDOWN = 100;

  /** Simple Fun on and the toy's switch ({@code SimplefunConfig.Fun} field name) on. */
  public static boolean on(String key) {
    return SimplefunCommon.on() && FunDelights.enabled(key);
  }

  // ---------------------------------------------------------------- FUN11-1 party popper

  /** Confetti in the popper's colour (no colour: a mix), one use; {@code false} while cooling down. */
  public static boolean pop(ServerPlayer player, ItemStack stack) {
    if (!on("partyPopper") || player.getCooldowns().isOnCooldown(stack)) return false;
    player.getCooldowns().addCooldown(stack, POPPER_COOLDOWN);
    ServerLevel level = player.level();
    DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
    Vec3 eye = player.getEyePosition().add(player.getLookAngle().scale(0.6));
    int[] mix = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F};
    int sent = 0;
    for (int i = 0; i < POPPER_PARTICLES; i++) {
      int rgb = dyed != null ? (i % 5 == 4 ? lighter(dyed.rgb()) : dyed.rgb()) : mix[i % mix.length];
      sent +=
          level.sendParticles(
              new DustParticleOptions(rgb, 1.0f), eye.x, eye.y + 0.3, eye.z, 1, 0.5, 0.6, 0.5, 0.0);
    }
    level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6f, 1.8f);
    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    stack.consume(1, player);
    return true;
  }

  static int lighter(int rgb) {
    int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
    return ((r + 255) / 2 << 16) | ((g + 255) / 2 << 8) | (b + 255) / 2;
  }

  // ---------------------------------------------------------------- FUN11-2 dice and coin

  /** The server's roll: dice 1–6 (two dice when sneaking), coin 0 = heads, 1 = tails. */
  public static int[] rollValues(RandomSource random, RollItem.Kind kind, boolean twoDice) {
    if (kind == RollItem.Kind.COIN) return new int[] {random.nextInt(2)};
    return twoDice
        ? new int[] {1 + random.nextInt(6), 1 + random.nextInt(6)}
        : new int[] {1 + random.nextInt(6)};
  }

  /** Rolls, shows the result above the head and in the chat of every player within 16 blocks; null while cooling down. */
  public static int[] roll(ServerPlayer player, ItemStack stack, RollItem.Kind kind) {
    if (!on("diceAndCoin") || player.getCooldowns().isOnCooldown(stack)) return null;
    player.getCooldowns().addCooldown(stack, ROLL_COOLDOWN);
    ServerLevel level = player.level();
    int[] values = rollValues(level.getRandom(), kind, player.isShiftKeyDown());
    Component result;
    if (kind == RollItem.Kind.COIN) {
      result = Component.translatable(values[0] == 0 ? "simplefun.toys.coin.heads" : "simplefun.toys.coin.tails");
    } else if (values.length == 2) {
      result =
          Component.translatable(
              "simplefun.toys.dice.two", values[0], values[1], values[0] + values[1]);
    } else {
      result = Component.literal(String.valueOf(values[0]));
    }
    Component line =
        Component.translatable(
                kind == RollItem.Kind.COIN ? "simplefun.toys.coin.chat" : "simplefun.toys.dice.chat",
                player.getDisplayName(),
                result.copy().withStyle(ChatFormatting.GOLD))
            .withStyle(ChatFormatting.GRAY);
    for (ServerPlayer other : level.players())
      if (other.distanceToSqr(player) <= ROLL_RANGE * ROLL_RANGE) other.sendSystemMessage(line);
    floatText(level, player.position().add(0, player.getBbHeight() + 0.5, 0), result.copy().withStyle(ChatFormatting.GOLD));
    level.playSound(
        null,
        player.blockPosition(),
        kind == RollItem.Kind.COIN ? SoundEvents.CHAIN_HIT : SoundEvents.BONE_BLOCK_HIT,
        SoundSource.PLAYERS,
        0.7f,
        kind == RollItem.Kind.COIN ? 1.8f : 1.4f);
    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    return values;
  }

  /** A text that floats up for two seconds and is never saved with the chunk. */
  public static Display.TextDisplay floatText(ServerLevel level, Vec3 at, Component text) {
    Display.TextDisplay display =
        new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level) {
          @Override
          public void tick() {
            super.tick();
            if (tickCount > ROLL_TEXT_TICKS) discard();
            else setPos(getX(), getY() + 0.01, getZ());
          }

          @Override
          public boolean shouldBeSaved() {
            return false;
          }
        };
    CompoundTag tag = new CompoundTag();
    tag.put(
        "text",
        ComponentSerialization.CODEC
            .encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), text)
            .getOrThrow());
    tag.putString("billboard", "center");
    tag.putBoolean("shadow", true);
    tag.putInt("background", 0);
    display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    display.setPos(at.x, at.y, at.z);
    level.addFreshEntity(display);
    return display;
  }

  // ---------------------------------------------------------------- FUN11-3 / FUN11-4 gestures

  private record Raised(UUID target, long until) {}

  private static final Map<ServerPlayer, Raised> RAISED = new WeakHashMap<>();
  private static final Map<ServerPlayer, Long> NEXT_RAISE = new WeakHashMap<>();
  private static final Map<Entity, Long> NEXT_PET = new WeakHashMap<>();

  public enum HighFive {
    NONE,
    RAISED,
    CLAPPED
  }

  /**
   * Sneak + empty main hand on another entity: a player gets a high five, an own tamed animal is petted. {@code true}
   * consumes the click (no Vanilla follow-up such as sit toggling); horses keep their Vanilla sneak click.
   */
  public static boolean interact(ServerPlayer actor, Entity target, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND || !actor.isShiftKeyDown() || !actor.getMainHandItem().isEmpty()
        || actor.isSpectator()) return false;
    if (target instanceof ServerPlayer other) return highFive(actor, other) != HighFive.NONE;
    if (pet(actor, target)) return target instanceof TamableAnimal;
    return false;
  }

  /** First click raises the hand (3 s window), the other player's click back claps. */
  public static HighFive highFive(ServerPlayer actor, ServerPlayer other) {
    if (!on("highFive") || actor == other) return HighFive.NONE;
    ServerLevel level = actor.level();
    long now = level.getGameTime();
    Raised theirs = RAISED.get(other);
    if (theirs != null && theirs.target().equals(actor.getUUID()) && now <= theirs.until()) {
      RAISED.remove(other);
      RAISED.remove(actor);
      Vec3 mid = actor.getEyePosition().add(other.getEyePosition()).scale(0.5);
      level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y + 0.2, mid.z, 12, 0.15, 0.15, 0.15, 0.2);
      for (ServerPlayer p : List.of(actor, other)) {
        p.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        level.sendParticles(ParticleTypes.WAX_OFF, p.getX(), p.getEyeY() + 0.3, p.getZ(), 4, 0.2, 0.1, 0.2, 0.0);
        p.awardStat(ModToys.STAT_HIGH_FIVES);
        p.sendOverlayMessage(
            Component.translatable(
                "simplefun.toys.high_five.done",
                (p == actor ? other : actor).getDisplayName(),
                p.getStats().getValue(Stats.CUSTOM.get(ModToys.STAT_HIGH_FIVES))));
      }
      level.playSound(null, BlockPos.containing(mid), ModToys.HIGH_FIVE_CLAP, SoundSource.PLAYERS, 1.0f, 0.95f + level.getRandom().nextFloat() * 0.1f);
      return HighFive.CLAPPED;
    }
    if (now < NEXT_RAISE.getOrDefault(actor, Long.MIN_VALUE)) return HighFive.RAISED;
    NEXT_RAISE.put(actor, now + HIGH_FIVE_RAISE_COOLDOWN);
    RAISED.put(actor, new Raised(other.getUUID(), now + HIGH_FIVE_WINDOW));
    actor.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
    Vec3 hand = actor.getEyePosition().add(actor.getLookAngle().scale(0.4));
    level.sendParticles(ParticleTypes.END_ROD, hand.x, hand.y + 0.4, hand.z, 3, 0.05, 0.15, 0.05, 0.0);
    other.sendOverlayMessage(Component.translatable("simplefun.toys.high_five.raised", actor.getDisplayName()));
    return HighFive.RAISED;
  }

  /** The owner pets an own tamed animal: hearts and its contented sound, at most every 10 s per animal. */
  public static boolean pet(ServerPlayer owner, Entity target) {
    if (!on("petting") || !(target instanceof OwnableEntity ownable) || ownable.getOwner() != owner) return false;
    if (target instanceof TamableAnimal tame && !tame.isTame()) return false;
    ServerLevel level = owner.level();
    long now = level.getGameTime();
    if (now < NEXT_PET.getOrDefault(target, Long.MIN_VALUE)) return true;
    NEXT_PET.put(target, now + PET_COOLDOWN);
    level.broadcastEntityEvent(target, (byte) 7); // Vanilla taming hearts
    if (target instanceof Mob mob) mob.playAmbientSound();
    owner.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
    owner.awardStat(ModToys.STAT_PETS_GIVEN);
    return true;
  }

  // ---------------------------------------------------------------- FUN11-5 / 6 / 8 thrown toys

  /** Throws a skipping stone or water balloon; null while cooling down. */
  public static ThrowableItemProjectile throwToy(ServerPlayer player, ItemStack stack, ToyThrowItem.Kind kind) {
    if (!on(kind.key) || player.getCooldowns().isOnCooldown(stack)) return null;
    player.getCooldowns().addCooldown(stack, kind.cooldown);
    ServerLevel level = player.level();
    ThrowableItemProjectile projectile =
        kind == ToyThrowItem.Kind.SKIPPING_STONE
            ? new SkippingStoneEntity(level, player, stack.copyWithCount(1))
            : new WaterBalloonEntity(level, player, stack.copyWithCount(1));
    projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.5f, 0.5f);
    level.addFreshEntity(projectile);
    level.playSound(
        null,
        player.blockPosition(),
        SoundEvents.SNOWBALL_THROW,
        SoundSource.PLAYERS,
        0.5f,
        0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    stack.consume(1, player);
    return projectile;
  }

  /** Throws a paper plane; a book in the other hand folds its first page in as the note (the book stays). */
  public static PaperPlaneEntity throwPlane(ServerPlayer player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!on("paperPlane") || player.getCooldowns().isOnCooldown(stack)) return null;
    player.getCooldowns().addCooldown(stack, 10);
    ItemStack plane = stack.copyWithCount(1);
    String note =
        noteFrom(player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));
    if (note != null) withNote(plane, note);
    ServerLevel level = player.level();
    PaperPlaneEntity entity = new PaperPlaneEntity(level, player, plane);
    entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 0.9f, 0.5f);
    level.addFreshEntity(entity);
    level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.4f);
    stack.consume(1, player);
    return entity;
  }

  /** The first page of a book and quill or written book, cut to {@link #PLANE_NOTE_MAX} characters; null otherwise. */
  public static String noteFrom(ItemStack book) {
    String text = null;
    WritableBookContent writable = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
    if (writable != null) text = writable.getPages(false).findFirst().orElse(null);
    WrittenBookContent written = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
    if (text == null && written != null && !written.pages().isEmpty())
      text = written.pages().getFirst().raw().getString();
    if (text == null || text.isBlank()) return null;
    text = text.strip();
    return text.length() > PLANE_NOTE_MAX ? text.substring(0, PLANE_NOTE_MAX) : text;
  }

  public static ItemStack withNote(ItemStack plane, String note) {
    plane.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(note).withStyle(ChatFormatting.GRAY))));
    return plane;
  }

  /** Sneak + use: the folded note goes to the reader's chat. */
  public static boolean readNote(ServerPlayer player, ItemStack plane) {
    ItemLore lore = plane.get(DataComponents.LORE);
    if (lore == null || lore.lines().isEmpty()) {
      player.sendOverlayMessage(Component.translatable("simplefun.toys.paper_plane.empty"));
      return false;
    }
    player.sendSystemMessage(
        Component.translatable("simplefun.toys.paper_plane.note", lore.lines().getFirst()).withStyle(ChatFormatting.GRAY));
    player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f, 1.0f);
    return true;
  }

  /** A record (the most skips in one throw) kept as the stat {@code simplefun:best_stone_skips}. */
  public static void recordSkips(Entity owner, int skips) {
    if (!(owner instanceof ServerPlayer player) || skips <= 0) return;
    var stat = Stats.CUSTOM.get(ModToys.STAT_BEST_STONE_SKIPS);
    if (skips > player.getStats().getValue(stat)) player.getStats().setValue(player, stat, skips);
  }

  // ---------------------------------------------------------------- FUN11-7 bubble wand

  private static final Map<ServerPlayer, ArrayDeque<Long>> BUBBLES = new WeakHashMap<>();

  /** Bubbles of this player still in the air (spawned within the bubble lifetime). */
  public static int bubblesAlive(ServerPlayer player) {
    ArrayDeque<Long> times = BUBBLES.get(player);
    if (times == null) return 0;
    long now = player.level().getGameTime();
    while (!times.isEmpty() && times.peekFirst() + BUBBLE_LIFETIME <= now) times.pollFirst();
    return times.size();
  }

  /** One bubble in front of the face; false at the cap of {@link #MAX_BUBBLES} or with the switch off. Costs one use. */
  public static boolean blowBubble(ServerPlayer player, ItemStack wand) {
    if (!on("bubbleWand") || bubblesAlive(player) >= MAX_BUBBLES) return false;
    ServerLevel level = player.level();
    BUBBLES.computeIfAbsent(player, p -> new ArrayDeque<>()).addLast(level.getGameTime());
    Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.9));
    Vec3 drift = player.getLookAngle().scale(0.03);
    level.sendParticles(ModToys.SOAP_BUBBLE, at.x, at.y, at.z, 0, drift.x, 0.04, drift.z, 1.0);
    if (level.getRandom().nextInt(3) == 0)
      level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 0.2f, 1.8f);
    InteractionHand hand = player.getUsedItemHand();
    wand.hurtAndBreak(1, player, hand);
    return true;
  }

  // ---------------------------------------------------------------- FUN11-10 snow angel

  private static final class AngelWatch {
    long stillSince = -1;
    Vec3 stillAt;
    long readyUntil = -1;
    boolean wasOnGround;
    double lastY;
    long nextAngel;
  }

  private record Angel(ServerLevel level, Vec3 center, float yaw, float scale, long until) {}

  private static final Map<ServerPlayer, AngelWatch> WATCH = new WeakHashMap<>();
  private static final Map<ServerPlayer, Angel> ANGELS = new WeakHashMap<>();

  /** Snow under the player: a snow layer in the feet block, a snow block or powder snow below. */
  public static boolean onSnow(Player player) {
    BlockPos feet = player.blockPosition();
    BlockState here = player.level().getBlockState(feet);
    BlockState below = player.level().getBlockState(feet.below());
    return here.is(Blocks.SNOW) || here.is(Blocks.POWDER_SNOW) || below.is(Blocks.SNOW_BLOCK)
        || below.is(Blocks.POWDER_SNOW) || below.is(Blocks.SNOW);
  }

  /** Called every player tick: watches sneak-still-on-snow, then a jump makes the angel; re-sends live angels. */
  public static void tick(ServerPlayer player) {
    Angel angel = ANGELS.get(player);
    long now = player.level().getGameTime();
    if (angel != null) {
      if (now >= angel.until() || angel.level() != player.level()) ANGELS.remove(player);
      else if (now % ANGEL_EMIT_EVERY == 0) emitAngel(angel);
    }
    if (!on("snowAngel")) return;
    AngelWatch w = WATCH.computeIfAbsent(player, p -> new AngelWatch());
    boolean ground = player.onGround();
    if (player.isShiftKeyDown() && ground && onSnow(player)) {
      if (w.stillSince < 0 || w.stillAt == null || w.stillAt.distanceToSqr(player.position()) > 0.01) {
        w.stillSince = now;
        w.stillAt = player.position();
      } else if (now - w.stillSince >= ANGEL_STILL_TICKS) {
        if (w.readyUntil < now)
          player.sendOverlayMessage(Component.translatable("simplefun.toys.snow_angel.ready"));
        w.readyUntil = now + 20;
      }
    } else if (!player.isShiftKeyDown() || !ground) {
      w.stillSince = -1;
    }
    boolean jumped = w.wasOnGround && !ground && player.getY() > w.lastY + 0.05;
    if (jumped && now <= w.readyUntil) {
      w.readyUntil = -1;
      trySnowAngel(player, ANGEL_STILL_TICKS);
    }
    w.wasOnGround = ground;
    w.lastY = player.getY();
  }

  /**
   * Makes the angel when the player lay still on snow for {@code stillTicks} (≥ 3 s): a 30 s particle print of an angel
   * on the snow, larger in a snowstorm. One angel per player; false without snow, too short or cooling down.
   */
  public static boolean trySnowAngel(ServerPlayer player, long stillTicks) {
    if (!on("snowAngel") || stillTicks < ANGEL_STILL_TICKS || !onSnow(player)) return false;
    ServerLevel level = player.level();
    long now = level.getGameTime();
    AngelWatch w = WATCH.computeIfAbsent(player, p -> new AngelWatch());
    if (now < w.nextAngel) return false;
    w.nextAngel = now + ANGEL_COOLDOWN;
    BlockPos feet = player.blockPosition();
    double y = level.getBlockState(feet).is(Blocks.SNOW) ? feet.getY() + 0.15 : feet.getY() + 0.05;
    boolean storm =
        level.isRainingAt(feet.above())
            && level.getBiome(feet).value().getPrecipitationAt(feet, level.getSeaLevel()) == Biome.Precipitation.SNOW;
    Angel angel = new Angel(level, new Vec3(player.getX(), y, player.getZ()), player.getYRot(), storm ? 1.5f : 1.0f, now + ANGEL_TICKS);
    ANGELS.put(player, angel);
    emitAngel(angel);
    level.playSound(null, feet, SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    player.awardStat(ModToys.STAT_SNOW_ANGELS);
    return true;
  }

  /** Whether the player has a live angel (GameTests). */
  public static boolean hasAngel(ServerPlayer player) {
    return ANGELS.containsKey(player);
  }

  /** The angel outline in blocks (x across, z along the body; head at +z): head ring, two wing fans, the robe. */
  public static List<double[]> angelShape() {
    List<double[]> points = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      double a = Math.PI * 2 * i / 8;
      points.add(new double[] {Math.cos(a) * 0.2, 0.95 + Math.sin(a) * 0.2});
    }
    for (int side = -1; side <= 1; side += 2)
      for (int i = 0; i <= 6; i++) {
        double a = Math.toRadians(-10 + i * 14);
        points.add(new double[] {side * (0.15 + Math.cos(a) * 0.75), 0.3 + Math.sin(a) * 0.55});
      }
    for (int i = 0; i <= 5; i++) {
      double t = i / 5.0;
      points.add(new double[] {-0.2 - t * 0.35, 0.4 - t * 1.1});
      points.add(new double[] {0.2 + t * 0.35, 0.4 - t * 1.1});
    }
    for (int i = -2; i <= 2; i++) points.add(new double[] {i * 0.13, -0.7});
    return points;
  }

  private static void emitAngel(Angel angel) {
    double rad = Math.toRadians(angel.yaw());
    double cos = Math.cos(rad), sin = Math.sin(rad);
    var white = new DustParticleOptions(0xF4FBFF, 0.9f);
    for (double[] p : angelShape()) {
      double x = p[0] * angel.scale(), z = p[1] * angel.scale();
      double wx = angel.center().x + x * cos - z * sin;
      double wz = angel.center().z + x * sin + z * cos;
      angel.level().sendParticles(white, wx, angel.center().y, wz, 1, 0, 0, 0, 0);
    }
    angel.level()
        .sendParticles(ParticleTypes.SNOWFLAKE, angel.center().x, angel.center().y + 0.2, angel.center().z, 3, 0.5 * angel.scale(), 0.05, 0.5 * angel.scale(), 0.0);
  }

  /** Stat ids (GameTests). */
  public static int stat(ServerPlayer player, Identifier stat) {
    return player.getStats().getValue(Stats.CUSTOM.get(stat));
  }
}
