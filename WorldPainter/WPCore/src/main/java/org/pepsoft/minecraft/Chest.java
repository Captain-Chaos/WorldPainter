/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.ArrayList;
import java.util.List;
import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class Chest extends TileEntity implements ItemContainer {
    public Chest() {
        super(ID_CHEST);
    }

    public Chest(CompoundTag tag) {
        super(tag);
    }

    @Override
    public List<InventoryItem> getItems() {
        List<Tag> list = getList(TAG_ITEMS);
        if (list != null) {
            List<InventoryItem> items = new ArrayList<InventoryItem>(list.size());
            for (Tag tag: list) {
                items.add(new InventoryItem((CompoundTag) tag));
            }
            return items;
        } else {
            return new ArrayList<InventoryItem>();
        }
    }

    public void setItems(List<InventoryItem> items) {
        List<Tag> list = new ArrayList<Tag>(items.size());
        for (InventoryItem item: items) {
            list.add(item.toNBT());
        }
        setList(TAG_ITEMS, CompoundTag.class, list);
    }

    private static final long serialVersionUID = 1L;
}