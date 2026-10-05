package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.effect.CraftyShulkerEffect;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.fletching.ArrowParts;
import com.simplebuilding.fletching.ArrowRecovery;
import com.simplebuilding.fletching.CraftedArrow;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.mixin.AbstractArrowAccessor;
import com.simplebuilding.tweaks.item.LaserBeam;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Kampf-Welle 2026-10-02 (docs/ai/PLAN-COMBAT-2026-10-02.md): Pfeile kommen beim Tod des Ziels zurueck,
 * Shulkerkisten sind nicht verzauberbar, der Resonanzstab scannt Lebewesen, der Listige Shulker springt sicher weg.
 */
public final class CombatTests {
    private CombatTests() {
    }

    // ------------------------------------------------------------------ Pfeile

    private static ServerPlayer shooter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static Mob cow(GameTestHelper helper, BlockPos pos) {
        Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, pos);
        McVersion.setInvulnerable(cow, false);
        return cow;
    }

    /** Ein Treffer ohne Flug: Geschwindigkeit 1 (2 Schaden bei Grundschaden 2), dann onHitEntity. */
    private static void hit(AbstractArrow arrow, LivingEntity target) {
        arrow.setPos(target.getX(), target.getY() + 0.5, target.getZ() - 1.0);
        arrow.setDeltaMovement(0.0, 0.0, 1.0);
        McVersion.resetInvulnerableTime(target);
        ((AbstractArrowAccessor) arrow).simplebuilding$onHitEntity(new EntityHitResult(target));
    }

    private static List<ItemStack> stuck(LivingEntity entity) {
        return ((ArrowRecovery.Holder) entity).simplebuilding$stuckArrows();
    }

    private static int droppedMatching(GameTestHelper helper, LivingEntity near, ItemStack like) {
        int count = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, near.getBoundingBox().inflate(3.0))) {
            if (ItemStack.isSameItemSameComponents(item.getItem(), like)) {
                count += item.getItem().getCount();
                item.discard();
            }
        }
        return count;
    }

    /** Ein Spieler-Pfeil steckt im Mob; beim Tod faellt er mit allen Teilen, ein getraenkter Pfeil mit seinem Trank. */
    public static void arrowsInMobsDropWhenTheyDie(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Mob cow = cow(helper, new BlockPos(3, 2, 3));
        cow.setHealth(cow.getMaxHealth());
        ItemStack tipped = PotionContents.createItemStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.SWIFTNESS);
        hit(new Arrow(helper.getLevel(), player, tipped.copyWithCount(1), new ItemStack(Items.BOW)), cow);
        ItemStack partsStack = McVersion.FLETCHING
                ? ArrowParts.stack(new ArrowParts.Parts(ArrowParts.Tip.DIAMOND, ArrowParts.Shaft.BLAZE_ROD, ArrowParts.Fletching.FEATHER), 1)
                : new ItemStack(Items.ARROW);
        if (McVersion.FLETCHING) {
            hit(new CraftedArrow(helper.getLevel(), player, partsStack.copy(), new ItemStack(Items.BOW)), cow);
        } else {
            hit(new Arrow(helper.getLevel(), player, partsStack.copy(), new ItemStack(Items.BOW)), cow);
        }
        helper.assertTrue(cow.isAlive(), "the cow died from two arrows, so the stuck list is not tested");
        helper.assertValueEqual(stuck(cow).size(), 2, "arrows remembered by the cow");

        // Speichern und Laden behaelt die Pfeile (Chunk entladen, Server-Neustart).
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        cow.saveWithoutId(output);
        Mob loaded = cow(helper, new BlockPos(5, 2, 3));
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertValueEqual(stuck(loaded).size(), 2, "arrows after saving and loading");
        stuck(loaded).clear();
        loaded.discard();

        cow.kill(helper.getLevel());
        helper.assertValueEqual(droppedMatching(helper, cow, tipped), 1, "tipped arrows dropped by the dead cow (with their potion)");
        helper.assertValueEqual(droppedMatching(helper, cow, partsStack), 1, "crafted arrows dropped by the dead cow (with their parts)");
        helper.assertTrue(stuck(cow).isEmpty(), "the dead cow still remembers arrows");
        helper.succeed();
    }

    /** Toedlicher Treffer: der Pfeil faellt sofort. */
    public static void killingArrowsDropAtOnce(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Mob cow = cow(helper, new BlockPos(3, 2, 3));
        cow.setHealth(1.0F);
        hit(new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW)), cow);
        helper.assertFalse(cow.isAlive(), "the arrow did not kill the cow");
        helper.assertValueEqual(droppedMatching(helper, cow, new ItemStack(Items.ARROW)), 1, "arrows dropped by the killing hit");
        helper.succeed();
    }

    /**
     * Keine Vermehrung: Unendlichkeit/Kreativ (CREATIVE_ONLY), Pfeile von Skeletten, durchbohrende Pfeile, Treffer auf
     * Spieler, Amethyst-Spitzen und der Serverschalter aus geben keinen Pfeil zurueck.
     */
    public static void onlyPickableArrowsFromPlayersComeBack(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Mob cow = cow(helper, new BlockPos(3, 2, 3));
        ServerLevel level = helper.getLevel();

        Arrow infinity = new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        infinity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        helper.assertFalse(ArrowRecovery.recoverable(infinity, cow), "an Infinity/Creative arrow comes back");

        Mob skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(1, 2, 1));
        Arrow fromSkeleton = new Arrow(level, skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        helper.assertFalse(ArrowRecovery.recoverable(fromSkeleton, cow), "a skeleton's arrow comes back");
        skeleton.discard();

        Arrow piercing = new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        ((AbstractArrowAccessor) piercing).simplebuilding$setPierceLevel((byte) 1);
        helper.assertFalse(ArrowRecovery.recoverable(piercing, cow), "a piercing arrow (it flies on) is also remembered");

        ServerPlayer target = shooter(helper);
        helper.assertFalse(ArrowRecovery.recoverable(new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW)), target),
                "an arrow in a player is remembered");

        if (McVersion.FLETCHING) {
            CraftedArrow amethyst = new CraftedArrow(level, player,
                    ArrowParts.stack(new ArrowParts.Parts(ArrowParts.Tip.AMETHYST, ArrowParts.Shaft.STICK, ArrowParts.Fletching.FEATHER), 1), null);
            helper.assertFalse(ArrowRecovery.recoverable(amethyst, cow), "a shattering amethyst arrow comes back");
        }

        Arrow normal = new Arrow(level, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        helper.assertTrue(ArrowRecovery.recoverable(normal, cow), "a plain player arrow does not come back, so the checks above prove nothing");
        var arrows = ServerTuning.get().arrows;
        boolean saved = arrows.recoverFromMobs;
        try {
            arrows.recoverFromMobs = false;
            helper.assertFalse(ArrowRecovery.recoverable(normal, cow), "arrows come back with server.arrows.recoverFromMobs off");
        } finally {
            arrows.recoverFromMobs = saved;
        }

        // Obergrenze je Lebewesen.
        if (McVersion.GADGET_REWORK) {
            double chance = arrows.recoveryChance;
            try {
                stuck(cow).clear();
                arrows.recoveryChance = 0;
                ArrowRecovery.onHit(normal, cow);
                helper.assertTrue(stuck(cow).isEmpty(), "zero recovery chance still saves arrows");
                arrows.recoveryChance = 1;
                ArrowRecovery.onHit(normal, cow);
                helper.assertValueEqual(stuck(cow).size(), 1, "full recovery chance loses an eligible arrow");
            } finally { arrows.recoveryChance = chance; stuck(cow).clear(); }
        }
        int max = ServerTuning.arrowsPerMob();
        cow.setHealth(cow.getMaxHealth());
        stuck(cow).clear();
        for (int i = 0; i < max; i++) {
            stuck(cow).add(new ItemStack(Items.ARROW));
        }
        ArrowRecovery.onHit(normal, cow);
        helper.assertValueEqual(stuck(cow).size(), max, "arrows over the per-mob cap");
        stuck(cow).clear();
        helper.succeed();
    }

    // ------------------------------------------------------------------ Shulkerkisten

    /** Besitzer 2026-10-02: keine Shulkerkiste (Vanilla, Farben, Mod-Stufen) laesst sich verzaubern. */
    public static void shulkerBoxesCannotBeEnchanted(GameTestHelper helper) {
        List<Holder.Reference<Enchantment>> enchantments = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).listElements().toList();
        int boxes = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem block) || !(block.getBlock() instanceof ShulkerBoxBlock)) {
                continue;
            }
            boxes++;
            ItemStack stack = new ItemStack(item);
            helper.assertFalse(stack.has(DataComponents.ENCHANTABLE), BuiltInRegistries.ITEM.getKey(item) + " is enchantable at the table");
            for (Holder.Reference<Enchantment> enchantment : enchantments) {
                helper.assertFalse(enchantment.value().isSupportedItem(stack),
                        BuiltInRegistries.ITEM.getKey(item) + " accepts " + enchantment.key().identifier() + " in the anvil");
            }
        }
        helper.assertTrue(boxes >= 20, "found only " + boxes + " shulker boxes (17 vanilla + 3 tiers expected)");
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ModItems.ENDERITE_SHULKER_BOX).getNamespace().equals(Simplebuilding.MOD_ID),
                "the enderite shulker box is not registered");
        helper.succeed();
    }

    // ------------------------------------------------------------------ Resonanzstab

    /**
     * Scannen (2026-10-02): ein feuerfester Mob leuchtet nach der einfachen Verweildauer (brennt aber nicht), ein
     * anderer Spieler ohne {@code scanPlayers} nie; mit Schalter aus leuchtet nichts.
     */
    public static void theResonanceRodScansCreaturesButNotPlayers(GameTestHelper helper) {
        ServerPlayer player = shooter(helper);
        Vec3 eye = helper.absoluteVec(new Vec3(2.5, 2.0, 6.5));
        player.snapTo(eye.x, eye.y, eye.z, 180.0F, 0.0F);
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        Mob blaze = helper.spawnWithNoFreeWill(EntityTypes.BLAZE, new BlockPos(2, 2, 2));
        var switches = ServerTuning.get().laser;
        boolean scan = switches.scanEntities;
        int scanInterval = switches.scanIntervalTicks;
        try {
            switches.scanIntervalTicks = 40;
            EntityHitResult hit = new EntityHitResult(blaze, blaze.position().add(0.0, 0.8, 0.0));
            int scanAt = LaserBeam.dwellTicks(LaserBeam.IGNITE_TICKS, player.getEyePosition().distanceTo(hit.getLocation()));
            for (int i = 0; i < scanAt - 1; i++) {
                LaserBeam.beamAtEntity(player, lens, hit);
            }
            helper.assertFalse(blaze.hasEffect(MobEffects.GLOWING), "the blaze glows before the scan dwell time");
            LaserBeam.beamAtEntity(player, lens, hit);
            helper.assertTrue(blaze.hasEffect(MobEffects.GLOWING), "the blaze does not glow after the scan dwell time");
            helper.assertValueEqual(blaze.getEffect(MobEffects.GLOWING).getDuration(), McVersion.GADGET_REWORK ? 50 : 110,
                    "configured scan glow duration");
            helper.assertFalse(blaze.getRemainingFireTicks() > 0, "a fire-immune blaze caught fire");

            ServerPlayer other = shooter(helper);
            other.snapTo(blaze.getX() + 1.0, blaze.getY(), blaze.getZ(), 0.0F, 0.0F);
            helper.assertFalse(LaserBeam.canScan(player, other), "another player is scanned with server.laser.scanPlayers off");

            switches.scanEntities = false;
            helper.assertFalse(LaserBeam.canScan(player, blaze), "creatures are scanned with server.laser.scanEntities off");
        } finally {
            switches.scanEntities = scan;
            switches.scanIntervalTicks = scanInterval;
            blaze.discard();
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ Listiger Shulker

    /** Effekt, beide Traenke und die Brau-Rezepte (Trank, Wurf, Verweil, verlaengert) sind da. */
    public static void craftyShulkerPotionsAreRegisteredAndBrewable(GameTestHelper helper) {
        if (!McVersion.CRAFTY_SHULKER) {
            helper.succeed();
            return;
        }
        helper.assertTrue(ModEffects.CRAFTY_SHULKER != null && ModEffects.CRAFTY_SHULKER_POTION != null
                && ModEffects.LONG_CRAFTY_SHULKER_POTION != null, "the crafty shulker effect or potions are not registered");
        helper.assertTrue(ModEffects.CRAFTY_SHULKER.value().isBeneficial(), "the crafty shulker effect is not beneficial");
        helper.assertValueEqual(ModEffects.LONG_CRAFTY_SHULKER_POTION.value().getEffects().getFirst().getDuration(), ModEffects.LONG_DURATION,
                "duration of the long potion");
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String path : List.of("brewing/potion_awkward_shulker_head", "brewing/splash_potion_awkward_shulker_head",
                "brewing/lingering_potion_awkward_shulker_head", "brewing/potion_crafty_shulker_redstone",
                "brewing/lingering_potion_crafty_shulker_redstone", "brewing/potion_crafty_shulker_gunpowder",
                "brewing/splash_potion_long_crafty_shulker_dragon_breath")) {
            ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path));
            helper.assertTrue(recipes.byKey(key).isPresent(), "missing brewing recipe " + key.identifier());
        }
        helper.succeed();
    }

    /** Nur Schaden durch ein Wesen loest aus; Fall, Feuer und Leere nicht. */
    public static void craftyShulkerOnlyTriggersOnHitsByCreatures(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        var sources = level.damageSources();
        helper.assertTrue(CraftyShulkerEffect.triggers(sources.mobAttack(zombie)), "a zombie hit does not trigger");
        helper.assertFalse(CraftyShulkerEffect.triggers(sources.fall()), "fall damage triggers");
        helper.assertFalse(CraftyShulkerEffect.triggers(sources.inFire()), "fire damage triggers");
        helper.assertFalse(CraftyShulkerEffect.triggers(sources.fellOutOfWorld()), "void damage triggers");
        zombie.discard();
        helper.succeed();
    }

    /** Raster 5x5 um (cx, cz): Boden in y=0, Oberflaeche y=1..2 aus {@code surface}; Luft darueber. */
    private static void floor(GameTestHelper helper, int cx, int cz, net.minecraft.world.level.block.Block surface) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), surface);
                helper.setBlock(new BlockPos(x, 2, z), surface);
                for (int y = 3; y <= 5; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    /**
     * Sichere Stelle: auf einem 5x5-Feld aus Magma mit Steinsaeule unter dem Huhn und einem Stein in der Ecke landet es
     * nur auf dem Ecken-Stein (nie auf Magma, nie zu nah); in einem Lavasee ohne Boden springt es gar nicht.
     */
    public static void craftyShulkerLandsOnlyOnSafeGround(GameTestHelper helper) {
        if (!McVersion.CRAFTY_SHULKER) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        int cx = 3;
        int cz = 3;
        floor(helper, cx, cz, Blocks.MAGMA_BLOCK);
        helper.setBlock(new BlockPos(cx, 2, cz), Blocks.STONE);
        helper.setBlock(new BlockPos(cx + 2, 2, cz + 2), Blocks.STONE);
        Mob chicken = helper.spawnWithNoFreeWill(EntityTypes.CHICKEN, new BlockPos(cx, 3, cz));
        Vec3 start = helper.absoluteVec(new Vec3(cx + 0.5, 3.0, cz + 0.5));
        chicken.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
        RandomSource random = RandomSource.create(42L);
        boolean moved = false;
        for (int i = 0; i < 300 && !moved; i++) {
            moved = CraftyShulkerEffect.teleport(level, chicken, random, 2);
        }
        helper.assertTrue(moved, "the chicken never found the one safe stone");
        BlockPos below = chicken.blockPosition().below();
        helper.assertTrue(level.getBlockState(below).is(Blocks.STONE), "the chicken landed on " + level.getBlockState(below));
        helper.assertValueEqual(below, helper.absolutePos(new BlockPos(cx + 2, 2, cz + 2)), "landing block");

        floor(helper, cx, cz, Blocks.LAVA);
        helper.setBlock(new BlockPos(cx, 1, cz), Blocks.STONE);
        helper.setBlock(new BlockPos(cx, 2, cz), Blocks.STONE);
        chicken.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
        for (int i = 0; i < 50; i++) {
            helper.assertFalse(CraftyShulkerEffect.teleport(level, chicken, random, 2), "the chicken jumped into the lava lake");
        }
        helper.assertTrue(chicken.position().distanceTo(start) < 1.0E-6, "the chicken moved although there was no safe spot");
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                for (int y = 0; y <= 2; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        chicken.discard();
        helper.succeed();
    }

    /** Am Wesen: Treffer durch einen Zombie -> Sprung; ein zweiter Treffer in der Abklingzeit -> kein Sprung. */
    public static void craftyShulkerTeleportsWhenHitAndRespectsTheCooldown(GameTestHelper helper) {
        if (!McVersion.CRAFTY_SHULKER) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        int cx = 3;
        int cz = 3;
        floor(helper, cx, cz, Blocks.STONE);
        Mob cow = cow(helper, new BlockPos(cx, 3, cz));
        Vec3 start = helper.absoluteVec(new Vec3(cx + 0.5, 3.0, cz + 0.5));
        cow.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
        cow.addEffect(new MobEffectInstance(ModEffects.CRAFTY_SHULKER, 200));
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 3, 1));
        var tuning = ServerTuning.get().craftyShulker;
        int radius = tuning.radius;
        try {
            tuning.radius = 2;
            CraftyShulkerEffect.resetCooldown(cow);
            boolean jumped = false;
            for (int i = 0; i < 20 && !jumped; i++) {
                McVersion.resetInvulnerableTime(cow);
                cow.setHealth(cow.getMaxHealth());
                cow.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
                jumped = cow.position().distanceTo(start) >= 1.0;
                if (!jumped) {
                    CraftyShulkerEffect.resetCooldown(cow);
                }
            }
            helper.assertTrue(jumped, "the cow with the crafty shulker effect never teleported when hit");
            helper.assertTrue(level.getBlockState(cow.blockPosition().below()).isFaceSturdy(level, cow.blockPosition().below(), Direction.UP),
                    "the cow landed without ground");
            Vec3 after = cow.position();
            McVersion.resetInvulnerableTime(cow);
            cow.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
            helper.assertTrue(cow.position().distanceTo(after) < 1.0E-6, "the cow teleported again within the cooldown");
            McVersion.resetInvulnerableTime(cow);
            Vec3 beforeFall = cow.position();
            CraftyShulkerEffect.resetCooldown(cow);
            cow.hurtServer(level, level.damageSources().fall(), 1.0F);
            helper.assertTrue(cow.position().distanceTo(beforeFall) < 1.0E-6, "fall damage teleported the cow");
        } finally {
            tuning.radius = radius;
            zombie.discard();
            cow.discard();
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    for (int y = 0; y <= 2; y++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
            }
        }
        helper.succeed();
    }
}
