package com.simplebuilding.dummy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.dummy.SmallArmorStand;
import com.simplebuilding.version.McClientVersion;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.animal.nautilus.NautilusArmorModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Kleiner Ruestungsstaender (Nachtrag 29, Referenzen <preview-dir>/refs-stands/2-4): ein Holzpfosten mit Querholz (T) auf
 * einer Steinplatte, gezeichnet mit Vanillas Ruestungsstaender-Textur (Holz mit Ringen, Steinplatte). Vanillas Modell
 * bleibt unsichtbar; eigene Ebenen zeichnen Pfosten, das eine Ruestungsteil (verschoben, damit es am Pfosten haengt) und
 * eine Tier-Ruestung mit Vanillas Pferde-, Wolfs- oder Nautilus-Ruestungsmodell, seitlich gedreht, Pfosten im Bauch.
 */
public class SmallArmorStandRenderer extends ArmorStandRenderer {
    /** Pfosten-Hoehe in Pixeln (Oberkante Querholz). */
    static final float POST_TOP = 16.0F;

    public SmallArmorStandRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = this.getModel().root();
        for (String part : new String[]{"head", "body", "right_arm", "left_arm", "right_leg", "left_leg", "right_body_stick",
                "left_body_stick", "shoulder_stick", "base_plate"}) {
            root.getChild(part).visible = false;
        }
        this.layers.clear();
        this.addLayer(new PostLayer(this));
        this.addLayer(new ShiftedArmorLayer(this, new HumanoidArmorLayer<>(this,
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
                context.getEquipmentRenderer())));
        this.addLayer(new AnimalArmorLayer(this, context));
    }

    /** Der Render-Zustand mit der Tier-Ruestung (Vanillas Staender kennt keinen Koerper-Slot). */
    public static final class State extends ArmorStandRenderState {
        public ItemStack bodyArmor = ItemStack.EMPTY;
        public SmallArmorStand.@Nullable Animal animal;
    }

    @Override
    public ArmorStandRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArmorStand stand, ArmorStandRenderState state, float partialTick) {
        super.extractRenderState(stand, state, partialTick);
        state.isSmall = false;
        state.showArms = false;
        state.showBasePlate = true;
        if (state instanceof State own) {
            own.bodyArmor = stand.getItemBySlot(EquipmentSlot.BODY).copy();
            own.animal = SmallArmorStand.animal(own.bodyArmor);
        }
    }

    /** Platte 12x1x12, Pfosten 2x2 (11 + 2 px, Holz der Bein- und Halsstange), Querholz 8x2x2 (Schulterstange) oben. */
    static final class PostLayer extends RenderLayer<ArmorStandRenderState, ArmorStandArmorModel> {
        private final ModelPart post;

        PostLayer(RenderLayerParent<ArmorStandRenderState, ArmorStandArmorModel> parent) {
            super(parent);
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, 23.0F, -6.0F, 12.0F, 1.0F, 12.0F), PartPose.ZERO);
            root.addOrReplaceChild("post", CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, 12.0F, -1.0F, 2.0F, 11.0F, 2.0F), PartPose.ZERO);
            root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
            root.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 48).addBox(-4.0F, 24.0F - POST_TOP, -1.0F, 8.0F, 2.0F, 2.0F),
                    PartPose.ZERO);
            this.post = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorStandRenderState state, float yRot, float xRot) {
            if (state.isMarker || state.isInvisible) {
                return;
            }
            McClientVersion.submitTintedModelPart(collector, this.post, poseStack, RenderTypes.entityCutout(DEFAULT_SKIN_LOCATION), lightCoords,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), -1);
        }
    }

    /**
     * Vanillas Ruestungsebene, je Teil verschoben (Pixel, positiv = tiefer): Brust 8 (Schultern auf dem Querholz), Helm 7
     * (sitzt auf dem T), Hose und Stiefel -1 (stehen auf der Platte, der Guertel umschliesst das Querholz).
     */
    static final class ShiftedArmorLayer extends RenderLayer<ArmorStandRenderState, ArmorStandArmorModel> {
        private final HumanoidArmorLayer<ArmorStandRenderState, ArmorStandArmorModel, ArmorStandArmorModel> armor;

        ShiftedArmorLayer(RenderLayerParent<ArmorStandRenderState, ArmorStandArmorModel> parent,
                HumanoidArmorLayer<ArmorStandRenderState, ArmorStandArmorModel, ArmorStandArmorModel> armor) {
            super(parent);
            this.armor = armor;
        }

        static float shift(ArmorStandRenderState state) {
            if (!state.chestEquipment.isEmpty()) {
                return 8.0F;
            }
            if (!state.headEquipment.isEmpty()) {
                return 7.0F;
            }
            return -1.0F;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorStandRenderState state, float yRot, float xRot) {
            poseStack.pushPose();
            poseStack.translate(0.0F, shift(state) / 16.0F, 0.0F);
            this.armor.submit(poseStack, collector, lightCoords, state, yRot, xRot);
            poseStack.popPose();
        }
    }

    /**
     * Tier-Ruestung in Tierform mit Vanillas Ruestungsmodellen und Ebenen (Farbe, Glanz wie am Tier), quer zum Staender
     * gedreht, damit man sie von vorn seitlich sieht; Wolf 5 px und Nautilus 7 px hoeher, damit der Pfosten im Bauch steckt.
     * Die Zustaende sind feste Ruhe-Posen (keine Animation), deshalb teilen sich alle Staender je einen.
     */
    static final class AnimalArmorLayer extends RenderLayer<ArmorStandRenderState, ArmorStandArmorModel> {
        private final EquipmentLayerRenderer equipment;
        private final HorseModel horse;
        private final AdultWolfModel wolf;
        private final NautilusArmorModel nautilus;
        private final HorseRenderState horseState = new HorseRenderState();
        private final WolfRenderState wolfState = new WolfRenderState();
        private final NautilusRenderState nautilusState = new NautilusRenderState();

        AnimalArmorLayer(RenderLayerParent<ArmorStandRenderState, ArmorStandArmorModel> parent, EntityRendererProvider.Context context) {
            super(parent);
            this.equipment = context.getEquipmentRenderer();
            this.horse = new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR));
            this.wolf = new AdultWolfModel(context.bakeLayer(ModelLayers.WOLF_ARMOR));
            this.nautilus = new NautilusArmorModel(context.bakeLayer(ModelLayers.NAUTILUS_ARMOR));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorStandRenderState state, float yRot, float xRot) {
            if (!(state instanceof State own) || own.animal == null || state.isInvisible) {
                return;
            }
            Equippable equippable = own.bodyArmor.get(DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.assetId().isEmpty()) {
                return;
            }
            poseStack.pushPose();
            poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(90.0)));
            switch (own.animal) {
                case HORSE -> render(EquipmentClientInfo.LayerType.HORSE_BODY, equippable, this.horse, this.horseState, own, poseStack, collector,
                        lightCoords);
                case WOLF -> {
                    poseStack.translate(0.0F, -5.0F / 16.0F, 0.0F);
                    render(EquipmentClientInfo.LayerType.WOLF_BODY, equippable, this.wolf, this.wolfState, own, poseStack, collector, lightCoords);
                }
                case NAUTILUS -> {
                    poseStack.translate(0.0F, -7.0F / 16.0F, 0.0F);
                    render(EquipmentClientInfo.LayerType.NAUTILUS_BODY, equippable, this.nautilus, this.nautilusState, own, poseStack, collector,
                            lightCoords);
                }
            }
            poseStack.popPose();
        }

        private <S> void render(EquipmentClientInfo.LayerType type, Equippable equippable, Model<? super S> model, S animalState, State own,
                PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
            this.equipment.renderLayers(type, equippable.assetId().get(), model, animalState, own.bodyArmor, poseStack, collector, lightCoords,
                    own.outlineColor);
        }
    }
}
