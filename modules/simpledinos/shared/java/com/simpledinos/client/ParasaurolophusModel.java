package com.simpledinos.client;

import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Parasaurolophus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Parasaurolophus (a-dinorest3): a quadruped hadrosaur with a long raised neck, a duck bill and a tubular crest sweeping back. Walks in the
 * diagonal gait, turns the head with the look and raises it when something stands in front (part boxes, {@link Parasaurolophus#RIG}); the
 * call raises the head and opens the bill. Saddle and armor (crest hood, hip blanket) only when worn. Layout: tools/gen_models.py.
 */
public class ParasaurolophusModel extends EntityModel<ParasaurolophusRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleDinos.id("parasaurolophus"), "main");
    /** The call pose: neck and head turn up by these (radians), the jaw opens; the crest stays clear of the rider. */
    public static final float CALL_NECK = -0.2F, CALL_HEAD = -0.15F, CALL_JAW = 0.45F;
    private final ModelPart body, neck, head, jaw, tail1, tail2, tail3;
    private final ModelPart frontRight, frontLeft, backRight, backLeft, frontRightShin, frontLeftShin, backRightShin, backLeftShin;
    private final ModelPart saddle, armorCrest, armorCrestMid, armorBack;

    public ParasaurolophusModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        ModelPart crest = head.getChild("crest");
        ModelPart crestMid = crest.getChild("crest_mid");
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
        this.armorCrest = crest.getChild("armor_crest");
        this.armorCrestMid = crestMid.getChild("armor_crest_mid");
        this.armorBack = body.getChild("armor_back");
    }

    public static LayerDefinition createBodyLayer() {
        return ParasaurolophusLayers.create();
    }

    private static void add(ModelPart part, float[] rot) {
        if (rot == null) return;
        part.xRot += rot[0];
        part.yRot += rot[1];
        part.zRot += rot[2];
    }

    @Override
    public void setupAnim(ParasaurolophusRenderer.State state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos * 0.6F, speed = Math.min(1.0F, state.walkAnimationSpeed * 1.6F);
        float swing = Mth.cos(pos) * 0.5F * speed;
        // diagonal gait: front right with back left, front left with back right; the shin folds a little on the back swing
        this.frontRight.xRot = swing;
        this.backLeft.xRot = swing;
        this.frontLeft.xRot = -swing;
        this.backRight.xRot = -swing;
        this.frontRightShin.xRot += Math.max(0.0F, -swing) * 0.6F;
        this.backLeftShin.xRot += Math.max(0.0F, -swing) * 0.6F;
        this.frontLeftShin.xRot += Math.max(0.0F, swing) * 0.6F;
        this.backRightShin.xRot += Math.max(0.0F, swing) * 0.6F;
        this.body.y += Mth.abs(Mth.sin(pos)) * 0.6F * speed;
        // head: look (shared with the neck), raised when something stands in front - the same pose the server tested (part boxes)
        float[][] pose = Parasaurolophus.RIG.pose(state.yRot, state.xRot, state.headTuck, state.tailBend);
        add(this.neck, pose[Parasaurolophus.RIG.neck()]);
        add(this.head, pose[Parasaurolophus.RIG.head()]);
        int[] tail = Parasaurolophus.RIG.tailBones();
        ModelPart[] tailParts = {this.tail1, this.tail2, this.tail3};
        for (int i = 0; i < tail.length && i < tailParts.length; i++) add(tailParts[i], pose[tail[i]]);
        // the call: head up, bill open (drawn only; the part boxes keep the tested pose)
        this.neck.xRot += CALL_NECK * state.call;
        this.head.xRot += CALL_HEAD * state.call;
        this.jaw.xRot += CALL_JAW * state.call;
        // the neck bobs a little with the gait, the deep tail sways
        this.neck.xRot += Mth.sin(pos * 2.0F) * 0.03F * speed;
        float sway = (Mth.sin(state.ageInTicks * 0.06F) * 0.05F + Mth.sin(pos) * 0.07F * speed) * (1.0F - Math.min(1.0F, Math.abs(state.tailBend)) * 0.5F);
        this.tail1.yRot += sway;
        this.tail2.yRot += sway * 1.2F;
        this.tail3.yRot += sway * 1.4F;
        this.saddle.visible = state.saddled;
        boolean armor = state.armorTier > 0;
        this.armorCrest.visible = armor;
        this.armorCrestMid.visible = armor;
        this.armorBack.visible = armor;
    }
}
