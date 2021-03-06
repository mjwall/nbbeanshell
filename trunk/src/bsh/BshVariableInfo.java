/*
 * nbBeanShell -- a integration of BeanShell into the NetBeans IDE
 * Copyright (C) 2012 Thomas Werner
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with this library; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package bsh;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Thomas Werner
 */
public class BshVariableInfo implements BshInfo {
    
    /**
     * Comparator for BshVariableInfo objects.
     */
    public static final class Comparator implements java.util.Comparator<BshVariableInfo> {

        @Override
        public int compare(BshVariableInfo o1, BshVariableInfo o2) {
            return o1.getName().compareTo(o2.getName());
        }
        
    }
    
    private Set<BshModifierInfo> modifiers;
    private String name;
    private String type;
    private int lineNumber;

    public BshVariableInfo() {
        modifiers = new HashSet<BshModifierInfo>();
    }
    
    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public Set<BshModifierInfo> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    public void addModifier(BshModifierInfo modifier) {
        modifiers.add(modifier);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getIconPath() {
        final boolean isStatic = modifiers.contains(BshModifierInfo.Static);
        if(modifiers.contains(BshModifierInfo.Private)) {
            return PRFX +(isStatic ? "variableStPrivate.png" : "variablePrivate.png");
        } else if(modifiers.contains(BshModifierInfo.Protected)) {
            return PRFX +(isStatic ? "variableStProtected.png" : "variableProtected.png");
        } else if(modifiers.contains(BshModifierInfo.Public)) {
            return PRFX +(isStatic ? "variableStPublic.png" : "variablePublic.png");
        } else {
            return PRFX +(isStatic ? "variableStPackage.png" : "variablePackage.png");
        }
    }
    
    @Override
    public String toString() {
        return new StringBuilder(type).append(" : ").append(name).toString();
    }
    
}
