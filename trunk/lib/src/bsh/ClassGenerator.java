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
 *  You should have received a copy of the GNU General Public License along with this program.                         *
 *  If not, see <http://www.gnu.org/licenses/>.                                                                        *
 *                                                                                                                     *
 *  Patrick Niemeyer (pat@pat.net)                                                                                     *
 *  Author of Learning Java, O'Reilly & Associates                                                                     *
 *  http://www.pat.net/~pat/                                                                                           *
 *                                                                                                                     *
 **********************************************************************************************************************/
package bsh;

import bsh.Capabilities.Unavailable;
import java.lang.reflect.InvocationTargetException;

public abstract class ClassGenerator {

    private static ClassGenerator cg;

    public static ClassGenerator getClassGenerator()
            throws UtilEvalError {
        if (cg == null) {
            try {
                Class clas = Class.forName("bsh.ClassGeneratorImpl");
                cg = (ClassGenerator) clas.newInstance();
            } catch (Exception e) {
                throw new Unavailable("ClassGenerator unavailable: " + e);
            }
        }

        return cg;
    }

    /**
     * Parse the BSHBlock for the class definition and generate the class.
     */
    public abstract Class generateClass(
            String name, Modifiers modifiers,
            Class[] interfaces, Class superClass, BSHBlock block,
            boolean isInterface, CallStack callstack, Interpreter interpreter)
            throws EvalError;

    /**
     * Invoke a super.method() style superclass method on an object instance. This is not a normal function of the Java
     * reflection API and is provided by generated class accessor methods.
     */
    public abstract Object invokeSuperclassMethod(
            BshClassManager bcm, Object instance, String methodName, Object[] args)
            throws UtilEvalError, ReflectError, InvocationTargetException;

    /**
     * Change the parent of the class instance namespace. This is currently used for inner class support. Note: This
     * method will likely be removed in the future.
     */
    public abstract void setInstanceNameSpaceParent(
            Object instance, String className, NameSpace parent);
}
