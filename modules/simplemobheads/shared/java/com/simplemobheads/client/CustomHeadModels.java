package com.simplemobheads.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Hand-made geometry of the 16 heads that moved in from SimpleBuilding ({@code ModSkullModels}) and Simple Fun
 * ({@code AnimalSkullModels}) - unchanged, the owner accepted these looks (2026-09-29 to 10-02: the cow's muzzle, the
 * silverfish's size, the shulker's half-open shell). Each is the head of the Vanilla entity model moved onto the skull
 * floor; heads with eyes, clothes or a translucent shell get their passes (eyes {@value #EYES_INFLATE} px in front of the
 * face because the head itself is drawn with {@code entityCutoutZOffset}).
 */
public final class CustomHeadModels {
    public static final Identifier ENDERMAN_EYES = Identifier.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png");
    public static final Identifier DROWNED_OUTER = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png");
    public static final Identifier SPIDER_EYES = Identifier.withDefaultNamespace("textures/entity/spider/spider_eyes.png");
    public static final Identifier STRAY_OVERLAY = Identifier.withDefaultNamespace("textures/entity/skeleton/stray_overlay.png");
    public static final Identifier BOGGED_OVERLAY = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png");
    public static final Identifier BREEZE_EYES = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png");
    private static final float EYES_INFLATE = 0.25F;

    private CustomHeadModels() {
    }

    /** The model of custom geometry {@code name} for a look with {@code texture}; null for an unknown name. */
    public static @Nullable MobHeadModel create(String name, Identifier texture) {
        return switch (name) {
            case "blaze" -> plain(SkullModel.createMobHeadLayer());
            case "enderman" -> layered(endermanHeadLayer(), pass(endermanEyesLayer(), RenderTypes::eyes, ENDERMAN_EYES));
            case "husk" -> plain(SkullModel.createHumanoidHeadLayer());
            case "drowned" -> layered(SkullModel.createHumanoidHeadLayer(),
                    pass(humanoidHeadLayer(new CubeDeformation(0.25F), 64, 64), RenderTypes::entityCutoutZOffset, DROWNED_OUTER));
            case "spider" -> layered(spiderHeadLayer(0.0F), pass(spiderHeadLayer(EYES_INFLATE), RenderTypes::eyes, SPIDER_EYES));
            case "stray" -> layered(SkullModel.createMobHeadLayer(),
                    pass(humanoidHeadLayer(new CubeDeformation(0.25F), 64, 32), RenderTypes::entityCutoutZOffset, STRAY_OVERLAY));
            case "bogged" -> layered(boggedHeadLayer(),
                    pass(humanoidHeadLayer(new CubeDeformation(0.2F), 64, 32), RenderTypes::entityCutoutZOffset, BOGGED_OVERLAY));
            case "slime" -> layered(slimeInnerLayer(), pass(slimeOuterLayer(), RenderTypes::entityTranslucent, texture));
            case "silverfish" -> plain(silverfishHeadLayer());
            case "breeze" -> layered(breezeHeadLayer(0.0F), pass(breezeHeadLayer(EYES_INFLATE), RenderTypes::breezeEyes, BREEZE_EYES));
            case "shulker" -> plain(shulkerHeadLayer());
            case "pig", "cow", "chicken" -> plain(animalHeadLayer(name));
            default -> null;
        };
    }

    private static MobHeadModel plain(LayerDefinition definition) {
        return new MobHeadModel(definition.bakeRoot(), List.of());
    }

    private static MobHeadModel layered(LayerDefinition base, MobHeadModel.Pass... passes) {
        return new MobHeadModel(base.bakeRoot(), new ArrayList<>(List.of(passes)));
    }

    private static MobHeadModel.Pass pass(LayerDefinition definition, Function<Identifier, RenderType> type, Identifier texture) {
        return new MobHeadModel.Pass(new MobHeadModel.Part(definition.bakeRoot()), type.apply(texture), -1);
    }

    /** Simple Fun's animal heads (Vanilla 26.3 pig/cow/chicken heads on the skull floor). */
    private static LayerDefinition animalHeadLayer(String name) {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 0);
        switch (name) {
            // PigModel: the snout is 4x3x1 at UV 16,16 (a 2 deep box read the wrong texels and stuck out one pixel too far).
            case "pig" -> cube.addBox(-4, -8, -4, 8, 8, 8).texOffs(16, 16).addBox(-2, -4, -5, 4, 3, 1);
            // CowModel head (64x64): head, muzzle at UV 1,33 and two horns (owner 2026-10-01).
            case "cow" -> cube.addBox(-4, -8, -3, 8, 8, 6).texOffs(1, 33).addBox(-3, -3, -4, 6, 3, 1)
                    .texOffs(22, 0).addBox(-5, -9, -2, 1, 3, 1).addBox(4, -9, -2, 1, 3, 1);
            default -> cube.addBox(-2, -6, -2, 4, 6, 3).texOffs(14, 0).addBox(-2, -4, -4, 4, 2, 2).texOffs(14, 4).addBox(-1, -2, -3, 2, 2, 2);
        }
        mesh.getRoot().addOrReplaceChild("head", cube, PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, name.equals("chicken") ? 32 : 64);
    }

    /** {@code EndermanModel} head: cube plus the drawn-in jaw, texture 64x32. */
    private static LayerDefinition endermanHeadLayer() {
        MeshDefinition mesh = SkullModel.createHeadModel();
        mesh.getRoot().getChild("head").addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(-0.5F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition endermanEyesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F, 0.0F, EYES_INFLATE)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition humanoidHeadLayer(CubeDeformation deformation, int width, int height) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation), PartPose.ZERO);
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation.extend(0.5F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, width, height);
    }

    private static LayerDefinition spiderHeadLayer(float depth) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 4)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F, 0.0F, depth)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition boggedHeadLayer() {
        MeshDefinition mesh = SkullModel.createHeadModel();
        PartDefinition mushrooms = mesh.getRoot().getChild("head").addOrReplaceChild("mushrooms", CubeListBuilder.create(), PartPose.ZERO);
        mushrooms.addOrReplaceChild("red_mushroom_1", CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) (Math.PI / 4), 0.0F));
        mushrooms.addOrReplaceChild("red_mushroom_2", CubeListBuilder.create().texOffs(50, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(3.0F, -8.0F, 3.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0), 0.0F));
        mushrooms.addOrReplaceChild("brown_mushroom_1", CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) (Math.PI / 4), 0.0F));
        mushrooms.addOrReplaceChild("brown_mushroom_2", CubeListBuilder.create().texOffs(50, 22).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.0F, (float) (Math.PI * 3.0 / 4.0), 0.0F));
        mushrooms.addOrReplaceChild("brown_mushroom_3", CubeListBuilder.create().texOffs(50, 28).addBox(-3.0F, -4.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, (float) (Math.PI / 4)));
        mushrooms.addOrReplaceChild("brown_mushroom_4", CubeListBuilder.create().texOffs(50, 28).addBox(-3.0F, -4.0F, 0.0F, 6.0F, 4.0F, 0.0F),
                PartPose.offsetAndRotation(-2.0F, -1.0F, 4.0F, (float) (-Math.PI / 2), 0.0F, (float) (Math.PI * 3.0 / 4.0)));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition slimeInnerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(32, 0).addBox(-3.25F, -6.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(32, 4).addBox(1.25F, -6.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(32, 8).addBox(0.0F, -3.0F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition slimeOuterLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition silverfishHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("segment0", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        head.addOrReplaceChild("segment1", CubeListBuilder.create().texOffs(0, 4).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static LayerDefinition breezeHeadLayer(float depth) {
        MeshDefinition mesh = new MeshDefinition();
        CubeDeformation deformation = new CubeDeformation(0.0F, 0.0F, depth);
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(4, 24).addBox(-5.0F, -5.0F, -4.2F, 10.0F, 3.0F, 4.0F, deformation)
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static LayerDefinition shulkerHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO.withScale(0.5F));
        head.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 8.0F, 16.0F), PartPose.ZERO);
        head.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 12.0F, 16.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        head.addOrReplaceChild("shulker_head", CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
