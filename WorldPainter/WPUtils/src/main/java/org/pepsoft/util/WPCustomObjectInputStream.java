/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A custom object input stream, which can use a specific class loader to load
 * local classes, and which can override the serialVersionUID for specific
 * classes.
 * 
 * @author pepijn
 */
public class WPCustomObjectInputStream extends ObjectInputStream {
    public WPCustomObjectInputStream(InputStream in, ClassLoader classLoader, Class<?>... patchClasses) throws IOException {
        super(in);
        this.classLoader = classLoader;
        this.patchClasses = new HashSet<Class<?>>(Arrays.asList(patchClasses));
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            return Class.forName(desc.getName(), false, classLoader);
        } catch (ClassNotFoundException e) {
            return super.resolveClass(desc);
        }
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();
        Class localClass = resolveClass(resultClassDescriptor);
        if (patchClasses.contains(localClass)) {
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
            if (localClassDescriptor != null) {
                final long localSUID = localClassDescriptor.getSerialVersionUID();
                final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                if (streamSUID != localSUID) {
                    logger.warning("Overriding serialized class version mismatch: local serialVersionUID = " + localSUID + " stream serialVersionUID = " + streamSUID);
                    resultClassDescriptor = localClassDescriptor;
                }
            }
        } else if (localClass == null) {
            logger.warning("No local class for " + resultClassDescriptor.getName());
        }
        return resultClassDescriptor;
    }
    
    private final ClassLoader classLoader;
    private final Set<Class<?>> patchClasses;

    private static final Logger logger = Logger.getLogger(WPCustomObjectInputStream.class.getName());
}