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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Werner
 */
public class BshScriptInfo extends BshInfoContainer {
 
    protected final List<BshImportInfo> imports = new LinkedList<BshImportInfo>();
    
    public List<BshImportInfo> getImports() {
        return imports;
    }

    public void addImports(Collection<BshImportInfo> importInfos) {
        imports.addAll(importInfos);
    }
    
}
