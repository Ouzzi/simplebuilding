package com.simplebuilding.gametest;

import com.simplebuilding.tweaks.PotionPadRules;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * The per-effect balancing of the potion pads (owner 2026-09-29, docs/TRANK-PADS.md,
 * {@link PotionPadRules}), one test per rule category: who receives an effect (everyone, owner only,
 * nobody, never a mob), how strong and how long (vanilla level, never longer than the potion), how often
 * (cooldown from the granted duration times the effect multiplier, lockout per player across pads), that
 * the potion is never used up, and that every vanilla effect has a deliberate rule.
 */
public final class PotionPadRuleTests {

    private PotionPadRuleTests() {
    }

    /**
     * Harmful effects reach only the pad's owner. A stranger on a pad with Swiftness and Poison gets the
     * Swiftness but no Poison; on a pad with only Poison a stranger does not even charge it - no effect,
     * no cooldown, so they cannot block the owner's pad - while the owner gets the Poison and starts the
     * cooldown.
     *
     * <p>What breaks it: the owner check missing from {@code grant}, harmful effects dropping out of
     * {@code #simplebuilding:potion_pad/owner_only} and their category fallback, and a stranger with
     * nothing to receive still charging the pad.
     */
    public static void harmfulEffectsReachOnlyTheOwnerAndStrangersCannotChargeThePad(GameTestHelper helper) {
        ServerPlayer owner = mockPlayer(helper, new Vec3(0.5, 3.0, 0.5));
        BlockPos mixedPos = new BlockPos(1, 1, 2);
        PotionPadBlockEntity mixed = place(helper, mixedPos, TweaksBlocks.POTION_PAD, custom(
                new MobEffectInstance(MobEffects.SPEED, 3600, 0), new MobEffectInstance(MobEffects.POISON, 900, 0)));
        mixed.setOwner(owner.getUUID());
        ServerPlayer stranger = mockPlayer(helper, onTop(mixedPos));
        stranger.removeAllEffects();
        tickPad(helper, mixedPos, 3 * PotionPadBlockEntity.stepTicks());
        helper.assertTrue(stranger.hasEffect(MobEffects.SPEED), "a stranger on a swiftness + poison pad got no swiftness");
        helper.assertFalse(stranger.hasEffect(MobEffects.POISON), "a stranger on someone else's pad was poisoned");
        helper.assertTrue(mixed.isCoolingDown(), "the stranger's full charge of the allowed swiftness started no cooldown");

        BlockPos poisonPos = new BlockPos(4, 1, 2);
        PotionPadBlockEntity poison = place(helper, poisonPos, TweaksBlocks.POTION_PAD, new PotionContents(Potions.POISON));
        poison.setOwner(owner.getUUID());
        moveTo(helper, stranger, onTop(poisonPos));
        stranger.removeAllEffects();
        tickPad(helper, poisonPos, 4 * PotionPadBlockEntity.stepTicks());
        helper.assertFalse(stranger.hasEffect(MobEffects.POISON), "a stranger on a poison pad was poisoned");
        helper.assertFalse(poison.isCoolingDown(), "a stranger who gets nothing put the owner's pad on cooldown");

        moveTo(helper, owner, onTop(poisonPos));
        owner.removeAllEffects();
        tickPad(helper, poisonPos, 3 * PotionPadBlockEntity.stepTicks());
        helper.assertTrue(owner.hasEffect(MobEffects.POISON), "the owner did not get the poison of their own pad");
        helper.assertTrue(poison.isCoolingDown(), "the owner's full charge started no cooldown");
        helper.succeed();
    }

    /**
     * Blocked effects are never given: a pad holding Saturation and Bad Omen (a command-made potion)
     * gives nothing to anyone, starts no cooldown, and still keeps the potion.
     *
     * <p>What breaks it: {@code #simplebuilding:potion_pad/blocked} losing an entry or the blocked check
     * being skipped.
     */
    public static void blockedEffectsAreNeverGivenAndStartNoCooldown(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        PotionPadBlockEntity be = place(helper, pos, TweaksBlocks.INFUSED_POTION_PAD, custom(
                new MobEffectInstance(MobEffects.SATURATION, 2400, 0), new MobEffectInstance(MobEffects.BAD_OMEN, 2400, 0)));
        ServerPlayer player = mockPlayer(helper, onTop(pos));
        player.removeAllEffects();
        tickPad(helper, pos, 4 * PotionPadBlockEntity.stepTicks());
        helper.assertTrue(player.getActiveEffects().isEmpty(), "a pad with only blocked effects gave " + player.getActiveEffects());
        helper.assertFalse(be.isCoolingDown(), "a pad with only blocked effects went on cooldown");
        helper.assertTrue(be.getStored() != null, "the pad lost its potion");
        helper.succeed();
    }

    /**
     * Levels never go above what a vanilla potion brews and durations never above the potion itself
     * (all on an Infused Potion Pad III, 120 s): Strong Regeneration gives Regeneration II for its own
     * 22.5 s; a command potion with Resistance V and Swiftness X gives Resistance IV and Swiftness II;
     * Levitation lasts at most 10 s; an infinite Night Vision gets the tier's 120 s.
     *
     * <p>What breaks it: an amplifier or duration cap missing from {@code PotionPadRules} or ignored in
     * {@code grant}.
     */
    public static void levelsAndDurationsNeverExceedVanillaOrThePotion(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, new Vec3(0.5, 3.0, 0.5));
        int tier = 120 * 20;

        PotionPadBlockEntity regen = place(helper, new BlockPos(1, 1, 1), TweaksBlocks.INFUSED_POTION_PAD, new PotionContents(Potions.STRONG_REGENERATION));
        player.removeAllEffects();
        charge(level, regen, player);
        MobEffectInstance r = player.getEffect(MobEffects.REGENERATION);
        helper.assertTrue(r != null && r.getDuration() == 450 && r.getAmplifier() == 1,
                "strong regeneration on a tier III pad gave " + r + " instead of level II for the potion's own 450 ticks");

        PotionPadBlockEntity strong = place(helper, new BlockPos(3, 1, 1), TweaksBlocks.INFUSED_POTION_PAD, custom(
                new MobEffectInstance(MobEffects.RESISTANCE, 9600, 4), new MobEffectInstance(MobEffects.SPEED, 9600, 9)));
        player.removeAllEffects();
        charge(level, strong, player);
        MobEffectInstance res = player.getEffect(MobEffects.RESISTANCE);
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        helper.assertTrue(res != null && res.getAmplifier() == 3 && res.getDuration() == tier,
                "Resistance V on a pad gave " + res + " instead of Resistance IV (strong turtle master) for 120 s");
        helper.assertTrue(speed != null && speed.getAmplifier() == 1, "Swiftness X on a pad gave " + speed + " instead of Swiftness II");

        PotionPadBlockEntity lift = place(helper, new BlockPos(5, 1, 1), TweaksBlocks.INFUSED_POTION_PAD, custom(
                new MobEffectInstance(MobEffects.LEVITATION, 2400, 0)));
        player.removeAllEffects();
        charge(level, lift, player);
        MobEffectInstance lev = player.getEffect(MobEffects.LEVITATION);
        helper.assertTrue(lev != null && lev.getDuration() == 200, "levitation on a pad lasted " + lev + " instead of 10 s");
        player.removeAllEffects();

        PotionPadBlockEntity endless = place(helper, new BlockPos(1, 1, 4), TweaksBlocks.INFUSED_POTION_PAD, custom(
                new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0)));
        charge(level, endless, player);
        MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
        helper.assertTrue(nv != null && nv.getDuration() == tier, "an infinite night vision on a pad gave " + nv + " instead of the tier's 120 s");
        helper.succeed();
    }

    /**
     * The cooldown follows what was granted: twice the granted duration times the effect's multiplier.
     * On a tier III pad: long Night Vision (120 s granted, x0.5) cools 120 s, Strong Regeneration
     * (22.5 s granted, x1.5) 67.5 s, and Healing (instant, 30 s basis, x2) 120 s - the same as on a tier I
     * pad, so a better pad never heals less often. The potion stays stored through every cycle.
     *
     * <p>What breaks it: the cooldown going back to the tier duration, a multiplier missing, or the instant
     * basis depending on the tier.
     */
    public static void theCooldownFollowsTheGrantedDurationAndTheEffectMultiplier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, new Vec3(0.5, 3.0, 0.5));
        PotionPadBlockEntity nightVision = place(helper, new BlockPos(1, 1, 1), TweaksBlocks.INFUSED_POTION_PAD, new PotionContents(Potions.LONG_NIGHT_VISION));
        charge(level, nightVision, player);
        helper.assertTrue(nightVision.getCooldown() == 2400, "long night vision on tier III cooled " + nightVision.getCooldown() + " ticks instead of 2400");

        PotionPadBlockEntity regen = place(helper, new BlockPos(3, 1, 1), TweaksBlocks.INFUSED_POTION_PAD, new PotionContents(Potions.STRONG_REGENERATION));
        charge(level, regen, player);
        helper.assertTrue(regen.getCooldown() == 1350, "strong regeneration on tier III cooled " + regen.getCooldown() + " ticks instead of 1350");

        for (Block block : List.of(TweaksBlocks.POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD)) {
            ServerPlayer patient = mockPlayer(helper, new Vec3(0.5, 3.0, 0.5));
            PotionPadBlockEntity heal = place(helper, block == TweaksBlocks.POTION_PAD ? new BlockPos(5, 1, 1) : new BlockPos(5, 1, 4),
                    block, new PotionContents(Potions.HEALING));
            charge(level, heal, patient);
            helper.assertTrue(heal.getCooldown() == 2400, block + " with healing cooled " + heal.getCooldown() + " ticks instead of 2400");
        }

        // The potion is never used up: after the cooldown the same pad gives the same effect again.
        nightVision.setCooldown(1);
        BlockPos abs = nightVision.getBlockPos();
        PotionPadBlockEntity.serverTick(level, abs, level.getBlockState(abs), nightVision);
        player.removeAllEffects();
        charge(level, nightVision, player);
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION) && nightVision.getStored() != null
                        && nightVision.getStored().is(Potions.LONG_NIGHT_VISION),
                "the pad did not give its night vision a second time or lost the potion: " + nightVision.getStored());
        helper.succeed();
    }

    /**
     * Healing locks the player out of every pad for a minute: after a full charge on one healing pad, the
     * same player on a second healing pad is not healed and does not charge it (it stays ready), while
     * another player on that second pad is healed and starts its cooldown.
     *
     * <p>What breaks it: the lockout not being set or checked, or it binding the pad instead of the player.
     */
    public static void healingLocksThePlayerOutOfEveryPadForOneMinute(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = new BlockPos(1, 1, 2);
        BlockPos second = new BlockPos(4, 1, 2);
        PotionPadBlockEntity a = place(helper, first, TweaksBlocks.POTION_PAD, new PotionContents(Potions.HEALING));
        PotionPadBlockEntity b = place(helper, second, TweaksBlocks.POTION_PAD, new PotionContents(Potions.HEALING));
        ServerPlayer player = mockPlayer(helper, onTop(first));
        player.setHealth(4.0F);
        tickPad(helper, first, 3 * PotionPadBlockEntity.stepTicks());
        helper.assertTrue(player.getHealth() == 8.0F && a.isCoolingDown(), "the first pad did not heal (" + player.getHealth() + ")");
        helper.assertTrue(PotionPadRules.lockoutLeft(player.getUUID(), MobEffects.INSTANT_HEALTH, level.getGameTime()) == 1200,
                "healing set a lockout of " + PotionPadRules.lockoutLeft(player.getUUID(), MobEffects.INSTANT_HEALTH, level.getGameTime()) + " ticks instead of 1200");

        moveTo(helper, player, onTop(second));
        tickPad(helper, second, 4 * PotionPadBlockEntity.stepTicks());
        helper.assertTrue(player.getHealth() == 8.0F, "a second healing pad healed the locked out player to " + player.getHealth());
        helper.assertFalse(b.isCoolingDown(), "a locked out player charged the second pad");

        ServerPlayer other = mockPlayer(helper, onTop(second));
        other.setHealth(4.0F);
        tickPad(helper, second, 3 * PotionPadBlockEntity.stepTicks());
        helper.assertTrue(other.getHealth() == 8.0F, "another player on the second pad was not healed (" + other.getHealth() + ")");
        helper.assertTrue(b.isCoolingDown(), "the other player's full charge started no cooldown");
        helper.succeed();
    }

    /**
     * Mobs never receive anything: a zombie on a swiftness pad for four seconds gets no swiftness and the
     * pad stays ready.
     */
    public static void mobsNeverReceiveAnyPadEffect(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        PotionPadBlockEntity be = place(helper, pos, TweaksBlocks.POTION_PAD, new PotionContents(Potions.SWIFTNESS));
        Zombie zombie = spawnZombie(helper, pos);
        zombie.setNoAi(true);
        tickPad(helper, pos, 4 * PotionPadBlockEntity.stepTicks());
        helper.assertFalse(zombie.hasEffect(MobEffects.SPEED), "a zombie on the pad got swiftness");
        helper.assertFalse(be.isCoolingDown(), "a zombie charged the pad");
        zombie.discard();
        helper.succeed();
    }

    /**
     * Every vanilla effect has a deliberate rule: the blocked list is exactly the effects no potion should
     * hand out, every other harmful or neutral effect is owner only, every other beneficial effect public;
     * every table entry names a real effect; and no vanilla potion is ever weakened in level or cut below
     * its own duration on a tier III pad beyond what it lasts itself.
     */
    public static void everyVanillaEffectHasDeliberateRules(GameTestHelper helper) {
        Set<String> blocked = Set.of("minecraft:saturation", "minecraft:absorption", "minecraft:health_boost",
                "minecraft:conduit_power", "minecraft:dolphins_grace", "minecraft:hero_of_the_village",
                "minecraft:bad_omen", "minecraft:raid_omen", "minecraft:trial_omen", "minecraft:breath_of_the_nautilus");
        List<String> problems = new ArrayList<>();
        Set<String> registered = new TreeSet<>();
        BuiltInRegistries.MOB_EFFECT.listElements().forEach(holder -> {
            String id = PotionPadRules.id(holder);
            registered.add(id);
            if (!id.startsWith("minecraft:")) {
                return;
            }
            PotionPadRules.Target expected = blocked.contains(id) ? PotionPadRules.Target.BLOCKED
                    : holder.value().getCategory() == MobEffectCategory.BENEFICIAL ? PotionPadRules.Target.EVERYONE
                    : PotionPadRules.Target.OWNER_ONLY;
            if (PotionPadRules.target(holder) != expected) {
                problems.add(id + " is " + PotionPadRules.target(holder) + " instead of " + expected);
            }
        });
        for (String id : PotionPadRules.TABLE.keySet()) {
            if (!registered.contains(id)) {
                problems.add("the rule table names " + id + ", which is no registered effect");
            }
        }
        for (Potion potion : BuiltInRegistries.POTION) {
            for (MobEffectInstance effect : potion.getEffects()) {
                Holder<MobEffect> type = effect.getEffect();
                if (PotionPadRules.amplifier(type, effect.getAmplifier()) != effect.getAmplifier()) {
                    problems.add("a vanilla " + potion.name() + " potion's " + PotionPadRules.id(type) + " level "
                            + (effect.getAmplifier() + 1) + " is clamped to " + (PotionPadRules.amplifier(type, effect.getAmplifier()) + 1));
                }
                int full = PotionPadRules.fullDuration(effect, 120 * 20);
                if (full > Math.max(1, effect.getDuration())) {
                    problems.add("a " + potion.name() + " potion's " + PotionPadRules.id(type) + " lasts " + full
                            + " ticks on a pad, longer than its own " + effect.getDuration());
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "potion pad rules: " + String.join("; ", problems));
        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static PotionContents custom(MobEffectInstance... effects) {
        PotionContents contents = PotionContents.EMPTY;
        for (MobEffectInstance effect : effects) {
            contents = contents.withEffectAdded(effect);
        }
        return contents;
    }

    private static PotionPadBlockEntity place(GameTestHelper helper, BlockPos pos, Block block, PotionContents contents) {
        helper.setBlock(pos, block);
        PotionPadBlockEntity be = helper.getBlockEntity(pos, PotionPadBlockEntity.class);
        be.setStored(contents);
        return be;
    }

    /** All three ramp steps straight through {@code grant} (the player's position does not matter there). */
    private static void charge(ServerLevel level, PotionPadBlockEntity be, ServerPlayer player) {
        for (int step = 1; step <= PotionPadBlockEntity.RAMP_STEPS; step++) {
            be.grant(level, player, step);
        }
    }

    private static void tickPad(GameTestHelper helper, BlockPos pad, int ticks) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pad);
        for (int i = 0; i < ticks; i++) {
            PotionPadBlockEntity be = helper.getBlockEntity(pad, PotionPadBlockEntity.class);
            PotionPadBlockEntity.serverTick(level, abs, level.getBlockState(abs), be);
        }
    }

    private static Vec3 onTop(BlockPos pad) {
        return new Vec3(pad.getX() + 0.5, pad.getY() + 1.0 / 16.0, pad.getZ() + 0.5);
    }

    private static void moveTo(GameTestHelper helper, ServerPlayer player, Vec3 relative) {
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
    }

    private static Zombie spawnZombie(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, pos);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
