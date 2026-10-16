package com.simplebuilding.modules.simpleend.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Nibblemite: four Endermite-like segments at three quarters of the size, a round head with two big eyes and two
 * little feelers that wiggle. Texture 64 x 32 (layout in modules/simpleend/tools/gen_textures.py, NIBBLE_BOXES).
 */
public class NibblemiteModel extends EntityModel<NibblemiteRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("simpleend", "nibblemite"), "main");
    static final int[][] SIZES = {{4, 3, 3}, {6, 4, 5}, {4, 3, 2}, {2, 2, 1}};
    static final int[][] TEXS = {{0, 0}, {0, 6}, {0, 15}, {0, 20}};
    private final ModelPart[] segments = new ModelPart[4];
    private final ModelPart rightFeeler, leftFeeler;

    public NibblemiteModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 4; i++) segments[i] = root.getChild("segment" + i);
        rightFeeler = segments[0].getChild("right_feeler");
        leftFeeler = segments[0].getChild("left_feeler");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float placement = -3.5F;
        for (int i = 0; i < 4; i++) {
            PartDefinition seg = root.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(TEXS[i][0], TEXS[i][1])
                            .addBox(SIZES[i][0] * -0.5F, 0.0F, SIZES[i][2] * -0.5F, SIZES[i][0], SIZES[i][1], SIZES[i][2]),
                    PartPose.offset(0.0F, 24 - SIZES[i][1], placement));
            if (i == 0) {
                seg.addOrReplaceChild("right_feeler", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1), PartPose.offset(-1.0F, 0.0F, -1.0F));
                seg.addOrReplaceChild("left_feeler", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1), PartPose.offset(1.0F, 0.0F, -1.0F));
            }
            if (i < 3) placement += (SIZES[i][2] + SIZES[i + 1][2]) * 0.5F;
        }
        return LayerDefinition.create(mesh, 64, 32).apply(MeshTransformer.scaling(0.75F));
    }

    @Override
    public void setupAnim(NibblemiteRenderer.State state) {
        super.setupAnim(state);
        // Standing: a slow, small breath. Moving: the quick wiggle, in step with the ground covered (no sliding on the spot).
        // Both are blended by speed, and every Nibblemite has its own phase (entity id), so a group never wiggles in unison.
        float m = Mth.smoothstep(Mth.clamp(state.walkAnimationSpeed * 2.0F, 0.0F, 1.0F));
        float idleT = state.ageInTicks * 0.08F + state.phase;
        float walkT = state.walkAnimationPos * 0.6F + state.phase;
        for (int i = 0; i < segments.length; i++) {
            float off = i * 0.15F * (float) Math.PI;
            float yRot = Mth.lerp(m, Mth.cos(idleT + off) * 0.25F, Mth.cos(walkT + off));
            float x = Mth.lerp(m, Mth.sin(idleT + off) * 0.25F, Mth.sin(walkT + off));
            segments[i].yRot = yRot * (float) Math.PI * 0.01F * (1 + Math.abs(i - 2));
            segments[i].x = x * (float) Math.PI * 0.08F * Math.abs(i - 2);
        }
        float wiggle = Mth.lerp(m, 0.08F * Mth.sin(state.ageInTicks * 0.1F + state.phase), 0.25F * Mth.sin(state.walkAnimationPos * 0.35F + state.phase));
        rightFeeler.zRot = -0.3F + wiggle;
        leftFeeler.zRot = 0.3F - wiggle;
        if (state.chew > 0.0F) {
            // Chew cycle while nibbling End stone: the head dips to the block, nods four times, the feelers lie back; eased in and out.
            float env = Mth.smoothstep(Mth.clamp(Math.min(state.chew, 1.0F - state.chew) / 0.12F, 0.0F, 1.0F));
            segments[0].xRot += env * (0.45F + 0.3F * Mth.sin(state.chew * 8.0F * Mth.PI));
            rightFeeler.zRot *= 1.0F - 0.7F * env;
            leftFeeler.zRot *= 1.0F - 0.7F * env;
        }
        if (state.deathTime > 0.0F) {
            // Death: the body curls up sideways and the feelers droop (on top of the vanilla flip onto the back).
            float d = Mth.smoothstep(Mth.clamp(state.deathTime / 20.0F, 0.0F, 1.0F));
            for (int i = 0; i < segments.length; i++) segments[i].yRot += d * 0.3F * i;
            rightFeeler.zRot = Mth.lerp(d, rightFeeler.zRot, -1.3F);
            leftFeeler.zRot = Mth.lerp(d, leftFeeler.zRot, 1.3F);
        }
    }
}
