package com.simpleoctets.item;

import com.simpleoctets.block.OctetCells;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Achtel eines Materials (Holz, Queue Nachtrag 24). Setzt wie die Schach-Achtel von SimpleBuilding ins
 * Sub-Raster: Ziel ist der Punkt ein Viertel Block vor dem Trefferpunkt. Jede Achtelzelle wird ergaenzt (anderes
 * Material: gemischte Zelle, Queue N48), eine leere oder ersetzbare Stelle wird zur neuen Zelle; belegte Achtel lehnen ab. {@link #place}
 * nutzen auch die Melonenscheiben (Schleichen + Rechtsklick, {@code OctetCells#tryPlaceSlice}).
 */
public class MaterialOctetItem extends Item {
    private final Supplier<Block> cell;

    public MaterialOctetItem(Supplier<Block> cell, Item.Properties properties) {
        super(properties);
        this.cell = cell;
    }

    /** Die Achtelzelle, in die dieses Item setzt. */
    public Block cell() {
        return this.cell.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return place(context, this.cell.get());
    }

    /** How it is placed (AGENTS rule 14 (a): the sub-grid placing is not obvious). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.simpleoctets.material_octet").withStyle(ChatFormatting.GRAY));
    }

    /** Setzt ein Achtel der Zelle {@code block} nach dem Trefferpunkt und verbraucht ein Item ({@link OctetCells#place}). */
    public static InteractionResult place(UseOnContext context, @Nullable Block block) {
        return OctetCells.place(context, block);
    }

    /** Wie {@link OctetCells#place(UseOnContext, BlockState)}. */
    public static InteractionResult place(UseOnContext context, BlockState material) {
        return OctetCells.place(context, material);
    }
}
