/*
 * nbBeanShell -- a integration of BeanShell into the NetBeans IDE
 * Copyright (C) 2011 Thomas Werner
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package bsh;

import java.util.*;

/**
 *
 * @author Thomas Werner
 */
public class BshMethodInfo extends BshInfoContainer {
   
    private Set<BshModifierInfo> modifiers;
    private String name;
    private List<BshParameterInfo> parameters;
    private String returnType;    
    private int lineNumber;    
    private boolean clazz;
    
    public BshMethodInfo() {
        modifiers = new HashSet<BshModifierInfo>();
        parameters = new LinkedList<BshParameterInfo>();
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Set<BshModifierInfo> getModifiers() {
        return modifiers;
    }

    public void addModifiers(Collection<BshModifierInfo> modifiers) {
        this.modifiers.addAll(modifiers);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BshParameterInfo> getParameters() {
        return parameters;
    }

    public void addParameters(Collection<BshParameterInfo> parameters) {
        this.parameters.addAll(parameters);
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public boolean isClass() {
        return clazz;
    }

    public void setClass(boolean clazz) {
        this.clazz = clazz;
    }
        
}
