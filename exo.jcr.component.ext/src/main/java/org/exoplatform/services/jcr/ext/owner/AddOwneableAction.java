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
package org.exoplatform.services.jcr.ext.owner;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import java.security.AccessControlException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: AddOwneableAction.java 12017 2007-01-17 16:26:04Z ksm $
 */

public class AddOwneableAction implements Action
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("org.exoplatform.services.jcr.ext.owner.AddOwneableAction");

   public boolean execute(Context ctx) throws Exception
   {
      NodeImpl node = (NodeImpl)ctx.get("currentItem");
      if (node != null && node.canAddMixin("exo:owneable"))
      {
         try
         {
            node.addMixin("exo:owneable");
         }
         catch (AccessControlException exp)
         {
            LOG.debug("Can not add mixin type exo:owneable, missing access : add_node,set_property,remove " + node.getPath());
         }
      }
      return false;
   }

}
