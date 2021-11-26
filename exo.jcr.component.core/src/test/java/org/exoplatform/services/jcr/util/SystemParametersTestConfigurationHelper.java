/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.util;

import org.exoplatform.services.jcr.config.PropertiesParser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: SystemParametersTestConfigurationHelper.java 17.07.2012 dkuleshov $
 *
 */
public class SystemParametersTestConfigurationHelper
{
   public static List<String> generateForcedAndDefaultPropertiesPrefixes(String repositoryName,
      String workspacesUniqueName)
   {
      List<String> systemPropertyPrefixes = new ArrayList<String>();

      prepareForcedPropertyPrefixes(repositoryName, workspacesUniqueName, systemPropertyPrefixes);
      prepareDefaultPropertyPrefixes(repositoryName, workspacesUniqueName, systemPropertyPrefixes);

      return systemPropertyPrefixes;
   }

   public static List<String> generateDefaultPropertiesPrefixes(String repositoryName, String workspacesUniqueName)
   {
      List<String> systemPropertyPrefixes = new ArrayList<String>();

      prepareDefaultPropertyPrefixes(repositoryName, workspacesUniqueName, systemPropertyPrefixes);

      return systemPropertyPrefixes;
   }

   private static void prepareDefaultPropertyPrefixes(String repositoryName, String workspacesUniqueName,
      List<String> systemPropertyPrefixes)
   {
      systemPropertyPrefixes.add(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.DEFAULT_TYPE
         + PropertiesParser.WORKSPACE_SCOPE + workspacesUniqueName + "." + "cache" + ".");
      systemPropertyPrefixes.add(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.DEFAULT_TYPE
         + PropertiesParser.REPOSITORY_SCOPE + repositoryName + "." + "cache" + ".");
      systemPropertyPrefixes.add(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.DEFAULT_TYPE
         + PropertiesParser.ALL_SCOPE + "cache" + ".");
   }

   private static void prepareForcedPropertyPrefixes(String repositoryName, String workspacesUniqueName,
      List<String> systemPropertyPrefixes)
   {
      systemPropertyPrefixes.add(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE
         + PropertiesParser.WORKSPACE_SCOPE + workspacesUniqueName + "." + "cache" + ".");
      systemPropertyPrefixes.add(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE
         + PropertiesParser.REPOSITORY_SCOPE + repositoryName + "." + "cache" + ".");
      systemPropertyPrefixes.add(PropertiesParser.EXO_JCR_CONFIG + PropertiesParser.FORCE_TYPE
         + PropertiesParser.ALL_SCOPE + "cache" + ".");
   }

   public static List<String> generateForcedAndDefaultProperties(List<String> systemPropertyPrefixes,
      String currentIterationParameterName)
   {
      List<String> systemProperties = new ArrayList<String>(systemPropertyPrefixes.size());

      for (String parameterPrefix : systemPropertyPrefixes)
      {
         systemProperties.add(parameterPrefix + currentIterationParameterName);
      }

      return systemProperties;
   }
}
