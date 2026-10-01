package com.simplebuilding.modules.simpletweaks.mixin.claims;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(TransportItemsBetweenContainers.class)
public interface ClaimTransportInvoker {
 @Invoker("pickUpItems") void claims$pickup(PathfinderMob body,Container container);
 @Invoker("putDownItem") void claims$putdown(PathfinderMob body,Container container);
}
