package com.simplepads.item;

import com.simplepads.SimplePads;
import com.simplepads.block.ChunkLoaderBlock;
import com.simplepads.block.ElytraPadBlock;
import com.simplepads.block.FlypadBlock;
import com.simplepads.block.LaunchpadBlock;
import com.simplepads.block.LegacyTierBlock;
import com.simplepads.block.PadTiers;
import com.simplepads.block.PotionPadBlock;
import com.simplepads.block.SpawnTeleporterBlock;
import com.simplepads.block.entity.SpawnTeleporterBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Info lines of the pad tooltips (tier, area, charges, wait time), until 04.11. in SimpleBuilding's {@code InfoTooltips}. The client
 * appends {@link #lines} to every Simple Pads item; the game tests call it on the server.
 */
public final class PadTooltips {
    private PadTooltips() {
    }

    public static List<Component> lines(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        if (stack.getItem() instanceof BlockItem blockItem) {
            blockLines(blockItem.getBlock(), out);
        }
        return out;
    }

    private static void blockLines(Block block, List<Component> out) {
        if (block instanceof LegacyTierBlock) {
            return; // turns into its new tier on the first tick; no promises about the old one
        }
        if (block instanceof LaunchpadBlock pad) {
            out.add(tier(pad.getTier(), LaunchpadBlock.ENDERITE_TIER));
            out.add(gray("tooltip.simplepads.pad.charges", LaunchpadBlock.maxCharges(pad.getTier())));
            if (pad.isEnderite()) {
                out.add(gray("tooltip.simplepads.pad.launch_fall"));
            }
        } else if (block instanceof ChunkLoaderBlock loader) {
            out.add(tier(loader.getTier(), ChunkLoaderBlock.MAX_TIER));
            out.add(gray("tooltip.simplepads.pad.chunks." + loader.getTier()));
        } else if (block instanceof ElytraPadBlock pad) {
            int t = pad.getTier();
            out.add(tier(t, PadTiers.MAX));
            out.add(gray("tooltip.simplepads.pad.area", PadTiers.width(t), PadTiers.width(t), PadTiers.height(t)));
            if (PadTiers.hasEnderiteBonus(t)) {
                out.add(gray("tooltip.simplepads.pad.elytra_boost"));
            }
        } else if (block instanceof FlypadBlock pad) {
            int t = pad.getTier();
            out.add(tier(t, PadTiers.FLYPAD_MAX));
            out.add(gray("tooltip.simplepads.pad.fly_area", PadTiers.flyWidth(t), PadTiers.flyWidth(t), PadTiers.flyHeight(t)));
            out.add(gray("tooltip.simplepads.pad.fly_safety"));
        } else if (block instanceof SpawnTeleporterBlock pad) {
            int t = pad.getTier();
            out.add(tier(t, SpawnTeleporterBlock.MAX_TIER));
            out.add(gray("tooltip.simplepads.pad.teleport_wait", seconds(SpawnTeleporterBlockEntity.requiredTicks(t))));
            out.add(gray("tooltip.simplepads.pad.teleport_home"));
        } else if (block instanceof PotionPadBlock pad) {
            int t = pad.getTier();
            int duration = PotionPadBlock.effectDuration(t);
            out.add(tier(t, PotionPadBlock.MAX_TIER));
            int cooldown = (int) Math.round(SimplePads.config().padTuning.potionPadCooldown() * duration);
            out.add(gray("tooltip.simplepads.pad.potion", seconds(duration), seconds(cooldown)));
        }
    }

    private static Component tier(int tier, int max) {
        return gray("tooltip.simplepads.tier", Component.translatable("enchantment.level." + tier),
                Component.translatable("enchantment.level." + max));
    }

    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private static MutableComponent gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }
}
