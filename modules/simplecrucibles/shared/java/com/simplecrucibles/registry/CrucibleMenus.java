package com.simplecrucibles.registry;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.crucible.CrucibleMenu;
import com.simplecrucibles.crucible.CrucibleTier;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** One menu type per tier, so the client knows the slot count without extra open data. */
public final class CrucibleMenus {
    public static final Map<CrucibleTier, MenuType<CrucibleMenu>> CRUCIBLES = new EnumMap<>(CrucibleTier.class);

    public static void register() {
        for (CrucibleTier tier : CrucibleTier.values()) {
            CRUCIBLES.put(tier, Registry.register(BuiltInRegistries.MENU, SimpleCrucibles.id(tier.id() + "_crucible"),
                    new MenuType<>((id, inventory) -> CrucibleMenu.client(tier, id, inventory), FeatureFlags.VANILLA_SET)));
        }
    }
    }

    public static MenuType<CrucibleMenu> forTier(CrucibleTier tier) {
        return CRUCIBLES.get(tier);
    }

    private CrucibleMenus() {}
}
