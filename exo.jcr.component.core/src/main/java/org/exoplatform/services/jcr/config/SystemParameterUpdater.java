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

package org.exoplatform.services.jcr.config;

import org.exoplatform.services.jcr.impl.core.WorkspaceInitializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: SystemPropertiesUpdater.java 19.07.2012 dkuleshov $
 *
 */
public class SystemParameterUpdater
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SystemParameterUpdater");

   private WorkspaceInitializer workspaceInitializer;

   private MappedParametrizedObjectEntry parameters;

   private PropertiesParser propertiesParser;

   private Set<String> updatedParameterNames = new HashSet<String>();

   private Set<String> unmodifiableParameters = new HashSet<String>();

   private Set<String> beforeInitializeParameters = new HashSet<String>();

   private Map<String, Exception> parametersToValidate = new HashMap<String, Exception>();

   private Map<String, String> oldParameters;

   private Map<String, String> systemProperties;

   private SystemParametersPersistenceConfigurator sppc;

   public SystemParameterUpdater(MappedParametrizedObjectEntry parameters, SystemParametersPersistenceConfigurator sppc,
      String componentName, WorkspaceEntry workspaceEntry)
   {
      this.parameters = parameters;
      this.unmodifiableParameters = sppc.getUnmodifiableParametersForWorkspaceComponent(componentName);
      this.beforeInitializeParameters = sppc.getBeforeInitializeParametersForWorkspaceComponent(componentName);
      this.oldParameters = sppc.getOldParameters();
      this.systemProperties = sppc.getSystemProperties();
      this.propertiesParser = new PropertiesParser(workspaceEntry, componentName);
      this.sppc = sppc;
   }

   public void updateParameter(String parameterName)
   {
      if (updatedParameterNames.contains(parameterName))
      {
         return;
      }

      updateForcedParameterIfNeeded(parameterName, getAlreadySetParameter(parameterName));
      updateDefaultParameterIfNeeded(parameterName, getAlreadySetParameter(parameterName));
   }

   private void updateForcedParameterIfNeeded(String parameterName, SimpleParameterEntry alreadySetParameter)
   {
      String actualForcedParameterValue = propertiesParser.getForcedParameterValue(parameterName, systemProperties);

      String previousForcedParameterValue = propertiesParser.getForcedParameterValue(parameterName, oldParameters);

      if (actualForcedParameterValue != null)
      {
         if (alreadySetParameter != null && actualForcedParameterValue.equals(alreadySetParameter.getValue()))
         {
            return;
         }

         if (!actualForcedParameterValue.equals(previousForcedParameterValue) && !isAllowedToOverride(parameterName))
         {
            return;
         }

         if (alreadySetParameter != null)
         {
            alreadySetParameter.setValue(actualForcedParameterValue);
         }
         else
         {
            parameters.addParameter(new SimpleParameterEntry(parameterName, actualForcedParameterValue));
         }

         updatedParameterNames.add(parameterName);
         return;
      }
   }

   private void updateDefaultParameterIfNeeded(String parameterName, SimpleParameterEntry alreadySetParameter)
   {
      String actualDefaultParameterValue = propertiesParser.getDefaultParameterValue(parameterName, systemProperties);
      String previousDefaultParameterValue = propertiesParser.getDefaultParameterValue(parameterName, oldParameters);

      if (actualDefaultParameterValue != null && alreadySetParameter == null)
      {
         if (!actualDefaultParameterValue.equals(previousDefaultParameterValue) && !isAllowedToOverride(parameterName))
         {
            return;
         }

         parameters.addParameter(new SimpleParameterEntry(parameterName, actualDefaultParameterValue));
         updatedParameterNames.add(parameterName);
         return;
      }
   }

   private SimpleParameterEntry getAlreadySetParameter(String parameterName)
   {
      return parameters.getParameter(parameterName);
   }

   public void unupdateParameter(String parameterName)
   {
      updatedParameterNames.remove(parameterName);
   }

   public void updateSystemParameters()
   {
      updatedParameterNames.clear();

      for (String parameterName : propertiesParser.getParameterNames(systemProperties.keySet()))
      {
         updateParameter(parameterName);
      }
   }

   public void setParameters(MappedParametrizedObjectEntry parameters)
   {
      this.parameters = parameters;
      updatedParameterNames.clear();
      updateSystemParameters();
   }

   public boolean isAlreadyUpdated(String parameterName)
   {
      return updatedParameterNames.contains(parameterName);
   }

   public boolean isAllowedToOverride(String parameterName)
   {
      if (unmodifiableParameters.contains(parameterName))
      {
         if (LOG.isWarnEnabled())
         {
            LOG.warn("Parameter " + parameterName
               + " is not overridden because it is set to 'unmodifiable' via system properties in the "
               + SystemParametersPersistenceConfigurator.class.getSimpleName());
         }
         return false;
      }

      if (beforeInitializeParameters.contains(parameterName))
      {
         if (workspaceInitializer == null)
         {
            parametersToValidate.put(parameterName, new Exception());
            return true;
         }
         else
         {
            try
            {
               if (workspaceInitializer.isWorkspaceInitialized())
               {
                  if (LOG.isWarnEnabled())
                  {
                     LOG.warn("Parameter "
                        + parameterName
                        + " is not overridden because workspace is already initialized and parameter is set to 'before-initialize'"
                        + " via system properties in the "
                        + SystemParametersPersistenceConfigurator.class.getSimpleName());
                  }
                  return false;
               }
            }
            catch (RepositoryException e)
            {
               LOG.error("Could not know if the workspace has been initialized or not:" + e.getMessage());
               return false;
            }
         }
      }

      return true;
   }

   public void setWorkspaceInitializer(WorkspaceInitializer workspaceInitializer)
   {
      this.workspaceInitializer = workspaceInitializer;
   }

   public void validateOverriddenParameters()
   {
      for (Map.Entry<String, Exception> mapEntry : parametersToValidate.entrySet())
      {
         parametersToValidate.remove(mapEntry.getKey());

         try
         {
            if (workspaceInitializer.isWorkspaceInitialized())
            {
               sppc.rollback();

               throw new IllegalStateException("Unable to override parameter '" + mapEntry.getKey()
                  + "' set in system property because this operation is allowed only for non initialized workspaces",
                  mapEntry.getValue());
            }
         }
         catch (RepositoryException e)
         {
            sppc.rollback();
            throw new IllegalStateException("Unable to override parameters without being able to know if the workspace " +
               "has been initialized or not",
               e);
         }
      }
   }

   public int getUpdatedParametersAmount()
   {
      return updatedParameterNames.size();
   }
}