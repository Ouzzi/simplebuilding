package com.simplebuilding.modules.simplevault;

import com.simplebuilding.modules.simplevault.gen.VaultPiece;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** {@link VaultPiece} → BlockState, the only place the Bedrock Vault generator touches Minecraft blocks (parsed once, cached). */
public final class VaultBlocks {
    private static volatile BlockState[] states;

    private VaultBlocks() {}

    public static BlockState state(VaultPiece piece) {
        BlockState[] s = states;
        if (s == null) {
            s = new BlockState[VaultPiece.values().length];
            for (VaultPiece p : VaultPiece.values()) s[p.ordinal()] = parse(p.id);
            states = s;
        }
        return s[piece.ordinal()];
    }

    /** {@code namespace:path[key=value,...]}; unknown blocks or values fall back (air / default value). */
    static BlockState parse(String id) {
        int open = id.indexOf('[');
        String name = open < 0 ? id : id.substring(0, open);
        Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(name)).orElse(Blocks.AIR);
        BlockState state = block.defaultBlockState();
        if (open < 0) return state;
        for (String kv : id.substring(open + 1, id.length() - 1).split(",")) {
            String[] p = kv.split("=");
            Property<?> property = block.getStateDefinition().getProperty(p[0]);
            if (property != null) state = with(state, property, p[1]);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
        Optional<T> v = property.getValue(value);
        return v.isPresent() ? state.setValue(property, v.get()) : state;
    }
}
