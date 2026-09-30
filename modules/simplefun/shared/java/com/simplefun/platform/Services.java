package com.simplefun.platform;

import com.simplefun.Constants;
import com.simplefun.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Locates platform-specific implementations of common-side service interfaces at runtime
 * via Java's {@link ServiceLoader}. The implementation is selected per loader through the
 * META-INF/services file present in each loader module.
 */
public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
