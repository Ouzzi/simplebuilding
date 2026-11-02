package com.simplefoundry.block;

import com.simplefoundry.Metal;
import java.util.List;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The melting chamber. Four window parts (s0 = lowest) show the melt that lies at that height, so the layers of the tank are drawn on top of
 * each other behind glass (owner 02.11., Variant A). The desk sets the parts ({@code DeskBlockEntity#refreshChambers}); they are display only.
 */
public class ChamberBlock extends Block {
    public static final int PARTS = 4;

    public enum Fill implements StringRepresentable {
        NONE("none"), GOLD("gold"), COPPER("copper"), IRON("iron");

        private final String id;

        Fill(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        public static Fill of(Metal metal) {
            return metal == null ? NONE : values()[metal.ordinal() + 1];
        }
    }

    public static final EnumProperty<Fill> S0 = EnumProperty.create("s0", Fill.class);
    public static final EnumProperty<Fill> S1 = EnumProperty.create("s1", Fill.class);
    public static final EnumProperty<Fill> S2 = EnumProperty.create("s2", Fill.class);
    public static final EnumProperty<Fill> S3 = EnumProperty.create("s3", Fill.class);
    public static final List<EnumProperty<Fill>> SLOTS = List.of(S0, S1, S2, S3);

    public ChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(S0, Fill.NONE).setValue(S1, Fill.NONE).setValue(S2, Fill.NONE).setValue(S3, Fill.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(S0, S1, S2, S3);
    }
}
