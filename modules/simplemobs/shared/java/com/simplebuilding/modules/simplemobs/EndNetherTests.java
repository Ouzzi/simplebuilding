package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.EndGatewayFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Shellker, High Gravity and Soulfire Blaze GameTests (shared by Fabric wrappers, NeoForge and Forge). Standalone-safe. */
final class EndNetherTests {
    private EndNetherTests() {}

    private static void floor(GameTestHelper h, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("endnether_registered", h -> {
            for (var key : java.util.List.of(EndNetherMobs.SHELLKER_KEY, EndNetherMobs.SHELLKER_BULLET_KEY, EndNetherMobs.SOULFIRE_BLAZE_KEY, EndNetherMobs.SOUL_FIREBALL_KEY))
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(key.identifier()), "entity type " + key.identifier());
            for (var key : java.util.List.of(EndNetherMobs.SHELL_KEY, EndNetherMobs.HELMET_KEY, EndNetherMobs.SHELLKER_EGG_KEY, EndNetherMobs.ROD_KEY, EndNetherMobs.SOULFIRE_EGG_KEY))
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(key.identifier()), "item " + key.identifier());
            h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(EndNetherMobs.GRAVITY_KEY.identifier()), "high gravity effect");
            h.assertTrue(BuiltInRegistries.POTION.containsKey(EndNetherMobs.GRAVITY_POTION_KEY.identifier())
                    && BuiltInRegistries.POTION.containsKey(EndNetherMobs.LONG_GRAVITY_POTION_KEY.identifier()), "gravity potions");
            h.assertTrue(ShellkerEntity.createAttributes().build().getBaseValue(Attributes.MAX_HEALTH) == 120.0, "Shellker has 4x Shulker health");
            ItemStack helmet = new ItemStack(EndNetherMobs.HELMET);
            h.assertTrue(helmet.is(ItemTags.HEAD_ARMOR) && helmet.is(ItemTags.TRIMMABLE_ARMOR), "helmet is head armor and trimmable");
            var mods = helmet.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
            double[] sum = new double[4];
            mods.forEach(EquipmentSlot.HEAD, (attr, mod) -> {
                if (attr.equals(Attributes.ARMOR)) sum[0] += mod.amount();
                if (attr.equals(Attributes.ARMOR_TOUGHNESS)) sum[1] += mod.amount();
                if (attr.equals(Attributes.KNOCKBACK_RESISTANCE)) sum[2] += mod.amount();
                if (attr.equals(Attributes.FALL_DAMAGE_MULTIPLIER)) sum[3] += mod.amount();
            });
            h.assertTrue(sum[0] == 3.0 && sum[1] == 1.0 && Math.abs(sum[2] - 0.1) < 1e-6 && Math.abs(sum[3] + 0.2) < 1e-6,
                    "helmet: armor 3, toughness 1, knockback 10 %, fall damage -20 %: " + java.util.Arrays.toString(sum));
            h.assertTrue(helmet.getMaxDamage() == net.minecraft.world.item.Items.IRON_HELMET.getDefaultInstance().getMaxDamage(), "iron durability");
            h.succeed();
        });
        add.accept("endnether_rules", h -> {
            BlockPos g = new BlockPos(10, 70, 10);
            var cells = EndNetherLogic.gatewayGuardCells(g);
            h.assertTrue(cells.size() == 4 && cells.stream().allMatch(c -> c.getY() == 70 && c.distManhattan(g) == 1), "four guards beside the portal");
            h.assertTrue(EndNetherLogic.shellBlocks(true, false) && !EndNetherLogic.shellBlocks(false, false) && !EndNetherLogic.shellBlocks(true, true), "closed shell blocks");
            h.assertTrue(EndNetherLogic.cloneSucceeds(1, 0.0f) && !EndNetherLogic.cloneSucceeds(6, 0.99f), "clone like Shulkers");
            Vec3 lev = EndNetherLogic.heavyMotion(new Vec3(0, 0.5, 0), false, true);
            h.assertTrue(Math.abs(lev.y - 0.05) < 1e-9, "levitation keeps 10 %");
            h.assertTrue(EndNetherLogic.heavyMotion(new Vec3(0, 0.5, 0), false, false).y == 0.5, "no levitation, no change");
            Vec3 glide = EndNetherLogic.heavyMotion(new Vec3(1, -0.05, 0), true, false);
            h.assertTrue(glide.x < 1 && glide.y < -0.1, "elytra sinks and slows");
            h.assertTrue(EndNetherLogic.BULLET_SPEED == 0.15 * 1.5, "bullet 1.5x faster");
            h.assertTrue(EndNetherLogic.attachFace(Direction.UP, d -> d == Direction.DOWN) == Direction.DOWN
                    && EndNetherLogic.attachFace(Direction.UP, d -> false) == Direction.UP, "keeps a face, never teleports");
            h.assertTrue(EndNetherLogic.maySoulSpawn(true, true, true, 0, 0.0f, 0.2f), "valley spawn");
            h.assertFalse(EndNetherLogic.maySoulSpawn(true, false, true, 0, 0.0f, 0.2f), "only Soul Sand Valleys");
            h.assertFalse(EndNetherLogic.maySoulSpawn(true, true, false, 0, 0.0f, 0.2f), "only on soul sand/soil");
            h.assertFalse(EndNetherLogic.maySoulSpawn(true, true, true, 3, 0.0f, 0.2f), "cap");
            h.assertFalse(EndNetherLogic.maySoulSpawn(false, true, true, 0, 0.0f, 0.2f), "config off");
            h.succeed();
        });
        add.accept("shellker_shell", h -> {
            ServerLevel l = h.getLevel();
            floor(h, 0, 0, 4, 4);
            ShellkerEntity s = h.spawn(EndNetherMobs.SHELLKER, 2, 2, 2);
            s.setNoAi(true);
            h.assertTrue(s.isShut(), "starts closed (bedrock disguise)");
            h.assertFalse(s.hurtServer(l, l.damageSources().generic(), 10.0f), "closed: no damage");
            h.assertTrue(s.getHealth() == 120.0f, "full health after a closed hit");
            s.setOpen(true);
            h.assertFalse(s.isShut(), "open");
            h.assertTrue(Math.abs(s.getBoundingBox().getYsize() - 1.0) < 1e-6, "does not grow when open");
            h.assertTrue(s.hurtServer(l, l.damageSources().generic(), 10.0f), "open: takes damage");
            h.assertTrue(s.getHealth() < 120.0f, "health dropped");
            h.assertFalse(s.canBeAffected(new MobEffectInstance(EndNetherMobs.gravity(), 200)), "immune to its own gravity");
            BlockPos before = s.blockPosition();
            for (int i = 2; i <= 7; i++) s.hurtServer(l, l.damageSources().generic(), i * 10.0f); // rising hits pierce the hurt cooldown
            h.assertTrue(s.blockPosition().equals(before), "never teleports, even when weak");
            var loot = l.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("simplemobs", "entities/shellker")));
            h.assertTrue(loot != LootTable.EMPTY, "loot table loads");
            int total = 0;
            for (int i = 0; i < 200; i++) {
                var params = new LootParams.Builder(l).withParameter(LootContextParams.THIS_ENTITY, s).withParameter(LootContextParams.ORIGIN, s.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, l.damageSources().generic()).create(LootContextParamSets.ENTITY);
                int n = 0;
                for (ItemStack st : loot.getRandomItems(params)) if (st.is(EndNetherMobs.SHELL)) n += st.getCount();
                h.assertTrue(n >= 2 && n <= 3, "2-3 shells without Looting: " + n);
                total += n;
            }
            h.assertTrue(total > 440 && total < 560, "about 2.5 shells on average: " + total / 200.0);
            s.discard();
            h.succeed();
        });
        add.accept("shellker_convert", h -> {
            floor(h, 0, 0, 4, 4);
            Shulker shulker = h.spawn(EntityTypes.SHULKER, 2, 2, 2);
            shulker.setNoAi(true);
            var player = h.makeMockServerPlayerInLevel();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EndNetherMobs.SHELL, 2));
            var result = EndNetherMobs.SHELL.interactLivingEntity(player.getMainHandItem(), player, shulker, InteractionHand.MAIN_HAND);
            h.assertTrue(result.consumesAction(), "dressing a Shulker succeeds");
            h.assertTrue(shulker.isRemoved(), "the Shulker is gone");
            var shellkers = h.getLevel().getEntities(EndNetherMobs.SHELLKER, e -> e.blockPosition().equals(h.absolutePos(new BlockPos(2, 2, 2))));
            h.assertTrue(shellkers.size() == 1, "a Shellker took its place");
            h.assertTrue(EndNetherMobs.SHELL.interactLivingEntity(player.getMainHandItem(), player, shellkers.getFirst(), InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS,
                    "a Shellker cannot be dressed again");
            shellkers.forEach(e -> e.discard());
            h.succeed();
        });
        add.accept("shellker_gateway", h -> {
            ServerLevel l = h.getLevel();
            BlockPos origin = h.absolutePos(new BlockPos(4, 4, 4));
            EndGatewayFeature.delayedExitSearch().place(l, l.getChunkSource().getGenerator(), l.getRandom(), origin);
            h.assertTrue(l.getBlockState(origin).is(Blocks.END_GATEWAY), "gateway placed");
            h.assertTrue(l.getEntities(EndNetherMobs.SHELLKER, e -> e.blockPosition().closerThan(origin, 3)).isEmpty(), "no guards outside the End");
            h.assertTrue(EndNetherServer.placeGuards(l, origin) == EndNetherLogic.GATEWAY_GUARDS, "four guards");
            var guards = l.getEntities(EndNetherMobs.SHELLKER, e -> e.blockPosition().closerThan(origin, 3));
            h.assertTrue(guards.size() == 4, "four Shellkers at the gateway");
            for (ShellkerEntity g : guards) {
                h.assertTrue(g.blockPosition().getY() == origin.getY() && g.blockPosition().distManhattan(origin) == 1, "guard beside the portal");
                h.assertTrue(g.getAttachFace() == Direction.DOWN && l.getBlockState(g.blockPosition().below()).is(Blocks.BEDROCK), "sits on bedrock");
                h.assertTrue(g.isShut(), "disguised");
                g.discard();
            }
            h.succeed();
        });
        add.accept("high_gravity", h -> {
            floor(h, 0, 0, 4, 4);
            var pig = h.spawn(EntityTypes.PIG, 2, 2, 2);
            pig.setNoAi(true);
            double base = pig.getAttributeValue(Attributes.JUMP_STRENGTH);
            h.assertTrue(pig.addEffect(new MobEffectInstance(EndNetherMobs.gravity(), 200)), "effect applies");
            h.assertTrue(Math.abs(pig.getAttributeValue(Attributes.JUMP_STRENGTH) - base * EndNetherLogic.UPWARD_FACTOR) < 1e-6, "jump keeps 10 %");
            h.assertTrue(EndNetherMobs.heavy(pig), "heavy");
            pig.removeAllEffects();
            pig.setItemSlot(EquipmentSlot.HEAD, new ItemStack(EndNetherMobs.HELMET));
            h.assertFalse(pig.addEffect(new MobEffectInstance(EndNetherMobs.gravity(), 200)), "the helmet blocks High Gravity");
            h.assertFalse(EndNetherMobs.heavy(pig), "not heavy with the helmet");
            h.assertTrue(pig.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 200)), "other effects still apply");
            pig.discard();
            h.succeed();
        });
        add.accept("soulfire_blaze", h -> {
            ServerLevel l = h.getLevel();
            floor(h, 0, 0, 6, 8);
            SoulfireBlaze blaze = h.spawn(EndNetherMobs.SOULFIRE_BLAZE, 5, 2, 0);
            blaze.setNoAi(true);
            h.assertTrue(blaze.fireImmune() && blaze.isSensitiveToWater(), "fire immune, hurt by water like a Blaze");
            h.assertTrue(l.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.fromNamespaceAndPath("simplemobs", "entities/soulfire_blaze"))) != LootTable.EMPTY, "loot table loads");
            var pig = h.spawn(EntityTypes.PIG, 2, 2, 6);
            pig.setNoAi(true);
            var at = pig.position();
            SoulFireball ball = new SoulFireball(l, blaze, new Vec3(0, 0, 1));
            ball.setPos(at.x, at.y + 0.4, at.z - 2.0);
            l.addFreshEntity(ball);
            h.assertTrue(ball.getItem().has(net.minecraft.core.component.DataComponents.ITEM_MODEL), "drawn as a soul fire charge");
            h.succeedWhen(() -> h.assertTrue(pig.getRemainingFireTicks() > 0 && pig.getHealth() < pig.getMaxHealth(), "the soul fireball burns"));
        });
    }
}
