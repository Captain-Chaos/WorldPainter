package org.pepsoft.minecraft;

/**
 * Vertical orientation schemes for materials supported by {@link Material}.
 */
enum VerticalOrientationScheme {
    /**
     * {@code half} property containing {@code top} or {@code bottom}.
     */
    HALF,

    /**
     * {@code up} property containing {@code true} or {@code false}.
     */
    UP,

    /**
     * {@code type} property containing {@code top} or {@code bottom}.
     */
    TYPE,

    /**
     * {@code up} <em>and</em> {@code down} property containing {@code true} or {@code false}.
     */
    UP_DOWN,

    /**
     * {@code vertical_direction} property containing {@code up} or {@code down}.
     */
    VERTICAL_DIRECTION
}