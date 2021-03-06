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
 *  Thomas Werner                                                                                                      *
 *  http://www.xing.com/profile/Thomas_Werner108                                                                       *
 *                                                                                                                     *
 **********************************************************************************************************************/
package bsh;

import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

/**
 * This class uses the bsh.Parser to gather information on the structure of a script. The bsh.Parser works on a 
 * SimpleNode objects - which are package protected. Therefor the ParserConnector is placed in the bsh package, too.
 * 
 * @author Thomas Werner
 */
public class ParserConnector {
    
    public static final String LOOSE_TYPE = "<loose type>";
    
    /**
     * Gathers information on the structure of a script.
     * 
     * @param inputString the script
     * @return a {@code BshScriptInfo} object containing the data that has been gathered
     * @throws ParseException if the script is not valid
     */
    public BshScriptInfo parse(String inputString) throws ParseException {
        return parse(new Parser(new StringReader(inputString)));
    }
    
    /**
     * Gathers information on the structure of a script.
     * 
     * @param inputStream the script
     * @return a {@code BshScriptInfo} object containing the data that has been gathered
     * @throws ParseException if the script is not valid
     */
    public BshScriptInfo parse(InputStream inputStream) throws ParseException {
        return parse(new Parser(inputStream));
    }
    
    /**
     * Performs the actual work using the given parser.
     * 
     * @param parser the parser to be used
     * @return a {@code BshScriptInfo} object containing the data that has been gathered
     * @throws ParseException if the script is not valid
     */
    private BshScriptInfo parse(Parser parser) throws ParseException {
        final BshScriptInfo result = new BshScriptInfo();
        parser.setRetainComments(true);
        try {
            while(!parser.Line()) {
                final SimpleNode node = parser.popNode();
                if(isMethod(node)) {
                    result.addMethods(Collections.singleton(buildMethodInfo(node)));
                } else if(isVariable(node)) {
                    result.addVariables(Collections.singleton(buildVariableInfo(node)));
                } else if(isClass(node))
                    result.addMethods(Collections.singleton(buildClassInfo(node)));
            }
            removeDuplicateVariables(result.getVariables().iterator());
            removeLooselyTypedOuterVariables(result, getVariableNames(new ArrayList<BshVariableInfo>()));
        } catch(Error err) {
            throw new ParseException(err.getMessage());
        }
        return result;
    }    
    
    //----------------------------------------------------------------------------------------------------------------//
    // Class handling                                                                                                 //
    //----------------------------------------------------------------------------------------------------------------//
    
    /**
     * @param node a SimpleNode to be checked
     * @return {@code true} if the given node is a class declaration
     */
    private boolean isClass(SimpleNode node) {
        return node instanceof BSHClassDeclaration;
    }
    
    /**
     * @param node node that declares a class
     * @return a {@code BshMethodInfo} for a class declaration node
     */
    private BshMethodInfo buildClassInfo(SimpleNode node) {
        final BshMethodInfo result = new BshMethodInfo();
        final BSHClassDeclaration clss = (BSHClassDeclaration) node;
        
        result.setLineNumber(node.getLineNumber());
        result.setName(getClassName(clss));
        result.setReturnType("void");
        result.setSuperClass(getSuperClass(clss));
        result.setInterface(clss.isInterface);
        result.setClass(true);       
        result.addInterfaces(getClassInterfaces(clss));
        result.addModifiers(getClassModifiers(clss));
        result.addMethods(getClassInnerMethods(clss));
        result.addVariables(getFields(clss));        
                
        identifyConstructors(result, result.getName());
        return result;
    }
    
    /**
     * @param node a node that declares a class
     * @return a {@code List} of {@code BshMethodInfo} objects describing the inner methods of the given node
     */
    private List<BshMethodInfo> getClassInnerMethods(BSHClassDeclaration node) {
        final List<BshMethodInfo> result = new LinkedList<BshMethodInfo>();
        for(int i=0; i<node.jjtGetNumChildren(); i++) 
            if(node.getChild(i) instanceof BSHBlock) {
                final SimpleNode block = node.getChild(i);
                for(int j=0; j<block.jjtGetNumChildren(); j++) {
                    final SimpleNode child = block.getChild(j);
                    if(isMethod(child))
                        result.add(buildMethodInfo(child));
                }
            }
        return result;
    }
    
    /**
     * @param node a node that declares a class
     * @return the modifiers of the given class node
     */
    private Set<BshModifierInfo> getClassModifiers(BSHClassDeclaration node) {
        final Set<BshModifierInfo> result = new HashSet<BshModifierInfo>();
        final String modifiersAndType = node.getText().substring(0, node.getText().indexOf(getClassName(node)));
        final StringTokenizer tokenizer = new StringTokenizer(modifiersAndType, " ");
        while(tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            for(BshModifierInfo modifier: BshModifierInfo.values())
                if(modifier.toString().equals(token))
                    result.add(modifier);
        }
        return result;
    }
    
    /**
     * @param node a node that declares a class
     * @return the name of the given class node
     */
    private String getClassName(BSHClassDeclaration node) {
        return node.name;        
    }
    
    /**
     * Sets the constructor flag on all contained methods that qualify as constructors
     * 
     * @param container the container that may contain constructors
     * @param className the classname of the container
     */
    private void identifyConstructors(BshInfoContainer container, String className) {
        for(BshMethodInfo method: container.methods)
            method.setConstructor(method.getName().equals(className) && "void".equals(method.getReturnType()));
    }
    
    /**
     * @param node the class declaration
     * @return the defined superclass - or "Object"
     */
    private String getSuperClass(BSHClassDeclaration node) {
        final String declaration = node.getText();
        if(!declaration.contains(" extends "))
            return "Object";
        
        final StringTokenizer tokenizer = new StringTokenizer(declaration);
        while(tokenizer.hasMoreTokens()) {
            if("extends".equals(tokenizer.nextToken()) && tokenizer.hasMoreTokens()) {
                String result = tokenizer.nextToken();
                if(result.endsWith("{"))
                    result = result.substring(0, result.length() -1);
                return result;
            }
        }
            
        return "Object";
    }
     
    /**
     * @param clss the class to be inspected
     * @return the interfaces that are implemented by the given class
     */
    private List<String> getClassInterfaces(BSHClassDeclaration clss) {
        final List<String> result = new LinkedList<String>();
        if(0 < clss.numInterfaces) 
            for(int j=0; j<clss.jjtGetNumChildren(); j++) {
                final SimpleNode child = clss.getChild(j);
                if(child instanceof BSHAmbiguousName)
                    result.add(child.getText().trim());
            }
        
        return result;
    }
    
    //----------------------------------------------------------------------------------------------------------------//
    // Variable handling                                                                                              //
    //----------------------------------------------------------------------------------------------------------------//
    
    /**
     * @param node a SimpleNode to be checked
     * @return {@code true} if the given node is a variable declaration
     */
    private boolean isVariable(SimpleNode node) {
        return (node instanceof BSHTypedVariableDeclaration) ||
               ((node instanceof BSHAssignment) && (node.children.length > 0) && 
                (node.getChild(0).children.length > 0) && (node.getChild(0).getChild(0) instanceof BSHAmbiguousName));
    }
    
    /**
     * @param node node that declares a variable
     * @return a {@code BshVariableInfo} for a variable declaration node
     */
    private BshVariableInfo buildVariableInfo(SimpleNode node) {
        if(node instanceof BSHTypedVariableDeclaration) {
            final BshVariableInfo varInfo = new BshVariableInfo();
            for(BshModifierInfo modifier: BshModifierInfo.values()) {
                final BSHTypedVariableDeclaration varDecl = (BSHTypedVariableDeclaration) node;
                if((null != varDecl.modifiers) && varDecl.modifiers.hasModifier(modifier.toString()))
                    varInfo.addModifier(modifier);
            }
            varInfo.setLineNumber(node.getLineNumber());
            varInfo.setType(((BSHType) node.getChild(0)).getText().trim());
            varInfo.setName(((BSHVariableDeclarator) node.getChild(1)).name.trim());
            return varInfo;
        }
                    
        if((node instanceof BSHAssignment) && (node.children.length > 0) && 
           (node.getChild(0).children.length > 0) && (node.getChild(0).getChild(0) instanceof BSHAmbiguousName)) {
            final BshVariableInfo varInfo = new BshVariableInfo();
            varInfo.setLineNumber(node.getLineNumber());
            varInfo.setType(LOOSE_TYPE);
            varInfo.setName(((BSHAmbiguousName) node.getChild(0).getChild(0)).getText().trim());
            return varInfo;
        }
        
        throw new AssertionError("Not a variable declaration: " +node);
    }
    
    /**
     * The ParserConnector identifies each access to a loosely typed variable as a possible variable declaration. 
     * Therefor this method removes variables that occur more than once within a given collection.
     * @param iterator iterator for a collection of {@code BshVariableInfo} objects
     */
    private void removeDuplicateVariables(Iterator<BshVariableInfo> iterator) {
        final Set<String> variableNames = new HashSet<String>();
        while(iterator.hasNext()) {
            final BshVariableInfo varInfo = iterator.next();
            if(variableNames.contains(varInfo.getName())) {
                iterator.remove();
                continue;
            }
            variableNames.add(varInfo.getName());
        }
    }
    
    /**
     * The ParserConnector identifies each access to a loosely typed variable as a possible variable declaration. 
     * Therefor this method removes variables from the given container that are loosely typed redeclarations of 
     * variables of the outer scope. This combination means, that no new variable is declared - instead the existing 
     * variable from the outer scope will be used.
     * 
     * @see (test) bsh/scripts/cascadedDeclaration.bsh
     * 
     * @param container the container to (possibly) remove variable declarations from
     * @param outerScope the variables that have been declared in the outer scope
     */
    private void removeLooselyTypedOuterVariables(BshInfoContainer container, Collection<String> outerScope) {
        final Iterator<BshVariableInfo> iterator = container.variables.iterator();
        while(iterator.hasNext()) {
            final BshVariableInfo varInfo = iterator.next();
            if(LOOSE_TYPE.equals(varInfo.getType()) && outerScope.contains(varInfo.getName()))
                iterator.remove();
        }
        
        final Set<String> thisScope = new HashSet<String>(outerScope);
        thisScope.addAll(getVariableNames(container.variables));
        
        for(BshInfoContainer innerContainer: container.getMethods())
            removeLooselyTypedOuterVariables(innerContainer, thisScope);
    }
    
    //----------------------------------------------------------------------------------------------------------------//
    // Method handling                                                                                                //
    //----------------------------------------------------------------------------------------------------------------//
    
    /**
     * @param node a SimpleNode to be checked
     * @return {@code true} if the given node is a method declaration
     */
    private boolean isMethod(SimpleNode node) {
        return node instanceof BSHMethodDeclaration;
    }
    
    /**
     * @param node node that declares a method
     * @return a {@code BshMethodInfo} for a method declaration node
     */
    private BshMethodInfo buildMethodInfo(SimpleNode node) {
        final BshMethodInfo result = new BshMethodInfo();
        final BSHMethodDeclaration method = (BSHMethodDeclaration) node;
        
        result.setLineNumber(node.getLineNumber());
        result.setName(getMethodName(method));
        result.setReturnType(getMethodReturnType(method));
        result.setClass(isScriptedClass(node));
        result.addModifiers(getMethodModifiers(method));
        result.addParameters(getMethodFormalParameters(method));
        result.addMethods(getMethodInnerMethods(method));
        result.addVariables(getFields(method));
        
        return result;
    }
    
    /**
     * @param node a node that declares a method
     * @return a {@code List} of {@code BshMethodInfo} objects describing the inner methods of the given node
     */
    private List<BshMethodInfo> getMethodInnerMethods(BSHMethodDeclaration node) {
        final List<BshMethodInfo> result = new LinkedList<BshMethodInfo>();
        for(int i=0; i<node.jjtGetNumChildren(); i++) 
            if(node.getChild(i) instanceof BSHBlock) {
                final SimpleNode block = node.getChild(i);
                for(int j=0; j<block.jjtGetNumChildren(); j++) {
                    final SimpleNode child = block.getChild(j);
                    if(isMethod(child))
                        result.add(buildMethodInfo(child));
                }
            }
        return result;
    }
    
    /**
     * @param node a node that declares a method
     * @return the return type of the given method node
     */
    private String getMethodReturnType(BSHMethodDeclaration node) {
        final String modifiersAndType = node.getText().substring(0, node.getText().indexOf(getMethodName(node)));
        final StringTokenizer tokenizer = new StringTokenizer(modifiersAndType, " ");
        while(tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            
            boolean contained = false;
            for(BshModifierInfo modifier: BshModifierInfo.values())
                if(modifier.toString().equals(token))
                    contained = true;
            if(!contained)
                return token;
        }
        
        for(int i=0; i<node.jjtGetNumChildren(); i++) 
            if(node.getChild(i) instanceof BSHBlock) {
                final SimpleNode block = node.getChild(i);
                for(int j=0; j<block.jjtGetNumChildren(); j++) {
                    final SimpleNode child = block.getChild(j);
                    if(child instanceof BSHReturnStatement) 
                        return LOOSE_TYPE;
                }
            }
        
        return "void";
    }
    
    /**
     * @param node a node that declares a method
     * @return the modifiers of the given method node
     */
    private Set<BshModifierInfo> getMethodModifiers(BSHMethodDeclaration node) {
        final Set<BshModifierInfo> result = new HashSet<BshModifierInfo>();
        final String modifiersAndType = node.getText().substring(0, node.getText().indexOf(getMethodName(node)));
        final StringTokenizer tokenizer = new StringTokenizer(modifiersAndType, " ");
        while(tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            for(BshModifierInfo modifier: BshModifierInfo.values())
                if(modifier.toString().equals(token))
                    result.add(modifier);
        }
        return result;
    }
    
    /**
     * @param node a node that declares a method
     * @return the name of the given method node
     */
    private String getMethodName(BSHMethodDeclaration node) {
        return node.name;        
    }
    
    /**
     * @param node a node that declares a method
     * @return the {@code List} of formal parameters of the given method node
     */
    private List<BshParameterInfo> getMethodFormalParameters(BSHMethodDeclaration node) {        
        final List<BshParameterInfo> result = new LinkedList<BshParameterInfo>();
        final String text = node.getText().substring(node.getText().indexOf("(") +1, node.getText().indexOf(")"));
        final StringTokenizer tokenizer = new StringTokenizer(text, ",");

        while(tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            if(token.isEmpty())
                continue;
            
            final int index = token.indexOf(" ");
            if(-1 == index) {
                result.add(new BshParameterInfo(token, LOOSE_TYPE));
            } else {
                result.add(new BshParameterInfo(token.substring(index +1), token.substring(0, index)));
            }
        }
        
        return result;
    }
 
    /**
     * @param node the node to be checked
     * @return {@code true} if the given node describes a class (a method declaration that returns a self reference)
     */
    private boolean isScriptedClass(SimpleNode node) {
        if(!(node instanceof BSHMethodDeclaration))
            return false;
        
        for(int i=0; i<node.jjtGetNumChildren(); i++) 
            if(node.getChild(i) instanceof BSHBlock) {
                final SimpleNode block = node.getChild(i);
                for(int j=0; j<block.jjtGetNumChildren(); j++) {
                    final SimpleNode child = block.getChild(j);
                    if((child instanceof BSHReturnStatement) && 
                       (block.jjtGetNumChildren() > 0) && 
                       ("this".equals(child.getChild(0).getText().trim()))) 
                        return true;
                }
            }
        
        return false;
    }
    
    //----------------------------------------------------------------------------------------------------------------//
    // Utilities                                                                                                      //
    //----------------------------------------------------------------------------------------------------------------//
    
    /**
     * @param node a node that declares a method
     * @return a {@code List} of {@code BshMethodInfo} objects describing the inner methods of the given node
     */
    private List<BshVariableInfo> getFields(SimpleNode node) {
        final List<BshVariableInfo> result = new LinkedList<BshVariableInfo>();
        for(int i=0; i<node.jjtGetNumChildren(); i++) 
            if(node.getChild(i) instanceof BSHBlock) {
                final SimpleNode block = node.getChild(i);
                for(int j=0; j<block.jjtGetNumChildren(); j++) {
                    final SimpleNode child = block.getChild(j);
                    if(isVariable(child)) 
                        result.add(buildVariableInfo(child));
                }
            }
        removeDuplicateVariables(result.iterator());
        return result;
    }
    
    private Set<String> getVariableNames(Collection<BshVariableInfo> variables) {
        final Set<String> result = new HashSet<String>();
        for(BshVariableInfo varInfo: variables)
            result.add(varInfo.getName());
        return result;
    }
    
}
