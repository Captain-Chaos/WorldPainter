/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

/**
 * The cardinal directions, with methods for manipulating them.
 *
 * @author pepijn
 */
public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public Direction rotate(int steps) {
        int direction = ordinal();
        direction = (direction + steps) & 0x03;
        switch (direction) {
            case 0:
                return NORTH;
            case 1:
                return EAST;
            case 2:
                return SOUTH;
            case 3:
                return WEST;
            default:
                throw new InternalError();
        }
    }

    // TODO move implementations into individual enum constants:

    /**
     * Mirror this direction in an axis indicated by a separated direction.
     *
     * @param axis The direction of the axis in which to mirror this direction.
     * @return This direction mirrored in the specified axis.
     */
    public Direction mirror(Direction axis) {
        if ((((axis == WEST) || (axis == EAST)) && ((this == NORTH) || (this == SOUTH)))
                || (((axis == NORTH) || (axis == SOUTH)) && ((this == WEST) || (this == EAST)))) {
            return opposite();
        } else {
            return this;
        }
    }
    
    public Direction opposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case EAST:
                return WEST;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                throw new InternalError();
        }
    }

    public Direction left() {
        switch (this) {
            case NORTH:
                return WEST;
            case EAST:
                return NORTH;
            case SOUTH:
                return EAST;
            case WEST:
                return SOUTH;
            default:
                throw new InternalError();
        }
    }

    public Direction right() {
        switch (this) {
            case NORTH:
                return EAST;
            case EAST:
                return SOUTH;
            case SOUTH:
                return WEST;
            case WEST:
                return NORTH;
            default:
                throw new InternalError();
        }
    }
    
    public int getDx() {
        switch (this) {
            case NORTH:
                return 0;
            case EAST:
                return 1;
            case SOUTH:
                return 0;
            case WEST:
                return -1;
            default:
                throw new InternalError();
        }
    }

    public int getDy() {
        switch (this) {
            case NORTH:
                return -1;
            case EAST:
                return 0;
            case SOUTH:
                return 1;
            case WEST:
                return 0;
            default:
                throw new InternalError();
        }
    }

    public static Direction parse(String str) {
        return valueOf(str.toUpperCase());
    }
}