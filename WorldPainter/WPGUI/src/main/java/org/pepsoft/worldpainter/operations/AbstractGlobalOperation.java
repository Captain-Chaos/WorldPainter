package org.pepsoft.worldpainter.operations;

import javax.swing.*;

public abstract class AbstractGlobalOperation extends AbstractOperation implements GlobalOperation {
    protected AbstractGlobalOperation(String name, String description) {
        super(name, description);
    }

    protected AbstractGlobalOperation(String name, String description, String iconName) {
        super(name, description, iconName);
    }

    @Override
    public final void interrupt() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void activate() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void deactivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final JPanel getOptionsPanel() {
        throw new UnsupportedOperationException();
    }
}