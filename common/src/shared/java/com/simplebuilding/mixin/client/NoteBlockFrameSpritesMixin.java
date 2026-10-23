package com.simplebuilding.mixin.client;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Der Notenblock mit dem Jukebox-Rahmen rundum wie die Verstaerker (Besitzer 23.10., W23-4). Die Textur
 * {@code simplebuilding:textures/block/note_block_framed.png} wird beim Build aus dem Vanilla-Notenblock abgeleitet
 * (tools/textures/misctex23b_2026_10_23.py, nie eingecheckt) und landet hier unter {@code minecraft:block/note_block}
 * im Block-Atlas - nur mit der Client-Option {@code noteBlockFrameTexture} (Standard aus: Vanilla-Bloecke werden nicht
 * ungefragt geaendert) und nur, wenn der Notenblock aus dem Vanilla-Paket kommt (ein Ressourcenpaket gewinnt).
 * Ohne die Datei (Build ohne Jar) passiert nichts. Wirkt beim Laden der Ressourcen (F3+T).
 */
@Mixin(DirectoryLister.class)
public abstract class NoteBlockFrameSpritesMixin {
    @Shadow
    public abstract String sourcePath();

    @Inject(method = "run", at = @At("TAIL"))
    private void simplebuilding$noteBlockFrame(ResourceManager manager, SpriteSource.Output output, CallbackInfo ci) {
        if (!"block".equals(this.sourcePath())) return;
        SimplebuildingConfig config = Simplebuilding.getConfig();
        if (config == null || !config.noteBlockFrameTexture) return;
        Optional<Resource> ours = manager.getResource(
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/block/note_block_framed.png"));
        Optional<Resource> current = manager.getResource(Identifier.withDefaultNamespace("textures/block/note_block.png"));
        if (ours.isPresent() && current.isPresent() && "vanilla".equals(current.get().sourcePackId())) {
            output.add(Identifier.withDefaultNamespace("block/note_block"), ours.get());
        }
    }
}
