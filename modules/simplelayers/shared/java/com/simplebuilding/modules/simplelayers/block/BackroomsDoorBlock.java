package com.simplebuilding.modules.simplelayers.block;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.backrooms.door.DoorNet;
import com.simplebuilding.modules.simplelayers.backrooms.door.Entrances;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BackroomsStackGenerator;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack;
import com.simplebuilding.modules.simplelayers.backrooms.stack.Bands;
import com.simplelib.api.portal.PortalDoorBlock;
import com.simplelib.api.portal.PortalLinks;
import com.simplelib.api.portal.PortalRule;
import com.simplelib.api.portal.PortalShape;
import com.simplelib.api.portal.PortalTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The core's Backrooms door (BACK22 core K5, SimpleLib portal door): only generated (no item), unbreakable. {@code symbol} = the level it
 * leads to (0 = the house), so whoever remembers the symbols knows the way back. Where it leads is computed, not stored: stack doors
 * from {@link DoorNet} (both ends in the same chunk), the Lobby entrance and the cellar door from {@link Entrances} plus the house's
 * {@link PortalLinks} entry. Repaired/activated with the part of the level the player stands in ({@code Band.repairPart}, R8).
 * Level branches may register their own skin with the same behaviour (subclass) - concept K-R8.
 */
public class BackroomsDoorBlock extends PortalDoorBlock {
    public static final IntegerProperty SYMBOL = IntegerProperty.create("symbol", 0, BandStack.BANDS);
    public static final int SURFACE_COLOR = 0x9FB8D0;

    public BackroomsDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
        registerDefaultState(defaultBlockState().setValue(SYMBOL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SYMBOL);
    }

    /** The world seed the stack is generated from (null while the stack dimension is missing). */
    public static @Nullable Long seed(MinecraftServer server) {
        ServerLevel stack = server.getLevel(SimpleLayers.STACK);
        return stack == null ? null : BackroomsStackGenerator.seed(stack.getChunkSource().randomState());
    }

    @Override
    public @Nullable PortalTarget resolve(ServerLevel level, PortalShape shape) {
        MinecraftServer server = level.getServer();
        Long seedOrNull = seed(server);
        if (seedOrNull == null) return null;
        long seed = seedOrNull;
        BlockPos a = shape.anchor();
        int cx = a.getX() >> 4, cz = a.getZ() >> 4;
        if (level.dimension() == SimpleLayers.STACK) {
            DoorNet.Found f = DoorNet.at(seed, a.getX(), a.getY(), a.getZ());
            if (f != null) return stackTarget(seed, f, cx, cz);
            DoorEnd entrance = BandStack.bandAt(a.getY()) == 1 ? Entrances.lobbyEnd(seed, cx, cz) : null;
            if (entrance == null || entrance.x() != a.getX() || entrance.z() != a.getZ() || BandStack.worldY(1, entrance.ly()) != a.getY()) return null;
            long link = Entrances.link(cx, cz);
            PortalLinks.End house = PortalLinks.get(server, link);
            if (house != null) return new PortalTarget(house.dimension(), house.anchor(), house.facing(), house.width(), link, PortalRule.TWO_WAY, SURFACE_COLOR);
            // the house was never built here (steep or wet ground): out onto the surface of this chunk
            ServerLevel overworld = server.overworld();
            int x = cx * 16 + 8, z = cz * 16 + 8;
            overworld.getChunk(cx, cz);
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            return new PortalTarget(Level.OVERWORLD, new BlockPos(x, y, z + 1), Direction.SOUTH, 1, link, PortalRule.TWO_WAY, SURFACE_COLOR);
        }
        // the cellar door of a house: only the one the house registered
        long link = Entrances.link(cx, cz);
        PortalLinks.End mine = PortalLinks.get(server, link);
        DoorEnd lobby = Entrances.lobbyEnd(seed, cx, cz);
        if (mine == null || lobby == null || !mine.anchor().equals(a) || mine.dimension() != level.dimension()) return null;
        return target(SimpleLayers.STACK, 1, lobby, link, PortalRule.TWO_WAY, Bands.get(1).view().fogColor());
    }

    static PortalTarget stackTarget(long seed, DoorNet.Found f, int cx, int cz) {
        PortalRule rule = Bands.get(f.upper()).doorRule().portalRule();
        long link = DoorNet.link(f.upper(), cx, cz);
        int to = f.target();
        int color = Bands.get(to).view().fogColor();
        DoorEnd other = f.down() ? DoorNet.up(seed, to, cx, cz) : DoorNet.down(seed, to, cx, cz);
        if (other != null) return target(SimpleLayers.STACK, to, other, link, rule, color);
        // one-way down (BACK22c §8.1): arrival on a free spot of the level below, no door back
        int[] spot = Bands.get(to).layout().arrival(seed, cx * 16 + 8, cz * 16 + 8);
        BlockPos front = new BlockPos(spot[0], BandStack.worldY(to, spot[1]), spot[2]);
        return new PortalTarget(SimpleLayers.STACK, front.relative(Direction.SOUTH), Direction.SOUTH, 1, link, rule, color);
    }

    static PortalTarget target(ResourceKey<Level> dimension, int band, DoorEnd e, long link, PortalRule rule, int color) {
        return new PortalTarget(dimension, new BlockPos(e.x(), BandStack.worldY(band, e.ly()), e.z()), Direction.from2DDataValue(e.facing()),
                e.width(), link, rule, color);
    }

    @Override
    public boolean mendsWith(ServerLevel level, PortalShape shape, ItemStack stack) {
        if (level.dimension() != SimpleLayers.STACK) return false;
        int band = BandStack.bandAt(shape.anchor().getY());
        if (band == 0) return false;
        Item part = Bands.get(band).repairPart();
        return part != null && stack.is(part);
    }
}
