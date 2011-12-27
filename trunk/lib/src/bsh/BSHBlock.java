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

class BSHBlock extends SimpleNode {

    public boolean isSynchronized = false;

    BSHBlock(int id) {
        super(id);
    }

    @Override
    public Object eval(CallStack callstack, Interpreter interpreter, DebuggerContext dContext) throws EvalError {
        return eval(callstack, interpreter, false);
    }

    /**
     * @param overrideNamespace if set to true the block will be executed in the current namespace (not a subordinate
     * one). <p> If true *no* new BlockNamespace will be swapped onto the stack and the eval will happen in the current
     * top namespace. This is used by BshMethod, TryStatement, etc. which must intialize the block first and also for
     * those that perform multiple passes in the same block.
     */
    public Object eval(
            CallStack callstack, Interpreter interpreter,
            boolean overrideNamespace)
            throws EvalError {
        Object syncValue = null;
        if(isSynchronized) {
            // First node is the expression on which to sync
            SimpleNode exp = ((SimpleNode) jjtGetChild(0));
            syncValue = exp.eval(callstack, interpreter, null);
        }

        Object ret;
        if(isSynchronized) // Do the actual synchronization
        {
            synchronized (syncValue) {
                ret = evalBlock(
                        callstack, interpreter, overrideNamespace, null/*
                         * filter
                         */);
            }
        } else {
            ret = evalBlock(
                    callstack, interpreter, overrideNamespace, null/*
                     * filter
                     */);
        }

        return ret;
    }

    Object evalBlock(
            CallStack callstack, Interpreter interpreter,
            boolean overrideNamespace, NodeFilter nodeFilter)
            throws EvalError {
        Object ret = Primitive.VOID;
        NameSpace enclosingNameSpace = null;
        if(!overrideNamespace) {
            enclosingNameSpace = callstack.top();
            BlockNameSpace bodyNameSpace =
                    new BlockNameSpace(enclosingNameSpace);

            callstack.swap(bodyNameSpace);
        }

        int startChild = isSynchronized ? 1 : 0;
        int numChildren = jjtGetNumChildren();

        try {
            /*
             * Evaluate block in two passes: First do class declarations then do everything else.
             */
            for(int i = startChild; i < numChildren; i++) {
                SimpleNode node = ((SimpleNode) jjtGetChild(i));

                if(nodeFilter != null && !nodeFilter.isVisible(node)) {
                    continue;
                }

                if(node instanceof BSHClassDeclaration) {
                    node.eval(callstack, interpreter, null);
                }
            }
            for(int i = startChild; i < numChildren; i++) {
                SimpleNode node = ((SimpleNode) jjtGetChild(i));
                if(node instanceof BSHClassDeclaration) {
                    continue;
                }

                // filter nodes
                if(nodeFilter != null && !nodeFilter.isVisible(node)) {
                    continue;
                }

                ret = node.eval(callstack, interpreter, null);

                // statement or embedded block evaluated a return statement
                if(ret instanceof ReturnControl) {
                    break;
                }
            }
        } finally {
            // make sure we put the namespace back when we leave.
            if(!overrideNamespace) {
                callstack.swap(enclosingNameSpace);
            }
        }
        return ret;
    }

    public interface NodeFilter {

        public boolean isVisible(SimpleNode node);
    }
}
