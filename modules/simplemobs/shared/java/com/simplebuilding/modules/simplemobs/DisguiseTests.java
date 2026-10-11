package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Deceiver stage 2 GameTests: cloak, disguise, reveal, mirror, veiled invisibility, the doubling attack. Standalone-safe. */
final class DisguiseTests {
    private static final TargetingConditions SEEK = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight();

    private DisguiseTests() {}

    private static void floor(GameTestHelper h, int from, int to) {
        for (int x = from; x < to; x++) for (int z = from; z < to; z++) h.getLevel().setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    /** A survival player standing at the relative block, facing east (+x). */
    private static ServerPlayer player(GameTestHelper h, int x, int z) {
        if (h.getLevel().getDifficulty() == Difficulty.PEACEFUL) h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer p = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(x, 1, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90f, 0f);
        p.setYHeadRot(-90f);
        return p;
    }

    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, int x, int z) {
        T m = h.spawn(type, x, 1, z);
        m.setNoAi(true);
        return m;
    }

    private static boolean sees(ServerLevel level, Mob m, ServerPlayer p) { return SEEK.test(level, m, p); }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("cloak_registered", h -> {
            var cloak = new ItemStack(DisguiseRegistry.CLOAK);
            var eq = cloak.get(DataComponents.EQUIPPABLE);
            h.assertTrue(eq != null && eq.slot() == EquipmentSlot.CHEST, "cloak sits on the chest slot like the elytra");
            h.assertTrue(cloak.getMaxDamage() == CloakLogic.DURABILITY, "240 durability");
            h.assertTrue(cloak.isValidRepairItem(new ItemStack(MobsRegistry.CLOTH)), "repaired with Deceiver Cloth");
            h.assertTrue(new ItemStack(DisguiseRegistry.MIRROR).getMaxDamage() == 64, "mirror 64 uses");
            var recipes = h.getLevel().getServer().getRecipeManager();
            for (String r : new String[] {"deceiver_cloak", "trick_mirror", "brewing/potion_invisibility_deceiver_cloth",
                    "brewing/potion_veiled_invisibility_redstone", "brewing/potion_veiled_invisibility_gunpowder", "brewing/splash_potion_veiled_invisibility_dragon_breath"}) {
                h.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("simplemobs", r))).isPresent(), "recipe " + r);
            }
            h.assertTrue(Disguise.seesThrough(EntityTypes.WARDEN)
                    && Disguise.seesThrough(EntityTypes.WITHER) && Disguise.seesThrough(MobsRegistry.HOLLOW_HOWLER), "bosses, Warden and Howler see through");
            h.succeed();
        });
        add.accept("veiled_invisibility", h -> {
            h.assertTrue(BuiltInRegistries.POTION.containsKey(DisguiseRegistry.VEILED_KEY) && BuiltInRegistries.POTION.containsKey(DisguiseRegistry.LONG_VEILED_KEY), "potions registered");
            var effect = DisguiseRegistry.VEILED.getEffects().getFirst();
            h.assertTrue(effect.is(MobEffects.INVISIBILITY) && effect.getDuration() == 3600, "vanilla invisibility 3:00");
            h.assertTrue(!effect.isVisible() && !effect.isAmbient() && effect.showIcon(), "no particles, icon shown");
            h.assertTrue(DisguiseRegistry.LONG_VEILED.getEffects().getFirst().getDuration() == 9600, "long 8:00");
            var p = player(h, 1, 1);
            p.addEffect(new MobEffectInstance(effect));
            var got = p.getEffect(MobEffects.INVISIBILITY);
            h.assertTrue(got != null && !got.isVisible(), "drinker is invisible without particles");
            h.succeed();
        });
        add.accept("cloak_detection", h -> {
            var level = h.getLevel();
            floor(h, 0, 16);
            var p = player(h, 1, 1);
            var far = mob(h, EntityTypes.ZOMBIE, 7, 1);
            var near = mob(h, EntityTypes.ZOMBIE, 3, 1);
            var warden = mob(h, EntityTypes.WARDEN, 1, 8);
            h.assertTrue(sees(level, far, p), "without cloak a zombie 6 blocks away notices the player");
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DisguiseRegistry.CLOAK));
            h.assertTrue(!sees(level, far, p), "cloaked: not noticed 6 blocks away");
            h.assertTrue(sees(level, near, p), "cloaked: noticed within 3 blocks");
            h.assertTrue(sees(level, warden, p), "the Warden sees through");
            h.succeed();
        });
        add.accept("cloak_morph", h -> {
            var level = h.getLevel();
            floor(h, 0, 12);
            var p = player(h, 1, 1);
            var cow = mob(h, EntityTypes.COW, 5, 5);
            var zombie = mob(h, EntityTypes.ZOMBIE, 3, 1);
            var skeleton = mob(h, EntityTypes.SKELETON, 1, 3);
            h.assertTrue(!Disguise.morph(p, cow), "no cloak, no disguise");
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DisguiseRegistry.CLOAK));
            h.assertTrue(Disguise.morph(p, cow) && Disguise.morphType(p) == EntityTypes.COW, "turns into a cow");
            h.assertTrue(p.getBbHeight() == EntityTypes.COW.getHeight() && p.getBbWidth() == EntityTypes.COW.getWidth(), "takes the cow's hit box: " + p.getBbHeight());
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == CloakLogic.MORPH_COST, "a disguise costs 1 durability");
            h.assertTrue(sees(level, zombie, p), "as a cow a zombie 2 blocks away still notices");
            h.assertTrue(Disguise.morph(p, zombie) && !sees(level, zombie, p), "as a zombie zombies take him for one of theirs");
            h.assertTrue(sees(level, skeleton, p), "skeletons still notice close up");
            var boss = mob(h, EntityTypes.WARDEN, 8, 8);
            h.assertTrue(!Disguise.morph(p, boss) && Disguise.morphType(p) == EntityTypes.ZOMBIE, "the Warden cannot be imitated");
            BlockPos stone = h.absolutePos(new BlockPos(10, 0, 10));
            p.gameMode.destroyBlock(stone);
            h.assertTrue(!Disguise.disguised(p) && p.getBbHeight() == 1.8f, "breaking a block ends the disguise");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DisguiseRegistry.CLOAK));
            Disguise.morph(p, zombie);
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            Disguise.tick(p);
            h.assertTrue(!Disguise.disguised(p), "taking the cloak off ends the disguise");
            h.succeed();
        });
        add.accept("cloak_player_morph", h -> {
            floor(h, 0, 8);
            var p = player(h, 1, 1);
            var other = player(h, 3, 1);
            other.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            other.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DisguiseRegistry.CLOAK));
            h.assertTrue(Disguise.morph(p, other), "turns into another player");
            String spec = Disguise.spec(p);
            h.assertTrue(CloakLogic.isPlayerSpec(spec) && other.getUUID().equals(CloakLogic.specUuid(spec)) && CloakLogic.specName(spec).equals(other.getGameProfile().name()), "skin of the other player: " + spec);
            h.assertTrue(Disguise.gear(p, 0).is(Items.DIAMOND_HELMET) && Disguise.gear(p, 4).is(Items.IRON_SWORD), "shows his armor and gear");
            h.assertTrue(p.getBbHeight() == 1.8f && Disguise.morphType(p) == null, "keeps the player box");
            var zombie = mob(h, EntityTypes.ZOMBIE, 2, 3);
            p.attack(zombie);
            h.assertTrue(!Disguise.disguised(p) && Disguise.gear(p, 0).isEmpty(), "a hit ends the disguise");
            h.succeed();
        });
        add.accept("cloak_reveal", h -> {
            var level = h.getLevel();
            floor(h, 0, 16);
            var p = player(h, 1, 1);
            p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DisguiseRegistry.CLOAK));
            var victim = mob(h, EntityTypes.ZOMBIE, 3, 1);
            var neighbour = mob(h, EntityTypes.ZOMBIE, 5, 1);
            var mid = mob(h, EntityTypes.SKELETON, 1, 8);
            var distant = mob(h, EntityTypes.SKELETON, 1, 13);
            h.assertTrue(Disguise.morph(p, victim), "disguised as a zombie");
            h.assertTrue(!sees(level, victim, p) && !sees(level, neighbour, p) && !sees(level, mid, p), "nobody notices before the attack");
            victim.hurtServer(level, level.damageSources().playerAttack(p), 1.0f);
            h.assertTrue(!Disguise.disguised(p), "the attack ends the disguise");
            h.assertTrue(sees(level, victim, p), "the attacked zombie knows him");
            h.assertTrue(sees(level, neighbour, p), "a zombie in the 5x5 around it knows him (4 blocks away)");
            h.assertTrue(sees(level, mid, p), "others notice within the raised radius (7 blocks)");
            h.assertTrue(!sees(level, distant, p), "but not as far as vanilla (12 blocks)");
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == CloakLogic.MORPH_COST + CloakLogic.REVEAL_COST, "revealing costs 10 durability");
            h.assertTrue(CloakLogic.hidden(false, false, false, 7 * 7) && !CloakLogic.hidden(false, false, true, 7 * 7), "radius rule 3 -> 8");
            h.succeed();
        });
        add.accept("trick_mirror", h -> {
            var level = h.getLevel();
            floor(h, 0, 16);
            var p = player(h, 1, 6);
            var front = h.spawn(MobsRegistry.DECEIVER, 8, 1, 6);
            var behind = h.spawn(MobsRegistry.DECEIVER, 1, 1, 13);
            front.setNoAi(true);
            behind.setNoAi(true);
            h.assertTrue(front.disguiseAs(EntityTypes.ZOMBIE) && behind.disguiseAs(EntityTypes.ZOMBIE), "both disguised");
            var fake = mob(h, EntityTypes.PIG, 6, 7);
            fake.addTag(DeceiverEntity.TAG_FAKE);
            var plain = mob(h, EntityTypes.PIG, 5, 5);
            int found = TrickMirrorItem.reveal(level, p);
            h.assertTrue(front.look() == null, "the Deceiver in the cone is unmasked");
            h.assertTrue(behind.look() == EntityTypes.ZOMBIE, "the one outside the cone stays disguised");
            h.assertTrue(fake.hasEffect(MobEffects.GLOWING) && !plain.hasEffect(MobEffects.GLOWING), "fakes glow, real mobs do not");
            h.assertTrue(found == 2, "two deceptions found: " + found);
            h.succeed();
        });
        add.accept("double_chance", h -> {
            float full = DeceiverLogic.doubleChance(200f, 200f), low = DeceiverLogic.doubleChance(40f, 200f);
            h.assertTrue(Math.abs(full - 0.05f) < 1e-4, "5 % at full health: " + full);
            h.assertTrue(Math.abs(low - 0.80f) < 1e-4 && Math.abs(DeceiverLogic.doubleChance(1f, 200f) - 0.80f) < 1e-4, "80 % from 20 % health down: " + low);
            float mid = DeceiverLogic.doubleChance(120f, 200f);
            h.assertTrue(mid > 0.2f && mid < 0.3f, "about 24 % at 60 % health: " + mid);
            float last = 0f;
            for (int hp = 200; hp >= 0; hp -= 5) {
                float c = DeceiverLogic.doubleChance(hp, 200f);
                h.assertTrue(c >= last && c <= 0.80f + 1e-4, "rises with lost health");
                last = c;
            }
            h.assertTrue(DeceiverLogic.doubleCopies(3) == 3 && DeceiverLogic.doubleCopies(12) == DeceiverLogic.DOUBLE_MAX_COPIES, "one copy per summon, at most 8");
            h.assertTrue(DeceiverLogic.doubleCopies(20) == 4 && DeceiverLogic.doubleCopies(24) == 0 && DeceiverLogic.doubleCopies(30) == 0, "never above the cap of 24");
            h.assertTrue(DeceiverLogic.DOUBLE_COOLDOWN_TICKS == 600, "30 s cooldown");
            h.succeed();
        });
        add.accept("double_cap", h -> {
            var level = h.getLevel();
            for (int x = -10; x < 30; x++) for (int z = -10; z < 30; z++) level.setBlock(h.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
            var deceiver = h.spawn(MobsRegistry.DECEIVER, 10, 1, 10);
            var p = player(h, 10, 10);
            int n = deceiver.summonNow(level, p, true);
            h.assertTrue(n > 0, "a wave");
            int total = 0;
            for (int i = 0; i < 6; i++) {
                int before = deceiver.aliveSummons();
                int made = deceiver.doubleNow(level, p);
                total += made;
                h.assertTrue(made <= DeceiverLogic.DOUBLE_MAX_COPIES, "at most 8 copies per strike: " + made);
                h.assertTrue(deceiver.aliveSummons() == before + made && deceiver.aliveSummons() <= DeceiverLogic.HARD_CAP, "never above 24: " + deceiver.aliveSummons());
            }
            h.assertTrue(total > 0, "the doubling made copies");
            var box = deceiver.getBoundingBox().inflate(40);
            for (var m : level.getEntitiesOfClass(Mob.class, box, m -> m.entityTags().contains(DeceiverEntity.TAG_SUMMON))) {
                if (m.entityTags().contains(DeceiverEntity.TAG_FAKE)) h.assertTrue(m.getHealth() <= 1.0f, "copies are fakes");
            }
            deceiver.die(level.damageSources().playerAttack(p));
            long fakes = level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m.entityTags().contains(DeceiverEntity.TAG_FAKE)).size();
            h.assertTrue(fakes == 0, "copies vanish with its death: " + fakes);
            h.succeed();
        });
    }
}
