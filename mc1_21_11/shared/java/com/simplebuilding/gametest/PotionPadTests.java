package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.PotionPadBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.block.TweaksFamilies;
import com.simplebuilding.tweaks.easter.EasterEggs;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests des Trank-Pads und des Lohenkopfs (Besitzer 2026-09-28, docs/SIMPLETWEAKS-UEBERNAHME.md
 * Abschnitt 2.4): Speichern und Ersetzen eines Wurftranks, das Aufladen in drei Schritten (25/50/100 %
 * der Stufendauer), die Regel fuer Sofortwirkungen, die Abklingzeit (doppelte Wirkdauer, nur gesetzt,
 * reist abgebaut mit dem Item), Stapelgroesse 1 fuer alle Pads, die Rezepte aller Stufen und der
 * Lohenkopf aus der Explosion eines geladenen Creepers (und aus keinem anderen Tod).
 */
public final class PotionPadTests {

    private static final Block[] PADS = {TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD};

    /** Budget fuer den Werfer-/Trichter-Test (Werfer feuert nach 4 Ticks, der Trank fliegt zwei Bloecke). */
    public static final int AUTOMATION_MAX_TICKS = 100;

    private PotionPadTests() {
    }

    // =====================================================================================
    // Speichern
    // =====================================================================================

    /**
     * Ein Wurftrank, der auf das Pad faellt, wird gespeichert (echter Flug bis zum Zerschellen); ein
     * neuer Trank - auch ein Verweiltrank - ersetzt ihn, ein Wasser-Wurftrank wischt das Pad leer.
     */
    public static void splashPotionsLandingOnThePadAreStoredAndReplaced(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Ersetzen (wie der Treffer, den ein zerschellender Trank dem Block meldet): Splash, Verweil, Wasser
        BlockPos other = new BlockPos(5, 1, 2);
        helper.setBlock(other, TweaksBlocks.REINFORCED_POTION_PAD);
        Vec3 at = helper.absoluteVec(new Vec3(5.5, 2.5, 2.5));
        hitWith(helper, other, new ThrownSplashPotion(level, at.x, at.y, at.z,
                PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LEAPING)));
        PotionContents first = helper.getBlockEntity(other, PotionPadBlockEntity.class).getStored();
        helper.assertTrue(first != null && first.is(Potions.LEAPING), "a splash potion hitting the pad was not stored: " + first);
        hitWith(helper, other, new ThrownLingeringPotion(level, at.x, at.y, at.z,
                PotionContents.createItemStack(Items.LINGERING_POTION, Potions.STRENGTH)));
        PotionContents replaced = helper.getBlockEntity(other, PotionPadBlockEntity.class).getStored();
        helper.assertTrue(replaced != null && replaced.is(Potions.STRENGTH), "a new (lingering) potion did not replace the stored one: " + replaced);
        hitWith(helper, other, new ThrownSplashPotion(level, at.x, at.y, at.z,
                PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)));
        helper.assertTrue(helper.getBlockEntity(other, PotionPadBlockEntity.class).getStored() == null,
                "a water splash potion did not wipe the pad");

        // Echter Flug: ein Splash-Trank faellt auf das Pad und zerschellt dort.
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.POTION_PAD);
        Vec3 above = helper.absoluteVec(new Vec3(2.5, 3.5, 2.5));
        ThrownSplashPotion thrown = new ThrownSplashPotion(level, above.x, above.y, above.z,
                PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SWIFTNESS));
        thrown.setDeltaMovement(0.0, -0.6, 0.0);
        level.addFreshEntity(thrown);
        helper.succeedWhen(() -> {
            PotionContents stored = helper.getBlockEntity(pad, PotionPadBlockEntity.class).getStored();
            helper.assertTrue(stored != null && stored.is(Potions.SWIFTNESS), "the pad stored " + stored + " instead of the thrown swiftness potion");
        });
    }

    // =====================================================================================
    // Aufladen in drei Schritten
    // =====================================================================================

    /**
     * Wer auf dem Pad steht, bekommt nach 1 s 25 %, nach 2 s 50 % und nach 3 s 100 % von 30 s (I),
     * 60 s (II) bzw. 120 s (III), mit der Verstaerkung des Tranks (langer Trank, 8 min: die Stufendauer
     * deckelt, nicht der Trank - siehe PotionPadRuleTests); vor der ersten Sekunde nichts. Wer
     * nach einem Schritt absteigt, behaelt das Erhaltene, startet keine Abklingzeit und faengt beim
     * naechsten Betreten wieder bei 0 an.
     */
    public static void standingOnThePadRampsTheEffectToTwentyFiveFiftyAndOneHundredPercentInThreeSeconds(GameTestHelper helper) {
        int[] seconds = {30, 60, 120};
        int[] percent = {25, 50, 100};
        for (int i = 0; i < PADS.length; i++) {
            BlockPos pad = new BlockPos(1 + i * 3, 1, 2);
            PotionPadBlockEntity be = placeFilled(helper, pad, PADS[i], Potions.LONG_SWIFTNESS);
            helper.assertTrue(((PotionPadBlock) PADS[i]).getTier() == i + 1, PADS[i] + " is not tier " + (i + 1));
            int full = seconds[i] * 20;
            String tier = "potion pad tier " + (i + 1);

            ServerPlayer player = mockPlayer(helper, onTop(pad));
            player.removeAllEffects();
            tickPad(helper, pad, PotionPadBlockEntity.RAMP_STEP_TICKS - 1);
            helper.assertTrue(!player.hasEffect(MobEffects.SPEED), tier + " gave swiftness before the first second");
            for (int step = 1; step <= 3; step++) {
                tickPad(helper, pad, step == 1 ? 1 : PotionPadBlockEntity.RAMP_STEP_TICKS);
                MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
                int expected = full * percent[step - 1] / 100;
                helper.assertTrue(speed != null && speed.getDuration() == expected, tier + " gave " + (speed == null ? "no" : speed.getDuration() + " ticks of")
                        + " swiftness after " + step + " s instead of " + expected + " (" + percent[step - 1] + " %)");
                helper.assertTrue(speed.getAmplifier() == 0, tier + " gave swiftness level " + (speed.getAmplifier() + 1) + " instead of the potion's level I");
                helper.assertTrue(be.isCoolingDown() == (step == 3),
                        tier + (step == 3 ? " did not start its cooldown at 100 %" : " started its cooldown at " + percent[step - 1] + " %"));
            }
            player.removeAllEffects();
            moveTo(helper, player, new Vec3(0.5, 3.0, 0.5));

            // Abbruch: auf einem frischen Pad derselben Stufe nach dem ersten Schritt absteigen.
            BlockPos early = pad.offset(0, 0, 3);
            PotionPadBlockEntity earlyBe = placeFilled(helper, early, PADS[i], Potions.LONG_SWIFTNESS);
            ServerPlayer leaver = mockPlayer(helper, onTop(early));
            leaver.removeAllEffects();
            tickPad(helper, early, PotionPadBlockEntity.RAMP_STEP_TICKS);
            moveTo(helper, leaver, onTop(early).add(2.0, 0.0, 0.0));
            tickPad(helper, early, 3 * PotionPadBlockEntity.RAMP_STEP_TICKS);
            MobEffectInstance kept = leaver.getEffect(MobEffects.SPEED);
            helper.assertTrue(kept != null && kept.getDuration() == full / 4, tier + ": leaving after 1 s kept " + kept + " instead of 25 %");
            helper.assertTrue(!earlyBe.isCoolingDown(), tier + ": leaving after 1 s put the pad on cooldown");
            // Wieder betreten: der Zaehler beginnt bei 0 (nach 19 Ticks nichts, nach 20 wieder 25 %).
            moveTo(helper, leaver, onTop(early));
            leaver.removeAllEffects();
            tickPad(helper, early, PotionPadBlockEntity.RAMP_STEP_TICKS - 1);
            helper.assertTrue(!leaver.hasEffect(MobEffects.SPEED), tier + ": stepping back on continued the old ramp");
            tickPad(helper, early, 1);
            helper.assertTrue(leaver.hasEffect(MobEffects.SPEED) && leaver.getEffect(MobEffects.SPEED).getDuration() == full / 4,
                    tier + ": stepping back on did not restart at 25 %");
            leaver.removeAllEffects();
            moveTo(helper, leaver, new Vec3(0.5, 3.0, 0.5));
        }
        succeed(helper);
    }

    /**
     * Sofortwirkungen wirken einmal, beim 3-s-Schritt (nicht bei 1 s oder 2 s); die Dauerwirkung desselben
     * Tranks steigt dabei mit. Danach ist das Pad in der Abklingzeit und heilt nicht noch einmal.
     */
    public static void instantEffectsApplyOnceAtTheThreeSecondMark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.POTION_PAD);
        PotionPadBlockEntity be = helper.getBlockEntity(pad, PotionPadBlockEntity.class);
        ItemStack mixed = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        mixed.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                mixed.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS)
                        .withEffectAdded(new MobEffectInstance(MobEffects.STRENGTH, 1200, 0)));
        PotionPadBlock.absorb(be, mixed);
        ServerPlayer player = mockPlayer(helper, onTop(pad));
        player.removeAllEffects();
        player.setHealth(4.0F);
        int full = PotionPadBlock.effectDuration(1);

        be.grant(level, player, 1);
        helper.assertTrue(player.getHealth() == 4.0F, "the 1 s step healed (" + player.getHealth() + ")");
        helper.assertTrue(player.hasEffect(MobEffects.STRENGTH) && player.getEffect(MobEffects.STRENGTH).getDuration() == full / 4,
                "the lasting effect was not given for 25 % at 1 s");
        be.grant(level, player, 2);
        helper.assertTrue(player.getHealth() == 4.0F, "the 2 s step healed (" + player.getHealth() + ")");
        be.grant(level, player, 3);
        helper.assertTrue(player.getHealth() == 8.0F, "the 3 s step healed to " + player.getHealth() + " instead of 8 (Instant Health I = 4)");
        helper.assertTrue(player.getEffect(MobEffects.STRENGTH).getDuration() == full, "the lasting effect was not given for the full tier duration at 3 s");
        // Healing asks for twice the plain cooldown on the 30 s instant basis (PotionPadRules): 2 x 2 x 600.
        helper.assertTrue(be.getCooldown() == 4 * full, "the 3 s step set a cooldown of " + be.getCooldown() + " ticks instead of " + 4 * full);
        be.grant(level, player, 3);
        helper.assertTrue(player.getHealth() == 8.0F, "a cooling pad healed again (" + player.getHealth() + ")");
        succeed(helper);
    }

    // =====================================================================================
    // Abklingzeit
    // =====================================================================================

    /**
     * Die volle Anwendung setzt das Pad fuer die doppelte Wirkdauer in die Abklingzeit (I 60 s, II 120 s,
     * III 240 s) und zeigt den Blockzustand {@code cooling=true}. Waehrend der Abklingzeit bekommt niemand
     * etwas, auch nicht nach 3 s Stehen; sie laeuft Tick fuer Tick ab, danach ist das Pad wieder bereit.
     */
    public static void aFullApplicationPutsThePadOnCooldownForTwiceTheEffectDuration(GameTestHelper helper) {
        int[] cooldownSeconds = {60, 120, 240};
        for (int i = 0; i < PADS.length; i++) {
            BlockPos pad = new BlockPos(1 + i * 3, 1, 2);
            PotionPadBlockEntity be = placeFilled(helper, pad, PADS[i], Potions.LONG_SWIFTNESS);
            String tier = "potion pad tier " + (i + 1);
            int cooldown = cooldownSeconds[i] * 20;
            helper.assertTrue(((PotionPadBlock) PADS[i]).cooldownAt(helper.getLevel(), helper.absolutePos(pad)) == cooldown,
                    tier + " has the wrong cooldown length");
            ServerPlayer player = mockPlayer(helper, onTop(pad));
            player.removeAllEffects();
            tickPad(helper, pad, 3 * PotionPadBlockEntity.RAMP_STEP_TICKS);
            helper.assertTrue(be.getCooldown() == cooldown, tier + " went on cooldown for " + be.getCooldown()
                    + " ticks instead of " + cooldownSeconds[i] + " s (twice the effect duration)");
            helper.assertTrue(helper.getBlockState(pad).getValue(PotionPadBlock.COOLING), tier + " does not show the cooling state");

            // In der Abklingzeit: langes Stehen gibt nichts, die Zeit laeuft Tick fuer Tick ab.
            player.removeAllEffects();
            tickPad(helper, pad, 5 * PotionPadBlockEntity.RAMP_STEP_TICKS);
            helper.assertTrue(!player.hasEffect(MobEffects.SPEED), tier + " gave swiftness while cooling down");
            helper.assertTrue(be.getCooldown() == cooldown - 5 * PotionPadBlockEntity.RAMP_STEP_TICKS,
                    tier + ": the cooldown did not tick down while placed (" + be.getCooldown() + ")");

            // Ablauf: mit 1 Tick Rest bleibt es gesperrt, danach ist es bereit und laedt von vorn.
            be.setCooldown(1);
            helper.assertTrue(helper.getBlockState(pad).getValue(PotionPadBlock.COOLING), tier + " left the cooling state early");
            tickPad(helper, pad, 1);
            helper.assertTrue(!be.isCoolingDown() && !helper.getBlockState(pad).getValue(PotionPadBlock.COOLING),
                    tier + " did not become ready when the cooldown ran out");
            tickPad(helper, pad, PotionPadBlockEntity.RAMP_STEP_TICKS);
            helper.assertTrue(player.hasEffect(MobEffects.SPEED), tier + " gave nothing after its cooldown ran out");
            player.removeAllEffects();
            moveTo(helper, player, new Vec3(0.5, 3.0, 0.5));
        }
        succeed(helper);
    }

    /**
     * Abgebaut in der Abklingzeit faellt das Pad als eigener, nicht stapelbarer Item-Zustand mit der
     * Restzeit (Komponente {@code potion_pad_cooldown}; das Item-Modell zeigt dann die animierte
     * Abklingtextur) und behaelt Trank und Easter-Stufe. Solange es nicht gesetzt ist, steht die Zeit;
     * gesetzt laeuft sie genau dort weiter. Ein bereites Pad faellt ohne Restzeit.
     */
    public static void aPadBrokenDuringCooldownKeepsTheRemainingTimeAndResumesWhenPlacedAgain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 1, 3);
        ServerPlayer builder = mockPlayer(helper, new Vec3(6.5, 1.0, 6.5));
        placeFromItem(helper, builder, EasterEggs.create(TweaksFamilies.Family.POTION_PAD, 3), pos);
        PotionPadBlockEntity be = helper.getBlockEntity(pos, PotionPadBlockEntity.class);
        PotionPadBlock.absorb(be, PotionContents.createItemStack(Items.LINGERING_POTION, Potions.LONG_STRENGTH));
        ServerPlayer drinker = mockPlayer(helper, onTop(pos));
        drinker.removeAllEffects();
        tickPad(helper, pos, 3 * PotionPadBlockEntity.RAMP_STEP_TICKS);
        int expected = 480 * 20;
        helper.assertTrue(be.getCooldown() == expected, "the final easter potion pad went on cooldown for " + be.getCooldown() + " ticks instead of 480 s");
        moveTo(helper, drinker, new Vec3(0.5, 3.0, 0.5));
        tickPad(helper, pos, 100);
        expected -= 100;
        helper.assertTrue(be.getCooldown() == expected, "the cooldown did not tick while placed (" + be.getCooldown() + ")");

        List<ItemStack> drops = breakAndCollect(helper, builder, pos);
        ItemStack dropped = drops.size() == 1 ? drops.get(0) : ItemStack.EMPTY;
        Integer rest = dropped.get(TweaksComponents.POTION_PAD_COOLDOWN);
        PotionContents potion = dropped.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        helper.assertTrue(dropped.is(TweaksBlocks.INFUSED_POTION_PAD.asItem()) && rest != null && rest == expected,
                "breaking a cooling pad dropped " + drops + " with remaining cooldown " + rest + " instead of " + expected);
        helper.assertTrue(potion != null && potion.is(Potions.LONG_STRENGTH) && EasterEggs.stageOf(dropped) == 3,
                "the cooling pad item lost its potion (" + potion + ") or its easter stage (" + EasterEggs.stageOf(dropped) + ")");
        helper.assertTrue(dropped.getMaxStackSize() == 1 && !dropped.isStackable(), "the cooling pad item stacks to " + dropped.getMaxStackSize());
        helper.assertTrue(!ItemStack.isSameItemSameComponents(dropped, EasterEggs.create(TweaksFamilies.Family.POTION_PAD, 3)),
                "the cooling pad item is the same item state as a ready one");

        // Wieder setzen: Restzeit, Zustand, Trank und Easter-Stufe kommen zurueck; die Zeit laeuft weiter.
        placeFromItem(helper, builder, dropped, pos);
        PotionPadBlockEntity again = helper.getBlockEntity(pos, PotionPadBlockEntity.class);
        boolean cooling = helper.getBlockState(pos).getValue(PotionPadBlock.COOLING);
        helper.assertTrue(again.getCooldown() == expected && cooling,
                "placed again, the pad has " + again.getCooldown() + " ticks of cooldown (cooling=" + cooling + ") instead of " + expected);
        helper.assertTrue(again.getStored() != null && again.getStored().is(Potions.LONG_STRENGTH) && again.easterStage() == 3
                        && EasterEggs.isBoosted(level, helper.absolutePos(pos)),
                "placed again, the pad holds " + again.getStored() + " at easter stage " + again.easterStage());
        tickPad(helper, pos, 20);
        helper.assertTrue(again.getCooldown() == expected - 20, "placed again, the cooldown does not resume (" + again.getCooldown() + ")");
        drinker.removeAllEffects();
        moveTo(helper, drinker, onTop(pos));
        tickPad(helper, pos, 3 * PotionPadBlockEntity.RAMP_STEP_TICKS);
        helper.assertTrue(!drinker.hasEffect(MobEffects.STRENGTH), "a pad placed again while cooling gave its effect");
        moveTo(helper, drinker, new Vec3(0.5, 3.0, 0.5));

        // Ohne Abklingzeit: kein Restzeit-Stempel auf dem Item.
        again.setCooldown(0);
        List<ItemStack> ready = breakAndCollect(helper, builder, pos);
        helper.assertTrue(ready.size() == 1 && !ready.get(0).has(TweaksComponents.POTION_PAD_COOLDOWN),
                "a ready pad dropped with a cooldown stamp: " + ready);
        succeed(helper);
    }

    // =====================================================================================
    // Stapelgroesse
    // =====================================================================================

    /**
     * Jedes Pad-Item stapelt nicht (Stapelgroesse 1): alle Stufen aller Familien (Elytra-Pad, Flypad,
     * Spawn-Teleporter, Launchpad, Chunk-Loader, Trank-Pad), ihre Easter-Stufen, ein abklingendes
     * Trank-Pad und die alten Flypads. Gegenprobe: die Druckplatten sind keine Pads und stapeln bis 64.
     */
    public static void everyPadItemStacksToOne(GameTestHelper helper) {
        List<Block> pads = new java.util.ArrayList<>();
        for (TweaksFamilies.Family family : TweaksFamilies.Family.values()) {
            if (family != TweaksFamilies.Family.PRESSURE_PLATE) {
                pads.addAll(TweaksFamilies.tiers(family));
            }
        }
        pads.addAll(TweaksBlocks.legacy());
        helper.assertTrue(pads.size() == 24, "expected 24 pad blocks (22 tiers + 2 legacy flypads), found " + pads.size());
        for (Block pad : pads) {
            ItemStack stack = new ItemStack(pad);
            helper.assertTrue(stack.getMaxStackSize() == 1, pad + " stacks to " + stack.getMaxStackSize() + " instead of 1");
            helper.assertTrue(TweaksItems.padItems().contains(stack.getItem()), pad + " is missing from TweaksItems#padItems");
        }
        helper.assertTrue(TweaksItems.padItems().size() == pads.size(), "TweaksItems#padItems lists " + TweaksItems.padItems().size() + " items");
        for (TweaksFamilies.Family family : EasterEggs.families()) {
            for (int stage = 1; stage <= EasterEggs.stageCount(family); stage++) {
                ItemStack easter = EasterEggs.create(family, stage);
                helper.assertTrue(easter.getMaxStackSize() == 1, family + " easter stage " + stage + " stacks to " + easter.getMaxStackSize());
            }
        }
        ItemStack cooling = new ItemStack(TweaksBlocks.POTION_PAD);
        cooling.set(TweaksComponents.POTION_PAD_COOLDOWN, 1200);
        helper.assertTrue(cooling.getMaxStackSize() == 1, "a cooling potion pad stacks to " + cooling.getMaxStackSize());
        for (Block plate : List.of(TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE,
                TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE)) {
            helper.assertTrue(new ItemStack(plate).getMaxStackSize() == 64, plate + " no longer stacks to 64 (it is not a pad)");
        }
        succeed(helper);
    }

    // =====================================================================================
    // Rezepte
    // =====================================================================================

    /**
     * I = beliebige Vorlage + Netherit-Druckplatte + Lohenkopf (Schmiede seit 2026-09-28, wie das
     * Elytra-Pad; die formlose Werkbank-Variante gibt es nicht mehr); II = Enderit-Vorlage + I +
     * Enderit-Druckplatte; III = Enderit-Vorlage + II + Enderit-Kern. Ohne Lohenkopf kein Pad, und die
     * Aufwertungen nehmen keine Netherit-Platte bzw. keinen Barren.
     */
    public static void potionPadRecipesCoverAllThreeTiers(GameTestHelper helper) {
        Item netheritePlate = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        Item enderitePlate = TweaksBlocks.ENDERITE_PRESSURE_PLATE.asItem();
        Item end = ModItems.ENDERITE_UPGRADE_TEMPLATE;

        for (Item template : List.of(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
            expect(helper, template, netheritePlate, TweaksItems.BLAZE_HEAD, TweaksBlocks.POTION_PAD);
        }
        expectNothing(helper, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, netheritePlate, Items.SKELETON_SKULL);
        for (List<ItemStack> grid : List.of(List.of(new ItemStack(netheritePlate), new ItemStack(TweaksItems.BLAZE_HEAD)),
                List.of(new ItemStack(TweaksItems.BLAZE_HEAD), new ItemStack(netheritePlate)))) {
            CraftingInput input = CraftingInput.of(2, 1, grid);
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(),
                    "the old shapeless crafting recipe still turns " + grid + " into a potion pad");
        }

        expect(helper, end, TweaksBlocks.POTION_PAD, enderitePlate, TweaksBlocks.REINFORCED_POTION_PAD);
        expect(helper, end, TweaksBlocks.REINFORCED_POTION_PAD, ModItems.ENDERITE_CORE, TweaksBlocks.INFUSED_POTION_PAD);
        expectNothing(helper, end, TweaksBlocks.POTION_PAD, ModItems.ENDERITE_INGOT);
        expectNothing(helper, end, TweaksBlocks.POTION_PAD, netheritePlate);
        expectNothing(helper, end, TweaksBlocks.REINFORCED_POTION_PAD, enderitePlate);
        succeed(helper);
    }

    // =====================================================================================
    // Lohenkopf
    // =====================================================================================

    /**
     * Toetet die Explosion eines geladenen Creepers zwei Lohen, faellt genau ein Lohenkopf (wie bei
     * Vanillas Mob-Koepfen: einer je Explosion); ein zweiter geladener Creeper bringt den naechsten.
     */
    public static void chargedCreeperExplosionsDropOneBlazeHeadEach(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Creeper creeper = chargedCreeper(helper, new BlockPos(1, 2, 1));
        helper.assertTrue(creeper.isPowered(), "the lightning did not charge the creeper");
        Blaze first = helper.spawn(EntityType.BLAZE, new BlockPos(3, 2, 3));
        Blaze second = helper.spawn(EntityType.BLAZE, new BlockPos(5, 2, 3));
        first.hurtServer(level, level.damageSources().explosion(creeper, creeper), 1000.0F);
        second.hurtServer(level, level.damageSources().explosion(creeper, creeper), 1000.0F);
        helper.assertTrue(first.isDeadOrDying() && second.isDeadOrDying(), "the explosion did not kill both blazes");
        helper.assertTrue(blazeHeads(helper) == 1, "one charged creeper explosion dropped " + blazeHeads(helper) + " blaze heads instead of 1");

        Creeper another = chargedCreeper(helper, new BlockPos(1, 2, 5));
        Blaze third = helper.spawn(EntityType.BLAZE, new BlockPos(3, 2, 5));
        third.hurtServer(level, level.damageSources().explosion(another, another), 1000.0F);
        helper.assertTrue(blazeHeads(helper) == 2, "a second charged creeper did not drop a second blaze head (" + blazeHeads(helper) + ")");
        succeed(helper);
    }

    /** Eine Lohe, die ein Spieler oder ein ungeladener Creeper toetet, laesst keinen Kopf fallen. */
    public static void blazesKilledOtherwiseDropNoHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Creeper plain = helper.spawn(EntityType.CREEPER, new BlockPos(1, 2, 1));
        Blaze byCreeper = helper.spawn(EntityType.BLAZE, new BlockPos(3, 2, 3));
        byCreeper.hurtServer(level, level.damageSources().explosion(plain, plain), 1000.0F);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 5.5));
        Blaze byPlayer = helper.spawn(EntityType.BLAZE, new BlockPos(3, 2, 5));
        byPlayer.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(byCreeper.isDeadOrDying() && byPlayer.isDeadOrDying(), "the blazes did not die");
        helper.assertTrue(blazeHeads(helper) == 0, "a blaze killed by a player or an uncharged creeper dropped a head");
        plain.discard();
        succeed(helper);
    }

    // ---- owner decisions 2026-09-28 (begin)

    /**
     * A potion pad is switched off by redstone and reports its state to a comparator (owner,
     * 2026-09-28): powered, a player standing on it for the whole three second ramp gets nothing;
     * unpowered, the first step arrives. The comparator reads 0 without a potion, 15 while ready and
     * 1..14 during the cooldown, rising as the cooldown runs out, and 15 again once it is over.
     *
     * <p>What breaks it: the redstone check going missing from {@code serverTick}, or a comparator
     * signal that ignores the potion or the cooldown.
     */
    public static void potionPadsAreSwitchedOffByRedstoneAndReportTheirStateToAComparator(GameTestHelper helper) {
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.POTION_PAD);
        Assertions.valueEqual(helper, potionPadSignal(helper, pad), 0, "comparator signal of a potion pad without a potion");
        PotionPadBlockEntity be = placeFilled(helper, pad, TweaksBlocks.POTION_PAD, net.minecraft.world.item.alchemy.Potions.SWIFTNESS);
        Assertions.valueEqual(helper, potionPadSignal(helper, pad), 15, "comparator signal of a ready potion pad");

        ServerPlayer player = mockPlayer(helper, onTop(pad));
        helper.setBlock(pad.east(), Blocks.REDSTONE_BLOCK);
        tickPad(helper, pad, PotionPadBlockEntity.stepTicks() * PotionPadBlockEntity.RAMP_STEPS + 5);
        helper.assertFalse(player.hasEffect(MobEffects.SPEED), "a powered potion pad gave its effect");
        helper.assertFalse(be.isCoolingDown(), "a powered potion pad went into its cooldown");

        helper.setBlock(pad.east(), Blocks.AIR);
        tickPad(helper, pad, PotionPadBlockEntity.stepTicks());
        helper.assertTrue(player.hasEffect(MobEffects.SPEED), "the unpowered potion pad gave no effect after one step");

        // --- the full ramp starts the cooldown: the signal drops and climbs back ---
        tickPad(helper, pad, PotionPadBlockEntity.stepTicks() * (PotionPadBlockEntity.RAMP_STEPS - 1));
        helper.assertTrue(be.isCoolingDown(), "the full ramp did not start the cooldown");
        int early = potionPadSignal(helper, pad);
        helper.assertTrue(early >= 1 && early <= 3, "comparator signal right after the cooldown started: " + early);
        int cooldown = be.getCooldown();
        tickPad(helper, pad, cooldown / 2);
        int half = potionPadSignal(helper, pad);
        helper.assertTrue(half > early && half < 15, "comparator signal half way through the cooldown: " + half + " (start " + early + ")");
        tickPad(helper, pad, cooldown - cooldown / 2);
        helper.assertFalse(be.isCoolingDown(), "the cooldown did not run out");
        Assertions.valueEqual(helper, potionPadSignal(helper, pad), 15, "comparator signal once the cooldown is over");

        succeed(helper);
    }

    /**
     * Potions reach a pad only as a thrown potion (owner, 2026-09-28): a dispenser throwing a splash
     * potion of Swiftness down onto a potion pad fills it with exactly that effect, while a hopper
     * holding the same potion above a second pad cannot put it in.
     *
     * <p>What breaks it: a potion pad that turns into a container, or thrown potions no longer being
     * absorbed.
     */
    public static void aDispenserFillsThePotionPadButAHopperCannot(GameTestHelper helper) {
        BlockPos thrownPad = new BlockPos(1, 1, 1);
        BlockPos dispenserPos = thrownPad.above(2);
        helper.setBlock(thrownPad, TweaksBlocks.POTION_PAD);
        helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(dispenserPos, net.minecraft.world.level.block.entity.DispenserBlockEntity.class)
                .setItem(0, PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        helper.setBlock(dispenserPos.above(), Blocks.REDSTONE_BLOCK);

        BlockPos hopperPad = new BlockPos(5, 1, 1);
        helper.setBlock(hopperPad, TweaksBlocks.POTION_PAD);
        helper.setBlock(hopperPad.above(), Blocks.HOPPER);
        net.minecraft.world.level.block.entity.HopperBlockEntity hopper =
                helper.getBlockEntity(hopperPad.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));

        helper.succeedWhen(() -> {
            PotionContents stored = helper.getBlockEntity(thrownPad, PotionPadBlockEntity.class).getStored();
            helper.assertTrue(stored != null, "the potion the dispenser threw did not land in the pad");
            boolean speed = false;
            for (net.minecraft.world.effect.MobEffectInstance effect : stored.getAllEffects()) {
                speed |= effect.getEffect().is(MobEffects.SPEED);
            }
            helper.assertTrue(speed, "the pad holds " + stored + " instead of the thrown Swiftness");
            helper.assertTrue(hopper.getItem(0).is(Items.SPLASH_POTION), "the hopper gave its potion away");
            helper.assertTrue(helper.getBlockEntity(hopperPad, PotionPadBlockEntity.class).getStored() == null,
                    "a hopper filled a potion pad");
        });
    }

    private static int potionPadSignal(GameTestHelper helper, BlockPos pad) {
        BlockPos abs = helper.absolutePos(pad);
        return helper.getLevel().getBlockState(abs).getAnalogOutputSignal(helper.getLevel(), abs, Direction.EAST);
    }

    // ---- owner decisions 2026-09-28 (end)

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static Creeper chargedCreeper(GameTestHelper helper, BlockPos pos) {
        Creeper creeper = helper.spawn(EntityType.CREEPER, pos);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        creeper.thunderHit(helper.getLevel(), bolt);
        creeper.clearFire();
        creeper.setHealth(creeper.getMaxHealth());
        return creeper;
    }

    private static int blazeHeads(GameTestHelper helper) {
        int count = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(TweaksItems.BLAZE_HEAD))) {
            count += item.getItem().getCount();
        }
        return count;
    }

    /** Setzt das Pad und speichert einen Wurftrank darauf. */
    private static PotionPadBlockEntity placeFilled(GameTestHelper helper, BlockPos pad, Block block,
                                                    net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        helper.setBlock(pad, block);
        PotionPadBlockEntity be = helper.getBlockEntity(pad, PotionPadBlockEntity.class);
        PotionPadBlock.absorb(be, PotionContents.createItemStack(Items.SPLASH_POTION, potion));
        return be;
    }

    /** Faehrt den Block-Entity-Ticker des Pads {@code ticks} Mal von Hand, mit dem aktuellen Blockzustand. */
    private static void tickPad(GameTestHelper helper, BlockPos pad, int ticks) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pad);
        for (int i = 0; i < ticks; i++) {
            PotionPadBlockEntity be = helper.getBlockEntity(pad, PotionPadBlockEntity.class);
            PotionPadBlockEntity.serverTick(level, abs, level.getBlockState(abs), be);
        }
    }

    /** Setzt das Item wie ein Spieler (Klick auf die Oberseite eines Steins unter pos). */
    private static void placeFromItem(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.STONE);
        BlockPos abs = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs.below()).add(0, 0.5, 0), Direction.UP, abs.below(), false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(!helper.getBlockState(pos).isAir(), "placing " + stack + " at " + pos + " did nothing");
    }

    /** Baut den Block mit Drops ab und sammelt die herausgefallenen Items ein. */
    private static List<ItemStack> breakAndCollect(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        helper.getLevel().destroyBlock(abs, true, player);
        List<ItemStack> out = new java.util.ArrayList<>();
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.0))) {
            out.add(item.getItem().copy());
            item.discard();
        }
        return out;
    }

    /** Spieler steht mitten auf dem Pad (Oberkante 1/16 Block ueber dem Blockboden). */
    private static Vec3 onTop(BlockPos pad) {
        return new Vec3(pad.getX() + 0.5, pad.getY() + 1.0 / 16.0, pad.getZ() + 0.5);
    }

    private static void hitWith(GameTestHelper helper, BlockPos pad, net.minecraft.world.entity.projectile.Projectile potion) {
        BlockPos abs = helper.absolutePos(pad);
        BlockState state = helper.getBlockState(pad);
        state.onProjectileHit(helper.getLevel(), state, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false), potion);
    }

    private static void moveTo(GameTestHelper helper, ServerPlayer player, Vec3 relative) {
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
    }

    private static void succeed(GameTestHelper helper) {
        TestCleanup.succeed(helper);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>> smithing(
            GameTestHelper helper, SmithingRecipeInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());
    }

    private static SmithingRecipeInput input(Item template, ItemLike base, ItemLike addition) {
        return new SmithingRecipeInput(new ItemStack(template), new ItemStack(base), new ItemStack(addition));
    }

    private static void expect(GameTestHelper helper, Item template, ItemLike base, ItemLike addition, ItemLike result) {
        SmithingRecipeInput in = input(template, base, addition);
        var match = smithing(helper, in);
        helper.assertTrue(match.isPresent(), "no smithing recipe turns " + base + " with " + addition + " into " + result);
        ItemStack out = match.get().value().assemble(in, helper.getLevel().registryAccess());
        helper.assertTrue(out.is(result.asItem()), "smithing " + base + " with " + addition + " made " + out + " instead of " + result);
    }

    private static void expectNothing(GameTestHelper helper, Item template, ItemLike base, ItemLike addition) {
        var match = smithing(helper, input(template, base, addition));
        helper.assertTrue(match.isEmpty(), "smithing " + base + " with " + addition + " still works ("
                + match.map(h -> h.id().toString()).orElse("") + ")");
    }
}
