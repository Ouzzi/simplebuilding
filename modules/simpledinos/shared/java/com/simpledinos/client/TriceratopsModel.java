package com.simpledinos.client;

import com.simpledinos.SimpleDinos;
import com.simpledinos.entity.Triceratops;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Triceratops (a-dinorest1): a heavy quadruped with an arched back, the head with frill, brow and nose horns carried low, a short
 * tail. Walks in the diagonal gait of four-legged animals (front right with back left), lowers its head while the rider charges the
 * run and during the run, turns the head with the look and lifts it when something stands in front (part boxes, {@link Triceratops#RIG}).
 * Saddle and armor (frill shield, horn caps, chest plate) are model parts that show only when worn. Layout: tools/gen_models.py.
 */
public class TriceratopsModel extends EntityModel<TriceratopsRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(SimpleDinos.id("triceratops"), "main");
    private final ModelPart body, neck, head, tail1, tail2, tail3;
    private final ModelPart frontRight, frontLeft, backRight, backLeft, frontRightShin, frontLeftShin, backRightShin, backLeftShin;
    private final ModelPart saddle, armorFrill, armorHornRight, armorHornLeft, armorHornNose, armorChest;

    public TriceratopsModel(ModelPart root) {
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
        ModelPart frill = head.getChild("frill");
        this.armorFrill = frill.getChild("armor_frill");
        this.armorHornRight = head.getChild("horn_right").getChild("armor_horn_right");
        this.armorHornLeft = head.getChild("horn_left").getChild("armor_horn_left");
        this.armorHornNose = head.getChild("horn_nose").getChild("armor_horn_nose");
        this.armorChest = body.getChild("armor_chest");
    }

    public static LayerDefinition createBodyLayer() {
        return TriceratopsLayers.create();
    }

    private static void add(ModelPart part, float[] rot) {
        if (rot == null) return;
        part.xRot += rot[0];
        part.yRot += rot[1];
        part.zRot += rot[2];
    }

    @Override
    public void setupAnim(TriceratopsRenderer.State state) {
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
        this.body.y += Mth.abs(Mth.sin(pos)) * 0.8F * speed;
        // head: look (shared with the neck), lifted when something stands in front, the same pose the server tested (part boxes)
        float[][] pose = Triceratops.RIG.pose(state.yRot, state.xRot, state.headTuck, state.tailBend);
        add(this.neck, pose[Triceratops.RIG.neck()]);
        add(this.head, pose[Triceratops.RIG.head()]);
        // charge: the head goes down, horns forward (while the rider holds the jump key, and during the run)
        float lower = Math.max(state.ram, state.charge) * (1.0F - state.headTuck);
        this.neck.xRot += 0.18F * lower;
        this.head.xRot += 0.32F * lower;
        int[] tail = Triceratops.RIG.tailBones();
        ModelPart[] tailParts = {this.tail1, this.tail2, this.tail3};
        for (int i = 0; i < tail.length && i < tailParts.length; i++) add(tailParts[i], pose[tail[i]]);
        float sway = (Mth.sin(state.ageInTicks * 0.06F) * 0.04F + Mth.sin(pos) * 0.1F * speed) * (1.0F - Math.min(1.0F, Math.abs(state.tailBend)) * 0.5F);
        this.tail1.yRot += sway;
        this.tail2.yRot += sway * 1.2F;
        this.tail3.yRot += sway * 1.4F;
        this.saddle.visible = state.saddled;
        boolean armor = state.armorTier > 0;
        this.armorFrill.visible = armor;
        this.armorHornRight.visible = armor;
        this.armorHornLeft.visible = armor;
        this.armorHornNose.visible = armor;
        this.armorChest.visible = armor;
    }
}
