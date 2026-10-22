package com.simplehammers.engine;

import com.simplehammers.SimpleHammers;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Tooltip hint of every block item that the hammer upgrades in the world (Queue 738; until then SimpleBuilding's
 * {@code SledgehammerUpgrades#tooltipHints}): how it goes up and the off-hand material with its count. Read from the
 * effective table ({@link SledgehammerUpgrades#table}, SimpleLib's {@code in_world_upgrades}), so every mod's blocks get
 * it; the client adds it through {@code mixin/client/HammerTooltipMixin}. Two short lines (AGENTS.md rule 14: the
 * in-world way is not obvious, the count is shown nowhere else).
 */
public final class UpgradeHints {
    public static final String PREFIX = "tooltip." + SimpleHammers.MOD_ID + ".hammer_upgrade.";

    private UpgradeHints() {
    }

    /** The hint lines for {@code stack}, empty for anything the hammer does not upgrade (or with the mod switched off). */
    public static List<Component> lines(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !SimpleHammers.on()) return List.of();
        SledgehammerUpgrades.Upgrade upgrade = SledgehammerUpgrades.upgradeOf(blockItem.getBlock());
        return upgrade == null ? List.of() : lines(upgrade);
    }

    /** The hint lines of one upgrade step: what stays, then "Off hand: material x count". */
    public static List<Component> lines(SledgehammerUpgrades.Upgrade upgrade) {
        String way = upgrade.to() instanceof ShulkerBoxBlock ? "shulker_box"
                : upgrade.toEnderite() ? "enderite" : upgrade.toReinforced() ? "reinforced" : "netherite";
        return List.of(
                Component.translatable(PREFIX + way).withStyle(ChatFormatting.GRAY),
                Component.translatable(PREFIX + "material", upgrade.materialCost(),
                        Component.translatable(upgrade.nugget().getDescriptionId())).withStyle(ChatFormatting.GRAY));
    }
}
