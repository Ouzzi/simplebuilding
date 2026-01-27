package com.simplebuilding.mixin;

import com.simplebuilding.util.LockedFrameExtensions;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockAttachedEntity.class)
public class BlockAttachedEntityTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickBrush(CallbackInfo ci) {
        // Wir prüfen: Ist das Entity ein ItemFrame UND implementiert es unser Interface?
        if ((Object) this instanceof ItemFrameEntity frame && (Object) this instanceof LockedFrameExtensions extensions) {

            // Nur weitermachen, wenn gerade ein Spieler am Bürsten ist
            if (extensions.simplebuilding$getBrushingPlayer() != null) {
                World world = frame.getEntityWorld();

                if (!world.isClient()) {
                    PlayerEntity player = world.getPlayerByUuid(extensions.simplebuilding$getBrushingPlayer());

                    // --- Validierung ---
                    // Existiert der Spieler? Lebt er? Sneakt er? Hält er Pinsel? Benutzt er ihn?
                    boolean valid = player != null
                            && player.isAlive()
                            && player.isSneaking()
                            && player.getActiveItem().isOf(Items.BRUSH)
                            && player.isUsingItem();

                    if (valid) {
                        // Prüfen, ob der Spieler noch halbwegs auf das Frame schaut und nah genug ist
                        Vec3d eyePos = player.getEyePos();
                        Vec3d targetCenter = frame.getBoundingBox().getCenter();
                        double distSq = eyePos.squaredDistanceTo(targetCenter);
                        Vec3d lookDir = player.getRotationVec(1.0F).normalize();
                        Vec3d targetDir = targetCenter.subtract(eyePos).normalize();
                        double dot = lookDir.dotProduct(targetDir);

                        // Wenn weiter weg als 5 Blöcke (25 sq) oder Winkel zu groß -> Abbruch
                        if (distSq > 25.0 || dot < 0.6) {
                            valid = false;
                        }
                    }

                    if (!valid) {
                        // Vorgang abbrechen
                        extensions.simplebuilding$resetBrushing();
                        return;
                    }

                    // --- Prozess läuft weiter ---
                    extensions.simplebuilding$incrementBrushingTicks();
                    int ticks = extensions.simplebuilding$getBrushingTicks();

                    // Partikel spawnen (alle 5 Ticks)
                    if (ticks % 5 == 0 && world instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()),
                                frame.getX(), frame.getY() + 0.5, frame.getZ(),
                                3, 0.2, 0.2, 0.2, 0.05
                        );
                    }

                    // Sound abspielen (alle 20 Ticks = 1 Sekunde)
                    if (ticks % 20 == 0) {
                        world.playSound(null, frame.getBlockPos(), SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }

                    // --- FERTIG --- (nach 40 Ticks = 2 Sekunden)
                    if (ticks >= 40) {
                        frame.setInvisible(false);

                        world.playSound(null, frame.getBlockPos(), SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        player.sendMessage(Text.literal("Item Frame sichtbar gemacht.").formatted(Formatting.YELLOW), true);

                        // Item Benutzung beim Spieler stoppen
                        player.stopUsingItem();

                        extensions.simplebuilding$resetBrushing();
                    }
                }
            }
        }
    }
}