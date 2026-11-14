package com.simpledinos.client;

import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Utahraptor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Utahraptor (b-utah48): a slim biped with an S-neck, a long low skull, folded feathered arms, the raised sickle claw and a long stiff tail
 * with a feather vane. Runs with long strides, turns the head with the look and raises it when something stands in front (part boxes,
 * {@link Utahraptor#RIG}); at a wall the arms reach forward (the renderer raises the body). Saddle and armor (neck guard, chest plate, shin
 * guards) only when worn. Layout: tools/gen_models.py.
 */
public class UtahraptorModel extends EntityModel<UtahraptorRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleDinos.id("utahraptor"), "main");
    private final ModelPart body, neck, head, jaw, tail1, tail2, tail3, rightThigh, leftThigh, rightShin, leftShin, rightFoot, leftFoot;
    private final ModelPart armRight, armLeft, handRight, handLeft;
    private final ModelPart saddle, armorNeck, armorChest, armorShinRight, armorShinLeft;

    public UtahraptorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tail1 = body.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
        this.armRight = body.getChild("arm_right");
        this.armLeft = body.getChild("arm_left");
        this.handRight = armRight.getChild("hand_right");
        this.handLeft = armLeft.getChild("hand_left");
        this.rightThigh = root.getChild("right_thigh");
        this.leftThigh = root.getChild("left_thigh");
        this.rightShin = rightThigh.getChild("right_shin");
        this.leftShin = leftThigh.getChild("left_shin");
        this.rightFoot = rightShin.getChild("right_foot");
        this.leftFoot = leftShin.getChild("left_foot");
        this.saddle = body.getChild("saddle");
        this.armorNeck = neck.getChild("armor_neck");
        this.armorChest = body.getChild("armor_chest");
        this.armorShinRight = rightShin.getChild("armor_shin_right");
        this.armorShinLeft = leftShin.getChild("armor_shin_left");
    }

    public static LayerDefinition createBodyLayer() {
        return UtahraptorLayers.create();
    }

    private static void add(ModelPart part, float[] rot) {
        if (rot == null) return;
        part.xRot += rot[0];
        part.yRot += rot[1];
        part.zRot += rot[2];
    }

    @Override
    public void setupAnim(UtahraptorRenderer.State state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos * 0.55F, speed = Math.min(1.0F, state.walkAnimationSpeed * 1.6F);
        float swing = Mth.cos(pos) * 0.75F * speed * (1.0F - state.climb * 0.6F);
        this.rightThigh.xRot += swing;
        this.leftThigh.xRot -= swing;
        // the shin folds back while the thigh swings forward, the long foot stays upright
        this.rightShin.xRot += Math.max(0.0F, -swing) * 0.8F;
        this.leftShin.xRot += Math.max(0.0F, swing) * 0.8F;
        this.rightFoot.xRot -= Math.max(0.0F, -swing) * 0.5F;
        this.leftFoot.xRot -= Math.max(0.0F, swing) * 0.5F;
        this.body.y += Mth.abs(Mth.sin(pos)) * 0.8F * speed;
        // head: look (shared with the neck), raised when something stands in front - the same pose the server tested (part boxes)
        float[][] pose = Utahraptor.RIG.pose(state.yRot, state.xRot, state.headTuck, state.tailBend);
        add(this.neck, pose[Utahraptor.RIG.neck()]);
        add(this.head, pose[Utahraptor.RIG.head()]);
        int[] tail = Utahraptor.RIG.tailBones();
        ModelPart[] tailParts = {this.tail1, this.tail2, this.tail3};
        for (int i = 0; i < tail.length && i < tailParts.length; i++) add(tailParts[i], pose[tail[i]]);
        // the jaw opens a little now and then (panting), the stiff tail sways only a little
        this.jaw.xRot += Math.max(0.0F, Mth.sin(state.ageInTicks * 0.07F)) * 0.12F;
        float sway = (Mth.sin(state.ageInTicks * 0.08F) * 0.03F + Mth.sin(pos) * 0.06F * speed) * (1.0F - Math.min(1.0F, Math.abs(state.tailBend)) * 0.5F);
        this.tail1.yRot += sway;
        this.tail2.yRot += sway * 0.8F;
        this.tail3.yRot += sway * 0.6F;
        // at a wall the arms reach forward, the hands grip, the head looks up along the wall
        this.armRight.xRot -= state.climb * 1.1F;
        this.armLeft.xRot -= state.climb * 1.1F;
        this.handRight.xRot += state.climb * 0.6F;
        this.handLeft.xRot += state.climb * 0.6F;
        this.neck.xRot -= state.climb * 0.3F;
        this.saddle.visible = state.saddled;
        boolean armor = state.armorTier > 0;
        this.armorNeck.visible = armor;
        this.armorChest.visible = armor;
        this.armorShinRight.visible = armor;
        this.armorShinLeft.visible = armor;
    }
}
