package com.simplefun.heads;

import com.simplefun.SimplefunCommon;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Was die Tierkoepfe von Simple Fun beim Tragen koennen (Besitzer 2026-10-02, wie die Kopf-Faehigkeiten von
 * SimpleBuilding: Spass oder ein kleiner Nutzen, nichts Ausnutzbares). Jede Faehigkeit hat einen Server-Schalter.
 *
 * <ul>
 *   <li>Kuh: ein anderer Spieler melkt den Traeger mit einem Eimer (wie eine Kuh).</li>
 *   <li>Schaf: ein anderer Spieler schert den Traeger mit einer Schere: 1-3 Wolle, dann {@link #SHEAR_COOLDOWN}
 *       Ticks Pause (die Wolle waechst nach).</li>
 *   <li>Huhn: der Traeger legt ab und zu ein Ei - so oft wie ein Huhn (alle 5-10 Minuten).</li>
 *   <li>Schwein: Trueffelnase - wer schleichend {@link #TRUFFLE_SNIFF_TICKS} Ticks auf Gras, Erde, Podsol, Myzel,
 *       Moos oder Wurzelerde steht, scharrt etwas aus (Kartoffel, Karotte, Rote-Bete-Samen, Pilze, selten eine
 *       goldene Karotte); danach {@link #TRUFFLE_COOLDOWN} Ticks Pause.</li>
 * </ul>
 */
public final class HeadAbilities {
  public static final int SHEAR_COOLDOWN = 2400;
  public static final int EGG_MIN = 6000;
  public static final int EGG_RANDOM = 6000;
  public static final int TRUFFLE_SNIFF_TICKS = 60;
  public static final int TRUFFLE_COOLDOWN = 2400;

  private static final Map<UUID, Long> SHORN = new ConcurrentHashMap<>();
  private static final Map<Player, Long> NEXT_EGG = new WeakHashMap<>();
  private static final Map<Player, Integer> SNIFFING = new WeakHashMap<>();
  private static final Map<UUID, Long> TRUFFLE_READY = new ConcurrentHashMap<>();

  private HeadAbilities() {}

  public static boolean wears(Player player, AnimalHead head) {
    return player.getItemBySlot(EquipmentSlot.HEAD).is(AnimalHeads.ITEMS.get(head));
  }

  /** Rechtsklick eines Spielers auf einen anderen ({@code Entity#interact}); null = nichts zu tun. */
  public static InteractionResult interact(Player target, Player user, InteractionHand hand) {
    var fun = SimplefunCommon.getConfig().fun;
    if (!fun.enableAnimalHeads || target == user) return null;
    ItemStack held = user.getItemInHand(hand);
    if (fun.cowHeadMilking && held.is(Items.BUCKET) && wears(target, AnimalHead.COW)) {
      if (!user.level().isClientSide()) {
        user.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
        user.setItemInHand(hand, ItemUtils.createFilledResult(held, user, Items.MILK_BUCKET.getDefaultInstance()));
      }
      return InteractionResult.SUCCESS;
    }
    if (fun.sheepHeadShearing && held.is(Items.SHEARS) && wears(target, AnimalHead.SHEEP)) {
      if (user.level() instanceof ServerLevel level) {
        long now = level.getGameTime();
        Long until = SHORN.get(target.getUUID());
        if (until != null && until > now) return InteractionResult.PASS;
        SHORN.put(target.getUUID(), now + SHEAR_COOLDOWN);
        int wool = 1 + level.getRandom().nextInt(3);
        target.spawnAtLocation(level, new ItemStack(Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE), wool), 1.0F);
        level.playSound(null, target, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        held.hurtAndBreak(1, user, hand);
      }
      return InteractionResult.SUCCESS;
    }
    return null;
  }

  /** Jeden Server-Tick des Traegers. */
  public static void tick(ServerPlayer player) {
    var fun = SimplefunCommon.getConfig().fun;
    if (!fun.enableAnimalHeads || player.isSpectator() || !(player.level() instanceof ServerLevel level)) return;
    long now = level.getGameTime();
    if (fun.chickenHeadEggs && wears(player, AnimalHead.CHICKEN)) {
      Long next = NEXT_EGG.get(player);
      if (next == null) {
        NEXT_EGG.put(player, now + EGG_MIN + level.getRandom().nextInt(EGG_RANDOM));
      } else if (now >= next) {
        layEgg(level, player);
        NEXT_EGG.put(player, now + EGG_MIN + level.getRandom().nextInt(EGG_RANDOM));
      }
    } else {
      NEXT_EGG.remove(player);
    }
    if (fun.pigHeadTruffles && wears(player, AnimalHead.PIG) && player.isShiftKeyDown() && player.onGround()
        && sniffable(level.getBlockState(player.getOnPos()))
        && TRUFFLE_READY.getOrDefault(player.getUUID(), 0L) <= now) {
      int ticks = SNIFFING.getOrDefault(player, 0) + 1;
      if (ticks >= TRUFFLE_SNIFF_TICKS) {
        dig(level, player);
        TRUFFLE_READY.put(player.getUUID(), now + TRUFFLE_COOLDOWN);
        ticks = 0;
      }
      SNIFFING.put(player, ticks);
    } else {
      SNIFFING.remove(player);
    }
  }

  public static void layEgg(ServerLevel level, Player player) {
    level.playSound(null, player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F,
        (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F);
    player.spawnAtLocation(level, new ItemStack(Items.EGG), 0.2F);
  }

  public static boolean sniffable(BlockState state) {
    return state.is(BlockTags.DIRT) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL) || state.is(Blocks.MOSS_BLOCK);
  }

  /** Was die Trueffelnase ausscharrt: gewichtet, die goldene Karotte selten. */
  public static ItemStack truffle(net.minecraft.util.RandomSource random) {
    int roll = random.nextInt(100);
    if (roll < 30) return new ItemStack(Items.POTATO);
    if (roll < 60) return new ItemStack(Items.CARROT);
    if (roll < 80) return new ItemStack(Items.BEETROOT_SEEDS);
    if (roll < 90) return new ItemStack(Items.BROWN_MUSHROOM);
    if (roll < 98) return new ItemStack(Items.RED_MUSHROOM);
    return new ItemStack(Items.GOLDEN_CARROT);
  }

  public static void dig(ServerLevel level, Player player) {
    BlockPos ground = player.getOnPos();
    level.levelEvent(2001, ground, net.minecraft.world.level.block.Block.getId(level.getBlockState(ground)));
    level.playSound(null, player, SoundEvents.SNIFFER_DIGGING, SoundSource.PLAYERS, 0.6F, 1.4F);
    player.spawnAtLocation(level, truffle(level.getRandom()), 0.3F);
  }

  /** Nur fuer Tests: die Schur-Sperre eines Traegers loeschen. */
  public static void resetForTests(Player player) {
    SHORN.remove(player.getUUID());
    TRUFFLE_READY.remove(player.getUUID());
    NEXT_EGG.remove(player);
    SNIFFING.remove(player);
  }
}
