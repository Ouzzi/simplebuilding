package com.simplebuilding.items.tooltip;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.LevitatingBlock;
import com.simplebuilding.blocks.custom.ModBlastFurnaceBlock;
import com.simplebuilding.blocks.custom.ModFurnaceBlock;
import com.simplebuilding.blocks.custom.ModHopperBlock;
import com.simplebuilding.blocks.custom.ModSmokerBlock;
import com.simplebuilding.blocks.custom.ReinforcedPistonBlock;
import com.simplebuilding.blocks.entity.custom.FurnaceTierPerks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.ChunkLoaderBlock;
import com.simplebuilding.tweaks.block.ElytraPadBlock;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.block.LegacyTierBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.block.PotionPadBlock;
import com.simplebuilding.tweaks.block.SpawnTeleporterBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.block.Block;

/**
 * Info lines for item tooltips (Immersion 2026-09-28): pads (tier, area, charges, wait time),
 * machines (speed, hopper rate), pistons, floating sand and gravel, building cores (what they are
 * used in), enderite armor and the mod's apples and carrots (what eating them gives). Item tooltips
 * are the owner's accepted place for this kind of text - gadgets themselves stay silent on screen.
 *
 * <p>Loader- and client-neutral: {@code ItemMixin} (client) appends {@link #lines(ItemStack)} to every
 * item's tooltip, the game tests call it directly on the server. All lines are gray (the vanilla
 * style for descriptions), food effects use the effect category's colour like potions.
 */
public final class InfoTooltips {
    /** Transfer cooldown of the Reinforced / Netherite / Enderite Hopper in ticks (vanilla: 8). */
    public static final int[] HOPPER_COOLDOWN = {4, 2, 1};
    /** Push limit of the Reinforced (Sticky) Piston ({@code PistonHandlerMixin}; vanilla 12). */
    public static final int REINFORCED_PUSH_LIMIT = 18;

    private InfoTooltips() {
    }

    /** The info lines for this stack; empty for items without any. */
    public static List<Component> lines(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            blockLines(blockItem.getBlock(), out);
        }
        List<Item> uses = coreUses().get(item);
        if (uses != null) {
            coreLines(uses, out);
        }
        if (item == ModItems.ENDERITE_HELMET || item == ModItems.ENDERITE_CHESTPLATE
                || item == ModItems.ENDERITE_LEGGINGS || item == ModItems.ENDERITE_BOOTS) {
            out.add(gray("tooltip.simplebuilding.enderite_armor.void"));
            out.add(gray("tooltip.simplebuilding.enderite_armor.glide"));
        }
        if (isModFood(item)) {
            foodLines(stack, out);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------
    // Blocks
    // ------------------------------------------------------------------------------------------

    private static void blockLines(Block block, List<Component> out) {
        if (block instanceof LegacyTierBlock) {
            return; // turns into its new tier on the first tick; no promises about the old one
        }
        if (block instanceof LaunchpadBlock pad) {
            out.add(tier(pad.getTier(), LaunchpadBlock.ENDERITE_TIER));
            out.add(gray("tooltip.simplebuilding.pad.charges", LaunchpadBlock.maxCharges(pad.getTier())));
            if (pad.isEnderite()) {
                out.add(gray("tooltip.simplebuilding.pad.launch_fall"));
            }
        } else if (block instanceof ChunkLoaderBlock loader) {
            out.add(tier(loader.getTier(), ChunkLoaderBlock.MAX_TIER));
            out.add(gray("tooltip.simplebuilding.pad.chunks." + loader.getTier()));
        } else if (block instanceof ElytraPadBlock pad) {
            int t = pad.getTier();
            out.add(tier(t, PadTiers.MAX));
            out.add(gray("tooltip.simplebuilding.pad.area", PadTiers.width(t), PadTiers.width(t), PadTiers.height(t)));
            if (PadTiers.hasEnderiteBonus(t)) {
                out.add(gray("tooltip.simplebuilding.pad.elytra_boost"));
            }
        } else if (block instanceof FlypadBlock pad) {
            int t = pad.getTier();
            out.add(tier(t, PadTiers.FLYPAD_MAX));
            out.add(gray("tooltip.simplebuilding.pad.fly_area", PadTiers.flyWidth(t), PadTiers.flyWidth(t), PadTiers.flyHeight(t)));
            out.add(gray("tooltip.simplebuilding.pad.fly_safety"));
        } else if (block instanceof SpawnTeleporterBlock pad) {
            int t = pad.getTier();
            out.add(tier(t, SpawnTeleporterBlock.MAX_TIER));
            out.add(gray("tooltip.simplebuilding.pad.teleport_wait", seconds(SpawnTeleporterBlockEntity.requiredTicks(t))));
            // Besitzer 2026-09-29: jede Stufe zum eigenen Spawn, mit Redstone zum Weltspawn.
            out.add(gray("tooltip.simplebuilding.pad.teleport_home"));
        } else if (block instanceof PotionPadBlock pad) {
            int t = pad.getTier();
            int duration = PotionPadBlock.effectDuration(t);
            out.add(tier(t, PotionPadBlock.MAX_TIER));
            // Abklingzeit wie PotionPadBlock#cooldownAt (Config tweaks.padTuning.potionPadCooldownFactor).
            int cooldown = (int) Math.round(com.simplebuilding.tweaks.SimpleTweaks.config().padTuning.potionPadCooldown() * duration);
            out.add(gray("tooltip.simplebuilding.pad.potion", seconds(duration), seconds(cooldown)));
        } else if (block instanceof ModFurnaceBlock || block instanceof ModSmokerBlock || block instanceof ModBlastFurnaceBlock) {
            int t = machineTier(block);
            if (t > 0) {
                out.add(gray("tooltip.simplebuilding.machine.speed", FurnaceTierPerks.speedFactor(block.defaultBlockState())));
                if (t >= 2) {
                    out.add(gray("tooltip.simplebuilding.machine.experience"));
                }
                if (block instanceof ModBlastFurnaceBlock && t >= 2) {
                    int period = t == 3 ? FurnaceTierPerks.ENDERITE_BONUS_PERIOD : FurnaceTierPerks.NETHERITE_BONUS_PERIOD;
                    out.add(gray("tooltip.simplebuilding.machine.ore_bonus", 100 / period));
                }
            }
        } else if (block instanceof ModHopperBlock) {
            int t = machineTier(block);
            if (t > 0) {
                int cooldown = HOPPER_COOLDOWN[t - 1];
                out.add(cooldown == 1 ? gray("tooltip.simplebuilding.hopper.speed_every_tick")
                        : gray("tooltip.simplebuilding.hopper.speed", cooldown));
                out.add(gray("tooltip.simplebuilding.hopper.filter"));
            }
        } else if (block instanceof ReinforcedPistonBlock) {
            out.add(gray("tooltip.simplebuilding.reinforced_piston.limit", REINFORCED_PUSH_LIMIT));
            out.add(gray("tooltip.simplebuilding.reinforced_piston.breach"));
        } else if (block instanceof LevitatingBlock) {
            out.add(gray("tooltip.simplebuilding.levitating"));
        } else if (block == ModBlocks.SUSPENDED_SAND || block == ModBlocks.SUSPENDED_GRAVEL) {
            out.add(gray("tooltip.simplebuilding.suspended"));
        }
    }

    /** 1 = Reinforced, 2 = Netherite, 3 = Enderite machine; 0 = none of the mod's tiered machines. */
    public static int machineTier(Block block) {
        if (block == ModBlocks.REINFORCED_FURNACE || block == ModBlocks.REINFORCED_SMOKER
                || block == ModBlocks.REINFORCED_BLAST_FURNACE || block == ModBlocks.REINFORCED_HOPPER) {
            return 1;
        }
        if (block == ModBlocks.NETHERITE_FURNACE || block == ModBlocks.NETHERITE_SMOKER
                || block == ModBlocks.NETHERITE_BLAST_FURNACE || block == ModBlocks.NETHERITE_HOPPER) {
            return 2;
        }
        if (block == ModBlocks.ENDERITE_FURNACE || block == ModBlocks.ENDERITE_SMOKER
                || block == ModBlocks.ENDERITE_BLAST_FURNACE || block == ModBlocks.ENDERITE_HOPPER) {
            return 3;
        }
        return 0;
    }

    // ------------------------------------------------------------------------------------------
    // Building cores
    // ------------------------------------------------------------------------------------------

    /**
     * What every core goes into - each recipe takes exactly one. {@code InfoTooltipTests} checks this
     * list against the recipes the server actually loads, so it cannot drift silently.
     */
    public static Map<Item, List<Item>> coreUses() {
        Map<Item, List<Item>> uses = new LinkedHashMap<>();
        uses.put(ModItems.COPPER_CORE, List.of(ModItems.COPPER_BUILDING_WAND, ModItems.VELOCITY_GAUGE,
                TweaksBlocks.CHUNK_LOADER.asItem()));
        uses.put(ModItems.IRON_CORE, List.of(ModItems.IRON_BUILDING_WAND, TweaksItems.LASER_POINTER, ModItems.MAGNET,
                ModItems.ROTATOR, TweaksBlocks.LAUNCHPAD.asItem()));
        uses.put(ModItems.GOLD_CORE, List.of(ModItems.GOLD_BUILDING_WAND, ModItems.ORE_DETECTOR, ModItems.OCTANT,
                TweaksBlocks.SPAWN_TELEPORTER.asItem()));
        uses.put(ModItems.DIAMOND_CORE, List.of(ModItems.DIAMOND_BUILDING_WAND, ModItems.NETHERITE_CORE,
                TweaksBlocks.ELYTRA_PAD.asItem()));
        uses.put(ModItems.NETHERITE_CORE, List.of(ModItems.ENDERITE_CORE, TweaksBlocks.POTION_PAD.asItem()));
        uses.put(ModItems.ENDERITE_CORE, List.of(TweaksItems.ECHO_COMPASS, TweaksBlocks.FLYPAD.asItem(),
                TweaksBlocks.INFUSED_POTION_PAD.asItem()));
        return uses;
    }

    /** "Used in:" and the item names, three to a line. */
    private static void coreLines(List<Item> uses, List<Component> out) {
        out.add(gray("tooltip.simplebuilding.core.used_in"));
        MutableComponent line = null;
        int onLine = 0;
        for (Item use : uses) {
            if (line == null) {
                line = Component.literal("  ");
            } else {
                line.append(Component.literal(", "));
            }
            line.append(Component.translatable(use.getDescriptionId()));
            if (++onLine == 3) {
                out.add(line.withStyle(ChatFormatting.DARK_GRAY));
                line = null;
                onLine = 0;
            }
        }
        if (line != null) {
            out.add(line.withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Food
    // ------------------------------------------------------------------------------------------

    public static boolean isModFood(Item item) {
        return item == ModItems.NETHERITE_APPLE || item == ModItems.NETHERITE_CARROT || item == ModItems.ENDERITE_APPLE
                || item == ModItems.ENDERITE_CARROT || item == ModItems.ENCHANTED_NETHERITE_APPLE
                || item == ModItems.ENCHANTED_ENDERITE_APPLE;
    }

    /** "When eaten:" and one line per status effect, like a potion: name, level, duration. */
    private static void foodLines(ItemStack stack, List<Component> out) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable == null) {
            return;
        }
        List<Component> effects = new ArrayList<>();
        for (ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
                for (MobEffectInstance instance : apply.effects()) {
                    effects.add(effectLine(instance));
                }
            }
        }
        if (!effects.isEmpty()) {
            out.add(gray("tooltip.simplebuilding.food.when_eaten"));
            out.addAll(effects);
        }
    }

    /** "Regeneration II (0:20)" in the effect category's colour. */
    public static Component effectLine(MobEffectInstance instance) {
        MutableComponent name = Component.translatable(instance.getDescriptionId());
        if (instance.getAmplifier() > 0) {
            name = Component.translatable("potion.withAmplifier", name,
                    Component.translatable("potion.potency." + instance.getAmplifier()));
        }
        name = Component.translatable("potion.withDuration", name, Component.literal(clock(instance.getDuration())));
        return Component.literal(" ").append(name).withStyle(instance.getEffect().value().getCategory().getTooltipFormatting());
    }

    /** Ticks as m:ss (vanilla's potion tooltip format). */
    public static String clock(int ticks) {
        int total = ticks / 20;
        return total / 60 + ":" + (total % 60 < 10 ? "0" : "") + total % 60;
    }

    // ------------------------------------------------------------------------------------------

    /** "Tier II of III" with vanilla's localised roman numerals. */
    private static Component tier(int tier, int max) {
        return gray("tooltip.simplebuilding.tier", Component.translatable("enchantment.level." + tier),
                Component.translatable("enchantment.level." + max));
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private static MutableComponent gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }
}
