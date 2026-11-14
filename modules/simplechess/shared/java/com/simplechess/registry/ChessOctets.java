package com.simplechess.registry;

import com.simplechess.ChessColor;
import com.simplechess.block.CheckerOctetBlock;
import com.simplechess.item.CheckerOctetItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * The chess octets - an optional integration with Simple Octets (N47 M7): {@code checker_octet} is a cell of Simple Octets'
 * {@code OctetCellBlock}, so it mixes with the material octets. Only this class and the two it names touch Simple Octets'
 * classes; {@link ChessRegistry} and {@link ChessItems} call it only while {@code simpleoctets} is loaded, so without it the
 * classes are never loaded. Colours that are not {@link ChessColor#available} get no octet item (the cell keeps all thirteen
 * colour values, saved cells stay valid).
 */
public final class ChessOctets {
    /** Octets of every colour in one cell (colour and eight bits in the state); drops from the state, no loot table. */
    private static @Nullable Block checkerOctet;

    private ChessOctets() {
    }

    static void registerBlock() {
        checkerOctet = ChessRegistry.block("checker_octet", BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.8F)
                .sound(SoundType.STONE).noLootTable().isValidSpawn((state, level, pos, type) -> false)
                .lightLevel(state -> state.getValue(CheckerOctetBlock.COLOR).light()), CheckerOctetBlock::new);
    }

    static void registerItems() {
        for (ChessColor color : ChessColor.values()) {
            if (!color.available()) continue;
            // Owner N18 asked for 128; 99 is the engine maximum (ItemStack and max_stack_size codecs allow 1..99).
            ChessItems.OCTETS.put(color, ChessItems.item(ChessItems.octetId(color), s -> new CheckerOctetItem(color, s.stacksTo(CheckerOctetItem.MAX_STACK))));
        }
    }

    /** The octet cell; null without Simple Octets. */
    public static @Nullable Block cell() {
        return checkerOctet;
    }
}
