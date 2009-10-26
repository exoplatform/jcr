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
package org.exoplatform.services.jcr.ext.access;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.security.Identity;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: DenyAccessManager.java 14111 2008-05-12 15:04:28Z gazarenkov $
 */
public class DenyAccessManager extends AccessManager
{
   private String denyName = "";

   public DenyAccessManager(RepositoryEntry config, WorkspaceEntry wsConfig) throws RepositoryException,
      RepositoryConfigurationException
   {
      super(config, wsConfig);
      this.denyName = wsConfig.getAccessManager().getParameterValue("name");

      if (log.isDebugEnabled())
         log.debug("DenyAccessManager created");
   }

   @Override
   public boolean hasPermission(AccessControlList acl, String[] permission, Identity user)
   {
      if (super.hasPermission(acl, permission, user))
      {
         if (user.getUserId().equals("root") || user.getUserId().equals(SystemIdentity.SYSTEM)
            || user.getUserId().equals("admin"))
            return true;

         if (context() != null)
         {
            int ivent = ((Integer)context().get("event")).intValue();
            if (ivent == ExtendedEvent.READ)
            {
               ItemImpl curItem = (ItemImpl)context().get("currentItem");

               if (curItem != null && curItem.getInternalName().getAsString().indexOf(denyName) > -1)
               {
                  if (log.isDebugEnabled())
                     log.debug("DenyAccessManager permission deny by rool name='" + denyName + "'");
                  return false;
               }
            }
         }
         else
            log.warn("Context = null");
      }
      return false;
   }
}
