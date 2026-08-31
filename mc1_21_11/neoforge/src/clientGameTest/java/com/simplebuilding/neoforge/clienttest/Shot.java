package com.simplebuilding.neoforge.clienttest;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * One screenshot of the main render target, taken and written asynchronously.
 *
 * <p>{@code Screenshot.takeScreenshot} copies the colour texture into a mapped GPU buffer and only
 * calls back once that copy has actually completed - which is not in the same frame. The step
 * machinery therefore polls {@link #poll()} until the PNG is on disk instead of blocking, which
 * would stall the very render loop that has to finish the copy.
 *
 * <p>Down-port of the 26.2 version. Only the source of the render target differs: 1.21.11 has no
 * {@code GameRenderer.mainRenderTarget()}, the target hangs off {@code Minecraft} itself
 * ({@code public RenderTarget getMainRenderTarget()}, verified in the 1.21.11 bytecode).
 */
final class Shot {

    private final String name;
    private volatile Path result;
    private volatile Throwable error;
    private boolean requested;

    Shot(String name) {
        this.name = name;
    }

    /** @return true once the PNG has been written; call once per tick from the client thread */
    boolean poll() {
        if (error != null) {
            throw new AssertionError("Screenshot '" + name + "' could not be taken", error);
        }

        if (result != null) {
            return true;
        }

        if (!requested) {
            requested = true;
            request();
        }

        return false;
    }

    Path path() {
        if (result == null) {
            throw new IllegalStateException("Screenshot '" + name + "' is not finished yet");
        }

        return result;
    }

    String name() {
        return name;
    }

    private void request() {
        Minecraft client = Minecraft.getInstance();

        Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> {
            try {
                Path directory = client.gameDirectory.toPath().resolve("screenshots");
                Files.createDirectories(directory);
                Path file = directory.resolve(name + ".png");
                image.writeToFile(file);
                result = file;
            } catch (Throwable t) {
                error = t;
            } finally {
                closeQuietly(image);
            }
        });
    }

    private static void closeQuietly(NativeImage image) {
        try {
            image.close();
        } catch (Throwable ignored) {
            // Nothing useful to do while already reporting a result.
        }
    }
}
