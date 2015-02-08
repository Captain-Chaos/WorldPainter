/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class GardenCategory extends Layer {
    public GardenCategory() {
        super("Category", "Garden category", DataSize.NIBBLE, 80);
    }
    
    /**
     * Unoccupied land
     */
    public static final int CATEGORY_UNOCCUPIED = 0;
    
    /**
     * Roadway
     */
    public static final int CATEGORY_ROAD = 1;
    
    /**
     * A building
     */
    public static final int CATEGORY_BUILDING = 2;
    
    /**
     * A garden, park or field
     */
    public static final int CATEGORY_FIELD = 3;
    
    /**
     * Street furniture such as awnings, lighting, seats, fountains, wells, etc.
     */
    public static final int CATEGORY_STREET_FURNITURE = 4;
    
    /**
     * Water
     */
    public static final int CATEGORY_WATER = 5;
    
    /**
     * Tree
     */
    public static final int CATEGORY_TREE = 6;
    
    public static final GardenCategory INSTANCE = new GardenCategory();

    private static final long serialVersionUID = 1L;
}