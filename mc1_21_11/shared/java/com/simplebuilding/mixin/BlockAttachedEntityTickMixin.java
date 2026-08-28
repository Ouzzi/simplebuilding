package com.simplebuilding.mixin;

import com.simplebuilding.util.LockedFrameExtensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockAttachedEntity.class)
public class BlockAttachedEntityTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickBrush(CallbackInfo ci) {
        // Wir prüfen: Ist das Entity ein ItemFrame UND implementiert es unser Interface?
        if ((Object) this instanceof ItemFrame frame && (Object) this instanceof LockedFrameExtensions extensions) {

            // Nur weitermachen, wenn gerade ein Spieler am Bürsten ist
            if (extensions.simplebuilding$getBrushingPlayer() != null) {
                Level world = frame.level();

                if (!world.isClientSide()) {
                    Player player = world.getPlayerByUUID(extensions.simplebuilding$getBrushingPlayer());

                    // --- Validierung ---
                    // Existiert der Spieler? Lebt er? Sneakt er? Hält er Pinsel? Benutzt er ihn?
                    boolean valid = player != null
                            && player.isAlive()
                            && player.isShiftKeyDown()
                            && player.getUseItem().is(Items.BRUSH)
                            && player.isUsingItem();

                    if (valid) {
                        // Prüfen, ob der Spieler noch halbwegs auf das Frame schaut und nah genug ist
                        Vec3 eyePos = player.getEyePosition();
                        Vec3 targetCenter = frame.getBoundingBox().getCenter();
                        double distSq = eyePos.distanceToSqr(targetCenter);
                        Vec3 lookDir = player.getViewVector(1.0F).normalize();
                        Vec3 targetDir = targetCenter.subtract(eyePos).normalize();
                        double dot = lookDir.dot(targetDir);

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
                    if (ticks % 5 == 0 && world instanceof ServerLevel serverWorld) {
                        serverWorld.sendParticles(
                                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()),
                                frame.getX(), frame.getY() + 0.5, frame.getZ(),
                                3, 0.2, 0.2, 0.2, 0.05
                        );
                    }

                    // Sound abspielen (alle 20 Ticks = 1 Sekunde)
                    if (ticks % 20 == 0) {
                        world.playSound(null, frame.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }

                    // --- FERTIG --- (nach 40 Ticks = 2 Sekunden)
                    if (ticks >= 40) {
                        frame.setInvisible(false);

                        world.playSound(null, frame.blockPosition(), SoundEvents.BRUSH_GRAVEL_COMPLETED, SoundSource.PLAYERS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.literal("Item Frame sichtbar gemacht.").withStyle(ChatFormatting.YELLOW), true);

                        // Item Benutzung beim Spieler stoppen
                        player.releaseUsingItem();

                        extensions.simplebuilding$resetBrushing();
                    }
                }
            }
        }
    }
}