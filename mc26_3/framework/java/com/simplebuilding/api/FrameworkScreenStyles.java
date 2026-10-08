package com.simplebuilding.api;

import com.simplebuilding.framework.api.ContainerStyleHints;

/** Adapter for the module screen restyling hints; only the 26.3 overlay publishes this service. */
public final class FrameworkScreenStyles implements ModuleScreenStyles.Bridge {
    @Override
    public boolean isRestyled(Object screen) {
        return ContainerStyleHints.isRestyled(screen);
    }
}
