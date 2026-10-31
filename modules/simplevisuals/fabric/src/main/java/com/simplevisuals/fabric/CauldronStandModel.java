package com.simplevisuals.fabric;

import com.simplevisuals.CauldronStand;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * IG31-21 (owner choice 31.10., variant A): wraps every cauldron block model (all fill levels, Vanilla and modded cauldrons) and,
 * when the block below is a campfire and {@code visuals.cauldronStand} is on, emits the stand of {@link CauldronStand#BOXES} in the
 * cauldron's own particle texture (the cauldron side). The cauldron itself is drawn unchanged; no block, no collision, no mixin.
 */
public final class CauldronStandModel extends WrapperBlockStateModel {
    private CauldronStandModel(BlockStateModel wrapped) {
        super(wrapped);
    }

    /** Hooks the wrapper into model baking (client init). */
    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, ctx) ->
                ctx.state().getBlock() instanceof AbstractCauldronBlock ? new CauldronStandModel(model) : model));
    }

    private static boolean standing(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return pos != null && CauldronStand.enabled() && CauldronStand.applies(state, level.getBlockState(pos.below()));
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        super.emitQuads(emitter, level, pos, state, random, cullTest);
        if (!standing(level, pos, state)) return;
        var material = this.wrapped.particleMaterial();
        for (CauldronStand.Box b : CauldronStand.BOXES) {
            float x0 = b.x0() / 16F, y0 = b.y0() / 16F, z0 = b.z0() / 16F, x1 = b.x1() / 16F, y1 = b.y1() / 16F, z1 = b.z1() / 16F;
            // Vertex order as Vanilla FaceInfo: top-left, bottom-left, bottom-right, top-right; uv in pixels (side faces v = -y).
            face(emitter, material, Direction.UP, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, b.x0(), b.z0(), b.x1(), b.z1());
            face(emitter, material, Direction.DOWN, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, b.x0(), b.z0(), b.x1(), b.z1());
            face(emitter, material, Direction.NORTH, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 16 - b.x1(), -b.y1(), 16 - b.x0(), -b.y0());
            face(emitter, material, Direction.SOUTH, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, b.x0(), -b.y1(), b.x1(), -b.y0());
            face(emitter, material, Direction.WEST, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, b.z0(), -b.y1(), b.z1(), -b.y0());
            face(emitter, material, Direction.EAST, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 16 - b.z1(), -b.y1(), 16 - b.z0(), -b.y0());
        }
    }

    private static void face(QuadEmitter e, net.minecraft.client.resources.model.sprite.Material.Baked material, Direction dir,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        e.pos(0, ax, ay, az).pos(1, bx, by, bz).pos(2, cx, cy, cz).pos(3, dx, dy, dz);
        e.uv(0, u0, v0).uv(1, u0, v1).uv(2, u1, v1).uv(3, u1, v0);
        for (int i = 0; i < 4; i++) e.color(i, -1);
        e.nominalFace(dir);
        e.materialBake(material, 0); // uv in 0..16 pixels, mapped onto the cauldron sprite
        e.emit();
    }

    /** No shared mesh cache while the stand is drawn: it depends on the block below. */
    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return standing(level, pos, state) ? null : super.createGeometryKey(level, pos, state, random);
    }
}
