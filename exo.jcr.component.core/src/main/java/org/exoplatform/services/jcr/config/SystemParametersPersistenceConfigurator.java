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

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: SystemParametersPersistenceConfigurator.java 16.07.2012 dkuleshov $
 *
 */
public class SystemParametersPersistenceConfigurator
{
   public static final String UNMODIFIABLE = "unmodifiable";

   public static final String BEFORE_INITIALIZE = "before-initialize";

   public static final String FILE_PATH_PARAMETER = "file-path";

   public static final String FILE_NAME = "overridden-parameters.dat";

   public static final String SEPARATOR = "=";

   private Set<String> beforeInitialize = new HashSet<String>();

   private Set<String> unmodifiable = new HashSet<String>();

   private Map<String, String> oldParameters = new HashMap<String, String>();

   private Map<String, String> systemProperties = new HashMap<String, String>();

   private String filePath;

   public SystemParametersPersistenceConfigurator()
   {
   }

   public SystemParametersPersistenceConfigurator(InitParams initialParameters)
   {
      initValuesParameters(initialParameters);
      initFileParameter(initialParameters);

      systemProperties = fetchSystemProperties();

      oldParameters = readFile();
      writeFile();
   }

   private void initValuesParameters(InitParams initialParameters)
   {
      Iterator<ValuesParam> valuesParamIterator = initialParameters.getValuesParamIterator();

      while (valuesParamIterator.hasNext())
      {
         ValuesParam valuesParam = valuesParamIterator.next();

         if (UNMODIFIABLE.equals(valuesParam.getName()))
         {
            unmodifiable.addAll(valuesParam.getValues());
         }

         if (BEFORE_INITIALIZE.equals(valuesParam.getName()))
         {
            beforeInitialize.addAll(valuesParam.getValues());
         }
      }
   }

   private void initFileParameter(InitParams initialParameters)
   {
      ValueParam filePathValueParam = initialParameters.getValueParam(FILE_PATH_PARAMETER);
      if (filePathValueParam == null)
      {
         throw new IllegalStateException("No temporary file path is specified for "
            + SystemParametersPersistenceConfigurator.class.getSimpleName());
      }

      filePath = filePathValueParam.getValue() + File.separator + FILE_NAME;
   }

   private Map<String, String> readFile()
   {
      try
      {
         return SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Map<String, String>>()
         {
            public Map<String, String> run() throws IOException
            {
               Map<String, String> parameters = new HashMap<String, String>();
               File file = new File(filePath);

               if (file.exists() && file.length() > 0)
               {
                  BufferedReader reader = null;
                  String st = "";
                  try
                  {
                     reader = new BufferedReader(new FileReader(file));
                     while ((st = reader.readLine()) != null)
                     {
                        String sa[] = st.split(SEPARATOR);
                        parameters.put(sa[0], sa[1]);
                     }
                  }
                  finally
                  {
                     if (reader != null)
                     {
                        reader.close();
                     }
                  }
               }
               return parameters;
            }
         });
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Cannot read a file.", e);
      }
   }

   public Set<String> getBeforeInitializeParametersForWorkspaceComponent(String componentName)
   {
      return getParametersOfWorkspaceComponent(beforeInitialize, componentName);
   }

   public Set<String> getUnmodifiableParametersForWorkspaceComponent(String componentName)
   {
      return getParametersOfWorkspaceComponent(unmodifiable, componentName);
   }

   private Set<String> getParametersOfWorkspaceComponent(Set<String> originalParameters, String componentName)
   {
      Set<String> parameters = new HashSet<String>();

      for (String parameterName : originalParameters)
      {
         if (parameterName.startsWith(componentName))
         {
            parameters.add(parameterName.substring(componentName.length() + 1));
         }
      }

      return parameters;
   }

   public Map<String, String> getOldParameters()
   {
      return oldParameters;
   }

   private void writeFile()
   {
      writePropertiesToFile(systemProperties);
   }

   public void rollback()
   {
      writePropertiesToFile(oldParameters);
   }

   private void writePropertiesToFile(final Map<String, String> properties)
   {
      try
      {
         SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws IOException
            {
               BufferedWriter bufferedWriter = null;

               try
               {
                  File file = new File(filePath);

                  if (!file.getParentFile().exists())
                  {
                     file.getParentFile().mkdirs();
                  }

                  if (properties.isEmpty())
                  {
                     file.delete();
                     return null;
                  }

                  bufferedWriter = new BufferedWriter(new FileWriter(file));

                  for (Map.Entry<String, String> entry : properties.entrySet())
                  {
                     bufferedWriter.write(entry.getKey() + SEPARATOR + entry.getValue());
                     bufferedWriter.newLine();
                  }
               }
               finally
               {
                  if (bufferedWriter != null)
                  {
                     bufferedWriter.close();
                  }
               }

               return null;
            }
         });
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Cannot persist system parameters to file", e);
      }
   }

   public Map<String, String> getSystemProperties()
   {
      return systemProperties;
   }

   private Map<String, String> fetchSystemProperties()
   {
      Map<String, String> parameters = new HashMap<String, String>();

      for (String propertyName : getSystemPropertiesNames())
      {
         if (propertyName.startsWith(PropertiesParser.EXO_JCR_CONFIG))
         {
            parameters.put(propertyName, PropertyManager.getProperty(propertyName));
         }
      }
      return parameters;
   }

   private Set<String> getSystemPropertiesNames()
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Set<String>>()
      {
         public Set<String> run()
         {
            return System.getProperties().stringPropertyNames();
         }
      });
   }
}
