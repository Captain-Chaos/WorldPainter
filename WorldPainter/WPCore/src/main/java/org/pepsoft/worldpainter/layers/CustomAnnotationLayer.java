package org.pepsoft.worldpainter.layers;

public class CustomAnnotationLayer extends CustomLayer {
    public CustomAnnotationLayer(String name, String description, Object paint) {
        super(name, description, DataSize.BIT, 65, paint);
    }

    @Override
    public boolean isExport() {
        return false;
    }

    @Override
    public void setExport(boolean export) {
        if (export) {
            throw new IllegalArgumentException();
        }
    }
}