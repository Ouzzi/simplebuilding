package com.simpledinos.client;

import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Stegosaurus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Stegosaurus (a-dinorest2): a heavy quadruped whose back rises to the hips, two rows of plates, a small low head and four tail spikes.
 * Walks in the diagonal gait, turns the head with the look and lowers it when something stands in front (part boxes,
 * {@link Stegosaurus#RIG}); the tail swings and swipes sideways when a monster hits it. The plow (hip girth, shafts, cross beam, three
 * shares) shows while worn, lowered to the ground or raised; saddle and armor (neck and flanks) only when worn. Layout: tools/gen_models.py.
 */
public class StegosaurusModel extends EntityModel<StegosaurusRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleDinos.id("stegosaurus"), "main");
    /** How far the raised plow turns up (radians). */
    public static final float PLOW_RAISE = 0.38F;
    private final ModelPart body, neck, head, tail1, tail2, tail3;
    private final ModelPart frontRight, frontLeft, backRight, backLeft, frontRightShin, frontLeftShin, backRightShin, backLeftShin;
    private final ModelPart saddle, armorNeck, armorFlankRight, armorFlankLeft, harness, plow;

    public StegosaurusModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.tail1 = body.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
        this.frontRight = root.getChild("front_right_leg");
        this.frontLeft = root.getChild("front_left_leg");
        this.backRight = root.getChild("back_right_leg");
        this.backLeft = root.getChild("back_left_leg");
        this.frontRightShin = frontRight.getChild("front_right_shin");
        this.frontLeftShin = frontLeft.getChild("front_left_shin");
        this.backRightShin = backRight.getChild("back_right_shin");
        this.backLeftShin = backLeft.getChild("back_left_shin");
        this.saddle = body.getChild("saddle");
        this.armorNeck = neck.getChild("armor_neck");
        this.armorFlankRight = body.getChild("armor_flank_right");
        this.armorFlankLeft = body.getChild("armor_flank_left");
        this.harness = body.getChild("plow_harness");
        this.plow = root.getChild("plow");
    }

    public static LayerDefinition createBodyLayer() {
        return StegosaurusLayers.create();
    }

    private static void add(ModelPart part, float[] rot) {
        if (rot == null) return;
        part.xRot += rot[0];
        part.yRot += rot[1];
        part.zRot += rot[2];
    }

    @Override
    public void setupAnim(StegosaurusRenderer.State state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos * 0.55F, speed = Math.min(1.0F, state.walkAnimationSpeed * 1.6F);
        float swing = Mth.cos(pos) * 0.45F * speed;
        // diagonal gait: front right with back left, front left with back right; the shin folds a little on the back swing
        this.frontRight.xRot = swing;
        this.backLeft.xRot = swing;
        this.frontLeft.xRot = -swing;
        this.backRight.xRot = -swing;
        this.frontRightShin.xRot += Math.max(0.0F, -swing) * 0.6F;
        this.backLeftShin.xRot += Math.max(0.0F, -swing) * 0.6F;
        this.frontLeftShin.xRot += Math.max(0.0F, swing) * 0.6F;
        this.backRightShin.xRot += Math.max(0.0F, swing) * 0.6F;
        this.body.y += Mth.abs(Mth.sin(pos)) * 0.7F * speed;
        // head: look (shared with the neck), lowered when something stands in front - the same pose the server tested (part boxes)
        float[][] pose = Stegosaurus.RIG.pose(state.yRot, state.xRot, state.headTuck, state.tailBend);
        add(this.neck, pose[Stegosaurus.RIG.neck()]);
        add(this.head, pose[Stegosaurus.RIG.head()]);
        int[] tail = Stegosaurus.RIG.tailBones();
        ModelPart[] tailParts = {this.tail1, this.tail2, this.tail3};
        for (int i = 0; i < tail.length && i < tailParts.length; i++) add(tailParts[i], pose[tail[i]]);
        // tail: a slow sway, and a sideways swipe when a monster hits it
        float sway = (Mth.sin(state.ageInTicks * 0.05F) * 0.05F + Mth.sin(pos) * 0.08F * speed) * (1.0F - Math.min(1.0F, Math.abs(state.tailBend)) * 0.5F);
        float swipe = state.swipe * 0.35F;
        this.tail1.yRot += sway + swipe;
        this.tail2.yRot += sway * 1.2F + swipe * 1.3F;
        this.tail3.yRot += sway * 1.4F + swipe * 1.6F;
        this.saddle.visible = state.saddled;
        boolean armor = state.armorTier > 0;
        this.armorNeck.visible = armor;
        this.armorFlankRight.visible = armor;
        this.armorFlankLeft.visible = armor;
        this.harness.visible = state.plow;
        this.plow.visible = state.plow;
        this.plow.xRot += PLOW_RAISE * state.plowLift;
    }
}
