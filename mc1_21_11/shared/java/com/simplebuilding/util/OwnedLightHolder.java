package com.simplebuilding.util;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Ein ruhender Traeger leuchtender Ruestung (Ruestungsstaender, Gegenstandsrahmen), der sich
 * merkt, welchen Lichtblock er gesetzt hat. Die Position wird mit der Entity gespeichert,
 * damit ein nach dem Neuladen des Chunks abgenommenes Teil sein Licht trotzdem wieder entfernt.
 */
public interface OwnedLightHolder {
    @Nullable BlockPos simplebuilding$getOwnedLight();

    void simplebuilding$setOwnedLight(@Nullable BlockPos pos);
}
