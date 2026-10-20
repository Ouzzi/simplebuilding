package com.simplemobheads;

import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Test centre stations (framework {@code TestCentreStations}, shown by SimpleBuilding's test centre when it is there): the
 * heads in their groups, so every head can be looked at, placed and worn.
 */
public final class TestCentre {
    public static final List<String> GROUPS = List.of("monster", "nether", "boss", "passive", "water", "mod", "secret");

    private TestCentre() {
    }

    public static void register() {
        for (String group : GROUPS) {
            List<String> items = new ArrayList<>();
            for (HeadDef head : HeadTable.HEADS) {
                if (head.group().equals(group)) {
                    items.add(SimpleMobHeads.MOD_ID + ":" + head.item());
                }
            }
            com.simplebuilding.framework.api.TestCentreStations.register(SimpleMobHeads.MOD_ID, "heads_" + group,
                    "station.simplemobheads.heads_" + group, items);
        }
    }
}
