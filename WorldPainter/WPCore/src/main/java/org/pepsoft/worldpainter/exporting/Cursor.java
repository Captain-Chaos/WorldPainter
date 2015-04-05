/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;

import java.awt.*;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.BLK_AIR;

/**
 *
 * @author pepijn
 */
public final class Cursor implements Cloneable {
    public Cursor(MinecraftWorld minecraftWorld, int x, int y, int height, Direction direction) {
        this.minecraftWorld = minecraftWorld;
        this.x = x;
        this.y = y;
        this.height = height;
        this.direction = direction;
    }
    
    public Cursor(MinecraftWorld minecraftWorld, Point location, int height, Direction direction) {
        this.minecraftWorld = minecraftWorld;
        this.x = location.x;
        this.y = location.y;
        this.height = height;
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
    
    public Material getBlock() {
        return minecraftWorld.getMaterialAt(x, y, height);
    }
    
    public void setBlock(Material material) {
        minecraftWorld.setMaterialAt(x, y, height, material);
    }
    
    public boolean isFree() {
        return minecraftWorld.getBlockTypeAt(x, y, height) == BLK_AIR;
    }
    
    public boolean isFreeOrInsubstantial() {
        int blockType = minecraftWorld.getBlockTypeAt(x, y, height);
        return (blockType == BLK_AIR) || BLOCKS[blockType].insubstantial;
    }
    
    public boolean setBlockIfFree(Material material) {
        int existingBlock = minecraftWorld.getBlockTypeAt(x, y, height);
        if ((existingBlock == BLK_AIR) || BLOCKS[existingBlock].insubstantial) {
            minecraftWorld.setMaterialAt(x, y, height, material);
            return true;
        } else {
            return false;
        }
    }
    
    public void setBlockWithDirection(Material material) {
        minecraftWorld.setMaterialAt(x, y, height, material.setDirection(direction));
    }
    
    public boolean setBlockWithDirectionIfFree(Material material) {
        int existingBlock = minecraftWorld.getBlockTypeAt(x, y, height);
        if ((existingBlock == BLK_AIR) || BLOCKS[existingBlock].insubstantial) {
            minecraftWorld.setMaterialAt(x, y, height, material.setDirection(direction));
            return true;
        } else {
            return false;
        }
    }
    
    public Material getBlockInFront() {
        return minecraftWorld.getMaterialAt(x + direction.getDx(), y + direction.getDy(), height);
    }
    
    public Material getBlockBehind() {
        return minecraftWorld.getMaterialAt(x - direction.getDx(), y - direction.getDy(), height);
    }
    
    public Material getBlockToTheLeft() {
        Direction leftDirection = direction.left();
        return minecraftWorld.getMaterialAt(x + leftDirection.getDx(), y + leftDirection.getDy(), height);
    }
    
    public Material getBlockToTheRight() {
        Direction rightDirection = direction.right();
        return minecraftWorld.getMaterialAt(x + rightDirection.getDx(), y + rightDirection.getDy(), height);
    }
    
    public Material getBlockAbove() {
        return minecraftWorld.getMaterialAt(x, y, height + 1);
    }
    
    public Material getBlockBelow() {
        return minecraftWorld.getMaterialAt(x, y, height - 1);
    }
    
    /**
     * @return Moves the cursor forward one block. Returns the cursor as a
     * convenience.
     */
    public Cursor forward() {
        x += direction.getDx();
        y += direction.getDy();
        return this;
    }
    
    /**
     * @return Moves the cursor backward one block, while continuing to face
     * in the same direction. Returns the cursor as a convenience.
     */
    public Cursor backward() {
        x -= direction.getDx();
        y -= direction.getDy();
        return this;
    }

    /**
     * @return Moves the cursor left one block, while continuing to face in the
     * same direction. Returns the cursor as a convenience.
     */
    public Cursor moveLeft() {
        x += direction.left().getDx();
        y += direction.left().getDy();
        return this;
    }
    
    /**
     * @return Moves the cursor right one block, while continuing to face in the
     * same direction. Returns the cursor as a convenience.
     */
    public Cursor moveRight() {
        x += direction.right().getDx();
        y += direction.right().getDy();
        return this;
    }
    
    /**
     * @return Rotates the cursor left 90 degrees. Returns the cursor as a
     * convenience.
     */
    public Cursor turnLeft() {
        direction = direction.left();
        return this;
    }

    /**
     * @return Rotates the cursor right 90 degrees. Returns the cursor as a
     * convenience.
     */
    public Cursor turnRight() {
        direction = direction.right();
        return this;
    }
    
    /**
     * @return Rotates the cursor 180 degrees. Returns the cursor as a
     * convenience.
     */
    public Cursor turnAround() {
        direction = direction.opposite();
        return this;
    }
    
    /**
     * @return Moves the cursor up one block. Returns the cursor as a
     * convenience.
     */
    public Cursor up() {
        height++;
        return this;
    }
    
    /**
     * @return Moves the cursor down one block. Returns the cursor as a
     * convenience.
     */
    public Cursor down() {
        height--;
        return this;
    }
    
    // Object/Cloneable
    
    @Override
    public Cursor clone() {
        try {
            return (Cursor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    
    private final MinecraftWorld minecraftWorld;
    private int x, y, height;
    private Direction direction;
}