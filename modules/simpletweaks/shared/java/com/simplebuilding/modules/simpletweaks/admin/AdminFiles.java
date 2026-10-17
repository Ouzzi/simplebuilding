package com.simplebuilding.modules.simpletweaks.admin;

import java.io.IOException;
import java.nio.file.*;

/** Small file helpers: atomic replace, safe folder names. */
public final class AdminFiles {
    public static void atomicWrite(Path file, String text) throws IOException {
        atomicWrite(file, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public static void atomicWrite(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, data);
        try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    /** A dimension id as one folder name: {@code minecraft:the_nether} becomes {@code minecraft_the_nether}. */
    public static String folder(String dimension) {
        var out = new StringBuilder();
        for (char c : dimension.toCharArray()) out.append(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' ? Character.toLowerCase(c) : '_');
        String s = out.toString();
        return s.isEmpty() || s.startsWith(".") ? "_" + s : s;
    }
    private AdminFiles() {}
}
