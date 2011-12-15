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

/**
 * EvalError indicates that we cannot continue evaluating the script or the script has thrown an exception.
 *
 * EvalError may be thrown for a script syntax error, an evaluation error such as referring to an undefined variable, an
 * internal error. <p>
 *
 * @see TargetError
 */
public class EvalError extends Exception {

    SimpleNode node;
    // Note: no way to mutate the Throwable message, must maintain our own
    String message;
    CallStack callstack;

    public EvalError(String s, SimpleNode node, CallStack callstack) {
        setMessage(s);
        this.node = node;
        // freeze the callstack for the stack trace.
        if (callstack != null) {
            this.callstack = callstack.copy();
        }
    }

    /**
     * Print the error with line number and stack trace.
     */
    @Override
    public String toString() {
        String trace;
        if (node != null) {
            trace = " : at Line: " + node.getLineNumber()
                    + " : in file: " + node.getSourceFile()
                    + " : " + node.getText();
        } else // Users should not normally see this.
        {
            trace = ": <at unknown location>";
        }

        if (callstack != null) {
            trace = trace + "\n" + getScriptStackTrace();
        }

        return getMessage() + trace;
    }

    /**
     * Re-throw the error, prepending the specified message.
     */
    public void reThrow(String msg)
            throws EvalError {
        prependMessage(msg);
        throw this;
    }

    /**
     * The error has trace info associated with it. i.e. It has an AST node that can print its location and source text.
     */
    SimpleNode getNode() {
        return node;
    }

    void setNode(SimpleNode node) {
        this.node = node;
    }

    public String getErrorText() {
        if (node != null) {
            return node.getText();
        } else {
            return "<unknown error>";
        }
    }

    public int getErrorLineNumber() {
        if (node != null) {
            return node.getLineNumber();
        } else {
            return -1;
        }
    }

    public String getErrorSourceFile() {
        if (node != null) {
            return node.getSourceFile();
        } else {
            return "<unknown file>";
        }
    }

    public String getScriptStackTrace() {
        if (callstack == null) {
            return "<Unknown>";
        }

        String trace = "";
        CallStack stack = callstack.copy();
        while (stack.depth() > 0) {
            NameSpace ns = stack.pop();
            SimpleNode node = ns.getNode();
            if (ns.isMethod) {
                trace = trace + "\nCalled from method: " + ns.getName();
                if (node != null) {
                    trace += " : at Line: " + node.getLineNumber()
                            + " : in file: " + node.getSourceFile()
                            + " : " + node.getText();
                }
            }
        }

        return trace;
    }

    /**
     * @see #toString() for a full display of the information
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String s) {
        message = s;
    }

    /**
     * Prepend the message if it is non-null.
     */
    protected void prependMessage(String s) {
        if (s == null) {
            return;
        }

        if (message == null) {
            message = s;
        } else {
            message = s + " : " + message;
        }
    }
}
