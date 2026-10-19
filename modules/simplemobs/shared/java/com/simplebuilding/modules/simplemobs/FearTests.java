package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Dread, Hollow Watcher, Shivering aura and immunity GameTests (Fabric wrappers, NeoForge, Forge). Standalone-safe. */
final class FearTests {
    private FearTests() {}

    private static void floor(GameTestHelper h, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    private static <T extends LivingEntity> T still(T e) {
        if (e instanceof net.minecraft.world.entity.Mob m) m.setNoAi(true);
        return e;
    }

    /** A tagged pig: stands in for a survival player (mock players are creative). */
    private static LivingEntity dummy(GameTestHelper h, int x, int z) {
        var pig = still(h.spawn(EntityTypes.PIG, x, 2, z));
        pig.addTag(FearAura.TEST_TAG);
        return pig;
    }

    private static Holder<MobEffect> shivering(GameTestHelper h) {
        Holder<MobEffect> s = FearMobs.shivering();
        h.assertTrue(s != null, "Shivering is registered (own or Simple Building's)");
        return s;
    }

    private static int level(LivingEntity e, Holder<MobEffect> s) {
        MobEffectInstance i = e.getEffect(s);
        return i == null ? 0 : i.getAmplifier() + 1;
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("fear_registered", h -> {
            for (var key : java.util.List.of(FearMobs.DREAD_KEY, FearMobs.WATCHER_KEY))
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(key.identifier()), "entity type " + key.identifier());
            for (var key : java.util.List.of(FearMobs.LINK_KEY, FearMobs.AMULET_KEY, FearMobs.EYE_KEY, FearMobs.DREAD_EGG_KEY, FearMobs.WATCHER_EGG_KEY))
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(key.identifier()), "item " + key.identifier());
            h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(FearMobs.FOCUS_KEY.identifier()), "focus effect");
            h.assertTrue(BuiltInRegistries.POTION.containsKey(FearMobs.FOCUS_POTION_KEY.identifier()) && BuiltInRegistries.POTION.containsKey(FearMobs.LONG_FOCUS_POTION_KEY.identifier()), "focus potions");
            boolean sb = BuiltInRegistries.MOB_EFFECT.containsKey(FearMobs.SB_SHIVERING);
            h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(FearMobs.SHIVERING_KEY.identifier()) != sb, "own Shivering exactly when Simple Building is absent (sb=" + sb + ")");
            h.assertTrue(shivering(h).is(sb ? FearMobs.SB_SHIVERING : FearMobs.SHIVERING_KEY.identifier()), "Shivering resolves to the owner's effect");
            var dread = DreadEntity.createAttributes().build();
            h.assertTrue(dread.getBaseValue(Attributes.MAX_HEALTH) == 260.0 && dread.getBaseValue(Attributes.ATTACK_DAMAGE) == 18.0, "Dread 260 health, 18 damage");
            h.assertTrue(HollowWatcherEntity.createAttributes().build().getBaseValue(Attributes.MAX_HEALTH) == 200.0, "Watcher 200 health");
            h.assertTrue(Math.abs(FearMobs.DREAD.getHeight() - 2.6f) < 1e-4 && Math.abs(FearMobs.WATCHER.getHeight() - 4.0f) < 1e-4, "2.6 and 4 blocks tall");
            h.assertTrue(new ItemStack(FearMobs.AMULET).getMaxStackSize() == 1, "amulet does not stack");
            h.succeed();
        });
        add.accept("fear_dread_summon", h -> {
            ServerLevel level = h.getLevel();
            Difficulty old = level.getDifficulty();
            if (old == Difficulty.PEACEFUL) level.getServer().setDifficulty(Difficulty.NORMAL, true);
            try {
                floor(h, 0, 0, 7, 4);
                var player = h.makeMockServerPlayerInLevel();
                player.getAbilities().instabuild = false;
                // incomplete figure (iron block instead of the anvil, the Iron Golem's T): smoke, the helmet stays
                build(h, 6, Blocks.IRON_BLOCK.defaultBlockState());
                h.assertTrue(helmetOn(h, player, new BlockPos(6, 4, 2)), "the click is used");
                h.assertTrue(h.getEntities(FearMobs.DREAD).isEmpty(), "no Dread without the anvil");
                h.assertTrue(player.getMainHandItem().is(Items.IRON_HELMET), "helmet kept on a wrong figure");
                h.assertTrue(level.getBlockState(h.absolutePos(new BlockPos(6, 4, 2))).is(Blocks.SKELETON_SKULL), "skull stays");
                // complete figure: the helmet goes on the skull and the Dread rises
                build(h, 2, Blocks.CHIPPED_ANVIL.defaultBlockState());
                helmetOn(h, player, new BlockPos(2, 4, 2));
                var dreads = h.getEntities(FearMobs.DREAD);
                h.assertTrue(dreads.size() == 1, "one Dread rises: " + dreads.size());
                h.assertTrue(dreads.getFirst().rising() && dreads.getFirst().isPersistenceRequired(), "it rises and stays");
                h.assertTrue(player.getMainHandItem().isEmpty(), "the helmet is used up");
                for (BlockPos p : java.util.List.of(new BlockPos(2, 2, 2), new BlockPos(2, 3, 2), new BlockPos(1, 3, 2), new BlockPos(3, 3, 2), new BlockPos(2, 4, 2)))
                    h.assertTrue(level.getBlockState(h.absolutePos(p)).isAir(), "figure used up at " + p);
                h.assertTrue(h.getEntities(EntityTypes.IRON_GOLEM).isEmpty(), "no Iron Golem");
                boolean on = MobsConfig.dreadSummonEnabled;
                MobsConfig.dreadSummonEnabled = false;
                build(h, 6, Blocks.ANVIL.defaultBlockState());
                h.assertFalse(DreadPattern.trySummon(level, h.absolutePos(new BlockPos(6, 4, 2))), "config off: no summon");
                MobsConfig.dreadSummonEnabled = on;
                h.getEntities(FearMobs.DREAD).forEach(e -> e.discard());
            } finally {
                if (old == Difficulty.PEACEFUL) level.getServer().setDifficulty(old, true);
            }
            h.succeed();
        });
        add.accept("fear_aura_levels", h -> {
            h.assertTrue(FearLogic.auraLevel(20, true, false) == 0, "out of range");
            h.assertTrue(FearLogic.auraLevel(10, true, false) == 2 && FearLogic.auraLevel(7, true, false) == 3 && FearLogic.auraLevel(10, false, false) == 0, "needs sight or a trigger");
            h.assertTrue(FearLogic.auraLevel(3.9, true, false) == 5 && FearLogic.auraLevel(4.0, true, false) == 4, "V under 4 blocks");
            h.assertTrue(FearLogic.auraLevel(15.9, true, false) == 1, "I at the edge");
            h.assertTrue(FearLogic.auraLevel(4, true, true) == 1, "weakened: at most I");
            var s = shivering(h);
            floor(h, 0, 0, 7, 7);
            var dread = still(h.spawn(FearMobs.DREAD, 1, 2, 1));
            var near = dummy(h, 4, 1);
            var far = dummy(h, 7, 7);
            int hit = FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(near, s) == 5, "Shivering V at 3 blocks: " + level(near, s));
            h.assertTrue(level(far, s) == 3, "Shivering III at about 8.5 blocks: " + level(far, s));
            h.assertTrue(hit >= 2, "both hit: " + hit);
            h.assertTrue(near.getEffect(s).getDuration() == FearLogic.AURA_TICKS, "short, Milk helps only until the next pulse");
            near.removeAllEffects();
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(near, s) == 5, "after Milk the aura comes back");
            dread.discard();
            h.succeed();
        });
        add.accept("fear_dread_light", h -> {
            h.assertTrue(FearLogic.dreadDamage(18f, 12) == 9f && FearLogic.dreadDamage(18f, 11) == 18f, "half damage from light 12");
            h.assertTrue(FearLogic.dreadDamage(FearLogic.DREAD_STOMP, 15) == 5f, "stomp halved too");
            floor(h, 0, 0, 4, 4);
            var dread = still(h.spawn(FearMobs.DREAD, 2, 2, 2));
            h.getLevel().setBlock(h.absolutePos(new BlockPos(0, 2, 0)), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            h.getLevel().setBlock(h.absolutePos(new BlockPos(2, 5, 2)), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            h.succeedWhen(() -> {
                h.assertTrue(dread.light() >= 12, "bright: " + dread.light());
                h.assertTrue(dread.weakened(), "weakened in bright light");
                var s = shivering(h);
                var close = dummy(h, 3, 3);
                FearAura.pulse(h.getLevel(), dread, dread.weakened());
                h.assertTrue(level(close, s) == 1, "in light the aura gives only I: " + level(close, s));
                close.discard();
                dread.discard();
            });
        });
        add.accept("fear_dread_shield", h -> {
            floor(h, 0, 0, 4, 4);
            var dread = still(h.spawn(FearMobs.DREAD, 2, 2, 2));
            h.assertTrue(dread.getSecondsToDisableBlocking() == 10.0f, "10 s shield break (axe: 5 s)");
            var player = h.makeMockServerPlayerInLevel();
            ItemStack shield = new ItemStack(Items.SHIELD);
            player.setItemInHand(InteractionHand.OFF_HAND, shield);
            var blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
            h.assertTrue(blocks != null, "shield blocks attacks");
            blocks.disable(h.getLevel(), player, dread.getSecondsToDisableBlocking(), shield);
            for (int i = 0; i < 150; i++) player.getCooldowns().tick();
            h.assertTrue(player.getCooldowns().isOnCooldown(shield), "still disabled after 7.5 s");
            for (int i = 0; i < 60; i++) player.getCooldowns().tick();
            h.assertFalse(player.getCooldowns().isOnCooldown(shield), "free again after 10 s");
            dread.discard();
            h.succeed();
        });
        add.accept("fear_dread_stomp", h -> {
            h.assertTrue(FearLogic.stompHits(3, 0, true), "on the ground in the ring");
            h.assertFalse(FearLogic.stompHits(3, 0, false), "jumping dodges");
            h.assertFalse(FearLogic.stompHits(6.5, 0, true), "outside the ring");
            floor(h, 0, 0, 7, 7);
            var dread = still(h.spawn(FearMobs.DREAD, 4, 2, 4));
            var ground = dummy(h, 4, 1);
            var air = dummy(h, 1, 4);
            ground.setOnGround(true);
            air.setOnGround(false);
            float before = ground.getHealth(), airBefore = air.getHealth();
            int hit = dread.stomp(h.getLevel());
            h.assertTrue(hit >= 1 && ground.getHealth() < before, "the one on the ground is hit: " + hit);
            h.assertTrue(air.getHealth() == airBefore, "the one in the air is not");
            dread.discard();
            h.succeed();
        });
        add.accept("fear_watcher_gaze", h -> {
            floor(h, 0, 0, 6, 7);
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 6));
            var player = h.makeMockServerPlayerInLevel();
            player.addTag(FearAura.TEST_TAG); // mock players are creative: the tag makes the aura count them
            Vec3 at = h.absoluteVec(new Vec3(3.5, 2, 1.5));
            Vec3 head = watcher.position().add(0, watcher.getEyeHeight(), 0);
            float pitch = (float) -Math.toDegrees(Math.atan2(head.y - (at.y + player.getEyeHeight()), Math.hypot(head.x - at.x, head.z - at.z)));
            FearNet.set(player, 70);
            // direct look into its eyes: no freeze, aggressive against this player
            player.snapTo(at.x, at.y, at.z, 0.0f, pitch);
            player.setYHeadRot(0.0f);
            h.assertTrue(watcher.stared(player) && !watcher.seenBy(player), "crosshair on the eyes is a direct look");
            h.assertFalse(watcher.updateGaze(h.getLevel()) || watcher.watched(), "a direct look does not freeze it");
            h.assertTrue(watcher.aggressive() && watcher.aggroPlayer() == player, "it turns aggressive against the one who looks (aggressive " + watcher.aggressive() + ", aggro " + watcher.aggroPlayer() + ", affects " + FearAura.affects(player) + ")");
            watcher.setNoAi(false);
            watcher.playAction(HollowWatcherEntity.ACT_CLAW, 6);
            watcher.tick();
            watcher.setNoAi(true);
            h.assertTrue(watcher.action() != HollowWatcherEntity.ACT_NONE || watcher.getTarget() == player, "its wind-up is not stopped");
            watcher.discard();
            // looking near it (7 degrees to the side) keeps it calm, not aggressive
            var calm = still(h.spawn(FearMobs.WATCHER, 3, 2, 6));
            player.snapTo(at.x, at.y, at.z, 7.0f, pitch);
            player.setYHeadRot(7.0f);
            h.assertTrue(calm.seenBy(player) && !calm.stared(player), "near look is the calm zone");
            h.assertTrue(calm.updateGaze(h.getLevel()) && calm.watched() && !calm.aggressive(), "near look: unobtrusive, not aggressive");
            player.snapTo(at.x, at.y, at.z, 180.0f, 0.0f);
            player.setYHeadRot(180.0f);
            h.assertFalse(calm.seenBy(player) || calm.stared(player), "looking away");
            player.snapTo(at.x, at.y, at.z, 0.0f, pitch);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
            h.assertFalse(calm.stared(player) || calm.seenBy(player), "a blind player sees nothing");
            calm.discard();
            player.removeTag(FearAura.TEST_TAG);
            h.succeed();
        });
        add.accept("fear_watcher_range", h -> {
            floor(h, 0, 0, 8, 8);
            float range = MobsConfig.watcherRange;
            var watcher = h.spawn(FearMobs.WATCHER, 1, 2, 1);
            var dummy = dummy(h, 7, 7);
            dummy.snapTo(dummy.getX(), dummy.getY(), dummy.getZ(), 0.0f, 0.0f);
            dummy.setYHeadRot(0.0f);
            watcher.setTeleportCooldown(0);
            MobsConfig.watcherRange = 8.0f;
            watcher.setTarget(dummy);
            for (int i = 0; i < 5; i++) watcher.tick();
            h.assertTrue(watcher.getTarget() == null && watcher.action() == HollowWatcherEntity.ACT_NONE, "beyond range: no target, no teleport");
            MobsConfig.watcherRange = range;
            watcher.setTarget(dummy);
            for (int i = 0; i < 3 && watcher.action() != HollowWatcherEntity.ACT_TELEPORT; i++) watcher.tick();
            h.assertTrue(watcher.action() == HollowWatcherEntity.ACT_TELEPORT, "back in range: it teleports to the player (target " + watcher.getTarget() + ", action " + watcher.action() + ", dist " + watcher.distanceTo(dummy) + ", range " + MobsConfig.watcherRange + ", behind " + watcher.behind(h.getLevel(), dummy) + ", look " + dummy.getLookAngle() + ", below " + h.getLevel().getBlockState(dummy.blockPosition().below()) + ", spotBelow " + h.getLevel().getBlockState(net.minecraft.core.BlockPos.containing(dummy.getX(), dummy.getY() - 0.5, dummy.getZ() - 2.5)) + ", free " + h.getLevel().noCollision(watcher, watcher.getDimensions(watcher.getPose()).makeBoundingBox(dummy.position().add(0, 0, -2.5))) + ")");
            watcher.discard();
            dummy.discard();
            h.succeed();
        });
        add.accept("fear_watcher_range_config", h -> {
            h.assertTrue(MobsConfig.watcherRange == 48.0f && MobsConfig.watcherTeleportSeconds > 0, "defaults: 48 blocks, interval set");
            var cfg = new com.google.gson.JsonObject();
            cfg.addProperty("watcherRange", 9999);
            cfg.addProperty("watcherGazeDirect", 50);
            MobsConfig.apply(cfg);
            h.assertTrue(MobsConfig.watcherRange == 128.0f && MobsConfig.watcherGazeDirect <= MobsConfig.watcherGazeStare, "range and direct angle are clamped");
            MobsConfig.apply(new com.google.gson.JsonObject());
            h.assertTrue(MobsConfig.watcherRange == 48.0f && MobsConfig.watcherGazeDirect == FearLogic.GAZE_DIRECT_DEG, "defaults come back");
            h.assertTrue(FearLogic.directLook(3.9, 4.0f) && !FearLogic.directLook(4.1, 4.0f), "direct look bound");
            h.succeed();
        });
        add.accept("fear_watcher_spyglass", h -> {
            floor(h, 0, 0, 6, 7);
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 7));
            var player = h.makeMockServerPlayerInLevel();
            Vec3 at = h.absoluteVec(new Vec3(3.5, 2, 1.5));
            player.snapTo(at.x, at.y, at.z, 0.0f, -18.0f);
            player.setYHeadRot(0.0f);
            h.assertFalse(watcher.scopedBy(player), "no spyglass, no freeze");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPYGLASS));
            player.startUsingItem(InteractionHand.MAIN_HAND);
            h.assertTrue(player.isScoping(), "scoping");
            watcher.playAction(HollowWatcherEntity.ACT_SCREAM, 20);
            h.assertTrue(watcher.scopedBy(player), "spyglass look");
            watcher.updateGaze(h.getLevel());
            h.assertTrue(watcher.frozen() && watcher.frozenTicks() == FearLogic.FREEZE_TICKS, "frozen for 3 s: " + watcher.frozenTicks());
            h.assertTrue(watcher.action() == HollowWatcherEntity.ACT_NONE, "the scream is cut off");
            player.stopUsingItem();
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_watcher_hit_teleport", h -> {
            floor(h, -24, -24, 28, 28);
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 3));
            Vec3 start = watcher.position();
            h.assertTrue(watcher.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 1.0f), "the hit lands");
            for (int i = 0; i < FearLogic.HIT_TELEPORT_TICKS - 2; i++) watcher.tick();
            h.assertTrue(watcher.position().distanceTo(start) < 1.0, "still there before the second is up");
            for (int i = 0; i < 4; i++) watcher.tick();
            double d = watcher.position().distanceTo(start);
            h.assertTrue(d >= FearLogic.HIT_TELEPORT_MIN - 1.0 && d <= FearLogic.HIT_TELEPORT_MAX + 1.0, "blinked 12-20 blocks away: " + d);
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_watcher_zones", h -> {
            floor(h, 0, 0, 6, 7);
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 6));
            var player = h.makeMockServerPlayerInLevel();
            Vec3 at = h.absoluteVec(new Vec3(3.5, 2, 1.5));
            Vec3 head = watcher.position().add(0, watcher.getEyeHeight(), 0);
            float pitch = (float) -Math.toDegrees(Math.atan2(head.y - (at.y + player.getEyeHeight()), Math.hypot(head.x - at.x, head.z - at.z)));
            FearNet.set(player, 70);
            // looking straight at it, then 22 / 30 / 45 degrees to the side (the angle shrinks a little with the upward pitch)
            float[] yaw = {0.0f, 7.0f, 22.0f, 30.0f, 45.0f};
            int[] want = {FearLogic.ZONE_DIRECT, FearLogic.ZONE_STARE, FearLogic.ZONE_SLOW, FearLogic.ZONE_EDGE, FearLogic.ZONE_FREE};
            for (int i = 0; i < yaw.length; i++) {
                player.snapTo(at.x, at.y, at.z, yaw[i], pitch);
                player.setYHeadRot(yaw[i]);
                h.assertTrue(watcher.gazeZone(player) == want[i], "yaw " + yaw[i] + " gives zone " + want[i] + " but " + watcher.gazeZone(player) + " (angle " + watcher.gazeAngle(player) + ")");
            }
            h.assertTrue(watcher.seenBy(player) == false, "45 degrees off is not a stare");
            player.snapTo(at.x, at.y, at.z, 22.0f, pitch);
            FearNet.set(player, 30);
            h.assertTrue(watcher.gazeZone(player) == FearLogic.ZONE_FREE, "a narrow FOV (30) shrinks the zones: 22 degrees is free");
            player.snapTo(at.x, at.y, at.z, 45.0f, pitch);
            FearNet.set(player, 200);
            h.assertTrue(watcher.gazeZone(player) == FearLogic.ZONE_EDGE, "FOV is clamped to 110: 45 degrees is the edge zone, not free");
            FearNet.forget(player);
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_watcher_zone_logic", h -> {
            h.assertTrue(FearLogic.clampFov(10) == 30 && FearLogic.clampFov(500) == 110 && FearLogic.clampFov(70) == 70, "FOV clamp 30..110");
            h.assertTrue(FearLogic.gazeZone(10, 70, 10, 25, 35) == FearLogic.ZONE_STARE && FearLogic.gazeZone(10.5, 70, 10, 25, 35) == FearLogic.ZONE_SLOW, "10 degrees at FOV 70");
            h.assertTrue(FearLogic.gazeZone(25, 70, 10, 25, 35) == FearLogic.ZONE_SLOW && FearLogic.gazeZone(35, 70, 10, 25, 35) == FearLogic.ZONE_EDGE && FearLogic.gazeZone(36, 70, 10, 25, 35) == FearLogic.ZONE_FREE, "25 and 35 degrees");
            h.assertTrue(FearLogic.gazeZone(15, 110, 10, 25, 35) == FearLogic.ZONE_STARE && FearLogic.gazeZone(16, 30, 10, 25, 35) == FearLogic.ZONE_FREE, "zones scale with FOV");
            h.assertTrue(FearLogic.pickVictim(new double[0], 0.5f) == -1 && FearLogic.pickVictim(new double[] {5}, 0.9f) == 0, "victim pick: none / one");
            h.assertTrue(FearLogic.pickVictim(new double[] {2, 40}, 0.0f) == 0 && FearLogic.pickVictim(new double[] {2, 40}, 0.99f) == 1, "victim pick covers both ends, near ones weigh more");
            var cfg = new com.google.gson.JsonObject();
            cfg.addProperty("watcherGazeStare", 99);
            cfg.addProperty("watcherGazeSlow", 2);
            cfg.addProperty("watcherGazeEdge", "x");
            float s = MobsConfig.watcherGazeStare, sl = MobsConfig.watcherGazeSlow, e = MobsConfig.watcherGazeEdge;
            MobsConfig.apply(cfg);
            h.assertTrue(MobsConfig.watcherGazeStare == 60.0f && MobsConfig.watcherGazeSlow >= MobsConfig.watcherGazeStare && MobsConfig.watcherGazeEdge >= MobsConfig.watcherGazeSlow, "gaze config is clamped and ordered");
            MobsConfig.apply(new com.google.gson.JsonObject());
            h.assertTrue(MobsConfig.watcherGazeStare == s && MobsConfig.watcherGazeSlow == sl && MobsConfig.watcherGazeEdge == e, "defaults come back");
            h.succeed();
        });
        add.accept("fear_watcher_multi_freeze", h -> {
            floor(h, 0, 0, 8, 7);
            var watcher = still(h.spawn(FearMobs.WATCHER, 4, 2, 6));
            var a = h.makeMockServerPlayerInLevel();
            var b = h.makeMockServerPlayerInLevel();
            b.addTag(FearAura.TEST_TAG);
            Vec3 pa = h.absoluteVec(new Vec3(2.5, 2, 1.5)), pb = h.absoluteVec(new Vec3(6.5, 2, 1.5));
            Vec3 head = watcher.position().add(0, watcher.getEyeHeight(), 0);
            a.snapTo(pa.x, pa.y, pa.z, 180.0f, 0.0f);
            a.setYHeadRot(180.0f);
            float pitch = (float) -Math.toDegrees(Math.atan2(head.y - (pb.y + b.getEyeHeight()), Math.hypot(head.x - pb.x, head.z - pb.z)));
            float yaw = (float) Math.toDegrees(Math.atan2(-(head.x - pb.x), head.z - pb.z));
            b.snapTo(pb.x, pb.y, pb.z, 180.0f, 0.0f);
            b.setYHeadRot(180.0f);
            h.assertFalse(watcher.updateGaze(h.getLevel()), "nobody looks: free");
            h.assertTrue(watcher.zone() == FearLogic.ZONE_FREE, "zone free");
            b.snapTo(pb.x, pb.y, pb.z, yaw + 7.0f, pitch);
            b.setYHeadRot(yaw + 7.0f);
            h.assertTrue(watcher.updateGaze(h.getLevel()) && watcher.watched(), "one of two players looks near it: calm for all");
            h.assertTrue(watcher.looker() == b, "the one who looks is the looker");
            b.snapTo(pb.x, pb.y, pb.z, yaw, pitch);
            b.setYHeadRot(yaw);
            h.assertFalse(watcher.updateGaze(h.getLevel()), "a direct look is not calm");
            h.assertTrue(watcher.aggressive() && watcher.aggroPlayer() == b, "direct look of one of two players: aggressive against that one (aggressive " + watcher.aggressive() + ", aggro " + watcher.aggroPlayer() + ", affects " + FearAura.affects(b) + ")");
            watcher.discard();
            watcher = still(h.spawn(FearMobs.WATCHER, 4, 2, 6));
            b.snapTo(pb.x, pb.y, pb.z, yaw + 7.0f, pitch);
            b.setYHeadRot(yaw + 7.0f);
            watcher.updateGaze(h.getLevel());
            b.snapTo(pb.x, pb.y, pb.z, 180.0f, 0.0f);
            b.setYHeadRot(180.0f);
            h.assertFalse(watcher.updateGaze(h.getLevel()), "both look away: free again");
            watcher.discard();
            b.removeTag(FearAura.TEST_TAG);
            h.succeed();
        });
        add.accept("fear_watcher_hit_new_victim", h -> {
            floor(h, -24, -24, 28, 28);
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 3));
            var near = dummy(h, 8, 3);
            var far = dummy(h, -6, 3);
            Vec3 start = watcher.position();
            watcher.setTarget(near);
            h.assertTrue(watcher.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 1.0f), "the hit lands");
            for (int i = 0; i < FearLogic.HIT_TELEPORT_TICKS + 2; i++) watcher.tick();
            h.assertTrue(watcher.position().distanceTo(start) > 1.0, "it blinked away");
            // The arena reaches into neighbouring tests, so their entities can be candidates too: assert the skip, not who wins.
            // Asked directly, because after the blink the range check may already have dropped the chosen target.
            watcher.setTarget(near);
            for (int i = 0; i < 8; i++) {
                var next = watcher.pickVictim(h.getLevel());
                h.assertTrue(next != null && next != near, "with two candidates the current target is skipped: another one is chosen");
            }
            watcher.discard();
            near.discard();
            far.discard();
            h.succeed();
        });
        add.accept("fear_watcher_boat", h -> {
            floor(h, -24, -24, 28, 28);
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 3));
            var boat = h.spawn(EntityTypes.OAK_BOAT, 3, 2, 3);
            h.assertTrue(watcher.startRiding(boat, true, false) && watcher.isPassenger(), "it can ride in a boat");
            for (int i = 0; i < 5; i++) watcher.tick();
            h.assertTrue(watcher.isPassenger(), "it stays in the boat while nothing hits it");
            h.assertTrue(watcher.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 1.0f), "the hit lands");
            for (int i = 0; i < FearLogic.HIT_TELEPORT_TICKS + 2; i++) watcher.tick();
            h.assertFalse(watcher.isPassenger(), "the hit teleports it out of the boat");
            h.assertTrue(watcher.position().distanceTo(boat.position()) > 2.0, "and away from it");
            watcher.discard();
            boat.discard();
            h.succeed();
        });
        add.accept("fear_aura_decay", h -> {
            var s = shivering(h);
            floor(h, 0, 0, 7, 7);
            var near = dummy(h, 4, 1);
            near.addEffect(new net.minecraft.world.effect.MobEffectInstance(s, FearLogic.DECAY_WINDOW_TICKS, 3));
            FearAura.decay(near);
            h.assertTrue(level(near, s) == 3 && near.getEffect(s).getDuration() == FearLogic.DECAY_STEP_TICKS, "IV steps down to III: " + level(near, s));
            near.removeAllEffects();
            near.addEffect(new net.minecraft.world.effect.MobEffectInstance(s, 100, 3));
            FearAura.decay(near);
            h.assertTrue(level(near, s) == 4, "no step down while plenty of time is left");
            near.removeAllEffects();
            near.addEffect(new net.minecraft.world.effect.MobEffectInstance(s, 5, 0));
            FearAura.decay(near);
            h.assertTrue(level(near, s) == 1 && near.getEffect(s).getDuration() == 5, "level I simply runs out");
            h.succeed();
        });
        add.accept("fear_aura_triggered", h -> {
            var s = shivering(h);
            floor(h, 0, 0, 7, 7);
            var watcher = still(h.spawn(FearMobs.WATCHER, 1, 2, 1));
            var back = dummy(h, 3, 1);
            watcher.setTarget(back);
            FearAura.pulse(h.getLevel(), watcher, false);
            h.assertTrue(level(back, s) >= 4, "triggered: strong at 2 blocks: " + level(back, s));
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_watcher_scream", h -> {
            floor(h, 0, 0, 7, 4);
            var s = shivering(h);
            var watcher = still(h.spawn(FearMobs.WATCHER, 1, 2, 2));
            var near = dummy(h, 6, 2);
            int hit = watcher.scream(h.getLevel());
            h.assertTrue(hit >= 1, "the one within 12 blocks: " + hit);
            h.assertTrue(level(near, s) == 2 && near.getEffect(s).getDuration() == FearLogic.SCREAM_SHIVER_TICKS, "Shivering II for 10 s");
            h.assertTrue(near.hasEffect(MobEffects.BLINDNESS) && near.getEffect(MobEffects.BLINDNESS).getDuration() == FearLogic.SCREAM_BLIND_TICKS, "Blindness 3 s");
            h.assertTrue(FearLogic.STARE_LIMIT == 100, "staring for 5 s provokes the scream");
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_watcher_teleport", h -> {
            floor(h, 0, 0, 7, 7);
            var watcher = still(h.spawn(FearMobs.WATCHER, 1, 2, 1));
            var target = dummy(h, 4, 4);
            target.snapTo(target.getX(), target.getY(), target.getZ(), 0.0f, 0.0f);
            target.setYHeadRot(0.0f);
            Vec3 dest = watcher.behind(h.getLevel(), target);
            h.assertTrue(dest != null, "a spot behind the target");
            h.assertTrue(dest.z < target.getZ() - 2.0 && Math.abs(dest.x - target.getX()) < 0.5, "2.5 blocks behind its look (south): " + dest);
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_watcher_spawn_rules", h -> {
            h.assertTrue(FearLogic.mayWatcherSpawn(true, true, 1500, true, true, 0, 0.0f, 0.04f), "outer island spawn");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 500, true, true, 0, 0.0f, 0.04f), "not on the main island");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, false, 1500, true, true, 0, 0.0f, 0.04f), "End only");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 1500, false, true, 0, 0.0f, 0.04f), "on End stone only");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 1500, true, true, 1, 0.0f, 0.04f), "one at a time");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 1500, true, true, 0, 0.5f, 0.04f), "rare");
            h.assertFalse(FearLogic.mayWatcherSpawn(false, true, 1500, true, true, 0, 0.0f, 0.04f), "config off");
            h.assertTrue(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 0, 0.1f, 0.04f), "core biome: no distance rule, 3x chance");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 0, 0.13f, 0.04f), "core biome: still rare");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, false, 200, true, true, true, 0, 0.0f, 0.04f), "core biome: End only");
            // N66 forest rule: > ~2 min in a core biome lifts the chance to forestChance (25 %); no dwell, no lift; never outside a core
            h.assertTrue(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 0, 0.2f, 0.04f, true, 0.25f), "dwelling in the forest: may spawn at 20 %");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 0, 0.2f, 0.04f, false, 0.25f), "no dwell: stays at the core chance");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 0, 0.3f, 0.04f, true, 0.25f), "dwelling: still only a chance, not a must");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 200, false, true, true, 0, 0.0f, 0.04f, true, 0.25f), "dwell lift needs a core spot");
            h.assertFalse(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 1, 0.0f, 0.04f, true, 0.25f), "dwell: still one at a time");
            h.assertTrue(FearLogic.mayWatcherSpawn(true, true, 200, true, true, true, 0, 0.1f, 0.04f, true, 0.0f), "forest chance never lowers the core chance");
            h.assertTrue(FearLogic.FOREST_SECONDS == 120 && MobsConfig.hollowWatcherForestSeconds == 120 && MobsConfig.hollowWatcherForestChance == 0.25f, "defaults 120 s / 0.25");
            var times = new java.util.HashMap<java.util.UUID, Integer>();
            var id = java.util.UUID.randomUUID();
            for (int i = 0; i < 7; i++) FearServer.trackForest(times, id, true, 20);
            h.assertTrue(times.get(id) == 140, "uninterrupted time adds up");
            h.assertTrue(FearServer.trackForest(times, id, false, 20) == 0 && !times.containsKey(id), "leaving the core resets it");
            h.assertTrue(FearServer.trackForest(times, id, true, 20) == 20, "and it starts anew");
            h.succeed();
        });
        add.accept("fear_anim_ease", h -> {
            h.assertTrue(FearLogic.ease(0.0F) == 0.0F && FearLogic.ease(1.0F) == 1.0F && FearLogic.ease(0.5F) == 0.5F, "ends and middle fixed");
            h.assertTrue(FearLogic.ease(0.1F) < 0.1F && FearLogic.ease(0.9F) > 0.9F, "slow in, slow out");
            h.assertTrue(FearLogic.ease(-1.0F) == 0.0F && FearLogic.ease(2.0F) == 1.0F, "clamped outside 0..1");
            float last = 0.0F;
            for (int i = 1; i <= 20; i++) {
                float e = FearLogic.ease(i / 20.0F);
                h.assertTrue(e >= last, "monotonic at " + i);
                last = e;
            }
            h.assertTrue(FearLogic.RECOVER_TICKS >= 5 && FearLogic.RECOVER_TICKS <= 8, "recovery 5-8 ticks");
            h.succeed();
        });
        add.accept("fear_chain_pendulum", h -> {
            h.assertTrue(FearLogic.chainPendulum(-1.0F) == 0.0F && FearLogic.chainPendulum(0.0F) == 0.0F, "starts at rest");
            h.assertTrue(Math.abs(FearLogic.chainPendulum(2.0F)) > 0.3F, "swings out");
            h.assertTrue(Math.abs(FearLogic.chainPendulum(30.0F)) < 0.01F, "settles");
            h.succeed();
        });
        add.accept("fear_watcher_anim", h -> {
            int prev = 0;
            boolean[] seen = new boolean[FearLogic.WATCHER_POSES.length];
            for (int n = 1; n <= 200; n++) {
                int next = FearLogic.nextWatcherPose(prev, 42, n);
                h.assertTrue(next >= 0 && next < seen.length && next != prev, "pose in range and changed: " + next);
                seen[next] = true;
                prev = next;
            }
            for (boolean b : seen) h.assertTrue(b, "every pose is reached");
            h.assertTrue(FearLogic.nextWatcherPose(2, 7, 3) == FearLogic.nextWatcherPose(2, 7, 3), "deterministic");
            h.assertTrue(FearLogic.stopMotion(0.95F, 0.9F) == FearLogic.stopMotion(1.5F, 0.9F) && FearLogic.stopMotion(1.9F, 0.9F) > FearLogic.stopMotion(0.95F, 0.9F), "gait snaps to steps");
            h.assertTrue(FearLogic.legSwing((float) Math.PI / 2.0F, 1.0F) < -0.1F && FearLogic.legSwing(1.0F, 0.0F) == 0.0F, "trailing leg drags, still at speed 0");
            float r = 0.0F;
            for (int i = 0; i < 5; i++) r = FearLogic.ramp(r, 1.0F, 5.0F, 8.0F);
            h.assertTrue(Math.abs(r - 1.0F) < 1.0e-4F && FearLogic.ramp(r, 0.7F, 5.0F, 8.0F) < 1.0F && FearLogic.ramp(0.1F, 0.0F, 5.0F, 8.0F) == 0.0F, "ramp reaches goal without overshoot");
            h.succeed();
        });
        add.accept("fear_idle_gesture_waits", h -> {
            net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(7L);
            for (int i = 0; i < 200; i++) {
                int rattle = FearLogic.rattleWait(r), gesture = DeceiverLogic.gestureWait(r);
                h.assertTrue(rattle >= 160 && rattle <= 400, "rattle wait 8-20 s: " + rattle);
                h.assertTrue(gesture >= 120 && gesture <= 300, "gesture wait 6-15 s: " + gesture);
            }
            h.succeed();
        });
        add.accept("fear_drops", h -> {
            ServerLevel l = h.getLevel();
            floor(h, 0, 0, 4, 4);
            var player = h.makeMockServerPlayerInLevel();
            var tables = l.getServer().reloadableRegistries();
            var dread = still(h.spawn(FearMobs.DREAD, 1, 2, 1));
            var watcher = still(h.spawn(FearMobs.WATCHER, 3, 2, 3));
            LootTable dl = tables.getLootTable(dread.getLootTable().orElseThrow()), wl = tables.getLootTable(watcher.getLootTable().orElseThrow());
            h.assertTrue(dl != LootTable.EMPTY && wl != LootTable.EMPTY, "loot tables load");
            for (int i = 0; i < 50; i++) {
                int links = 0, eyes = 0;
                for (ItemStack st : dl.getRandomItems(params(l, dread, player))) if (st.is(FearMobs.LINK)) links += st.getCount();
                for (ItemStack st : wl.getRandomItems(params(l, watcher, player))) if (st.is(FearMobs.EYE)) eyes += st.getCount();
                h.assertTrue(links >= 2 && links <= 4, "2-4 Dread links: " + links);
                h.assertTrue(eyes == 1, "one Watcher eye: " + eyes);
            }
            var noPlayer = new LootParams.Builder(l).withParameter(LootContextParams.THIS_ENTITY, dread).withParameter(LootContextParams.ORIGIN, dread.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, l.damageSources().generic()).create(LootContextParamSets.ENTITY);
            h.assertTrue(dl.getRandomItems(noPlayer).isEmpty(), "only a player's kill drops links");
            var recipes = l.getServer().getRecipeManager();
            h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplemobs", "calm_amulet"))).isPresent(), "amulet recipe");
            // Owner N71: Focus is brewed from the warped potion with SimpleLib, from the awkward potion (nether wart) without it.
            boolean lib = BuiltInRegistries.POTION.containsKey(Identifier.fromNamespaceAndPath("simplelib", "warped"));
            String focus = lib ? "brewing/potion_warped_watcher_eye" : "brewing/potion_awkward_watcher_eye";
            h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplemobs", focus))).isPresent(), "Focus brewing " + focus);
            dread.discard();
            watcher.discard();
            h.succeed();
        });
        add.accept("fear_immunity", h -> {
            var s = shivering(h);
            floor(h, 0, 0, 6, 6);
            var dread = still(h.spawn(FearMobs.DREAD, 1, 2, 1));
            var player = h.makeMockServerPlayerInLevel();
            h.assertFalse(FearAura.affects(player), "creative players are left alone");
            player.addTag(FearAura.TEST_TAG); // mock players stay creative: the tag makes the aura count them
            Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
            player.snapTo(at.x, at.y, at.z, 0.0f, 0.0f);
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 4, "Shivering IV at 5 blocks without protection: " + level(player, s));
            player.getInventory().add(new ItemStack(FearMobs.AMULET));
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 0, "the Calm Amulet in the inventory removes and blocks Shivering");
            player.getInventory().clearContent();
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 4, "without it again");
            player.addEffect(new MobEffectInstance(FearMobs.focus(), FearLogic.FOCUS_TICKS));
            FearAura.cure(player);
            h.assertTrue(level(player, s) == 0, "Focus cures Shivering");
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 0, "Focus blocks the aura");
            h.assertTrue(FearMobs.focusPotion().getEffects().getFirst().getDuration() == 3600, "Focus potion 3:00");
            player.removeTag(FearAura.TEST_TAG);
            dread.discard();
            h.succeed();
        });
    }

    private static LootParams params(ServerLevel l, LivingEntity e, net.minecraft.world.entity.player.Player killer) {
        return new LootParams.Builder(l).withParameter(LootContextParams.THIS_ENTITY, e).withParameter(LootContextParams.ORIGIN, e.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, l.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).create(LootContextParamSets.ENTITY);
    }

    /** The Dread figure at column x (z = 2): iron block, the given center block, two chains, the wither skull on top. */
    private static void build(GameTestHelper h, int x, net.minecraft.world.level.block.state.BlockState center) {
        ServerLevel level = h.getLevel();
        level.setBlock(h.absolutePos(new BlockPos(x, 2, 2)), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(h.absolutePos(new BlockPos(x, 3, 2)), center, 3);
        level.setBlock(h.absolutePos(new BlockPos(x - 1, 3, 2)), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(h.absolutePos(new BlockPos(x + 1, 3, 2)), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(h.absolutePos(new BlockPos(x, 4, 2)), Blocks.SKELETON_SKULL.defaultBlockState(), 3);
    }

    /** The player right-clicks the skull at {@code rel} with an Iron Helmet; true if the click was used. */
    private static boolean helmetOn(GameTestHelper h, net.minecraft.world.entity.player.Player player, BlockPos rel) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HELMET));
        BlockPos abs = h.absolutePos(rel);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        return Items.IRON_HELMET.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }
}
