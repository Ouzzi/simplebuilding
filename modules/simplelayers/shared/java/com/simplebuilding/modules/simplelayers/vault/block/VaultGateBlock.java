package com.simplebuilding.modules.simplelayers.vault.block;

import com.simplebuilding.modules.simplelayers.vault.VaultTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Vault Gate (UNDER21): the way into the Bedrock Vault. In the Overworld it fills the bottom of a rare bedrock gap; inside the
 * vault six of them stand in the bedrock wall of the Entrance Hall. Open = no collision, faint light and rising motes (A14);
 * walking into it travels ({@link VaultTravel#gateContact}). The inner gate seals ({@link #OPEN} false, solid) some seconds
 * after the first entry while anyone is inside and opens again when the vault is empty. Unbreakable, no item.
 */
public class VaultGateBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public VaultGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, Boolean.TRUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    public static int light(BlockState state) {
        return state.getValue(OPEN) ? 6 : 0;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (state.getValue(OPEN) && level instanceof ServerLevel server) VaultTravel.gateContact(server, entity, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.04, 0.0);
        }
    }
}
