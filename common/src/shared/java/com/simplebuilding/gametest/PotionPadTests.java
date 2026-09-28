package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.PotionPadBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests des Trank-Pads und des Lohenkopfs (Besitzer 2026-09-28, docs/SIMPLETWEAKS-UEBERNAHME.md
 * Abschnitt 2.4): Speichern und Ersetzen eines Wurftranks, Wirkdauer je Stufe beim Betreten ohne
 * Aufstocken, die Regel fuer Sofortwirkungen, die Rezepte aller Stufen und der Lohenkopf aus der
 * Explosion eines geladenen Creepers (und aus keinem anderen Tod).
 */
public final class PotionPadTests {

    private static final Block[] PADS = {TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD};

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
    // Wirkdauer je Stufe
    // =====================================================================================

    /**
     * Betreten gibt die gespeicherte Wirkung mit der Verstaerkung des Tranks fuer 30 s (I), 60 s (II)
     * und 120 s (III). Stehenbleiben frischt nur bis zu dieser Dauer auf (kein Aufstocken), und wer
     * absteigt und wieder betritt, bekommt die volle Dauer zurueck.
     */
    public static void steppingOnThePadGivesTheStoredEffectsForThirtySixtyOrOneHundredTwentySeconds(GameTestHelper helper) {
        int[] seconds = {30, 60, 120};
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < PADS.length; i++) {
            BlockPos pad = new BlockPos(1 + i * 3, 1, 2);
            helper.setBlock(pad, PADS[i]);
            PotionPadBlockEntity be = helper.getBlockEntity(pad, PotionPadBlockEntity.class);
            PotionPadBlock.absorb(be, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_SWIFTNESS));
            helper.assertTrue(((PotionPadBlock) PADS[i]).getTier() == i + 1, PADS[i] + " is not tier " + (i + 1));

            ServerPlayer player = mockPlayer(helper, onTop(pad));
            player.removeAllEffects();
            BlockPos abs = helper.absolutePos(pad);
            BlockState state = helper.getBlockState(pad);
            PotionPadBlockEntity.serverTick(level, abs, state, be);
            MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
            helper.assertTrue(speed != null, "stepping onto potion pad tier " + (i + 1) + " gave no swiftness");
            helper.assertTrue(speed.getDuration() == seconds[i] * 20, "potion pad tier " + (i + 1) + " gave " + speed.getDuration()
                    + " ticks of swiftness instead of " + seconds[i] * 20);
            helper.assertTrue(speed.getAmplifier() == 1, "potion pad tier " + (i + 1) + " gave swiftness level " + (speed.getAmplifier() + 1)
                    + " instead of the potion's level II");

            // Stehenbleiben: jede Auffrischung bleibt bei der Stufendauer.
            for (int n = 0; n < 5; n++) {
                be.apply(level, player, false, 20L * n, state);
            }
            helper.assertTrue(player.getEffect(MobEffects.SPEED).getDuration() == seconds[i] * 20,
                    "standing on potion pad tier " + (i + 1) + " stacked swiftness to " + player.getEffect(MobEffects.SPEED).getDuration() + " ticks");

            // Absteigen, Wirkung laeuft ab, wieder betreten: volle Dauer.
            moveTo(helper, player, onTop(pad).add(2.0, 0.0, 0.0));
            PotionPadBlockEntity.serverTick(level, abs, state, be);
            player.removeEffect(MobEffects.SPEED);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
            moveTo(helper, player, onTop(pad));
            PotionPadBlockEntity.serverTick(level, abs, state, be);
            helper.assertTrue(player.getEffect(MobEffects.SPEED).getDuration() == seconds[i] * 20,
                    "stepping back onto potion pad tier " + (i + 1) + " did not refresh swiftness to the full " + seconds[i] + " s");
        }
        succeed(helper);
    }

    /**
     * Sofortwirkungen wirken einmal je Betreten und hoechstens alle 2 s je Spieler; Stehenbleiben
     * wiederholt sie nicht. Die Dauerwirkung desselben Tranks gibt es trotzdem bei jedem Schritt.
     */
    public static void instantEffectsApplyOncePerStepAndRespectTheirCooldown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.POTION_PAD);
        PotionPadBlockEntity be = helper.getBlockEntity(pad, PotionPadBlockEntity.class);
        ItemStack mixed = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        mixed.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                mixed.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS)
                        .withEffectAdded(new MobEffectInstance(MobEffects.STRENGTH, 200, 0)));
        PotionPadBlock.absorb(be, mixed);
        BlockState state = helper.getBlockState(pad);
        ServerPlayer player = mockPlayer(helper, onTop(pad));
        player.removeAllEffects();
        player.setHealth(4.0F);

        long t = 1000L;
        be.apply(level, player, true, t, state);
        helper.assertTrue(player.getHealth() == 8.0F, "the first step healed to " + player.getHealth() + " instead of 8 (Instant Health I = 4)");
        helper.assertTrue(player.hasEffect(MobEffects.STRENGTH), "the lasting effect of the same potion was not given");

        be.apply(level, player, false, t + 20, state);
        helper.assertTrue(player.getHealth() == 8.0F, "standing on the pad healed again (" + player.getHealth() + ")");

        be.apply(level, player, true, t + PotionPadBlockEntity.INSTANT_COOLDOWN_TICKS - 1, state);
        helper.assertTrue(player.getHealth() == 8.0F, "stepping on again within the 2 s cooldown healed again (" + player.getHealth() + ")");
        player.removeEffect(MobEffects.STRENGTH);
        be.apply(level, player, true, t + PotionPadBlockEntity.INSTANT_COOLDOWN_TICKS, state);
        helper.assertTrue(player.getHealth() == 12.0F, "a step after the cooldown did not heal (" + player.getHealth() + ")");
        helper.assertTrue(player.getEffect(MobEffects.STRENGTH).getDuration() == PotionPadBlock.effectDuration(1),
                "the lasting effect was not given for the tier duration on a step");

        // Stehenbleiben nach Ablauf der Abklingzeit: die Sofortwirkung kommt trotzdem nicht wieder.
        be.apply(level, player, false, t + 3L * PotionPadBlockEntity.INSTANT_COOLDOWN_TICKS, state);
        helper.assertTrue(player.getHealth() == 12.0F, "standing on the pad after the cooldown healed again (" + player.getHealth() + ")");
        succeed(helper);
    }

    // =====================================================================================
    // Rezepte
    // =====================================================================================

    /**
     * I = Netherit-Druckplatte + Lohenkopf (Werkbank, formlos, in beliebiger Lage); II = Enderit-Vorlage
     * + I + Enderit-Druckplatte; III = Enderit-Vorlage + II + Enderit-Kern. Ohne Lohenkopf keine Platte,
     * und die Aufwertungen nehmen keine Netherit-Platte bzw. keinen Barren.
     */
    public static void potionPadRecipesCoverAllThreeTiers(GameTestHelper helper) {
        Item netheritePlate = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        Item enderitePlate = TweaksBlocks.ENDERITE_PRESSURE_PLATE.asItem();
        Item end = ModItems.ENDERITE_UPGRADE_TEMPLATE;

        for (List<ItemStack> grid : List.of(List.of(new ItemStack(netheritePlate), new ItemStack(TweaksItems.BLAZE_HEAD)),
                List.of(new ItemStack(TweaksItems.BLAZE_HEAD), new ItemStack(netheritePlate)))) {
            CraftingInput input = CraftingInput.of(2, 1, grid);
            var match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(match.isPresent(), "no crafting recipe turns " + grid + " into a potion pad");
            ItemStack out = match.get().value().assemble(input);
            helper.assertTrue(out.is(TweaksBlocks.POTION_PAD.asItem()), "crafting " + grid + " made " + out + " instead of a potion pad");
        }
        CraftingInput withoutHead = CraftingInput.of(2, 1, List.of(new ItemStack(netheritePlate), new ItemStack(Items.SKELETON_SKULL)));
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, withoutHead, helper.getLevel()).isEmpty(),
                "a skeleton skull makes a potion pad too");

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
        Blaze first = helper.spawn(EntityTypes.BLAZE, new BlockPos(3, 2, 3));
        Blaze second = helper.spawn(EntityTypes.BLAZE, new BlockPos(5, 2, 3));
        first.hurtServer(level, level.damageSources().explosion(creeper, creeper), 1000.0F);
        second.hurtServer(level, level.damageSources().explosion(creeper, creeper), 1000.0F);
        helper.assertTrue(first.isDeadOrDying() && second.isDeadOrDying(), "the explosion did not kill both blazes");
        helper.assertTrue(blazeHeads(helper) == 1, "one charged creeper explosion dropped " + blazeHeads(helper) + " blaze heads instead of 1");

        Creeper another = chargedCreeper(helper, new BlockPos(1, 2, 5));
        Blaze third = helper.spawn(EntityTypes.BLAZE, new BlockPos(3, 2, 5));
        third.hurtServer(level, level.damageSources().explosion(another, another), 1000.0F);
        helper.assertTrue(blazeHeads(helper) == 2, "a second charged creeper did not drop a second blaze head (" + blazeHeads(helper) + ")");
        succeed(helper);
    }

    /** Eine Lohe, die ein Spieler oder ein ungeladener Creeper toetet, laesst keinen Kopf fallen. */
    public static void blazesKilledOtherwiseDropNoHead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Creeper plain = helper.spawn(EntityTypes.CREEPER, new BlockPos(1, 2, 1));
        Blaze byCreeper = helper.spawn(EntityTypes.BLAZE, new BlockPos(3, 2, 3));
        byCreeper.hurtServer(level, level.damageSources().explosion(plain, plain), 1000.0F);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 5.5));
        Blaze byPlayer = helper.spawn(EntityTypes.BLAZE, new BlockPos(3, 2, 5));
        byPlayer.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(byCreeper.isDeadOrDying() && byPlayer.isDeadOrDying(), "the blazes did not die");
        helper.assertTrue(blazeHeads(helper) == 0, "a blaze killed by a player or an uncharged creeper dropped a head");
        plain.discard();
        succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static Creeper chargedCreeper(GameTestHelper helper, BlockPos pos) {
        Creeper creeper = helper.spawn(EntityTypes.CREEPER, pos);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
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
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
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
        ItemStack out = match.get().value().assemble(in);
        helper.assertTrue(out.is(result.asItem()), "smithing " + base + " with " + addition + " made " + out + " instead of " + result);
    }

    private static void expectNothing(GameTestHelper helper, Item template, ItemLike base, ItemLike addition) {
        var match = smithing(helper, input(template, base, addition));
        helper.assertTrue(match.isEmpty(), "smithing " + base + " with " + addition + " still works ("
                + match.map(h -> h.id().toString()).orElse("") + ")");
    }
}
