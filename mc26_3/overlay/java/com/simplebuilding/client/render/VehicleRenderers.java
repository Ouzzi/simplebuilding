package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.entity.vehicle.BoatWoods;
import com.simplebuilding.entity.vehicle.TieredChestBoat;
import com.simplebuilding.entity.vehicle.TieredChestMinecart;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.RaftRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Quaternionf;

/**
 * Renderers of the tiered vehicles (Queue N19/N23): vanilla's cart and boat models. Furnace and hopper carts show
 * their tier block as vanilla shows its furnace and hopper; the chest cart and the chest boat draw the tier chest
 * (chest atlas of the tier, like {@link TieredChestRenderer}). The chest boat is vanilla's plain boat (or raft) of its
 * wood with the tier chest where vanilla's chest boat has its chest.
 */
public final class VehicleRenderers {

    /** The loader's renderer registration (Fabric registry, NeoForge/Forge event). */
    public interface Registrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    private VehicleRenderers() {
    }

    public static void register(Registrar registrar) {
        if (!com.simplebuilding.version.McVersion.TIERED_VEHICLES) {
            return;
        }
        registrar.register(ModEntities.REINFORCED_CHEST_MINECART, ChestCartRenderer::new);
        registrar.register(ModEntities.NETHERITE_CHEST_MINECART, ChestCartRenderer::new);
        registrar.register(ModEntities.ENDERITE_CHEST_MINECART, ChestCartRenderer::new);
        registrar.register(ModEntities.REINFORCED_FURNACE_MINECART, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART));
        registrar.register(ModEntities.NETHERITE_FURNACE_MINECART, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART));
        registrar.register(ModEntities.ENDERITE_FURNACE_MINECART, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART));
        registrar.register(ModEntities.REINFORCED_HOPPER_MINECART, c -> new MinecartRenderer(c, ModelLayers.HOPPER_MINECART));
        registrar.register(ModEntities.NETHERITE_HOPPER_MINECART, c -> new MinecartRenderer(c, ModelLayers.HOPPER_MINECART));
        registrar.register(ModEntities.ENDERITE_HOPPER_MINECART, c -> new MinecartRenderer(c, ModelLayers.HOPPER_MINECART));
        registrar.register(ModEntities.REINFORCED_CHEST_BOAT, ChestBoatRenderer::new);
        registrar.register(ModEntities.NETHERITE_CHEST_BOAT, ChestBoatRenderer::new);
        registrar.register(ModEntities.ENDERITE_CHEST_BOAT, ChestBoatRenderer::new);
    }

    /** The tier chest in vanilla's chest pose of a block (single chest, front to the cart's side like vanilla's). */
    private static void submitChest(ChestModel model, SpriteGetter sprites, ChestTier tier, Direction facing, PoseStack pose,
                                    SubmitNodeCollector collector, int light, int outline) {
        pose.pushPose();
        pose.mulPose(ChestRenderer.modelTransformation(facing));
        collector.submitModel(model, 0.0F, pose, light, OverlayTexture.NO_OVERLAY, -1,
                TieredChestRenderer.spriteFor(tier, ChestType.SINGLE), sprites, outline);
        pose.popPose();
    }

    // ------------------------------------------------------------------ chest cart

    static final class ChestCartRenderer extends AbstractMinecartRenderer<TieredChestMinecart, ChestCartRenderer.State> {
        static final class State extends MinecartRenderState {
            ChestTier tier;
        }

        private final BlockModelResolver blocks;
        private final ChestModel chest;
        private final SpriteGetter sprites;

        ChestCartRenderer(EntityRendererProvider.Context context) {
            super(context, ModelLayers.CHEST_MINECART);
            this.blocks = context.getBlockModelResolver();
            this.chest = new ChestModel(context.bakeLayer(ChestRenderer.LAYERS.select(ChestType.SINGLE)));
            this.sprites = context.getSprites();
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(TieredChestMinecart cart, State state, float partialTicks) {
            super.extractRenderState(cart, state, partialTicks);
            state.tier = null;
            // The tier chest has no block model (its block entity draws it). Vanilla's chest stands in as the content
            // model so the cart draws its contents at all; submitMinecartContents then draws the tier chest instead.
            if (cart.getDisplayBlockState().getBlock() instanceof TieredChestBlock) {
                state.tier = cart.tier();
                this.blocks.update(state.displayBlockModel, Blocks.CHEST.defaultBlockState(), BLOCK_DISPLAY_CONTEXT);
            }
        }

        @Override
        protected void submitMinecartContents(State state, BlockModelRenderState blockModel, PoseStack pose,
                                              SubmitNodeCollector collector, int light) {
            if (state.tier == null) {
                super.submitMinecartContents(state, blockModel, pose, collector, light);
                return;
            }
            submitChest(this.chest, this.sprites, state.tier, Direction.NORTH, pose, collector, light, state.outlineColor);
        }
    }

    // ------------------------------------------------------------------ chest boat

    static final class ChestBoatRenderer extends AbstractBoatRenderer {
        static final class State extends BoatRenderState {
            String wood = BoatWoods.DEFAULT;
            ChestTier tier = ChestTier.REINFORCED;
        }

        private final Map<String, AbstractBoatRenderer> boats = new HashMap<>();
        private final EntityModel<BoatRenderState> oak;
        private final ChestModel chest;
        private final SpriteGetter sprites;

        ChestBoatRenderer(EntityRendererProvider.Context context) {
            super(context, Identifier.withDefaultNamespace("textures/entity/boat/oak.png"));
            for (String wood : BoatWoods.ALL) {
                ModelLayerLocation layer = new ModelLayerLocation(Identifier.withDefaultNamespace("boat/" + wood), "main");
                this.boats.put(wood, BoatWoods.isRaft(wood) ? new RaftRenderer(context, layer) : new BoatRenderer(context, layer));
            }
            this.oak = new BoatModel(context.bakeLayer(ModelLayers.OAK_BOAT));
            this.chest = new ChestModel(context.bakeLayer(ChestRenderer.LAYERS.select(ChestType.SINGLE)));
            this.sprites = context.getSprites();
        }

        @Override
        protected EntityModel<BoatRenderState> model() {
            return this.oak;
        }

        @Override
        public BoatRenderState createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(AbstractBoat boat, BoatRenderState state, float partialTicks) {
            super.extractRenderState(boat, state, partialTicks);
            if (boat instanceof TieredChestBoat tiered && state instanceof State own) {
                own.wood = tiered.wood();
                own.tier = tiered.tier();
            }
        }

        @Override
        public void submit(BoatRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            State own = (State) state;
            AbstractBoatRenderer boat = this.boats.getOrDefault(own.wood, this.boats.get(BoatWoods.DEFAULT));
            boat.submit(state, pose, collector, camera);
            // The boat's pose (AbstractBoatRenderer#submit), then vanilla's chest boat model space: the chest stands
            // 12 px wide and high at the stern, x -14..-2, bottom at y 3 (raft: -2.1), y pointing down.
            pose.pushPose();
            pose.translate(0.0F, 0.375F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
            float hurt = state.hurtTime;
            if (hurt > 0.0F) {
                pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
            }
            if (!state.isUnderWater && !Mth.equal(state.bubbleAngle, 0.0F)) {
                pose.mulPose(new Quaternionf().setAngleAxis(state.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
            }
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.translate(-8.0F / 16.0F, (BoatWoods.isRaft(own.wood) ? -2.1F : 3.0F) / 16.0F, 0.0F);
            pose.scale(-1.0F, -1.0F, 1.0F);
            float size = 12.0F / 14.0F;
            pose.scale(size, size, size);
            pose.translate(-0.5F, 0.0F, -0.5F);
            submitChest(this.chest, this.sprites, own.tier, Direction.WEST, pose, collector, state.lightCoords, state.outlineColor);
            pose.popPose();
        }
    }
}
