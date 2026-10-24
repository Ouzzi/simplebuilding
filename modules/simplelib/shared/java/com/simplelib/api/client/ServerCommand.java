package com.simplelib.api.client;

/**
 * Sends a command to the server as a plain command packet, past the loader's client command hook (CMD24): used by
 * client command roots that forward what they do not know ({@link com.simplelib.api.config.ConfigCommands#forwardToServer}).
 * Through {@code ClientPacketListener.sendCommand} Fabric would run the client tree again and loop.
 */
public final class ServerCommand {
    private ServerCommand() {}

    public static void send(String command) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null) connection.send(new net.minecraft.network.protocol.game.ServerboundChatCommandPacket(command));
    }
}
