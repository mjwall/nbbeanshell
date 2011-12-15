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
	name [ = initializer ]
	evaluate name and return optional initializer
*/
class BSHVariableDeclarator extends SimpleNode
{
	// The token.image text of the name... never changes.
    public String name;

    BSHVariableDeclarator(int id) { super(id); }

	/**
		Evaluate the optional initializer value.  
		(The name was set at parse time.)

		A variable declarator can be evaluated with or without preceding
		type information. Currently the type info is only used by array 
		initializers in the case where there is no explicitly declared type.

		@param typeNode is the BSHType node.  Its info is passed through to any
		variable intializer children for the case where the array initializer
		does not declare the type explicitly. e.g.
			int [] a = { 1, 2 };
		typeNode may be null to indicate no type information available.
	*/
    public Object eval( 
		BSHType typeNode, CallStack callstack, Interpreter interpreter)  
		throws EvalError
    {
		// null value means no value
		Object value = null;

        if ( jjtGetNumChildren() > 0 ) 
		{
            SimpleNode initializer = (SimpleNode)jjtGetChild(0);

			/*
				If we have type info and the child is an array initializer
				pass it along...  Else use the default eval style.
				(This allows array initializer to handle the problem... 
				allowing for future enhancements in loosening types there).	
			*/
			if ( (typeNode != null) 
				&& initializer instanceof BSHArrayInitializer 
			)
            	value = ((BSHArrayInitializer)initializer).eval( 
					typeNode.getBaseType(), typeNode.getArrayDims(), 
					callstack, interpreter);
			else
				value = initializer.eval( callstack, interpreter);
		}

		if ( value == Primitive.VOID )
            throw new EvalError("Void initializer.", this, callstack );

        return value;
    }

	public String toString() { return "BSHVariableDeclarator "+name; }
}