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
package org.exoplatform.services.jcr.impl.ext.action;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: AddActionsPlugin.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Provisional
 */

public class AddActionsPlugin extends BaseComponentPlugin
{

   private ActionsConfig actionsConfig;

   /**
    * The default constructor of the plugin
    * @param params the init parameter from which we extract the
    * object parameter <i>actions</i> that contains the
    * {@link ActionConfiguration} objects
    */
   public AddActionsPlugin(InitParams params)
   {
      ObjectParameter param = params.getObjectParam("actions");

      if (param != null)
      {
         actionsConfig = (ActionsConfig)param.getObject();
      }
   }

   /**
    * @return returns a collection containing the configuration
    * of all the actions to be registered
    */
   public List<ActionConfiguration> getActions()
   {
      return actionsConfig.getActions();
   }

   public static class ActionsConfig
   {
      private List<ActionConfiguration> actions = new ArrayList<ActionConfiguration>();

      public List<ActionConfiguration> getActions()
      {
         return actions;
      }

      public void setActions(List<ActionConfiguration> actions)
      {
         this.actions = actions;
      }
   }
}
