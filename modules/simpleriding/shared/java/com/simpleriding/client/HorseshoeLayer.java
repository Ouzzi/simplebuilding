package com.simpleriding.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpleriding.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the worn horseshoes on adult horse-like models. One leg model per hoof (same mesh and
 * animation as Vanilla, slightly inflated) so each hoof can carry its own tier texture.
 */
public final class HorseshoeLayer<S extends EquineRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
 private static final Identifier[] TEXTURES=new Identifier[Horseshoes.Tier.values().length];
 static { for(var t:Horseshoes.Tier.values())TEXTURES[t.ordinal()]=Riding.id("textures/entity/horseshoe/"+t.material+".png"); }
 private final LegModel[] legs=new LegModel[Horseshoes.SLOTS];

 public HorseshoeLayer(RenderLayerParent<S, M> parent,float scale){
  super(parent);
  var definition=LayerDefinition.create(AbstractEquineModel.createBodyMesh(new CubeDeformation(0.02F)),64,64);
  if(scale!=1F)definition=definition.apply(MeshTransformer.scaling(scale));
  for(int i=0;i<legs.length;i++)legs[i]=new LegModel(definition.bakeRoot(),i);
 }

 @Override
 public void submit(PoseStack poseStack,SubmitNodeCollector collector,int light,S state,float yRot,float xRot){
  if(state.isBaby||state.isInvisible||!(state instanceof HorseshoeState shod))return;
  int code=shod.simpleriding$shoes();
  if(code==0)return;
  for(int i=0;i<legs.length;i++){
   int tier=Horseshoes.tierAt(code,i);
   if(tier>0&&tier<=TEXTURES.length)renderColoredCutoutModel(legs[i],TEXTURES[tier-1],poseStack,collector,light,state,-1,1);
  }
 }

 /** Vanilla equine mesh with everything hidden except one leg (slot order front left/right, hind left/right). */
 static final class LegModel extends AbstractEquineModel<EquineRenderState> {
  LegModel(ModelPart root,int slot){
   super(root);
   body.visible=false; headParts.visible=false;
   leftFrontLeg.visible=slot==0; rightFrontLeg.visible=slot==1; leftHindLeg.visible=slot==2; rightHindLeg.visible=slot==3;
  }
 }
}
