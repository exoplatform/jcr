/*
 * Copyright (C) 2009 eXo Platform SAS.
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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

/**
 * This class provides extra functionality for {@link MappedParametrizedObjectEntry}
 * to be able to get components configuration parameters from system properties.
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: ExtendedMappedParametrizedObjectEntry.java 14.06.2012 dkuleshov $
 *
 */
public abstract class ExtendedMappedParametrizedObjectEntry extends MappedParametrizedObjectEntry
{
   private SystemParameterUpdater systemParameterUpdater;

   private String componentName;

   private static final Log LOG = ExoLogger
      .getLogger("org.exoplatform.services.jcr.config.ExtendedMappedParametrizedObjectEntry");

   public ExtendedMappedParametrizedObjectEntry(String componentName)
   {
      super();
      this.componentName = componentName;
   }

   public ExtendedMappedParametrizedObjectEntry(String type, List parameters, String componentName)
   {
      super(type, parameters);
      this.componentName = componentName;
   }

   public void initSystemParameterUpdater(WorkspaceEntry workspaceEntry, SystemParametersPersistenceConfigurator sppc)
   {
      systemParameterUpdater = new SystemParameterUpdater(super.getParameters(), sppc, componentName, workspaceEntry);
      systemParameterUpdater.updateSystemParameters();
   }

   public SystemParameterUpdater getSystemParameterUpdater()
   {
      return systemParameterUpdater;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setParameters(List<SimpleParameterEntry> parameters)
   {
      super.setParameters(parameters);

      if (systemParameterUpdater != null)
      {
         systemParameterUpdater.setParameters(parameters);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void putParameterValue(String name, String value)
   {
      if (systemParameterUpdater != null && systemParameterUpdater.isAlreadyUpdated(name))
      {
         return;
      }

      super.putParameterValue(name, value);
   }
}