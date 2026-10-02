package com.simplebuilding.modules.simpleriding;

import com.simpleriding.Horseshoes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;

/** NeoForge forbids mixin-added synced entity data; the hoof code travels as a synced attachment. */
public final class RidingNeoAttachments {
 private RidingNeoAttachments() {}
 public static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,"simpleriding");
 public static final DeferredHolder<AttachmentType<?>,AttachmentType<Integer>> HORSESHOES=TYPES.register("horseshoes",
  ()->AttachmentType.<Integer>builder(()->0).sync((holder,player)->true,ByteBufCodecs.VAR_INT).build());
 public static void register(net.neoforged.bus.api.IEventBus bus){
  TYPES.register(bus);
  Horseshoes.SYNC=new Horseshoes.Sync(){
   public int get(AbstractHorse horse){return horse.getData(HORSESHOES);}
   public void set(AbstractHorse horse,int value){horse.setData(HORSESHOES,value);}
  };
 }
}
