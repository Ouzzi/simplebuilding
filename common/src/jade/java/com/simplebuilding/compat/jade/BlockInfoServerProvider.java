package com.simplebuilding.compat.jade;

import com.simplebuilding.compat.BlockInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of one server {@link BlockInfo.Topic}: turns the block entity into lines and puts them
 * into Jade's data tag, which {@link BlockInfoClientProvider} reads back on the client. Shares the id
 * {@code simplebuilding:<topic>} with its client half, as Jade's own split providers do.
 */
final class BlockInfoServerProvider implements IServerDataProvider<BlockAccessor> {
    private final BlockInfo.Topic topic;
    private final Identifier uid;

    BlockInfoServerProvider(BlockInfo.Topic topic) {
        this.topic = topic;
        this.uid = uidOf(topic);
    }

    static Identifier uidOf(BlockInfo.Topic topic) {
        return Identifier.fromNamespaceAndPath("simplebuilding", topic.id());
    }

    @Override
    public Identifier getUid() {
        return uid;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be != null && BlockInfo.handles(topic, be)) {
            BlockInfo.write(data, topic, BlockInfo.serverLines(topic, be));
        }
    }
}
