package com.simplebuilding.common;

public final class SimplebuildingStartupPlan {
    private final Runnable configure;
    private final Runnable registerContent;
    private final Runnable registerEvents;
    private final Runnable registerNetworking;
    private final Runnable finalizeBootstrap;

    private SimplebuildingStartupPlan(Builder builder) {
        this.configure = builder.configure;
        this.registerContent = builder.registerContent;
        this.registerEvents = builder.registerEvents;
        this.registerNetworking = builder.registerNetworking;
        this.finalizeBootstrap = builder.finalizeBootstrap;
    }

    public void run() {
        configure.run();
        registerContent.run();
        registerEvents.run();
        registerNetworking.run();
        finalizeBootstrap.run();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Runnable configure = () -> {};
        private Runnable registerContent = () -> {};
        private Runnable registerEvents = () -> {};
        private Runnable registerNetworking = () -> {};
        private Runnable finalizeBootstrap = () -> {};

        private Builder() {
        }

        public Builder configure(Runnable configure) {
            this.configure = configure != null ? configure : () -> {};
            return this;
        }

        public Builder registerContent(Runnable registerContent) {
            this.registerContent = registerContent != null ? registerContent : () -> {};
            return this;
        }

        public Builder registerEvents(Runnable registerEvents) {
            this.registerEvents = registerEvents != null ? registerEvents : () -> {};
            return this;
        }

        public Builder registerNetworking(Runnable registerNetworking) {
            this.registerNetworking = registerNetworking != null ? registerNetworking : () -> {};
            return this;
        }

        public Builder finalizeBootstrap(Runnable finalizeBootstrap) {
            this.finalizeBootstrap = finalizeBootstrap != null ? finalizeBootstrap : () -> {};
            return this;
        }

        public SimplebuildingStartupPlan build() {
            return new SimplebuildingStartupPlan(this);
        }
    }
}