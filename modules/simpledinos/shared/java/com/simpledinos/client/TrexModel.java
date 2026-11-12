package com.simpledinos.client;

import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Trex;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Tyrannosaurus rex: walking pose with a heavy body, a leaning neck, big skull with a lower jaw, a swaying tail and two
 * small arms. The saddle and the armor plates are parts of the model that show only when worn. Layout: tools/gen_models.py.
 */
public class TrexModel extends EntityModel<TrexRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleDinos.id("trex"), "main");
    private final ModelPart body, neck, head, jaw, tail1, tail2, tail3, rightThigh, leftThigh, rightShin, leftShin, rightFoot, leftFoot;
    private final ModelPart saddle, armorHead, armorBack;

    public TrexModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tail1 = body.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
        this.rightThigh = root.getChild("right_thigh");
        this.leftThigh = root.getChild("left_thigh");
        this.rightShin = rightThigh.getChild("right_shin");
        this.leftShin = leftThigh.getChild("left_shin");
        this.rightFoot = rightShin.getChild("right_foot");
        this.leftFoot = leftShin.getChild("left_foot");
        this.saddle = body.getChild("saddle");
        this.armorHead = head.getChild("armor_head");
        this.armorBack = body.getChild("armor_back");
    }

    private static void add(ModelPart part, float[] rot) {
        if (rot == null) return;
        part.xRot += rot[0];
        part.yRot += rot[1];
        part.zRot += rot[2];
    }

    public static LayerDefinition createBodyLayer() {
        return TrexLayers.create();
    }

    @Override
    public void setupAnim(TrexRenderer.State state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos * 0.45F, speed = Math.min(1.0F, state.walkAnimationSpeed * 1.6F);
        float swing = Mth.cos(pos) * 0.65F * speed;
        this.rightThigh.xRot = swing;
        this.leftThigh.xRot = -swing;
        // the shin folds back while the thigh swings forward, the foot stays flat
        this.rightShin.xRot += Math.max(0.0F, -swing) * 0.9F;
        this.leftShin.xRot += Math.max(0.0F, swing) * 0.9F;
        this.rightFoot.xRot -= Math.max(0.0F, -swing) * 0.6F;
        this.leftFoot.xRot -= Math.max(0.0F, swing) * 0.6F;
        this.body.y += Mth.abs(Mth.sin(pos)) * 1.2F * speed;
        // head follows the look direction (the neck takes a share) and pulls in when a wall or somebody is in the way, the tail bends
        // away from obstacles: the same pose the server tested with the part boxes (Trex.RIG, a-dino36b)
        float[][] pose = Trex.RIG.pose(state.yRot, state.xRot, state.headTuck, state.tailBend);
        add(this.neck, pose[Trex.RIG.neck()]);
        add(this.head, pose[Trex.RIG.head()]);
        int[] tail = Trex.RIG.tailBones();
        ModelPart[] tailParts = {this.tail1, this.tail2, this.tail3};
        for (int i = 0; i < tail.length && i < tailParts.length; i++) add(tailParts[i], pose[tail[i]]);
        // the jaw hangs a little open and breathes
        this.jaw.xRot += 0.05F + Mth.sin(state.ageInTicks * 0.09F) * 0.03F;
        // the tail sways against the walk (less while it is bent away from a wall)
        float sway = (Mth.sin(state.ageInTicks * 0.05F) * 0.05F + Mth.sin(pos) * 0.12F * speed) * (1.0F - Math.min(1.0F, Math.abs(state.tailBend)) * 0.5F);
        this.tail1.yRot += sway;
        this.tail2.yRot += sway * 1.2F;
        this.tail3.yRot += sway * 1.4F;
        this.saddle.visible = state.saddled;
        this.armorHead.visible = state.armorTier > 0;
        this.armorBack.visible = state.armorTier > 0;
    }
}
