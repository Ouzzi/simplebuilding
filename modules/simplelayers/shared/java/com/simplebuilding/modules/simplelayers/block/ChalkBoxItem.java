package com.simplebuilding.modules.simplelayers.block;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Chalk Box (BACK20, plan docs/ai/PLAN-B-BACK20-2026-11-10.md): a container and tool without a screen, like the Bundle. Click chalk
 * onto it (or it onto chalk) in the inventory and the box takes the chalk's remaining strokes, up to {@link #CAPACITY} per colour;
 * a glow ink sac adds {@link #GLOW_PER_SAC} glowing strokes. Used on a block it draws with the selected colour (glowing while glow
 * strokes are left); sneak + use in the air picks the next filled colour. The bar shows what is left of the selected colour, the
 * tooltip all colours. Contents live in {@code custom_data} (keys = colour names, {@code glow}, {@code selected}).
 */
public class ChalkBoxItem extends Item {
    public static final int CAPACITY = 4 * ChalkItem.USES, GLOW_PER_SAC = 16, GLOW_CAPACITY = 64;
    private static final int[] BAR = {0xF2F2EE, 0xC8402E, 0x3E6FD0, 0xE8C23A};

    public ChalkBoxItem(Properties properties) {
        super(properties);
    }

    static CompoundTag tag(ItemStack box) {
        return box.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static int strokes(ItemStack box, ChalkMarkBlock.Color color) {
        return tag(box).getIntOr(color.getSerializedName(), 0);
    }

    public static int glow(ItemStack box) {
        return tag(box).getIntOr("glow", 0);
    }

    public static ChalkMarkBlock.Color selected(ItemStack box) {
        int i = tag(box).getIntOr("selected", 0);
        return ChalkMarkBlock.Color.values()[Math.floorMod(i, ChalkMarkBlock.Color.values().length)];
    }

    static void put(ItemStack box, String key, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, box, t -> t.putInt(key, value));
    }

    /** Takes chalk (its remaining strokes) or a glow ink sac into the box; returns whether anything was taken. */
    public static boolean absorb(ItemStack box, ItemStack other) {
        if (other.getItem() instanceof ChalkItem chalk) {
            int have = strokes(box, chalk.color), left = other.getMaxDamage() - other.getDamageValue(), take = Math.min(left, CAPACITY - have);
            if (take <= 0) return false;
            put(box, chalk.color.getSerializedName(), have + take);
            if (take == left) other.shrink(1);
            else other.setDamageValue(other.getDamageValue() + take);
            if (have == 0 && strokes(box, selected(box)) == 0) put(box, "selected", chalk.color.ordinal());
            return true;
        }
        if (other.is(Items.GLOW_INK_SAC) && glow(box) + GLOW_PER_SAC <= GLOW_CAPACITY) {
            put(box, "glow", glow(box) + GLOW_PER_SAC);
            other.shrink(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack box, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (!absorb(box, other)) return false;
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack box, Slot slot, ClickAction action, Player player) {
        ItemStack other = slot.getItem();
        if (other.isEmpty() || !absorb(box, other)) return false;
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        return true;
    }

    /** The next colour after the selected one that has strokes (the selected one again when it is the only one). */
    public static ChalkMarkBlock.Color next(ItemStack box) {
        ChalkMarkBlock.Color[] all = ChalkMarkBlock.Color.values();
        int from = selected(box).ordinal();
        for (int i = 1; i <= all.length; i++) {
            ChalkMarkBlock.Color c = all[(from + i) % all.length];
            if (strokes(box, c) > 0) return c;
        }
        return selected(box);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack box = player.getItemInHand(hand);
        ChalkMarkBlock.Color c = next(box);
        put(box, "selected", c.ordinal());
        if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("item.simplelayers.chalk_box.selected", colorName(c), strokes(box, c)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack box = context.getItemInHand();
        ChalkMarkBlock.Color c = selected(box);
        int have = strokes(box, c), glow = glow(box);
        Player player = context.getPlayer();
        if (have <= 0) {
            if (player != null && !context.getLevel().isClientSide()) player.sendOverlayMessage(Component.translatable("item.simplelayers.chalk_box.empty", colorName(c)));
            return InteractionResult.FAIL;
        }
        boolean sneaking = player != null && player.isShiftKeyDown();
        if (!ChalkMarkBlock.draw(context.getLevel(), context.getClickedPos(), context.getClickedFace(), context.getClickLocation(), c, glow > 0, sneaking)) {
            return InteractionResult.PASS;
        }
        if (player == null || !player.getAbilities().instabuild) {
            put(box, c.getSerializedName(), have - 1);
            if (glow > 0) put(box, "glow", glow - 1);
        }
        return InteractionResult.SUCCESS;
    }

    static Component colorName(ChalkMarkBlock.Color c) {
        return Component.translatable("color.simplelayers.chalk." + c.getSerializedName());
    }

    @Override
    public boolean isBarVisible(ItemStack box) {
        return strokes(box, selected(box)) > 0;
    }

    @Override
    public int getBarWidth(ItemStack box) {
        return Math.min(13, Math.round(13.0F * strokes(box, selected(box)) / CAPACITY));
    }

    @Override
    public int getBarColor(ItemStack box) {
        return BAR[selected(box).ordinal()];
    }

    @Override
    public void appendHoverText(ItemStack box, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        for (String suffix : new String[] {"", ".2", ".3", ".4"}) {
            out.accept(Component.translatable("item.simplelayers.chalk_box.tooltip" + suffix).withStyle(ChatFormatting.GRAY));
        }
        ChalkMarkBlock.Color sel = selected(box);
        for (ChalkMarkBlock.Color c : ChalkMarkBlock.Color.values()) {
            int n = strokes(box, c);
            if (n > 0 || c == sel) {
                out.accept(Component.translatable("item.simplelayers.chalk_box.strokes", colorName(c), n, CAPACITY)
                        .withStyle(c == sel ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            }
        }
        if (glow(box) > 0) out.accept(Component.translatable("item.simplelayers.chalk_box.glow", glow(box)).withStyle(ChatFormatting.AQUA));
    }
}
