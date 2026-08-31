package com.simplebuilding.mixin;

import com.mojang.datafixers.util.Pair;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.StructureConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin extends ItemCombinerMenu {

    @Shadow private int repairItemCountCost;
    @Shadow @Final private DataSlot cost;
    @Shadow @Nullable private String itemName;

    public AnvilScreenHandlerMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    // --- 1. SLEDGEHAMMER CUSTOM LOGIC (HEAD) ---
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void updateResultHead(CallbackInfo ci) {
        ItemStack leftStack = this.inputSlots.getItem(0);
        ItemStack rightStack = this.inputSlots.getItem(1);

        if (leftStack.isEmpty()) return;

        if (leftStack.getItem() instanceof SledgehammerItem) {

            // FALL A: HAMMER + HAMMER
            if (rightStack.getItem() instanceof SledgehammerItem) {
                // 1. Haltbarkeit berechnen (Deine bestehende Logik)
                int leftDurability = leftStack.getMaxDamage() - leftStack.getDamageValue();
                int rightDurability = rightStack.getMaxDamage() - rightStack.getDamageValue();

                int bonus = (int) (leftStack.getMaxDamage() * 0.12f);
                int combinedDurability = leftDurability + rightDurability + bonus;

                int newDamage = leftStack.getMaxDamage() - combinedDurability;
                if (newDamage < 0) newDamage = 0;

                ItemStack result = leftStack.copy();
                result.setDamageValue(newDamage);
                result.set(DataComponents.REPAIR_COST, 0);

                // 2. VERZAUBERUNGEN KOMBINIEREN (NEU HINZUGEFÜGT)
                // Wir holen die Enchants von links und rechts
                var leftEnchants = EnchantmentHelper.getEnchantmentsForCrafting(leftStack);
                var rightEnchants = EnchantmentHelper.getEnchantmentsForCrafting(rightStack);

                // Builder starten mit den Enchants vom linken Item
                net.minecraft.world.item.enchantment.ItemEnchantments.Mutable builder = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(leftEnchants);

                // Durch alle Enchants des rechten Items iterieren
                for (var entry : rightEnchants.entrySet()) {
                    Holder<Enchantment> enchantment = entry.getKey();
                    int rightLevel = entry.getIntValue();
                    int leftLevel = leftEnchants.getLevel(enchantment);

                    // Vanilla Amboss-Logik für Level-Berechnung
                    int newLevel = leftLevel;
                    if (leftLevel == rightLevel) {
                        newLevel = rightLevel + 1;
                    } else {
                        newLevel = Math.max(leftLevel, rightLevel);
                    }

                    // Max Level des Enchantments beachten
                    if (newLevel > enchantment.value().getMaxLevel()) {
                        newLevel = enchantment.value().getMaxLevel();
                    }

                    // Enchantment setzen
                    builder.set(enchantment, newLevel);
                }

                // FIX: "set" statt "setEnchantments"
                EnchantmentHelper.setEnchantments(result, builder.toImmutable());

                // 3. Restliche Logik (Renaming, Kosten, Output)
                handleRenaming(leftStack, result);

                this.resultSlots.setItem(0, result);

                // Kostenberechnung könnte man hier noch verfeinern basierend auf Enchants,
                // aber für dein Mod-Design scheint 1 Level okay zu sein.
                this.cost.set(1);
                this.repairItemCountCost = 1;

                ci.cancel();
            }
            // FALL B: HAMMER + MATERIAL (Bleibt wie gehabt, da hier keine Enchants vom Material kommen)
            else if (leftStack.isDamaged() && leftStack.isValidRepairItem(rightStack)) {
                ItemStack result = leftStack.copy();

                // Teilen durch 11 statt 4
                int repairPerItem = result.getMaxDamage() / 11;
                if (repairPerItem <= 0) repairPerItem = 1;

                int damage = result.getDamageValue();
                int materialsUsed = 0;
                int materialsAvailable = rightStack.getCount();

                while (damage > 0 && materialsUsed < materialsAvailable) {
                    damage -= repairPerItem;
                    materialsUsed++;
                }

                if (damage < 0) damage = 0;
                result.setDamageValue(damage);

                this.repairItemCountCost = materialsUsed;

                int cost = materialsUsed;
                if (handleRenaming(leftStack, result)) {
                    cost += 1;
                }

                result.set(DataComponents.REPAIR_COST, 0);

                if (cost <= 0) cost = 1;
                this.cost.set(Math.min(cost, 39));

                this.resultSlots.setItem(0, result);
                ci.cancel();
            }
        }
    }

    // --- 2. GENERAL FIXES & RESTRICTIONS (RETURN) ---
    @Inject(method = "createResult", at = @At("RETURN"))
    private void applyAnvilTweaks(CallbackInfo ci) {
        ItemStack outputStack = this.resultSlots.getItem(0);

        if (outputStack.isEmpty()) return;

        // A) SLEDGEHAMMER REPAIR COST FIX
        if (outputStack.getItem() instanceof SledgehammerItem) {
            outputStack.set(DataComponents.REPAIR_COST, 0);
            if (this.cost.get() >= 40) this.cost.set(39);
        }

        // B) COLOR PALETTE RESTRICTION
        HolderLookup.Provider registryManager = this.player.level().registryAccess();
        var colorPaletteEntry = getEnchantment(registryManager, ModEnchantments.COLOR_PALETTE);
        var masterBuilderEntry = getEnchantment(registryManager, ModEnchantments.MASTER_BUILDER);

        if (colorPaletteEntry != null && masterBuilderEntry != null) {
            int colorPaletteLevel = EnchantmentHelper.getItemEnchantmentLevel(colorPaletteEntry, outputStack);
            int masterBuilderLevel = EnchantmentHelper.getItemEnchantmentLevel(masterBuilderEntry, outputStack);

            if (colorPaletteLevel > 0 && masterBuilderLevel <= 0) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.cost.set(0);
            }
        }
    }

    @Unique
    private boolean handleRenaming(ItemStack original, ItemStack result) {
        boolean renamed = false;
        if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
            if (!this.itemName.equals(original.getHoverName().getString())) {
                result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
                renamed = true;
            }
        } else if (original.has(DataComponents.CUSTOM_NAME)) {
            result.remove(DataComponents.CUSTOM_NAME);
            renamed = true;
        }
        return renamed;
    }

    @Unique
    private Holder<Enchantment> getEnchantment(HolderLookup.Provider registry, net.minecraft.resources.ResourceKey<Enchantment> key) {
        Optional<Holder.Reference<Enchantment>> optional = registry.lookupOrThrow(Registries.ENCHANTMENT).get(key);
        return optional.orElse(null);
    }

    // ... Imports bleiben gleich ...

    // FIX: Wir definieren die Tags explizit, um sicherzugehen, dass sie existieren.
    // In Vanilla 1.21 sind dies die korrekten Tag-IDs (JSON-Dateien in data/minecraft/tags/worldgen/structure/)
    @Unique private static final TagKey<Structure> ANCIENT_CITY_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "ancient_city"));
    @Unique private static final TagKey<Structure> BASTION_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "bastion_remnant"));
    @Unique private static final TagKey<Structure> FORTRESS_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "fortress"));
    @Unique private static final TagKey<Structure> OUTPOST_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "pillager_outpost"));
    @Unique private static final TagKey<Structure> END_CITY_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "end_city"));
    @Unique private static final TagKey<Structure> MINESHAFT_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "mineshaft"));
    @Unique private static final TagKey<Structure> VILLAGE_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "village"));
    @Unique private static final TagKey<Structure> SHIPWRECK_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "shipwreck"));
    @Unique private static final TagKey<Structure> IGLOO_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "igloo"));
    @Unique private static final TagKey<Structure> DESERT_PYRAMID_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "desert_pyramid"));
    @Unique private static final TagKey<Structure> JUNGLE_PYRAMID_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "jungle_pyramid"));
    @Unique private static final TagKey<Structure> SWAMP_HUT_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "swamp_hut"));
    @Unique private static final TagKey<Structure> STRONGHOLD_TAG = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "stronghold"));

    @Unique
    private static final Map<Item, StructureConfig> STRUCTURE_RECIPES = Map.ofEntries(
            // Bestehende
            Map.entry(Items.ECHO_SHARD,       new StructureConfig(ANCIENT_CITY_TAG, "Ancient City Locator", ChatFormatting.DARK_AQUA)),
            Map.entry(Items.TOTEM_OF_UNDYING, new StructureConfig(StructureTags.ON_WOODLAND_EXPLORER_MAPS, "Mansion Seeker", ChatFormatting.DARK_GREEN)),
            Map.entry(Items.HEART_OF_THE_SEA, new StructureConfig(StructureTags.ON_OCEAN_EXPLORER_MAPS, "Monument Tracker", ChatFormatting.AQUA)),
            Map.entry(Items.BLAZE_ROD,        new StructureConfig(FORTRESS_TAG, "Fortress Finder", ChatFormatting.RED)),
            Map.entry(Items.GOLD_BLOCK,       new StructureConfig(BASTION_TAG, "Bastion Compass", ChatFormatting.GOLD)),
            Map.entry(Items.TRIAL_KEY,        new StructureConfig(StructureTags.ON_TRIAL_CHAMBERS_MAPS, "Trial Key Compass", ChatFormatting.LIGHT_PURPLE)),
            Map.entry(Items.OMINOUS_BOTTLE,   new StructureConfig(OUTPOST_TAG, "Outpost Tracker", ChatFormatting.GRAY)),

            // Neue Strukturen (Balanced Kosten)
            Map.entry(Items.CHORUS_FRUIT,     new StructureConfig(END_CITY_TAG, "End City Compass", ChatFormatting.LIGHT_PURPLE)), // Nur im End!
            Map.entry(Items.RAIL,             new StructureConfig(MINESHAFT_TAG, "Mineshaft Detector", ChatFormatting.DARK_GRAY)),
            Map.entry(Items.EMERALD,          new StructureConfig(VILLAGE_TAG, "Village Finder", ChatFormatting.GREEN)),
            Map.entry(Items.PRISMARINE_SHARD, new StructureConfig(SHIPWRECK_TAG, "Shipwreck Sensor", ChatFormatting.BLUE)),
            Map.entry(Items.SNOW_BLOCK,       new StructureConfig(IGLOO_TAG, "Igloo Compass", ChatFormatting.WHITE)),
            Map.entry(Items.CHISELED_SANDSTONE, new StructureConfig(DESERT_PYRAMID_TAG, "Desert Pyramid Compass", ChatFormatting.GOLD)),
            Map.entry(Items.MOSSY_COBBLESTONE, new StructureConfig(JUNGLE_PYRAMID_TAG, "Jungle Temple Compass", ChatFormatting.DARK_GREEN)),
            Map.entry(Items.SLIME_BALL,       new StructureConfig(SWAMP_HUT_TAG, "Witch Hut Tracker", ChatFormatting.DARK_PURPLE)),
            Map.entry(Items.ENDER_EYE,        new StructureConfig(STRONGHOLD_TAG, "Stronghold Locator", ChatFormatting.DARK_PURPLE))
    );

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$createStructureCompass(CallbackInfo ci) {
        ItemStack leftStack = this.inputSlots.getItem(0);
        ItemStack rightStack = this.inputSlots.getItem(1);

        if (!leftStack.is(Items.COMPASS)) return;

        if (STRUCTURE_RECIPES.containsKey(rightStack.getItem())) {

            // SERVER LOGIK
            if (this.player.level() instanceof ServerLevel serverWorld) {
                StructureConfig config = STRUCTURE_RECIPES.get(rightStack.getItem());

                var structureListOptional = serverWorld.structureManager().registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .get(config.tag());

                BlockPos foundPos = null;

                // Sicherstellen, dass der Tag existiert
                if (structureListOptional.isPresent()) {
                    // FIX: Radius auf 300 (4800 Blöcke) erhöht!
                    Pair<BlockPos, Holder<Structure>> foundPair = serverWorld.getChunkSource().getGenerator().findNearestMapStructure(
                            serverWorld,
                            structureListOptional.get(),
                            this.player.blockPosition(),
                            300,
                            false
                    );
                    if (foundPair != null) {
                        foundPos = foundPair.getFirst();
                    }
                }

                // --- ERGEBNIS VERARBEITEN ---
                if (foundPos != null) {
                    // --- GEFUNDEN ---
                    ItemStack outputStack = new ItemStack(Items.COMPASS);
                    GlobalPos targetPos = GlobalPos.of(serverWorld.dimension(), foundPos);

                    outputStack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(targetPos), true));

                    if (this.itemName != null && !this.itemName.isBlank()) {
                        outputStack.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
                    } else {
                        outputStack.set(DataComponents.CUSTOM_NAME, Component.literal(config.name()).withStyle(config.color()));
                    }

                    outputStack.set(DataComponents.LORE, new ItemLore(List.of(
                            Component.literal("Dimension: " + serverWorld.dimension().identifier().getPath()).withStyle(ChatFormatting.DARK_GRAY)
                    )));

                    outputStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

                    this.resultSlots.setItem(0, outputStack);
                    this.cost.set(29);
                    this.repairItemCountCost = 1;
                } else {
                    // --- NICHT GEFUNDEN ---
                    ItemStack failStack = new ItemStack(Items.COMPASS);
                    failStack.set(DataComponents.CUSTOM_NAME, Component.literal("Kein Signal in Reichweite").withStyle(ChatFormatting.RED));
                    failStack.set(DataComponents.LORE, new ItemLore(List.of(
                            Component.literal("Struktur zu weit entfernt oder").withStyle(ChatFormatting.GRAY),
                            Component.literal("falsche Dimension.").withStyle(ChatFormatting.GRAY)
                    )));

                    this.resultSlots.setItem(0, failStack);
                    this.cost.set(0);
                    this.repairItemCountCost = 0;
                }
                ci.cancel();

            } else {
                // CLIENT LOGIK: Nur abbrechen
                ci.cancel();
            }
        }
    }

}