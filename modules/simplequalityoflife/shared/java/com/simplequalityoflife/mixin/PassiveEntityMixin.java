package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AgeableMob.class)
public abstract class PassiveEntityMixin extends LivingEntity {

    protected PassiveEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow public abstract int getAge();
    @Shadow public abstract void setAge(int age);

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        if (this.level().isClientSide()) return;

        if (this.tickCount % 100 != 0) return;
        if (!this.hasCustomName()) return;

        Component customName = this.getCustomName();
        if (customName == null) return;

        String name = customName.getString();
        List<String> suffixes = Simplequalityoflife.getConfig().qOL.nametagBabySuffixes;

        // Prüfen, ob der Name mit einem der Suffixe endet
        for (String suffix : suffixes) {
            // null/leer überspringen: name.endsWith(null) wirft NPE, endsWith("") wäre immer true
            if (suffix == null || suffix.isEmpty()) continue;
            if (name.endsWith(suffix)) {
                // Auf Baby-Alter (-24000) halten. WICHTIG: NICHT auf "ist gerade ein Baby" beschränken –
                // sonst kann ein Tier, das zwischen zwei Checks (alle 100 Ticks) erwachsen wird, dauerhaft
                // entkommen. So wird es notfalls wieder auf Baby zurückgesetzt.
                if (this.getAge() > -24000) {
                    this.setAge(-24000);
                }
                return;
            }
        }
    }
}
