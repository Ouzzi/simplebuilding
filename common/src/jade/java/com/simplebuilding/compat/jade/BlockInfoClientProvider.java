package com.simplebuilding.compat.jade;

import com.simplebuilding.compat.BlockInfo;
import java.util.List;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client half of one {@link BlockInfo.Topic}: reads the lines back from Jade's server data (server
 * topics, written by {@link BlockInfoServerProvider}) or builds them from the block state (state
 * topics) and adds one tooltip line each. Its id {@code simplebuilding:<topic>} is also the switch in
 * Jade's plugin config.
 *
 * <p>Deliberately NOT also an {@code IServerDataProvider}: since Minecraft 1.21.6 Jade refuses such a
 * provider on a physical client ({@code WailaCommonRegistration.checkDataProvider} throws and the
 * client does not start). {@code JadeProviderSplitTest} guards this.
 */
final class BlockInfoClientProvider implements IBlockComponentProvider {
    private final BlockInfo.Topic topic;
    private final Identifier uid;

    BlockInfoClientProvider(BlockInfo.Topic topic) {
        this.topic = topic;
        this.uid = BlockInfoServerProvider.uidOf(topic);
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
}
