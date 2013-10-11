/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.config;

import org.exoplatform.commons.utils.SecurityHelper;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Provides means to parse workspace components parameter values and names from system properties.
 * Properties have the following priority:
 *   <br>Forced properties:
 *   <br>&nbsp; * workspace scope
 *   <br>&nbsp; * repository scope
 *   <br>&nbsp; * all scope    
 *   <br>Default properties:
 *   <br>&nbsp; * workspace scope
 *   <br>&nbsp; * repository scope
 *   <br>&nbsp; * all scope
 *       
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: SystemPropertiesParser.java 02.07.2012 dkuleshov $
 *
 */
public class PropertiesParser
{
   public static final String EXO_JCR_CONFIG = "exo.jcr.config.";

   public static final String DEFAULT_TYPE = "default.";

   public static final String FORCE_TYPE = "force.";

   public static final String WORKSPACE_SCOPE = "workspace.";

   public static final String REPOSITORY_SCOPE = "repository.";

   public static final String ALL_SCOPE = "all.";

   protected final WorkspaceEntry workspaceEntry;

   private List<String> forceParameterPrefixes;

   private List<String> defaultParameterPrefixes;

   private final String componentName;

   PropertiesParser(WorkspaceEntry workspaceEntry, String componentName)
   {
      this.workspaceEntry = workspaceEntry;
      this.componentName = componentName;
   }

   public String getForcedParameterValue(String parameterShortName, Map<String, String> parameters)
   {
      List<String> longNames = generateForceParameterLongNames(parameterShortName);
      return chooseAppropriateParameterValue(longNames, parameters);
   }

   public String getDefaultParameterValue(String parameterShortName, Map<String, String> parameters)
   {
      List<String> longNames = generateDefaultParameterLongNames(parameterShortName);
      return chooseAppropriateParameterValue(longNames, parameters);
   }
   
   /**
    * Provides a set of workspace components parameters short names (without long prefixes 
    * e.g. "exo.jcr.config.force.all") defined (no matter how many times)
    * via system properties. Set is ok to be used because it is assumed that there should be
    * no naming collisions between different components' parameter names. 
    */
   public Set<String> getParameterNames(Set<String> allParameterNames)
   {
      Set<String> names = new HashSet<String>();

      for (String propertyName : allParameterNames)
      {
         int index = propertyNameMatchIndex(propertyName);
         if (index > 0)
         {
            names.add(propertyName.substring(index));
         }
      }

      return names;
   }

   private void prepareForceParameterPrefixes()
   {
      forceParameterPrefixes = new ArrayList<String>();

      forceParameterPrefixes.add(generateForceParameterPrefix(WORKSPACE_SCOPE));
      forceParameterPrefixes.add(generateForceParameterPrefix(REPOSITORY_SCOPE));
      forceParameterPrefixes.add(generateForceParameterPrefix(ALL_SCOPE));
   }

   private void prepareDefaultParameterPrefixes()
   {
      defaultParameterPrefixes = new ArrayList<String>();

      defaultParameterPrefixes.add(generateDafaultParameterPrefix(WORKSPACE_SCOPE));
      defaultParameterPrefixes.add(generateDafaultParameterPrefix(REPOSITORY_SCOPE));
      defaultParameterPrefixes.add(generateDafaultParameterPrefix(ALL_SCOPE));
   }

   private String generateForceParameterPrefix(String scope)
   {
      return generateParameterPrefix(FORCE_TYPE, scope);
   }

   private String generateDafaultParameterPrefix(String scope)
   {
      return generateParameterPrefix(DEFAULT_TYPE, scope);
   }

   private String generateParameterPrefix(String type, String scope)
   {
      StringBuilder sb = new StringBuilder(EXO_JCR_CONFIG);
      sb.append(type);
      sb.append(scope);

      if (REPOSITORY_SCOPE.equals(scope))
      {
         sb.append(generateRepositoryName());
         sb.append(".");
      }

      if (WORKSPACE_SCOPE.equals(scope))
      {
         sb.append(workspaceEntry.getUniqueName());
         sb.append(".");
      }

      sb.append(componentName + ".");

      return sb.toString();
   }

   private List<String> generateForceParameterLongNames(String parameterShortName)
   {
      prepareForceParameterPrefixes();
      return generateParameterLongNames(parameterShortName, forceParameterPrefixes);
   }

   private List<String> generateDefaultParameterLongNames(String parameterShortName)
   {
      prepareDefaultParameterPrefixes();
      return generateParameterLongNames(parameterShortName, defaultParameterPrefixes);
   }

   private List<String> generateParameterLongNames(String parameterShortName, List<String> parameterPrefixes)
   {
      List<String> forceParameterLongNames = new ArrayList<String>();
      for (String parameterPrefix : parameterPrefixes)
      {
         forceParameterLongNames.add(parameterPrefix + parameterShortName);
      }
      return forceParameterLongNames;
   }
   private String chooseAppropriateParameterValue(List<String> parameterNames, Map<String, String> parameters)
   {
      String parameterValue;

      for (String parameterName : parameterNames)
      {
         parameterValue = parameters.get(parameterName);
         if (parameterValue != null)
         {
            return parameterValue;
         }
      }

      return null;
   }

   private int propertyNameMatchIndex(String propertyFullName)
   {
      prepareForceParameterPrefixes();
      for (String prefix : forceParameterPrefixes)
      {
         if (propertyFullName.startsWith(prefix))
         {
            return prefix.length();
        }
      }

      prepareDefaultParameterPrefixes();
      for (String prefix : defaultParameterPrefixes)
      {
         if (propertyFullName.startsWith(prefix))
        {
            return prefix.length();
         }
      }

      return -1;
   }

   static public Properties getSystemProperties()
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Properties>()
      {
         public Properties run()
         {
            return System.getProperties();
         }
      });
   }

   private String generateRepositoryName()
   {
      String workspaceUniqueName = workspaceEntry.getUniqueName();
      int workspaceNameIndex = workspaceUniqueName.indexOf(workspaceEntry.getName());

      return workspaceUniqueName.substring(0, workspaceNameIndex - 1);
   }
}