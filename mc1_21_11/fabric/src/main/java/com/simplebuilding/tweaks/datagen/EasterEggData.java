package com.simplebuilding.tweaks.datagen;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.TweaksFamilies;
import com.simplebuilding.tweaks.block.TweaksFamilies.Family;
import com.simplebuilding.tweaks.easter.EasterEggs;
import com.simplebuilding.tweaks.easter.EasterSmithingRecipe;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

/**
 * Rezepte und Advancements der versteckten Kette ueber den Endstufen ({@link EasterEggs}; Spoiler in
 * docs/SIMPLETWEAKS-UEBERNAHME.md), 1.21.11-Fassung der 26.x-Datei unter src/main/java. Die vier
 * Advancements reisen wie dort ueber {@link RecipeOutput#accept} mit je einem Rezept der Kette.
 *
 * <p>Rezepte unter {@code recipe/easter/}: kein Freischalt-Advancement, kein Rezeptbuch, keine
 * JEI-Anzeige. Advancements unter {@code advancement/easter/}: alle versteckt, der erste bildet einen
 * eigenen Tab, der erst mit ihm erscheint.
 */
public final class EasterEggData {
    private EasterEggData() {
    }

    public static void generate(RecipeOutput output, HolderGetter<Item> items) {
        Map<String, AdvancementHolder> advancements = advancements(items);
        for (Family family : EasterEggs.families()) {
            for (EasterEggs.Step step : EasterEggs.steps(family)) {
                String name = recipeName(step);
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, SimpleTweaks.id(name));
                Recipe<?> recipe = new EasterSmithingRecipe(Optional.of(Ingredient.of(step.templates().toArray(ItemLike[]::new))),
                        Ingredient.of(step.base()), step.fromStage(),
                        step.addition() == null ? Optional.empty() : Optional.of(Ingredient.of(step.addition())),
                        step.result(), step.toStage());
                // Die Advancements haengen an den Schritten des Elytra-Pads (nur als Transportmittel).
                output.accept(key, recipe, family == Family.ELYTRA_PAD ? advancements.get(stepKey(step)) : null);
            }
        }
    }

    /** {@code easter/<familie>_stage_<n>} bzw. {@code easter/<familie>_funny_stick}. */
    public static String recipeName(EasterEggs.Step step) {
        String family = step.family().name().toLowerCase(Locale.ROOT);
        return "easter/" + family + (step.toStage() == 0 ? "_funny_stick" : "_stage_" + step.toStage());
    }

    private static String stepKey(EasterEggs.Step step) {
        if (step.toStage() == 0) {
            return "stick";
        }
        return step.toStage() == EasterEggs.stageCount(step.family()) ? "final" : "stage_" + step.toStage();
    }

    private static Map<String, AdvancementHolder> advancements(HolderGetter<Item> items) {
        Map<String, AdvancementHolder> out = new HashMap<>();
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(display(TweaksBlocks.ELYTRA_PAD.asItem(), "what_have_you_done", AdvancementType.TASK, true))
                .addCriterion("dont_do_it", InventoryChangeTrigger.TriggerInstance.hasItems(staged(items, null, 1)))
                .build(EasterEggs.ADV_WHAT_HAVE_YOU_DONE);
        out.put("stage_1", root);

        AdvancementHolder seriously = Advancement.Builder.advancement()
                .parent(root)
                .display(display(TweaksBlocks.REINFORCED_ELYTRA_PAD.asItem(), "seriously", AdvancementType.TASK, false))
                .addCriterion("seriously", InventoryChangeTrigger.TriggerInstance.hasItems(staged(items, null, 2)))
                .build(EasterEggs.ADV_SERIOUSLY);
        out.put("stage_2", seriously);

        Advancement.Builder worthIt = Advancement.Builder.advancement()
                .parent(seriously)
                .display(display(TweaksBlocks.FINE_ELYTRA_PAD.asItem(), "it_was_worth_it", AdvancementType.CHALLENGE, false))
                .requirements(AdvancementRequirements.Strategy.OR);
        for (Family family : EasterEggs.families()) {
            int last = EasterEggs.stageCount(family);
            worthIt.addCriterion(family.name().toLowerCase(Locale.ROOT),
                    InventoryChangeTrigger.TriggerInstance.hasItems(staged(items, TweaksFamilies.lastTier(family).asItem(), last)));
        }
        AdvancementHolder worth = worthIt.build(EasterEggs.ADV_WORTH_IT);
        out.put("final", worth);

        AdvancementHolder stick = Advancement.Builder.advancement()
                .parent(worth)
                .display(display(EasterEggs.funnyStick(), "all_that_for_a_stick", AdvancementType.CHALLENGE, false))
                .addCriterion("funny_stick", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(items, EasterEggs.funnyStick())))
                .build(EasterEggs.ADV_FUNNY_STICK);
        out.put("stick", stick);
        return out;
    }

    /** Ein Item (oder jedes, bei null) mit genau dieser Easter-Stufe. */
    private static ItemPredicate.Builder staged(HolderGetter<Item> items, Item item, int stage) {
        ItemPredicate.Builder builder = ItemPredicate.Builder.item();
        if (item != null) {
            builder.of(items, item);
        }
        return builder.withComponents(DataComponentMatchers.Builder.components()
                .exact(DataComponentExactPredicate.expect(EasterEggs.EASTER_STAGE, stage)).build());
    }

    /** Versteckt, mit Toast und Chat-Meldung; nur die Wurzel hat einen Hintergrund (eigener Tab). */
    private static DisplayInfo display(Item icon, String name, AdvancementType type, boolean root) {
        String key = "advancements.simplebuilding.easter." + name;
        Optional<ClientAsset.ResourceTexture> background = root
                ? Optional.of(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace("gui/advancements/backgrounds/end")))
                : Optional.empty();
        return new DisplayInfo(new ItemStack(icon), Component.translatable(key + ".title"),
                Component.translatable(key + ".description"), background, type, true, true, true);
    }
}
