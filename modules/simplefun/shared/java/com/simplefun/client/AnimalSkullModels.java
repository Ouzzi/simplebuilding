package com.simplefun.client;

import com.simplefun.heads.AnimalHead;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.object.skull.*;
import net.minecraft.resources.Identifier;

public final class AnimalSkullModels {
  public static Identifier texture(AnimalHead t) {
    return Identifier.withDefaultNamespace("textures/entity/" + t.texture + ".png");
  }

  public static SkullModelBase model(AnimalHead t) {
    var mesh = new MeshDefinition();
    var cube = CubeListBuilder.create().texOffs(0, 0);
    switch (t) {
      case PIG -> cube.addBox(-4, -8, -4, 8, 8, 8).texOffs(16, 16).addBox(-2, -4, -6, 4, 3, 2);
      // Vanilla 26.3 CowModel head (64x64 texture): head, muzzle at UV 1,33 and two horns, moved
      // down onto the skull floor and centred on the block (owner 2026-10-01: the old 64x32 box
      // stretched the texture and lacked the muzzle).
      case COW ->
          cube.addBox(-4, -8, -3, 8, 8, 6)
              .texOffs(1, 33)
              .addBox(-3, -3, -4, 6, 3, 1)
              .texOffs(22, 0)
              .addBox(-5, -9, -2, 1, 3, 1)
              .addBox(4, -9, -2, 1, 3, 1);
      case SHEEP -> cube.addBox(-3, -6, -4, 6, 6, 8);
      case CHICKEN ->
          cube.addBox(-2, -6, -2, 4, 6, 3)
              .texOffs(14, 0)
              .addBox(-2, -4, -4, 4, 2, 2)
              .texOffs(14, 4)
              .addBox(-1, -2, -3, 2, 2, 2);
    }
    mesh.getRoot().addOrReplaceChild("head", cube, PartPose.ZERO);
    return new SkullModel(
        LayerDefinition.create(mesh, 64, t == AnimalHead.PIG || t == AnimalHead.COW ? 64 : 32).bakeRoot());
  }
}
