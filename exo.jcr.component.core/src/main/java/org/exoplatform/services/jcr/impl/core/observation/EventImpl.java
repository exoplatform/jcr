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
package org.exoplatform.services.jcr.impl.core.observation;

import org.exoplatform.services.jcr.observation.ExtendedEvent;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: EventImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class EventImpl implements Event, ExtendedEvent
{

   private int type;

   private String path;

   private String userId;

   private Map<String, String> info;

   public EventImpl(int type, String path, String userId)
   {

      this.type = type;
      this.userId = userId;
      this.path = path;
   }

   public EventImpl(int type, String path,String userId,Map<String, String> info)
   {
      this(type,path,userId);
      this.info = new HashMap<String, String>(info);
   }

   /**
    * @see javax.jcr.observation.Event#getType
    */
   public int getType()
   {
      return this.type;
   }

   /**
    * @see javax.jcr.observation.Event#getPath
    */
   public String getPath()
   {
      return this.path;
   }

   /**
    * @see javax.jcr.observation.Event#getUserID
    */
   public String getUserID()
   {
      return this.userId;
   }

   /**
    * {@inheritDoc}
    */
   public Map<String, String> getInfo() throws RepositoryException
   {
      return info;
   }

}
