/***********************************************************************************************************************
 *                                                                                                                     *
 *  This file is part of the BeanShell Java Scripting distribution.                                                    *
 *  Documentation and updates may be found at http://www.beanshell.org/                                                *
 *                                                                                                                     *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General  *
 *  Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)  *
 *  any later version.                                                                                                 *
 *                                                                                                                     *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for    *
 *  more details.                                                                                                      *
 *                                                                                                                     *
 *  You should have received a copy of the GNU Lesser General Public License along with this program.                  *
 *  If not, see <http://www.gnu.org/licenses/>.                                                                        *
 *                                                                                                                     *
 *  Patrick Niemeyer (pat@pat.net)                                                                                     *
 *  Author of Learning Java, O'Reilly & Associates                                                                     *
 *  http://www.pat.net/~pat/                                                                                           *
 *                                                                                                                     *
 **********************************************************************************************************************/
package bsh;

import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The default CollectionManager (which remains Java 1.1 compatible) supports iteration over objects of type:
 * Enumeration, Vector, String, StringBuffer and array. The dynamically loaded CollectionManagerImpl supports additional
 * types when it is present.
 *
 * @see BshIterable.java
 */
public class CollectionManager {

    private static CollectionManager manager;

    public synchronized static CollectionManager getCollectionManager() {
        if (manager == null
                && Capabilities.classExists("java.util.Collection")) {
            Class clas;
            try {
                clas = Class.forName("bsh.collection.CollectionManagerImpl");
                manager = (CollectionManager) clas.newInstance();
            } catch (Exception e) {
                Interpreter.debug("unable to load CollectionManagerImpl: " + e);
            }
        }

        if (manager == null) {
            manager = new CollectionManager(); // default impl
        }
        return manager;
    }

    /**
     */
    public boolean isBshIterable(Object obj) {
        // This could be smarter...
        try {
            getBshIterator(obj);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public BshIterator getBshIterator(Object obj)
            throws IllegalArgumentException {
        return new BasicBshIterator(obj);
    }

    public boolean isMap(Object obj) {
        return obj instanceof Hashtable;
    }

    public Object getFromMap(Object map, Object key) {
        return ((Hashtable) map).get(key);
    }

    public Object putInMap(Object map, Object key, Object value) {
        return ((Hashtable) map).put(key, value);
    }

    /**
     * Determine dynamically if the target is an iterator by the presence of a pair of next() and hasNext() methods.
     * public static boolean isIterator() { }
     */
    /**
     * An implementation that works with JDK 1.1
     */
    public static class BasicBshIterator implements BshIterator {

        Enumeration enumeration;

        /**
         * Construct a basic BasicBshIterator
         *
         * @param The object over which we are iterating
         *
         * @throws java.lang.IllegalArgumentException If the argument is not a supported (i.e. iterable) type.
         *
         * @throws java.lang.NullPointerException If the argument is null
         */
        public BasicBshIterator(Object iterateOverMe) {
            enumeration = createEnumeration(iterateOverMe);
        }

        /**
         * Create an enumeration over the given object
         *
         * @param iterateOverMe Object of type Enumeration, Vector, String, StringBuffer or an array
         *
         * @return an enumeration
         *
         * @throws java.lang.IllegalArgumentException If the argument is not a supported (i.e. iterable) type.
         *
         * @throws java.lang.NullPointerException If the argument is null
         */
        protected Enumeration createEnumeration(Object iterateOverMe) {
            if (iterateOverMe == null) {
                throw new NullPointerException("Object arguments passed to "
                        + "the BasicBshIterator constructor cannot be null.");
            }

            if (iterateOverMe instanceof Enumeration) {
                return (Enumeration) iterateOverMe;
            }

            if (iterateOverMe instanceof Vector) {
                return ((Vector) iterateOverMe).elements();
            }

            if (iterateOverMe.getClass().isArray()) {
                final Object array = iterateOverMe;
                return new Enumeration() {

                    int index = 0, length = Array.getLength(array);

                    public Object nextElement() {
                        return Array.get(array, index++);
                    }

                    public boolean hasMoreElements() {
                        return index < length;
                    }
                };
            }

            if (iterateOverMe instanceof String) {
                return createEnumeration(((String) iterateOverMe).toCharArray());
            }

            if (iterateOverMe instanceof StringBuffer) {
                return createEnumeration(
                        iterateOverMe.toString().toCharArray());
            }

            throw new IllegalArgumentException(
                    "Cannot enumerate object of type " + iterateOverMe.getClass());
        }

        /**
         * Fetch the next object in the iteration
         *
         * @return The next object
         */
        public Object next() {
            return enumeration.nextElement();
        }

        /**
         * Returns true if and only if there are more objects available via the
         * <code>next()</code> method
         *
         * @return The next object
         */
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }
    }
}
