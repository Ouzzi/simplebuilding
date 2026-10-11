package com.simplebuilding.modules.simpleloot;

/** {@code config/simpleloot.json} (formerly {@code qOL.*} in Simple QoL). */
public final class LootConfig {
    public static final int DEFAULT_VAULT_COOLDOWN_DAYS = 100;
    /** Server: a vault rewards a player again once the cooldown has passed (off = Vanilla, once per player). */
    public boolean enableVaultCooldown = true;
    /** Minecraft days until the same player can loot a vault again, 1-36500. */
    public int vaultCooldownDays = DEFAULT_VAULT_COOLDOWN_DAYS;

    public void normalizeNumbers() {
        vaultCooldownDays = Math.clamp(vaultCooldownDays, 1, 36500);
    }

    public void normalize() {
        normalizeNumbers();
    }
}
