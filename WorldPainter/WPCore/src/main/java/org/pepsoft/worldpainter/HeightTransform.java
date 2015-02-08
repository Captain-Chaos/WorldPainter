/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author pepijn
 */
public class HeightTransform {
    private HeightTransform(int scaleAmount, int translateAmount) {
        scalingFactor = scaleAmount / 100f;
        this.translateAmount = translateAmount;
    }
    
    public boolean isIdentity() {
        return false;
    }
    
    public float transformHeight(float height) {
        return height * scalingFactor + translateAmount;
    }
    
    public int transformHeight(int height) {
        return (int) (height * scalingFactor + translateAmount + 0.5f);
    }
    
    public static HeightTransform get(int scaleAmount, int translateAmount) {
        if ((scaleAmount == 100) && (translateAmount == 0)) {
            return IDENTITY;
        } else {
            return new HeightTransform(scaleAmount, translateAmount);
        }
    }
    
    private final float scalingFactor;
    private final int translateAmount;
    
    public static final HeightTransform IDENTITY = new HeightTransform(100, 0) {
        @Override
        public boolean isIdentity() {
            return true;
        }

        @Override
        public float transformHeight(float height) {
            return height;
        }

        @Override
        public int transformHeight(int height) {
            return height;
        }
    };
}