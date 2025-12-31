package com.simplebuilding.mixin;

import com.mojang.datafixers.util.Pair;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.StructureConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.gen.structure.Structure;
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

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow private int repairItemUsage;
    @Shadow @Final private Property levelCost;
    @Shadow @Nullable private String newItemName;

    public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    // --- 1. SLEDGEHAMMER CUSTOM LOGIC (HEAD) ---
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void updateResultHead(CallbackInfo ci) {
        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);

        if (leftStack.isEmpty()) return;

        if (leftStack.getItem() instanceof SledgehammerItem) {

            // FALL A: HAMMER + HAMMER
            if (rightStack.getItem() instanceof SledgehammerItem) {
                int leftDurability = leftStack.getMaxDamage() - leftStack.getDamage();
                int rightDurability = rightStack.getMaxDamage() - rightStack.getDamage();

                int bonus = (int) (leftStack.getMaxDamage() * 0.12f);
                int combinedDurability = leftDurability + rightDurability + bonus;

                int newDamage = leftStack.getMaxDamage() - combinedDurability;
                if (newDamage < 0) newDamage = 0;

                ItemStack result = leftStack.copy();
                result.setDamage(newDamage);
                result.set(DataComponentTypes.REPAIR_COST, 0);

                handleRenaming(leftStack, result);

                this.output.setStack(0, result);
                this.levelCost.set(1);
                this.repairItemUsage = 1;

                ci.cancel();
            }
            // FALL B: HAMMER + MATERIAL (KORRIGIERT)
            // Nutze leftStack.canRepairWith(rightStack) - Methode von ItemStack!
            else if (leftStack.isDamaged() && leftStack.canRepairWith(rightStack)) {
                ItemStack result = leftStack.copy();

                // Teilen durch 11 statt 4
                int repairPerItem = result.getMaxDamage() / 11;
                if (repairPerItem <= 0) repairPerItem = 1;

                int damage = result.getDamage();
                int materialsUsed = 0;
                int materialsAvailable = rightStack.getCount();

                while (damage > 0 && materialsUsed < materialsAvailable) {
                    damage -= repairPerItem;
                    materialsUsed++;
                }

                if (damage < 0) damage = 0;
                result.setDamage(damage);

                this.repairItemUsage = materialsUsed;

                int cost = materialsUsed;
                if (handleRenaming(leftStack, result)) {
                    cost += 1;
                }

                result.set(DataComponentTypes.REPAIR_COST, 0);

                if (cost <= 0) cost = 1;
                this.levelCost.set(Math.min(cost, 39));

                this.output.setStack(0, result);
                ci.cancel();
            }
        }
    }

    // --- 2. GENERAL FIXES & RESTRICTIONS (RETURN) ---
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void applyAnvilTweaks(CallbackInfo ci) {
        ItemStack outputStack = this.output.getStack(0);

        if (outputStack.isEmpty()) return;

        // A) SLEDGEHAMMER REPAIR COST FIX
        if (outputStack.getItem() instanceof SledgehammerItem) {
            outputStack.set(DataComponentTypes.REPAIR_COST, 0);
            if (this.levelCost.get() >= 40) this.levelCost.set(39);
        }

        // B) COLOR PALETTE RESTRICTION
        RegistryWrapper.WrapperLookup registryManager = this.player.getEntityWorld().getRegistryManager();
        var colorPaletteEntry = getEnchantment(registryManager, ModEnchantments.COLOR_PALETTE);
        var masterBuilderEntry = getEnchantment(registryManager, ModEnchantments.MASTER_BUILDER);

        if (colorPaletteEntry != null && masterBuilderEntry != null) {
            int colorPaletteLevel = EnchantmentHelper.getLevel(colorPaletteEntry, outputStack);
            int masterBuilderLevel = EnchantmentHelper.getLevel(masterBuilderEntry, outputStack);

            if (colorPaletteLevel > 0 && masterBuilderLevel <= 0) {
                this.output.setStack(0, ItemStack.EMPTY);
                this.levelCost.set(0);
            }
        }
    }

    @Unique
    private boolean handleRenaming(ItemStack original, ItemStack result) {
        boolean renamed = false;
        if (this.newItemName != null && !StringHelper.isBlank(this.newItemName)) {
            if (!this.newItemName.equals(original.getName().getString())) {
                result.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
                renamed = true;
            }
        } else if (original.contains(DataComponentTypes.CUSTOM_NAME)) {
            result.remove(DataComponentTypes.CUSTOM_NAME);
            renamed = true;
        }
        return renamed;
    }

    @Unique
    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
        return optional.orElse(null);
    }

    // ... Imports bleiben gleich ...

    // FIX: Wir definieren die Tags explizit, um sicherzugehen, dass sie existieren.
    // In Vanilla 1.21 sind dies die korrekten Tag-IDs (JSON-Dateien in data/minecraft/tags/worldgen/structure/)
    @Unique private static final TagKey<Structure> ANCIENT_CITY_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "ancient_city"));
    @Unique private static final TagKey<Structure> BASTION_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "bastion_remnant"));
    @Unique private static final TagKey<Structure> FORTRESS_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "fortress"));
    @Unique private static final TagKey<Structure> OUTPOST_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "pillager_outpost"));
    @Unique private static final TagKey<Structure> END_CITY_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "end_city"));
    @Unique private static final TagKey<Structure> MINESHAFT_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "mineshaft"));
    @Unique private static final TagKey<Structure> VILLAGE_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "village"));
    @Unique private static final TagKey<Structure> SHIPWRECK_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "shipwreck"));
    @Unique private static final TagKey<Structure> IGLOO_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "igloo"));
    @Unique private static final TagKey<Structure> DESERT_PYRAMID_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "desert_pyramid"));
    @Unique private static final TagKey<Structure> JUNGLE_PYRAMID_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "jungle_pyramid"));
    @Unique private static final TagKey<Structure> SWAMP_HUT_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "swamp_hut"));
    @Unique private static final TagKey<Structure> STRONGHOLD_TAG = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft", "stronghold"));

    @Unique
    private static final Map<Item, StructureConfig> STRUCTURE_RECIPES = Map.ofEntries(
            // Bestehende
            Map.entry(Items.ECHO_SHARD,       new StructureConfig(ANCIENT_CITY_TAG, "Ancient City Locator", Formatting.DARK_AQUA)),
            Map.entry(Items.TOTEM_OF_UNDYING, new StructureConfig(StructureTags.ON_WOODLAND_EXPLORER_MAPS, "Mansion Seeker", Formatting.DARK_GREEN)),
            Map.entry(Items.HEART_OF_THE_SEA, new StructureConfig(StructureTags.ON_OCEAN_EXPLORER_MAPS, "Monument Tracker", Formatting.AQUA)),
            Map.entry(Items.BLAZE_ROD,        new StructureConfig(FORTRESS_TAG, "Fortress Finder", Formatting.RED)),
            Map.entry(Items.GOLD_BLOCK,       new StructureConfig(BASTION_TAG, "Bastion Compass", Formatting.GOLD)),
            Map.entry(Items.TRIAL_KEY,        new StructureConfig(StructureTags.ON_TRIAL_CHAMBERS_MAPS, "Trial Key Compass", Formatting.LIGHT_PURPLE)),
            Map.entry(Items.OMINOUS_BOTTLE,   new StructureConfig(OUTPOST_TAG, "Outpost Tracker", Formatting.GRAY)),

            // Neue Strukturen (Balanced Kosten)
            Map.entry(Items.CHORUS_FRUIT,     new StructureConfig(END_CITY_TAG, "End City Compass", Formatting.LIGHT_PURPLE)), // Nur im End!
            Map.entry(Items.RAIL,             new StructureConfig(MINESHAFT_TAG, "Mineshaft Detector", Formatting.DARK_GRAY)),
            Map.entry(Items.EMERALD,          new StructureConfig(VILLAGE_TAG, "Village Finder", Formatting.GREEN)),
            Map.entry(Items.PRISMARINE_SHARD, new StructureConfig(SHIPWRECK_TAG, "Shipwreck Sensor", Formatting.BLUE)),
            Map.entry(Items.SNOW_BLOCK,       new StructureConfig(IGLOO_TAG, "Igloo Compass", Formatting.WHITE)),
            Map.entry(Items.CHISELED_SANDSTONE, new StructureConfig(DESERT_PYRAMID_TAG, "Desert Pyramid Compass", Formatting.GOLD)),
            Map.entry(Items.MOSSY_COBBLESTONE, new StructureConfig(JUNGLE_PYRAMID_TAG, "Jungle Temple Compass", Formatting.DARK_GREEN)),
            Map.entry(Items.SLIME_BALL,       new StructureConfig(SWAMP_HUT_TAG, "Witch Hut Tracker", Formatting.DARK_PURPLE)),
            Map.entry(Items.ENDER_EYE,        new StructureConfig(STRONGHOLD_TAG, "Stronghold Locator", Formatting.DARK_PURPLE))
    );

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$createStructureCompass(CallbackInfo ci) {
        ItemStack leftStack = this.input.getStack(0);
        ItemStack rightStack = this.input.getStack(1);

        if (!leftStack.isOf(Items.COMPASS)) return;

        if (STRUCTURE_RECIPES.containsKey(rightStack.getItem())) {

            // SERVER LOGIK
            if (this.player.getEntityWorld() instanceof ServerWorld serverWorld) {
                StructureConfig config = STRUCTURE_RECIPES.get(rightStack.getItem());

                var structureListOptional = serverWorld.getStructureAccessor().getRegistryManager()
                        .getOrThrow(RegistryKeys.STRUCTURE)
                        .getOptional(config.tag());

                BlockPos foundPos = null;

                // Sicherstellen, dass der Tag existiert
                if (structureListOptional.isPresent()) {
                    // FIX: Radius auf 300 (4800 Blöcke) erhöht!
                    Pair<BlockPos, RegistryEntry<Structure>> foundPair = serverWorld.getChunkManager().getChunkGenerator().locateStructure(
                            serverWorld,
                            structureListOptional.get(),
                            this.player.getBlockPos(),
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
                    GlobalPos targetPos = GlobalPos.create(serverWorld.getRegistryKey(), foundPos);

                    outputStack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(targetPos), true));

                    if (this.newItemName != null && !this.newItemName.isBlank()) {
                        outputStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
                    } else {
                        outputStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(config.name()).formatted(config.color()));
                    }

                    outputStack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                            Text.literal("Dimension: " + serverWorld.getRegistryKey().getValue().getPath()).formatted(Formatting.DARK_GRAY)
                    )));

                    outputStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

                    this.output.setStack(0, outputStack);
                    this.levelCost.set(29);
                    this.repairItemUsage = 1;
                } else {
                    // --- NICHT GEFUNDEN ---
                    ItemStack failStack = new ItemStack(Items.COMPASS);
                    failStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Kein Signal in Reichweite").formatted(Formatting.RED));
                    failStack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                            Text.literal("Struktur zu weit entfernt oder").formatted(Formatting.GRAY),
                            Text.literal("falsche Dimension.").formatted(Formatting.GRAY)
                    )));

                    this.output.setStack(0, failStack);
                    this.levelCost.set(0);
                    this.repairItemUsage = 0;
                }
                ci.cancel();

            } else {
                // CLIENT LOGIK: Nur abbrechen
                ci.cancel();
            }
        }
    }
}