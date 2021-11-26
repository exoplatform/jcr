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

package org.exoplatform.services.jcr.impl.core.observation;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: ListenerCriteria.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ListenerCriteria
{

   private int eventTypes;

   private String absPath;

   private boolean deep;

   private String[] identifier;

   private String[] nodeTypeName;

   private boolean noLocal;

   private String sessionId;

   public ListenerCriteria(int eventTypes, String absPath, boolean isDeep, String[] identifier, String[] nodeTypeName,
      boolean noLocal, String sessionId) throws RepositoryException
   {
      this.eventTypes = eventTypes;
      this.absPath = absPath;
      this.deep = isDeep;
      this.identifier = identifier;
      this.nodeTypeName = nodeTypeName;
      this.noLocal = noLocal;
      this.sessionId = sessionId;
   }

   public int getEventTypes()
   {
      return this.eventTypes;
   }

   public String getAbsPath()
   {
      return this.absPath;
   }

   public boolean isDeep()
   {
      return deep;
   }

   public String[] getIdentifier()
   {
      return this.identifier;
   }

   public String[] getNodeTypeName()
   {
      return this.nodeTypeName;
   }

   /**
    * If noLocal is true, then events generated by the session through which the
    * listener was registered are ignored. Otherwise, they are not ignored.
    */
   public boolean getNoLocal()
   {
      return this.noLocal;
   }

   public String getSessionId()
   {
      return this.sessionId;
   }

}
