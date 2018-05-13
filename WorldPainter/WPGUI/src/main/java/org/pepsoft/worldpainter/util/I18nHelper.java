/*
 * Copyright (C) 2014 pepijn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.worldpainter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 *
 * @author pepijn
 */
public class I18nHelper {
    public static String m(String key) {
        return RESOURCE_BUNDLE.getString(key);
    }
    
    public static String m(Enum<?> e) {
        try {
            return ENUM_BUNDLES.get(e.getDeclaringClass()).getString(e.name());
        } catch (NullPointerException ex) {
            ResourceBundle rb = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources." + e.getDeclaringClass().getSimpleName());
            ENUM_BUNDLES.put(e.getClass(), rb);
            return rb.getString(e.name());
        }
    }
    
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.strings");
    private static final Map<Class<? extends Enum>, ResourceBundle> ENUM_BUNDLES = new HashMap<>();
}