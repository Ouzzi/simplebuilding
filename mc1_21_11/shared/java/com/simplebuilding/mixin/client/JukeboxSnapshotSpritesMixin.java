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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Die Jukebox aus 26.4 Snapshot 4 auf den aelteren Linien (Besitzer 21.10.). Der Build kopiert ihre drei Texturen
 * nach {@code simplebuilding:textures/jukebox_264/} und ihr Modell ({@code cube_bottom_top}, eigene Unterseite) nach
 * {@code minecraft:models/block/jukebox.json} (gradle/jukebox-264.gradle, nie eingecheckt). Hier landen die Texturen
 * im Block-Atlas unter den Vanilla-Namen {@code minecraft:block/jukebox_*}:
 * <ul>
 *   <li>Option {@code jukebox264Textures} an und die Jukebox-Seite kommt aus dem Vanilla-Paket: die 26.4-Texturen
 *       (ein Ressourcenpaket, das die Jukebox selbst aendert, gewinnt weiter);</li>
 *   <li>sonst bekommt die Unterseite, falls kein Paket eine liefert, die aktuelle Seite - wie im alten Modell.</li>
 * </ul>
 * Ohne die Dateien (Offline-Build, 26.4-Linie) passiert nichts. Wirkt beim Laden der Ressourcen (F3+T).
 */
@Mixin(DirectoryLister.class)
public abstract class JukeboxSnapshotSpritesMixin {
    @Shadow
    public abstract String sourcePath();

    @Inject(method = "run", at = @At("TAIL"))
    private void simplebuilding$jukebox264(ResourceManager manager, SpriteSource.Output output, CallbackInfo ci) {
        if (!"block".equals(this.sourcePath())) return;
        Optional<Resource> side = manager.getResource(simplebuilding$ours("side"));
        if (side.isEmpty()) return;
        Optional<Resource> current = manager.getResource(Identifier.withDefaultNamespace("textures/block/jukebox_side.png"));
        SimplebuildingConfig config = Simplebuilding.getConfig();
        boolean on = config == null || config.jukebox264Textures;
        if (on && current.isPresent() && "vanilla".equals(current.get().sourcePackId())) {
            for (String face : new String[]{"side", "top", "bottom"}) {
                Optional<Resource> ours = face.equals("side") ? side : manager.getResource(simplebuilding$ours(face));
                ours.ifPresent(r -> output.add(Identifier.withDefaultNamespace("block/jukebox_" + face), r));
            }
        } else if (current.isPresent()
                && manager.getResource(Identifier.withDefaultNamespace("textures/block/jukebox_bottom.png")).isEmpty()) {
            output.add(Identifier.withDefaultNamespace("block/jukebox_bottom"), current.get());
        }
    }

    @Unique
    private static Identifier simplebuilding$ours(String face) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/jukebox_264/jukebox_" + face + ".png");
    }
}
