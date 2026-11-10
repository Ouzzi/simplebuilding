package com.simplebuilding.modules.simplevault;

import com.simplebuilding.modules.simplevault.block.SealPart;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Seal part (UNDER21): what a solved riddle chamber's pedestal hands out; fits only the Ascent Altar of the vault it came from
 * (custom data {@code vault_rx/vault_rz}), so a spare part never skips another vault's riddles.
 */
public class SealFragmentItem extends Item {
    public final SealPart part;

    public SealFragmentItem(SealPart part, Properties properties) {
        super(properties);
        this.part = part;
    }

    public static ItemStack bound(SealPart part, int rx, int rz) {
        ItemStack stack = new ItemStack(VaultRegistry.FRAGMENTS[part.ordinal()]);
        CompoundTag tag = new CompoundTag();
        tag.putInt("vault_rx", rx);
        tag.putInt("vault_rz", rz);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Whether the stack is the {@code part} seal of vault (rx, rz). */
    public static boolean fits(ItemStack stack, SealPart part, int rx, int rz) {
        if (!(stack.getItem() instanceof SealFragmentItem f) || f.part != part) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        return tag.getIntOr("vault_rx", Integer.MIN_VALUE) == rx && tag.getIntOr("vault_rz", Integer.MIN_VALUE) == rz;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("item.simplevault.seal_fragment.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
