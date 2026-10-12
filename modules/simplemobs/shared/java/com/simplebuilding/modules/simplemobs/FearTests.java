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
            h.assertTrue(FearLogic.auraLevel(10, true, false) == 1 && FearLogic.auraLevel(10, false, false) == 0, "I needs sight");
            h.assertTrue(FearLogic.auraLevel(4, false, false) == 2 && FearLogic.auraLevel(5.9, true, false) == 2, "II close up");
            h.assertTrue(FearLogic.auraLevel(4, true, true) == 1, "weakened: at most I");
            var s = shivering(h);
            floor(h, 0, 0, 7, 7);
            var dread = still(h.spawn(FearMobs.DREAD, 1, 2, 1));
            var near = dummy(h, 4, 1);
            var far = dummy(h, 7, 7);
            int hit = FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(near, s) == 2, "Shivering II under 6 blocks: " + level(near, s));
            h.assertTrue(level(far, s) == 1, "Shivering I in sight: " + level(far, s));
            h.assertTrue(hit >= 2, "both hit: " + hit);
            h.assertTrue(near.getEffect(s).getDuration() == FearLogic.AURA_TICKS, "short, Milk helps only until the next pulse");
            near.removeAllEffects();
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(near, s) == 2, "after Milk the aura comes back");
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
            Vec3 at = h.absoluteVec(new Vec3(3.5, 2, 1.5));
            player.snapTo(at.x, at.y, at.z, 0.0f, -20.0f);
            player.setYHeadRot(0.0f);
            h.assertTrue(watcher.seenBy(player), "looking at it");
            h.assertTrue(watcher.updateGaze(h.getLevel()) && watcher.watched(), "watched: it stands still");
            watcher.playAction(HollowWatcherEntity.ACT_CLAW, 6);
            watcher.setNoAi(false);
            watcher.tick();
            watcher.setNoAi(true);
            h.assertTrue(watcher.action() == HollowWatcherEntity.ACT_NONE, "a wind-up stops while watched");
            player.snapTo(at.x, at.y, at.z, 180.0f, 0.0f);
            player.setYHeadRot(180.0f);
            h.assertFalse(watcher.seenBy(player), "looking away");
            player.snapTo(at.x, at.y, at.z, 0.0f, -20.0f);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
            h.assertFalse(watcher.seenBy(player), "a blind player sees nothing");
            watcher.discard();
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
            h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplemobs", "brewing/potion_awkward_watcher_eye"))).isPresent(), "Focus brewing");
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
            player.addTag(FearAura.TEST_TAG);
            Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
            player.snapTo(at.x, at.y, at.z, 0.0f, 0.0f);
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 2, "Shivering II without protection: " + level(player, s));
            player.getInventory().add(new ItemStack(FearMobs.AMULET));
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 0, "the Calm Amulet in the inventory removes and blocks Shivering");
            player.getInventory().clearContent();
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 2, "without it again");
            player.addEffect(new MobEffectInstance(FearMobs.focus(), FearLogic.FOCUS_TICKS));
            FearAura.cure(player);
            h.assertTrue(level(player, s) == 0, "Focus cures Shivering");
            FearAura.pulse(h.getLevel(), dread, false);
            h.assertTrue(level(player, s) == 0, "Focus blocks the aura");
            h.assertTrue(FearMobs.focusPotion().getEffects().getFirst().getDuration() == 3600, "Focus potion 3:00");
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
