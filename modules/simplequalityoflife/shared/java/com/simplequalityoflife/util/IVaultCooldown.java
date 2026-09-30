package com.simplequalityoflife.util;

import java.util.Map;
import java.util.UUID;

public interface IVaultCooldown {
    boolean hasLootedRecently(UUID playerUuid, long worldTime);
    void markLooted(UUID playerUuid, long worldTime);
    void removeLootData(UUID playerUuid);
    Map<UUID, Long> getLootTimesMap();
    void setLootTimesMap(Map<UUID, Long> map);
}
