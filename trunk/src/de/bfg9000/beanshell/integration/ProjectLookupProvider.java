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
package de.bfg9000.beanshell.integration;

import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.LookupProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Adds support for the "Run File (Shift-F6)" Action for BeanShell scripts to J2SE projects.
 *
 * @author Thomas Werner
 */
@LookupProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject")
public class ProjectLookupProvider implements LookupProvider {

    @Override
    public Lookup createAdditionalLookup(Lookup lookup) {
        final ActionProvider actionProvider = lookup.lookup(ActionProvider.class);
        if(null != actionProvider)
            return Lookups.fixed(new WrappingActionProvider(actionProvider));
        
        return Lookups.fixed(new StandaloneRunActionProvider(), new StandaloneCompileActionProvider());
    }

    /**
     * This class adds the "run.single" action to the predefined actions of the project. It therefore wrapps the 
     * existing action provider of the project. This setup is the preferred way up to NetBeans version 7.0.
     */
    private static class WrappingActionProvider implements ActionProvider {

        private final ActionProvider wrappedActionProvider;

        public WrappingActionProvider(ActionProvider wrapped) {
            wrappedActionProvider = wrapped;            
        }

        @Override
        public String[] getSupportedActions() {
            return wrappedActionProvider.getSupportedActions();
        }

        @Override
        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
            if(null == context.lookup(BeanShellDataObject.class)) {
                wrappedActionProvider.invokeAction(command, context);
                return;
            }

            if("compile.single".equals(command))
                new Thread(new CompileAction(context.lookup(BeanShellDataObject.class))).start();
            
            if("run.single".equals(command))
                new Thread(new RunAction(context.lookup(BeanShellDataObject.class))).start();
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
            final boolean wrappedResult = wrappedActionProvider.isActionEnabled(command, context);
            return wrappedResult || (null != context.lookup(BeanShellDataObject.class));
        }

    }
    
    /**
     * This class provides the "run.single" action. This very basic setup is the preferred way since NetBeans version 
     * 7.1.
     */
    private static final class StandaloneRunActionProvider implements ActionProvider {

        private final String COMMAND = "run.single";
        
        @Override
        public String[] getSupportedActions() {
            return new String[] {COMMAND};
        }

        @Override
        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
            if((null != context.lookup(BeanShellDataObject.class)) && (COMMAND.equals(command)))
                new Thread(new RunAction(context.lookup(BeanShellDataObject.class))).start();
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
            return null != context.lookup(BeanShellDataObject.class);
        }

    }
    
    /**
     * This class provides the "run.single" action. This very basic setup is the preferred way since NetBeans version 
     * 7.1.
     */
    private static final class StandaloneCompileActionProvider implements ActionProvider {

        private final String COMMAND = "compile.single";
        
        @Override
        public String[] getSupportedActions() {
            return new String[] {COMMAND};
        }

        @Override
        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
            if((null != context.lookup(BeanShellDataObject.class)) && (COMMAND.equals(command)))
                new Thread(new CompileAction(context.lookup(BeanShellDataObject.class))).start();
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
            return null != context.lookup(BeanShellDataObject.class);
        }

    }

}
