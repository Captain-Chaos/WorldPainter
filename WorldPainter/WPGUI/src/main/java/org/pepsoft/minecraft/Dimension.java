/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public class Dimension implements Serializable {
    public Dimension(int no, int maxHeight) {
        this.no = no;
        this.maxHeight = maxHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getNo() {
        return no;
    }

    private final int no, maxHeight;
    
    private static final long serialVersionUID = 1L;
}