package com.simplebuilding.modules.simpleinterfaces;

/**
 * {@code config/simpleinterfaces-server.json}: linked containers and Easy Shulkers / Ender Chests (formerly {@code qOL.*}
 * in Simple QoL); server values, sent to clients. The local screen style stays in {@link ContainersConfig}.
 */
public final class LinkedConfig {
    public static final int DEFAULT_LINKED_RANGE = 64;
    public static final int MIN_LINKED_RANGE = 8;
    public static final int MAX_LINKED_RANGE = 128;
    /** Sneak + right click marks a container; other GUIs show it below. */
    public boolean enableLinkedContainers = true;
    /** Blocks until the mark ends, 8-128. */
    public int linkedContainerRange = DEFAULT_LINKED_RANGE;
    /** Shulker boxes open from the hand or the inventory. */
    public boolean enableEasyShulkers = true;
    /** Ender chests open from the hand or the inventory. */
    public boolean enableEasyEnderChests = true;

    public void normalizeNumbers() {
        linkedContainerRange = Math.clamp(linkedContainerRange, MIN_LINKED_RANGE, MAX_LINKED_RANGE);
    }

    public void normalize() {
        normalizeNumbers();
    }
}
