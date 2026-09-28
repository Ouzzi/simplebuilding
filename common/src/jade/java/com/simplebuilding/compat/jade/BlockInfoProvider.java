package com.simplebuilding.compat.jade;

import com.simplebuilding.compat.BlockInfo;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * One {@link BlockInfo.Topic} as a Jade provider. All the work is in {@link BlockInfo}: on the server
 * it turns the block entity into lines and puts them into Jade's data tag, on the client it reads
 * them back (server topics) or builds them from the block state (state topics) and adds one tooltip
 * line each. Its id {@code simplebuilding:<topic>} is also the switch in Jade's plugin config.
 */
final class BlockInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private final BlockInfo.Topic topic;
    private final Identifier uid;

    BlockInfoProvider(BlockInfo.Topic topic) {
        this.topic = topic;
        this.uid = Identifier.fromNamespaceAndPath("simplebuilding", topic.id());
    }

    @Override
    public Identifier getUid() {
        return uid;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        List<BlockInfo.Line> lines = topic.fromServer()
                ? BlockInfo.read(accessor.getServerData(), topic)
                : BlockInfo.stateLines(topic, accessor.getBlockState());
        for (BlockInfo.Line line : lines) {
            tooltip.add(line.toComponent());
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be != null && BlockInfo.handles(topic, be)) {
            BlockInfo.write(data, topic, BlockInfo.serverLines(topic, be));
        }
    }
}
