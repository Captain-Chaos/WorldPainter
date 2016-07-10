package org.pepsoft.worldpainter.biomeschemes;

import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.Material;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Pepijn on 26-6-2016.
 */
public class MC10InterfaceHelper {
    public MC10InterfaceHelper(Class<?> compoundTagClass,
                               Method addTagToCompoundTagMethod,
                               Class<?> listTagClass,
                               Method addTagToListTagMethod,
                               Constructor<?> stringTagConstructor,
                               Method nbtToBlockDataMethod,
                               Method getBlockMethod,
                               Method blockToIdMethod,
                               Method blockToDataMethod) {
        this.compoundTagClass = compoundTagClass;
        this.listTagClass = listTagClass;
        this.stringTagConstructor = stringTagConstructor;
        this.nbtToBlockDataMethod = nbtToBlockDataMethod;
        this.getBlockMethod = getBlockMethod;
        this.blockToIdMethod = blockToIdMethod;
        this.blockToDataMethod = blockToDataMethod;
        this.addTagToCompoundTagMethod = addTagToCompoundTagMethod;
        this.addTagToListTagMethod = addTagToListTagMethod;
    }

    /**
     * Decode block information from the palette in a structure file.
     *
     * @param tag The NBT compound tag from the palette in the structure file.
     * @return The decoded material.
     */
    public Material decodeStructureMaterial(CompoundTag tag) {
        try {
            Object blockData = nbtToBlockDataMethod.invoke(null, convertNBT(tag));
            Object block = getBlockMethod.invoke(blockData);
            return Material.get((Integer) blockToIdMethod.invoke(null, block), (Integer) blockToDataMethod.invoke(block, blockData));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access denied while decoding structure block", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown while decoding structure block", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Instantiation exception thrown while decoding structure block", e);
        }
    }

    private Object convertNBT(Tag tag) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (tag instanceof CompoundTag) {
            return convertNBT((CompoundTag) tag);
        } else if (tag instanceof ListTag) {
            return convertNBT(((ListTag) tag));
        } else if (tag instanceof StringTag) {
            return convertNBT((StringTag) tag);
        } else {
            throw new UnsupportedOperationException("Unsupported tag type " + tag.getClass().getSimpleName() + " encountered");
        }
    }

    private Object convertNBT(CompoundTag compoundTag) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Object mcTag = compoundTagClass.newInstance();
        for (Map.Entry<String, Tag> entry: compoundTag.getValue().entrySet()) {
            addTagToCompoundTagMethod.invoke(mcTag, entry.getKey(), convertNBT(entry.getValue()));
        }
        return mcTag;
    }

    private Object convertNBT(ListTag listTag) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Object mcTag = listTagClass.newInstance();
        for (Tag tag: listTag.getValue()) {
            addTagToListTagMethod.invoke(mcTag, convertNBT(tag));
        }
        return mcTag;
    }

    private Object convertNBT(StringTag stringTag) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return stringTagConstructor.newInstance(stringTag.getValue());
    }

    private final Class<?> compoundTagClass, listTagClass;
    private final Constructor<?> stringTagConstructor;
    private final Method nbtToBlockDataMethod, getBlockMethod, blockToIdMethod, blockToDataMethod,
            addTagToCompoundTagMethod, addTagToListTagMethod;
}