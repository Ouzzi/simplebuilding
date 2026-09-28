package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.items.CreativeTabLayout;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.block.PotionPadBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.TweaksFamilies;
import com.simplebuilding.tweaks.block.TweaksFamilies.Family;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.ElytraPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.FlypadBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.easter.EasterEggs;
import com.simplebuilding.tweaks.easter.EasterSmithingRecipe;
import com.simplebuilding.tweaks.easter.FunnyStickItem;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests der versteckten Kette ueber den Pad-Endstufen ({@link EasterEggs}; Spoiler-Abschnitt in
 * docs/SIMPLETWEAKS-UEBERNAHME.md): Einstieg "Don't do it" mit Stufe-I-Verhalten, Namen und Kosten
 * jeder Stufe, doppelte Kraft der letzten Easter-Stufe, Advancements, Funny Stick, Unsichtbarkeit in
 * Rezeptbuch, JEI und Kreativ-Tabs.
 */
public final class TweaksEasterTests {

    private static final TagKey<Item> HIDDEN = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "hidden_from_recipe_viewers"));

    private TweaksEasterTests() {
    }

    // =====================================================================================
    // Einstieg
    // =====================================================================================

    /**
     * Die Endstufe jeder Familie wird im Schmiedetisch (Kosten der Stufe I) zu "Don't do it": der
     * Stufe-I-Block mit Easter-Stufe 1. Gesetzt ist er genau der Stufe-I-Block und wirkt wie er; abgebaut
     * faellt er als "Don't do it" heraus und bleibt es auch nach dem erneuten Setzen. Ein normales
     * Pad, das im Amboss "Don't do it" heisst, zaehlt nicht.
     */
    public static void theLastTierSmithsBackIntoDontDoItThatWorksLikeTierOne(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(0.5, 1.0, 0.5));
        int index = 0;
        for (Family family : EasterEggs.families()) {
            EasterEggs.Step entry = EasterEggs.steps(family).get(0);
            Block tier1 = TweaksFamilies.tiers(family).get(0);
            SmithingRecipeInput in = new SmithingRecipeInput(new ItemStack(entry.templates().get(0)),
                    new ItemStack(TweaksFamilies.lastTier(family)), new ItemStack(entry.addition()));
            ItemStack dont = easterResult(helper, in, family + " entry");
            helper.assertTrue(dont.is(tier1.asItem()), family + ": the last tier smiths into " + dont + " instead of " + tier1);
            helper.assertTrue(EasterEggs.stageOf(dont) == 1, family + ": 'Don't do it' has easter stage " + EasterEggs.stageOf(dont));
            helper.assertTrue(EasterEggs.stageNameKey(1).equals(nameKey(dont)), family + ": the entry is named " + nameKey(dont));

            BlockPos pos = new BlockPos(1 + 2 * (index % 4), 1, 3 + 2 * (index / 4));
            index++;
            placeFromItem(helper, player, dont.copy(), pos);
            helper.assertTrue(helper.getBlockState(pos).is(tier1), family + ": 'Don't do it' placed " + helper.getBlockState(pos).getBlock());
            BlockPos abs = helper.absolutePos(pos);
            helper.assertTrue(helper.getBlockEntity(pos, OwnedBlockEntity.class).easterStage() == 1,
                    family + ": the placed pad forgot its easter stage");
            helper.assertTrue(!EasterEggs.isBoosted(helper.getLevel(), abs), family + ": 'Don't do it' is boosted like the final stage");

            List<ItemStack> drops = breakAndCollect(helper, player, pos);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(tier1.asItem()), family + ": breaking it dropped " + drops);
            ItemStack dropped = drops.get(0);
            helper.assertTrue(EasterEggs.stageOf(dropped) == 1 && EasterEggs.stageNameKey(1).equals(nameKey(dropped)),
                    family + ": the dropped pad is stage " + EasterEggs.stageOf(dropped) + " named " + nameKey(dropped));
            placeFromItem(helper, player, dropped, pos);
            helper.assertTrue(helper.getBlockEntity(pos, OwnedBlockEntity.class).easterStage() == 1,
                    family + ": placed again, the pad is no longer 'Don't do it'");
            helper.setBlock(pos, Blocks.AIR);
        }

        // Wirkt wie Stufe I: das Launchpad fasst 4 Windkugeln, nicht 16 (Endstufe) und nicht 32.
        BlockPos launch = new BlockPos(1, 1, 6);
        placeFromItem(helper, player, EasterEggs.create(Family.LAUNCHPAD, 1), launch);
        int capacity = ((LaunchpadBlock) helper.getBlockState(launch).getBlock()).capacityAt(helper.getLevel(), helper.absolutePos(launch));
        helper.assertTrue(capacity == 4, "'Don't do it' (launchpad) holds " + capacity + " wind charges instead of tier I's 4");
        helper.setBlock(launch, Blocks.AIR);

        // Wirkt wie Stufe I: das Trank-Pad gibt 30 s, nicht 120 s (Endstufe) und nicht 240 s.
        BlockPos potion = new BlockPos(3, 1, 7);
        placeFromItem(helper, player, EasterEggs.create(Family.POTION_PAD, 1), potion);
        int potionTicks = ((PotionPadBlock) helper.getBlockState(potion).getBlock()).effectDurationAt(helper.getLevel(), helper.absolutePos(potion));
        helper.assertTrue(potionTicks == 30 * 20, "'Don't do it' (potion pad) gives effects for " + potionTicks + " ticks instead of tier I's 600");
        helper.setBlock(potion, Blocks.AIR);

        // Amboss: ein umbenanntes normales Pad bleibt ein normales Pad.
        ItemStack renamed = new ItemStack(TweaksBlocks.LAUNCHPAD);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Don't do it"));
        helper.assertTrue(EasterEggs.stageOf(renamed) == 0, "an anvil-renamed launchpad counts as an easter pad");
        placeFromItem(helper, player, renamed, launch);
        helper.assertTrue(helper.getBlockEntity(launch, OwnedBlockEntity.class).easterStage() == 0, "a placed anvil-renamed launchpad counts as an easter pad");
        helper.setBlock(launch, Blocks.AIR);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Kette
    // =====================================================================================

    /**
     * Jede Familie laeuft die ganze Kette: jede Easter-Stufe ist der Block ihrer Stufe, heisst "Don't do
     * it", "Seriously?", "Stop. Please.", "Last Chance" bzw. zuletzt wie die Endstufe (verschleiert), und
     * kostet dieselbe Vorlage und Zutat wie die normale Aufwertung auf diese Stufe. Kein normales
     * Umwandlungsrezept nimmt ein Easter-Pad als Basis. Die Sprachdateien tragen die Namen, die letzten
     * mit 1-2 verschleierten Zeichen und nicht laenger als der normale Name.
     */
    public static void easterChainNamesEveryStageAndCostsWhatTheNormalTiersCost(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (Family family : EasterEggs.families()) {
            List<Block> tiers = TweaksFamilies.tiers(family);
            int count = EasterEggs.stageCount(family);
            ItemStack current = new ItemStack(TweaksFamilies.lastTier(family));
            int stages = 0;
            for (EasterEggs.Step step : EasterEggs.steps(family)) {
                if (step.toStage() == 0) {
                    continue;
                }
                for (Item template : List.of(step.templates().get(0), step.templates().get(step.templates().size() - 1))) {
                    SmithingRecipeInput in = new SmithingRecipeInput(new ItemStack(template), current.copy(), new ItemStack(step.addition()));
                    for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
                        if (holder.value() instanceof SmithingTransformRecipe normal && normal.matches(in, helper.getLevel())) {
                            problems.add(family + " stage " + step.fromStage() + " also fits the normal recipe " + holder.id());
                        }
                    }
                    ItemStack out = easterResult(helper, in, family + " stage " + step.toStage());
                    String expectedName = step.toStage() == count ? EasterEggs.finalNameKey(family) : EasterEggs.stageNameKey(step.toStage());
                    if (!out.is(tiers.get(step.toStage() - 1).asItem()) || EasterEggs.stageOf(out) != step.toStage() || !expectedName.equals(nameKey(out))) {
                        problems.add(family + " step to stage " + step.toStage() + " made " + out + " stage " + EasterEggs.stageOf(out) + " named " + nameKey(out));
                    }
                }
                // Kosten wie die normale Stufe: dieselbe Vorlage und Zutat machen aus dem normalen Pad die normale Zielstufe.
                if (step.fromStage() >= 1) {
                    SmithingRecipeInput normalIn = new SmithingRecipeInput(new ItemStack(step.templates().get(0)),
                            new ItemStack(tiers.get(step.fromStage() - 1)), new ItemStack(step.addition()));
                    Optional<RecipeHolder<SmithingRecipe>> normal = smithing(helper, normalIn);
                    if (normal.isEmpty() || normal.get().value() instanceof EasterSmithingRecipe
                            || !assemble(helper, normal.get(), normalIn).is(tiers.get(step.toStage() - 1).asItem())) {
                        problems.add(family + ": the easter step to stage " + step.toStage() + " does not cost what the normal tier costs");
                    }
                }
                current = EasterEggs.create(family, step.toStage());
                stages++;
            }
            if (stages != count || count != Math.min(5, tiers.size())) {
                problems.add(family + " has " + stages + " easter stages instead of " + Math.min(5, tiers.size()));
            }
        }
        helper.assertTrue(EasterEggs.stageCount(Family.FLYPAD) == 3 && EasterEggs.stageCount(Family.ELYTRA_PAD) == 5,
                "the flypad chain has " + EasterEggs.stageCount(Family.FLYPAD) + " stages, the elytra pad chain " + EasterEggs.stageCount(Family.ELYTRA_PAD));

        Map<String, String> english = Map.of(EasterEggs.stageNameKey(1), "Don't do it", EasterEggs.stageNameKey(2), "Seriously?",
                EasterEggs.stageNameKey(3), "Stop. Please.", EasterEggs.stageNameKey(4), "Last Chance");
        for (String locale : List.of("en_us", "de_de")) {
            JsonObject lang = langFile(helper, locale);
            for (int stage = 1; stage <= 4; stage++) {
                JsonElement text = lang.get(EasterEggs.stageNameKey(stage));
                if (text == null || text.getAsString().isBlank()) {
                    problems.add(locale + " misses " + EasterEggs.stageNameKey(stage));
                } else if (locale.equals("en_us") && !text.getAsString().equals(english.get(EasterEggs.stageNameKey(stage)))) {
                    problems.add("easter stage " + stage + " is called " + text.getAsString());
                }
            }
            for (Family family : EasterEggs.families()) {
                JsonElement fancy = lang.get(EasterEggs.finalNameKey(family));
                String baseKey = TweaksFamilies.lastTier(family).getDescriptionId();
                JsonElement base = lang.get(baseKey);
                if (fancy == null || base == null) {
                    problems.add(locale + " misses " + EasterEggs.finalNameKey(family) + " or " + baseKey);
                    continue;
                }
                String visible = fancy.getAsString().replaceAll("§.", "");
                Matcher obfuscated = Pattern.compile("§k").matcher(fancy.getAsString());
                int obf = 0;
                while (obfuscated.find()) {
                    obf++;
                }
                if (!visible.equals(base.getAsString()) || obf < 1 || obf > 2 || !fancy.getAsString().contains("§l")) {
                    problems.add(locale + " " + family + ": final name shows '" + visible + "' with " + obf + " obfuscated characters (base '"
                            + base.getAsString() + "')");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " easter chain problems: " + problems);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Letzte Easter-Stufe: doppelte Kraft
    // =====================================================================================

    /**
     * Die letzte Easter-Stufe ist doppelt so stark wie die Endstufe: Elytra-Pad und Flypad doppelt so
     * breit und hoch (echter Durchlauf mit einem Spieler oberhalb der normalen Hoehe), Launchpad fasst 32
     * Windkugeln, Chunk-Loader haelt 5x5 Chunks, Spawn-Teleporter wartet halb so lange, Trank-Pad gibt
     * 240 s statt 120 s. Zwischenstufen wirken wie ihre normale Stufe. Das Trank-Pad behaelt beim Abbauen
     * und Wiedersetzen Easter-Stufe und gespeicherten Trank.
     */
    public static void theFinalEasterPadIsTwiceAsStrongAsTheLastTier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Elytra-Pad V: 128 -> 256 breit, 127 -> 254 hoch. Kein Durchlauf mit Spielern: der doppelte
        // Bereich reicht 127 Bloecke weit und wuerde den Spielern der Nachbartests Elytren anziehen.
        BlockPos elytra = new BlockPos(1, 1, 1);
        BlockPos elytraAbs = helper.absolutePos(elytra);
        OwnedBlockEntity elytraBe = placeStaged(helper, elytra, TweaksBlocks.FINE_ELYTRA_PAD, 0);
        AABB normalArea = ElytraPadBlockEntity.areaOf(level, elytraAbs, helper.getBlockState(elytra));
        elytraBe.setEasterStage(5);
        helper.assertTrue(EasterEggs.isBoosted(level, elytraAbs), "the final easter elytra pad is not boosted");
        AABB doubledArea = ElytraPadBlockEntity.areaOf(level, elytraAbs, helper.getBlockState(elytra));
        helper.assertTrue(Math.abs(normalArea.getXsize() - 128) < 1e-9 && Math.abs(doubledArea.getXsize() - 256) < 1e-9
                        && Math.abs(doubledArea.getZsize() - 256) < 1e-9
                        && Math.abs((doubledArea.maxY - elytraAbs.getY()) - 2 * (normalArea.maxY - elytraAbs.getY())) < 1e-9,
                "the final easter elytra pad covers " + doubledArea.getXsize() + " x " + (doubledArea.maxY - elytraAbs.getY())
                        + " instead of twice " + normalArea.getXsize() + " x " + (normalArea.maxY - elytraAbs.getY()));
        helper.setBlock(elytra, Blocks.AIR);

        // Zwischenstufe: Easter-Stufe 4 auf dem Enderit-Pad IV wirkt wie Stufe IV.
        BlockPos middle = new BlockPos(3, 1, 1);
        placeStaged(helper, middle, TweaksBlocks.ENDERITE_ELYTRA_PAD, 4);
        AABB middleArea = ElytraPadBlockEntity.areaOf(level, helper.absolutePos(middle), helper.getBlockState(middle));
        helper.assertTrue(!EasterEggs.isBoosted(level, helper.absolutePos(middle)) && Math.abs(middleArea.getXsize() - 32) < 1e-9,
                "easter stage 4 of the elytra pad does not work like tier IV (" + middleArea.getXsize() + " wide)");
        helper.setBlock(middle, Blocks.AIR);

        // Flypad III: 16x16x24 -> 32x32x48 (aus demselben Grund ohne Spieler-Durchlauf).
        BlockPos fly = new BlockPos(5, 1, 1);
        BlockPos flyAbs = helper.absolutePos(fly);
        OwnedBlockEntity flyBe = placeStaged(helper, fly, TweaksBlocks.STELLAR_FLYPAD, 0);
        AABB flyNormal = FlypadBlockEntity.areaOf(level, flyAbs, helper.getBlockState(fly));
        flyBe.setEasterStage(3);
        AABB flyDoubled = FlypadBlockEntity.areaOf(level, flyAbs, helper.getBlockState(fly));
        helper.assertTrue(Math.abs(flyNormal.getXsize() - 16) < 1e-9 && Math.abs(flyNormal.getYsize() - 24) < 1e-9
                        && Math.abs(flyDoubled.getXsize() - 32) < 1e-9 && Math.abs(flyDoubled.getZsize() - 32) < 1e-9
                        && Math.abs(flyDoubled.getYsize() - 48) < 1e-9,
                "the final easter flypad covers " + flyDoubled.getXsize() + "x" + flyDoubled.getYsize() + " instead of 32x48");
        helper.setBlock(fly, Blocks.AIR);

        // Launchpad III: 16 -> 32 Windkugeln.
        BlockPos launch = new BlockPos(7, 1, 1);
        placeStaged(helper, launch, TweaksBlocks.ENDERITE_LAUNCHPAD, 3);
        ServerPlayer loader = mockPlayer(helper, new Vec3(7.5, 1.0, 3.5));
        loader.getAbilities().instabuild = false;
        ItemStack charges = new ItemStack(Items.WIND_CHARGE, 64);
        ((LaunchpadBlock) TweaksBlocks.ENDERITE_LAUNCHPAD).deposit(charges, level, helper.absolutePos(launch), loader, true);
        int stored = helper.getBlockEntity(launch, LaunchpadBlockEntity.class).getCharges();
        helper.assertTrue(stored == 32 && charges.getCount() == 32, "the final easter launchpad holds " + stored + " wind charges instead of 32");
        helper.setBlock(launch, Blocks.AIR);

        // Spawn-Teleporter III (drei Stufen seit 2026-09-28): 100 -> 50 Ticks. Ein altes Easter-V
        // (gespeicherte Stufe 5 aus der Zeit mit fuenf Stufen) zaehlt als die letzte Stufe 3.
        BlockPos tp = new BlockPos(9, 1, 1);
        OwnedBlockEntity tpBe = placeStaged(helper, tp, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER, 0);
        int normalTicks = SpawnTeleporterBlockEntity.requiredTicks(level, helper.absolutePos(tp), 3);
        tpBe.setEasterStage(3);
        int easterTicks = SpawnTeleporterBlockEntity.requiredTicks(level, helper.absolutePos(tp), 3);
        helper.assertTrue(normalTicks == SpawnTeleporterBlockEntity.ENDERITE_TICKS && easterTicks * 2 == normalTicks,
                "the final easter spawn teleporter waits " + easterTicks + " ticks, the normal one " + normalTicks);
        tpBe.setEasterStage(5);
        helper.assertTrue(tpBe.easterStage() == 3 && EasterEggs.isBoosted(level, helper.absolutePos(tp)),
                "an old easter stage 5 on the enderite spawn teleporter reads as stage " + tpBe.easterStage() + " instead of the final 3");
        helper.setBlock(tp, Blocks.AIR);

        // Trank-Pad III: 120 s -> 240 s, auch wirklich am Spieler; Easter-Stufe 2 wirkt wie Stufe II (60 s).
        BlockPos potion = new BlockPos(1, 1, 5);
        BlockPos potionAbs = helper.absolutePos(potion);
        PotionPadBlockEntity potionBe = (PotionPadBlockEntity) placeStaged(helper, potion, TweaksBlocks.INFUSED_POTION_PAD, 0);
        PotionPadBlock infused = (PotionPadBlock) TweaksBlocks.INFUSED_POTION_PAD;
        int potionNormal = infused.effectDurationAt(level, potionAbs);
        potionBe.setEasterStage(3);
        int potionDoubled = infused.effectDurationAt(level, potionAbs);
        helper.assertTrue(potionNormal == 120 * 20 && potionDoubled == 240 * 20,
                "the final easter potion pad gives effects for " + potionDoubled + " ticks, the normal one " + potionNormal + " (expected 4800 and 2400)");
        PotionPadBlock.absorb(potionBe, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_SWIFTNESS));
        ServerPlayer drinker = mockPlayer(helper, new Vec3(1.5, 1.0, 5.5));
        drinker.removeAllEffects();
        for (int step = 1; step <= PotionPadBlockEntity.RAMP_STEPS; step++) {
            potionBe.grant(level, drinker, step);
        }
        int given = drinker.hasEffect(MobEffects.SPEED) ? drinker.getEffect(MobEffects.SPEED).getDuration() : -1;
        helper.assertTrue(given == 240 * 20, "standing 3 s on the final easter potion pad gave Speed for " + given + " ticks instead of 4800");
        // Abklingzeit: doppelte Wirkdauer, also 480 s statt 240 s.
        helper.assertTrue(potionBe.getCooldown() == 480 * 20, "the final easter potion pad cools down for " + potionBe.getCooldown() + " ticks instead of 9600");
        helper.setBlock(potion, Blocks.AIR);
        BlockPos potionMiddle = new BlockPos(3, 1, 5);
        placeStaged(helper, potionMiddle, TweaksBlocks.REINFORCED_POTION_PAD, 2);
        int middleTicks = ((PotionPadBlock) TweaksBlocks.REINFORCED_POTION_PAD).effectDurationAt(level, helper.absolutePos(potionMiddle));
        helper.assertTrue(!EasterEggs.isBoosted(level, helper.absolutePos(potionMiddle)) && middleTicks == 60 * 20,
                "easter stage 2 of the potion pad does not work like tier II (" + middleTicks + " ticks)");
        helper.setBlock(potionMiddle, Blocks.AIR);

        // Setzen und Abbauen behalten beides: die Easter-Stufe und den gespeicherten Trank.
        BlockPos keep = new BlockPos(5, 1, 5);
        ServerPlayer builder = mockPlayer(helper, new Vec3(5.5, 1.0, 7.5));
        placeFromItem(helper, builder, EasterEggs.create(Family.POTION_PAD, 3), keep);
        PotionPadBlockEntity keepBe = helper.getBlockEntity(keep, PotionPadBlockEntity.class);
        PotionPadBlock.absorb(keepBe, PotionContents.createItemStack(Items.LINGERING_POTION, Potions.STRONG_STRENGTH));
        List<ItemStack> keptDrops = breakAndCollect(helper, builder, keep);
        ItemStack kept = keptDrops.size() == 1 ? keptDrops.get(0) : ItemStack.EMPTY;
        PotionContents keptPotion = kept.get(DataComponents.POTION_CONTENTS);
        helper.assertTrue(kept.is(TweaksBlocks.INFUSED_POTION_PAD.asItem()) && EasterEggs.stageOf(kept) == 3
                        && EasterEggs.finalNameKey(Family.POTION_PAD).equals(nameKey(kept))
                        && keptPotion != null && keptPotion.is(Potions.STRONG_STRENGTH),
                "breaking the final easter potion pad dropped " + keptDrops + " (stage " + EasterEggs.stageOf(kept) + ", potion " + keptPotion + ")");
        placeFromItem(helper, builder, kept, keep);
        keepBe = helper.getBlockEntity(keep, PotionPadBlockEntity.class);
        helper.assertTrue(keepBe.easterStage() == 3 && EasterEggs.isBoosted(level, helper.absolutePos(keep))
                        && keepBe.getStored() != null && keepBe.getStored().is(Potions.STRONG_STRENGTH),
                "placed again, the final easter potion pad is stage " + keepBe.easterStage() + " holding " + keepBe.getStored());
        helper.setBlock(keep, Blocks.AIR);
        // Ein normales Trank-Pad behaelt seinen Trank ebenso, ohne Easter-Stufe.
        helper.setBlock(keep, TweaksBlocks.POTION_PAD);
        PotionPadBlock.absorb(helper.getBlockEntity(keep, PotionPadBlockEntity.class),
                PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE));
        List<ItemStack> plainDrops = breakAndCollect(helper, builder, keep);
        ItemStack plain = plainDrops.size() == 1 ? plainDrops.get(0) : ItemStack.EMPTY;
        PotionContents plainPotion = plain.get(DataComponents.POTION_CONTENTS);
        helper.assertTrue(plain.is(TweaksBlocks.POTION_PAD.asItem()) && EasterEggs.stageOf(plain) == 0
                        && plainPotion != null && plainPotion.is(Potions.FIRE_RESISTANCE),
                "breaking a normal potion pad dropped " + plainDrops + " (potion " + plainPotion + ")");
        placeFromItem(helper, builder, plain, keep);
        PotionContents replaced = helper.getBlockEntity(keep, PotionPadBlockEntity.class).getStored();
        helper.assertTrue(replaced != null && replaced.is(Potions.FIRE_RESISTANCE)
                        && helper.getBlockEntity(keep, PotionPadBlockEntity.class).easterStage() == 0,
                "placed again, the normal potion pad holds " + replaced);
        helper.setBlock(keep, Blocks.AIR);

        // Chunk-Loader III: 3x3 -> 5x5, weit weg von der Teststruktur.
        BlockPos far = helper.absolutePos(new BlockPos(1, 1, 1)).offset(12288 + 3 * 64, 0, 12288 + 64);
        int cx = far.getX() >> 4;
        int cz = far.getZ() >> 4;
        try {
            level.setBlock(far, TweaksBlocks.ENDERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity chunkBe = (ChunkLoaderBlockEntity) level.getBlockEntity(far);
            chunkBe.setEasterStage(3);
            chunkBe.update(level);
            helper.assertTrue(chunkBe.ownForced().size() == 25, "the final easter chunk loader forced " + chunkBe.ownForced().size() + " chunks instead of 25");
            helper.assertTrue(isForced(level, cx + 2, cz - 2) && chunkBe.covers(cx - 2, cz + 2) && !chunkBe.covers(cx + 3, cz),
                    "the final easter chunk loader does not cover exactly the 5x5 chunks");
            level.setBlock(far, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(!isForced(level, cx + 2, cz + 2), "a broken final easter chunk loader keeps its outer chunks forced");
        } finally {
            level.setBlock(far, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Advancements
    // =====================================================================================

    /**
     * Vier versteckte Advancements in einem eigenen Tab: "What have you done?" fuer "Don't do it",
     * "Seriously?" fuer Easter-Stufe 2, "It Was Worth It" fuer die letzte Easter-Stufe einer Familie
     * (Stufe 4 von 5 reicht nicht), "All That for a Stick?" fuer den Funny Stick. Ein umbenanntes
     * normales Pad loest nichts aus.
     */
    public static void theEasterAdvancementsAreHiddenAndFireAlongTheChain(GameTestHelper helper) {
        ServerAdvancementManager manager = helper.getLevel().getServer().getAdvancements();
        AdvancementHolder root = manager.get(EasterEggs.ADV_WHAT_HAVE_YOU_DONE);
        AdvancementHolder seriously = manager.get(EasterEggs.ADV_SERIOUSLY);
        AdvancementHolder worth = manager.get(EasterEggs.ADV_WORTH_IT);
        AdvancementHolder stick = manager.get(EasterEggs.ADV_FUNNY_STICK);
        helper.assertTrue(root != null && seriously != null && worth != null && stick != null, "an easter advancement is not loaded");
        helper.assertTrue(root.value().parent().isEmpty(), "'What have you done?' is not the root of its own tab");
        helper.assertTrue(seriously.value().parent().equals(Optional.of(root.id())) && worth.value().parent().equals(Optional.of(seriously.id()))
                && stick.value().parent().equals(Optional.of(worth.id())), "the easter advancements are not chained root -> seriously -> worth it -> stick");
        for (AdvancementHolder holder : List.of(root, seriously, worth, stick)) {
            JsonObject display = displayJson(helper, holder);
            helper.assertTrue(display != null && display.has("hidden") && display.get("hidden").getAsBoolean(), holder.id() + " is not hidden");
        }
        helper.assertTrue(displayJson(helper, root).has("background"), "the easter tab has no background");

        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ItemStack renamed = new ItemStack(TweaksBlocks.ELYTRA_PAD);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Don't do it"));
        give(player, renamed, 0);
        helper.assertTrue(!done(player, root), "an anvil-renamed pad earned 'What have you done?'");
        give(player, EasterEggs.create(Family.ELYTRA_PAD, 1), 1);
        helper.assertTrue(done(player, root) && !done(player, seriously), "'Don't do it' did not earn exactly 'What have you done?'");
        give(player, EasterEggs.create(Family.LAUNCHPAD, 2), 2);
        helper.assertTrue(done(player, seriously) && !done(player, worth), "easter stage 2 did not earn exactly 'Seriously?'");
        give(player, EasterEggs.create(Family.ELYTRA_PAD, 4), 3);
        helper.assertTrue(!done(player, worth), "easter stage 4 of the five-tier elytra pad earned 'It Was Worth It'");
        give(player, EasterEggs.create(Family.FLYPAD, 3), 4);
        helper.assertTrue(done(player, worth) && !done(player, stick), "the final easter flypad did not earn exactly 'It Was Worth It'");
        ServerPlayer brewer = mockPlayer(helper, new Vec3(3.5, 1.0, 1.5));
        give(brewer, EasterEggs.create(Family.POTION_PAD, 2), 0);
        helper.assertTrue(!done(brewer, worth), "easter stage 2 of the potion pad earned 'It Was Worth It'");
        give(brewer, EasterEggs.create(Family.POTION_PAD, 3), 1);
        helper.assertTrue(done(brewer, worth), "the final easter potion pad did not earn 'It Was Worth It'");
        give(player, new ItemStack(EasterEggs.funnyStick()), 5);
        helper.assertTrue(done(player, stick), "the Funny Stick did not earn 'All That for a Stick?'");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Funny Stick
    // =====================================================================================

    /**
     * Nur die letzte Easter-Stufe wird mit Netherit-Vorlage und Netheritbarren zum Funny Stick (ein
     * frischer Stock ohne die Komponenten des Pads); die Stufe davor und die normale Endstufe nicht. In
     * der Hand spruehen Funken.
     */
    public static void theFunnyStickIsSmithedFromTheFinalPadAndSparklesInTheHand(GameTestHelper helper) {
        Item stick = EasterEggs.funnyStick();
        for (Family family : EasterEggs.families()) {
            int count = EasterEggs.stageCount(family);
            SmithingRecipeInput in = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    EasterEggs.create(family, count), new ItemStack(Items.NETHERITE_INGOT));
            ItemStack out = easterResult(helper, in, family + " funny stick");
            helper.assertTrue(out.is(stick) && out.getComponentsPatch().isEmpty(), family + ": the final pad smiths into " + out);
            for (ItemStack base : List.of(EasterEggs.create(family, count - 1), new ItemStack(TweaksFamilies.lastTier(family)))) {
                Optional<RecipeHolder<SmithingRecipe>> none = smithing(helper,
                        new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(Items.NETHERITE_INGOT)));
                helper.assertTrue(none.isEmpty() || !assemble(helper, none.get(),
                                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(Items.NETHERITE_INGOT))).is(stick),
                        family + ": " + base + " (stage " + EasterEggs.stageOf(base) + ") smiths into a Funny Stick");
            }
        }
        ItemStack funny = new ItemStack(stick);
        helper.assertTrue(funny.getMaxStackSize() == 1 && funny.hasFoil() && funny.getRarity() == Rarity.EPIC,
                "the Funny Stick is not a glinting epic single item");
        helper.assertTrue(FunnyStickItem.isHeld(EquipmentSlot.MAINHAND) && FunnyStickItem.isHeld(EquipmentSlot.OFFHAND)
                && !FunnyStickItem.isHeld(null) && !FunnyStickItem.isHeld(EquipmentSlot.HEAD), "the Funny Stick sparkles outside the hands");
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, funny);
        int sent = FunnyStickItem.sparkle(helper.getLevel(), player, false);
        helper.assertTrue(sent >= 1, "the Funny Stick sent its sparkle to " + sent + " players, not even to its holder");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Versteckt
    // =====================================================================================

    /**
     * Versteckt: der Funny Stick steht in {@code c:hidden_from_recipe_viewers} und ist, wie jedes
     * Easter-Pad, in keinem Kreativ-Tab; die 28 Rezepte der Kette sind Spezialrezepte ohne Anzeige und
     * ohne Freischalt-Meldung, kein Rezept-Advancement verweist auf sie; die Testzentrale nimmt den Stock
     * begruendet aus.
     */
    public static void theEasterEggsAreHiddenFromRecipeViewersAndCreativeTabs(GameTestHelper helper) {
        Item stick = EasterEggs.funnyStick();
        List<String> problems = new ArrayList<>();
        if (!new ItemStack(stick).is(HIDDEN)) {
            problems.add("the Funny Stick is not in c:hidden_from_recipe_viewers");
        }
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            ModItemGroupsContent.populate(tab, (net.minecraft.world.item.CreativeModeTab.Output) (stack, visibility) -> {
                if (stack.is(stick) || EasterEggs.stageOf(stack) > 0) {
                    problems.add(tab + " offers " + stack);
                }
            }, helper.getLevel().registryAccess());
        }
        for (CreativeTabLayout.Row row : TweaksItems.functionalRows()) {
            for (ItemStack stack : row.stacks()) {
                if (EasterEggs.stageOf(stack) > 0) {
                    problems.add("the tab row " + row.name() + " holds an easter pad");
                }
            }
        }
        int easterRecipes = 0;
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof EasterSmithingRecipe recipe)) {
                continue;
            }
            easterRecipes++;
            if (!holder.id().identifier().getPath().startsWith("easter/")) {
                problems.add(holder.id() + " is an easter recipe outside recipe/easter/");
            }
            if (!recipe.isSpecial() || !recipe.display().isEmpty() || recipe.showNotification()) {
                problems.add(holder.id() + " shows up in the recipe book or in recipe viewers");
            }
        }
        int expected = 0;
        for (Family family : EasterEggs.families()) {
            expected += EasterEggs.stageCount(family) + 1;
        }
        if (easterRecipes != expected) {
            problems.add(easterRecipes + " easter recipes loaded instead of " + expected);
        }
        for (AdvancementHolder holder : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
            if (holder.id().getPath().startsWith("recipes/") && holder.id().getPath().contains("easter")) {
                problems.add("the recipe advancement " + holder.id() + " unlocks an easter recipe");
            }
        }
        if (!com.simplebuilding.dev.testcentre.TestCentreLayout.EXCLUDED.containsKey("simplebuilding:funny_stick")) {
            problems.add("the test centre does not exclude the Funny Stick on purpose");
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " easter visibility problems: " + problems);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static ItemStack easterResult(GameTestHelper helper, SmithingRecipeInput in, String what) {
        Optional<RecipeHolder<SmithingRecipe>> match = smithing(helper, in);
        helper.assertTrue(match.isPresent(), "no smithing recipe for " + what + " (" + in.base() + ")");
        helper.assertTrue(match.get().value() instanceof EasterSmithingRecipe, what + " is smithed by the normal recipe " + match.get().id());
        return assemble(helper, match.get(), in);
    }

    private static String nameKey(ItemStack stack) {
        Component name = stack.get(DataComponents.ITEM_NAME);
        return name != null && name.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(name);
    }

    /** Setzt das Item wie ein Spieler (BlockItem#place, also mit Block-Entity-Komponenten) auf den Boden. */
    private static void placeFromItem(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        // Fester Grund darunter: der Klick trifft dessen Oberseite, das Pad landet genau auf pos.
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
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.0))) {
            out.add(item.getItem().copy());
            item.discard();
        }
        return out;
    }

    private static OwnedBlockEntity placeStaged(GameTestHelper helper, BlockPos pos, Block block, int stage) {
        helper.setBlock(pos, block);
        OwnedBlockEntity be = helper.getBlockEntity(pos, OwnedBlockEntity.class);
        be.setEasterStage(stage);
        return be;
    }

    /** Legt das Item ins Inventar; der Container-Listener des Spielers meldet es wie im Spiel an inventory_changed. */
    private static void give(ServerPlayer player, ItemStack stack, int slot) {
        player.getInventory().setItem(9 + slot, stack);
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean done(ServerPlayer player, AdvancementHolder holder) {
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Die Anzeige als JSON (DisplayInfo ist auf 26.2 eine Klasse, auf 26.3 ein Record). */
    private static JsonObject displayJson(GameTestHelper helper, AdvancementHolder holder) {
        Optional<DisplayInfo> display = holder.value().display();
        if (display.isEmpty()) {
            return null;
        }
        return DisplayInfo.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), display.get())
                .getOrThrow().getAsJsonObject();
    }

    private static boolean isForced(ServerLevel level, int chunkX, int chunkZ) {
        return level.getForceLoadedChunks().contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static Optional<RecipeHolder<SmithingRecipe>> smithing(GameTestHelper helper, SmithingRecipeInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());
    }

    private static ItemStack assemble(GameTestHelper helper, RecipeHolder<SmithingRecipe> holder, SmithingRecipeInput input) {
        return holder.value().assemble(input, helper.getLevel().registryAccess());
    }

    private static JsonObject langFile(GameTestHelper helper, String locale) {
        String path = "assets/simplebuilding/lang/" + locale + ".json";
        try (InputStream in = TweaksEasterTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }
}
