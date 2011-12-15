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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Hashtable;

/**
 * XThis is a dynamically loaded extension which extends This.java and adds support for the generalized interface proxy
 * mechanism introduced in JDK1.3. XThis allows bsh scripted objects to implement arbitrary interfaces (be arbitrary
 * event listener types).
 *
 * Note: This module relies on new features of JDK1.3 and will not compile with JDK1.2 or lower. For those environments
 * simply do not compile this class.
 *
 * Eventually XThis should become simply This, but for backward compatability we will maintain This without requiring
 * support for the proxy mechanism.
 *
 * XThis stands for "eXtended This" (I had to call it something).
 *
 * @see JThis	See also JThis with explicit JFC support for compatability.
 * @see This
 */
public class XThis extends This {

    /**
     * A cache of proxy interface handlers. Currently just one per interface.
     */
    Hashtable interfaces;
    InvocationHandler invocationHandler = new Handler();

    public XThis(NameSpace namespace, Interpreter declaringInterp) {
        super(namespace, declaringInterp);
    }

    public String toString() {
        return "'this' reference (XThis) to Bsh object: " + namespace;
    }

    /**
     * Get dynamic proxy for interface, caching those it creates.
     */
    public Object getInterface(Class clas) {
        return getInterface(new Class[]{clas});
    }

    /**
     * Get dynamic proxy for interface, caching those it creates.
     */
    public Object getInterface(Class[] ca) {
        if (interfaces == null) {
            interfaces = new Hashtable();
        }

        // Make a hash of the interface hashcodes in order to cache them
        int hash = 21;
        for (int i = 0; i < ca.length; i++) {
            hash *= ca[i].hashCode() + 3;
        }
        Object hashKey = new Integer(hash);

        Object interf = interfaces.get(hashKey);

        if (interf == null) {
            ClassLoader classLoader = ca[0].getClassLoader(); // ?
            interf = Proxy.newProxyInstance(
                    classLoader, ca, invocationHandler);
            interfaces.put(hashKey, interf);
        }

        return interf;
    }

    /**
     * This is the invocation handler for the dynamic proxy. <p>
     *
     * Notes: Inner class for the invocation handler seems to shield this unavailable interface from JDK1.2 VM...      *
     * I don't understand this. JThis works just fine even if those classes aren't there (doesn't it?) This class
     * shouldn't be loaded if an XThis isn't instantiated in NameSpace.java, should it?
     */
    class Handler implements InvocationHandler, java.io.Serializable {

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            try {
                return invokeImpl(proxy, method, args);
            } catch (TargetError te) {
                // Unwrap target exception.  If the interface declares that 
                // it throws the ex it will be delivered.  If not it will be 
                // wrapped in an UndeclaredThrowable
                throw te.getTarget();
            } catch (EvalError ee) {
                // Ease debugging...
                // XThis.this refers to the enclosing class instance
                if (Interpreter.DEBUG) {
                    Interpreter.debug("EvalError in scripted interface: "
                            + XThis.this.toString() + ": " + ee);
                }
                throw ee;
            }
        }

        public Object invokeImpl(Object proxy, Method method, Object[] args)
                throws EvalError {
            String methodName = method.getName();
            CallStack callstack = new CallStack(namespace);

            /*
             * If equals() is not explicitly defined we must override the default implemented by the This object
             * protocol for scripted object. To support XThis equals() must test for equality with the generated proxy
             * object, not the scripted bsh This object; otherwise callers from outside in Java will not see a the proxy
             * object as equal to itself.
             */
            BshMethod equalsMethod = null;
            try {
                equalsMethod = namespace.getMethod(
                        "equals", new Class[]{Object.class});
            } catch (UtilEvalError e) {/*
                 * leave null
                 */ }
            if (methodName.equals("equals") && equalsMethod == null) {
                Object obj = args[0];
                return new Boolean(proxy == obj);
            }

            /*
             * If toString() is not explicitly defined override the default to show the proxy interfaces.
             */
            BshMethod toStringMethod = null;
            try {
                toStringMethod =
                        namespace.getMethod("toString", new Class[]{});
            } catch (UtilEvalError e) {/*
                 * leave null
                 */ }

            if (methodName.equals("toString") && toStringMethod == null) {
                Class[] ints = proxy.getClass().getInterfaces();
                // XThis.this refers to the enclosing class instance
                StringBuffer sb = new StringBuffer(
                        XThis.this.toString() + "\nimplements:");
                for (int i = 0; i < ints.length; i++) {
                    sb.append(" " + ints[i].getName()
                            + ((ints.length > 1) ? "," : ""));
                }
                return sb.toString();
            }

            Class[] paramTypes = method.getParameterTypes();
            return Primitive.unwrap(
                    invokeMethod(methodName, Primitive.wrap(args, paramTypes)));
        }
    };
}
