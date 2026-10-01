package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(net.minecraft.world.CompoundContainer.class)
public interface ClaimCompoundAccessor {
 @org.spongepowered.asm.mixin.gen.Accessor("container1") net.minecraft.world.Container claims$first();
 @org.spongepowered.asm.mixin.gen.Accessor("container2") net.minecraft.world.Container claims$second();
}
