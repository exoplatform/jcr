/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS. <br> Holds SystemSearchManager instance to be accessible from
 * RepositoryContainer.<br> Prevent SystemSearchManager instance to being started and stopped in
 * RepositoryContainer.<br> Holder is placed in dependency for workspace SearchManagers. <br>
 * Date: 06.06.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SystemSearchManagerHolder.java 34061 2009-07-16 11:06:01Z rainf0x $
 */
public class SystemSearchManagerHolder implements Startable
{

   protected final SystemSearchManager manager;

   public SystemSearchManagerHolder(SystemSearchManager manager)
   {
      this.manager = manager;
   }

   public SystemSearchManager get()
   {
      return manager;
   }

   /**
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      if (manager != null)
         manager.start();
   }

   /**
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
   }
}
