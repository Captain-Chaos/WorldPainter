/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

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
    public static long getSize(Object object, Set<Class<?>> stopAt) {
        if (object == null) {
            return 0L;
        } else if (JAVA_VERSION.isAtLeast(JAVA_9)) {
            // TODO: support Java 9
            return -1L;
        } else {
            IdentityHashMap<Object, Void> processedObjects = new IdentityHashMap<>();
            return getSize(object, processedObjects, stopAt/*, "root"*/);
        }
    }

    private static long getSize(Object object, IdentityHashMap<Object, Void> processedObjects, Set<Class<?>> stopAt/*, String trail*/) {
        if (processedObjects.containsKey(object)) {
            // This object has already been counted
            return 0L;
        } else {
            // Record that this object has been counted
            processedObjects.put(object, null);
            Class<?> type = object.getClass();
            if ((stopAt != null) && (! stopAt.isEmpty())) {
                for (Class<?> stopClass: stopAt) {
                    if (stopClass.isAssignableFrom(type)) {
                        return 0L;
                    }
                }
            }
            long objectSize = 8L; // Housekeeping
            if (type.isArray()) {
                objectSize += 4L; // Array length
                Class<?> arrayType = type.getComponentType();
                if (arrayType.isPrimitive()) {
                    if (arrayType == boolean.class) {
                        objectSize += ((boolean[]) object).length;
                    } else if (arrayType == byte.class) {
                        objectSize += ((byte[]) object).length;
                    } else if (arrayType == char.class) {
                        objectSize += ((char[]) object).length * 2L;
                    } else if (arrayType == short.class) {
                        objectSize += ((short[]) object).length * 2L;
                    } else if (arrayType == int.class) {
                        objectSize += ((int[]) object).length * 4L;
                    } else if (arrayType == float.class) {
                        objectSize += ((float[]) object).length * 4L;
                    } else if (arrayType == long.class) {
                        objectSize += ((long[]) object).length * 8L;
                    } else {
                        objectSize += ((double[]) object).length * 8L;
                    }
                } else {
                    Object[] array = (Object[]) object;
                    objectSize = array.length * 4L; // References
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
                            objectSize += 4L; // Reference
                            field.setAccessible(true); // Will fail if a security manager is installed!
                            try {
                                Object value = field.get(object);
                                if (value != null) {
                                    objectSize += getSize(value, processedObjects, stopAt/*, trail + '.' + field.getName()*/);
                                }
                            } catch (IllegalAccessException e) {
                                throw new MDCCapturingRuntimeException("Access denied trying to read field " + field.getName() + " of type " + myType.getName(), e);
                            }
                        }
                    }
                    myType = myType.getSuperclass();
                }
            }
            if ((objectSize % 8L) != 0L) {
                objectSize = ((objectSize >> 3) + 1L) << 3;
            }
//            System.out.println(trail + " (" + type.getSimpleName() + "): " + objectSize);
            return objectSize;
        }
    }
    
    private static final Map<Class<?>, Long> PRIMITIVE_TYPE_SIZES = new HashMap<>();
    
    static {
        PRIMITIVE_TYPE_SIZES.put(boolean.class, 1L);
        PRIMITIVE_TYPE_SIZES.put(byte.class,    1L);
        PRIMITIVE_TYPE_SIZES.put(char.class,    2L);
        PRIMITIVE_TYPE_SIZES.put(short.class,   2L);
        PRIMITIVE_TYPE_SIZES.put(int.class,     4L);
        PRIMITIVE_TYPE_SIZES.put(float.class,   4L);
        PRIMITIVE_TYPE_SIZES.put(long.class,    8L);
        PRIMITIVE_TYPE_SIZES.put(double.class,  8L);
    }
}