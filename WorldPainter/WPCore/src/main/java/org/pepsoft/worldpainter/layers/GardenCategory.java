/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import java.util.ResourceBundle;

/**
 *
 * @author pepijn
 */
public class GardenCategory extends Layer {
    public GardenCategory() {
        super("Category", "Garden category", DataSize.NIBBLE, 80);
    }
    
    public static String getLabel(ResourceBundle resourceBundle, int category) {
        switch(category) {
            case CATEGORY_BUILDING:
                return resourceBundle.getString("structure.building");
            case CATEGORY_FIELD:
                return resourceBundle.getString("structure.field");
            case CATEGORY_ROAD:
                return resourceBundle.getString("structure.road");
            case CATEGORY_STREET_FURNITURE:
                return resourceBundle.getString("structure.street.furniture");
            case CATEGORY_WATER:
                return resourceBundle.getString("structure.water");
            case CATEGORY_TREE:
                return "tree";
            case CATEGORY_OBJECT:
                return "object";
            default:
                return "unknown";
        }
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

    /**
     * A manually placed custom object
     */
    public static final int CATEGORY_OBJECT = 7;
    
    public static final GardenCategory INSTANCE = new GardenCategory();

    private static final long serialVersionUID = 1L;
}