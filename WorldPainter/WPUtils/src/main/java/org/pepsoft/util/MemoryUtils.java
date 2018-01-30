/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.pepsoft.util.SystemUtils.JAVA_9;
import static org.pepsoft.util.SystemUtils.JAVA_VERSION;

/**
 *
 * @author pepijn
 */
public class MemoryUtils {
    /**
     * Get the memory used by a particular object instance in bytes. To prevent runaway
     *
     * @param object The object of which to determine the memory used.
     * @param stopAt Types of references which should not be followed.
     * @return The number of bytes of RAM used by the object, or -1 if the size
     * could not be determined.
     */
    public static int getSize(Object object, Set<Class<?>> stopAt) {
        if (object == null) {
            return 0;
        } else if (JAVA_VERSION.isAtLeast(JAVA_9)) {
            // TODO: support Java 9
            return -1;
        } else {
            IdentityHashMap<Object, Void> processedObjects = new IdentityHashMap<>();
            return getSize(object, processedObjects, stopAt/*, "root"*/);
        }
    }

    private static int getSize(Object object, IdentityHashMap<Object, Void> processedObjects, Set<Class<?>> stopAt/*, String trail*/) {
        if (processedObjects.containsKey(object)) {
            // This object has already been counted
            return 0;
        } else {
            // Record that this object has been counted
            processedObjects.put(object, null);
            Class<?> type = object.getClass();
            if ((stopAt != null) && (! stopAt.isEmpty())) {
                for (Class<?> stopClass: stopAt) {
                    if (stopClass.isAssignableFrom(type)) {
                        return 0;
                    }
                }
            }
            int objectSize = 8; // Housekeeping
            if (type.isArray()) {
                objectSize += 4; // Array length
                Class<?> arrayType = type.getComponentType();
                if (arrayType.isPrimitive()) {
                    if (arrayType == boolean.class) {
                        objectSize += ((boolean[]) object).length;
                    } else if (arrayType == byte.class) {
                        objectSize += ((byte[]) object).length;
                    } else if (arrayType == char.class) {
                        objectSize += ((char[]) object).length * 2;
                    } else if (arrayType == short.class) {
                        objectSize += ((short[]) object).length * 2;
                    } else if (arrayType == int.class) {
                        objectSize += ((int[]) object).length * 4;
                    } else if (arrayType == float.class) {
                        objectSize += ((float[]) object).length * 4;
                    } else if (arrayType == long.class) {
                        objectSize += ((long[]) object).length * 8;
                    } else {
                        objectSize += ((double[]) object).length * 8;
                    }
                } else {
                    Object[] array = (Object[]) object;
                    objectSize = array.length * 4; // References
                    for (Object anArray : array) {
                        if (anArray != null) {
                            objectSize += getSize(anArray, processedObjects, stopAt/*, trail + '[' + i + ']'*/);
                        }
                    }
                }
            } else if (type.isPrimitive()) {
                objectSize += PRIMITIVE_TYPE_SIZES.get(type);
            } else {
                Class<?> myType = type;
                while (myType != null) {
                    Field[] fields = myType.getDeclaredFields();
                    for (Field field: fields) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            continue;
                        }
                        Class<?> fieldType = field.getType();
                        if (fieldType.isPrimitive()) {
                            objectSize += PRIMITIVE_TYPE_SIZES.get(fieldType);
                        } else {
                            objectSize += 4; // Reference
                            field.setAccessible(true); // Will fail if a security manager is installed!
                            try {
                                Object value = field.get(object);
                                if (value != null) {
                                    objectSize += getSize(value, processedObjects, stopAt/*, trail + '.' + field.getName()*/);
                                }
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Access denied trying to read field " + field.getName() + " of type " + myType.getName(), e);
                            }
                        }
                    }
                    myType = myType.getSuperclass();
                }
            }
            if ((objectSize % 8) != 0) {
                objectSize = ((objectSize >> 3) + 1) << 3;
            }
//            System.out.println(trail + " (" + type.getSimpleName() + "): " + objectSize);
            return objectSize;
        }
    }
    
    private static final Map<Class<?>, Integer> PRIMITIVE_TYPE_SIZES = new HashMap<>();
    
    static {
        PRIMITIVE_TYPE_SIZES.put(boolean.class, 1);
        PRIMITIVE_TYPE_SIZES.put(byte.class,    1);
        PRIMITIVE_TYPE_SIZES.put(char.class,    2);
        PRIMITIVE_TYPE_SIZES.put(short.class,   2);
        PRIMITIVE_TYPE_SIZES.put(int.class,     4);
        PRIMITIVE_TYPE_SIZES.put(float.class,   4);
        PRIMITIVE_TYPE_SIZES.put(long.class,    8);
        PRIMITIVE_TYPE_SIZES.put(double.class,  8);
    }
}