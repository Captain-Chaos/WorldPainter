package org.pepsoft.worldpainter.util;

import org.pepsoft.util.AwtUtils;
import org.pepsoft.util.LifoBlockingDeque;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.pepsoft.worldpainter.util.LazyLoadingIconToggleButton.State.*;

/**
 * A {@link JToggleButton} that displays a square icon, which is lazily loaded in the background only after the button
 * becomes visible for the first time.
 */
public class LazyLoadingIconToggleButton extends JToggleButton {
    /**
     * Create a new {@code LazyLoadingIconToggleButton} that will be display an icon of the specified size, supplied by
     * the specified icon supplier.
     *
     * @param size The width and height of the icon in pixels.
     * @param iconSupplier The supplier that will supply the icon. Will be invoked once, on a background thread, after
     *                     the button is displayed for the first time.
     */
    public LazyLoadingIconToggleButton(final int size, final Supplier<Icon> iconSupplier) {
        super(getEmptyIcon(size));
        requireNonNull(iconSupplier);
        this.iconSupplier = iconSupplier;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (state == NOT_LOADED) {
            state = LOADING;
            iconLoader.submit(() -> {
                Icon icon = iconSupplier.get();
                AwtUtils.doOnEventThread(() -> {
                    setIcon(icon);
                    state = LOADED; // Not actually necessary, but left for clarity
                });
            });
        }
        super.paintComponent(g);
    }

    private static Icon getEmptyIcon(int size) {
        return emptyIcons.computeIfAbsent(size, key -> new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) { /* Do nothing */ }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        });
    }

    private final Supplier<Icon> iconSupplier;
    private State state = NOT_LOADED;

    private static final ExecutorService iconLoader = new ThreadPoolExecutor(1, 1, 0, MILLISECONDS, new LifoBlockingDeque<>());
    private static final Map<Integer, Icon> emptyIcons = new HashMap<>();

    enum State { NOT_LOADED, LOADING, LOADED}
}