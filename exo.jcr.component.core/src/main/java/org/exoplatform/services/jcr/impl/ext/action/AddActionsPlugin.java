/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.ext.action;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS<br>
 *
 * The AddActionsPlugin class provides the methods to manage configuration
 * of all the actions registered
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
