/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: NullItemData.java 111 2008-11-11 11:11:11Z serg $
 */
public abstract class NullItemData implements ItemData
{

   public static final String NULL_ID = "_null_id";

   private final String id;

   private final String parentId;

   private final QPath path;

   public NullItemData(NodeData parentData, QPathEntry name)
   {
      this.parentId = parentData.getIdentifier();
      this.path = QPath.makeChildPath(parentData.getQPath(), name);
      this.id = NULL_ID;
   }

   /** 
    * This constructor must never be used for null nodes placed in cache. Only for returned values.
    * 
    * @param parentId
    * @param name
    */
   public NullItemData(String parentId, QPathEntry name)
   {
      this.parentId = parentId;
      this.path = null;
      this.id = NULL_ID;
   }

   public NullItemData(String id)
   {
      this.parentId = null;
      this.path = null;
      this.id = id;
   }

   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
   }

   public String getIdentifier()
   {
      return id;
   }

   public String getParentIdentifier()
   {
      return parentId;
   }

   public int getPersistedVersion()
   {
      return 0;
   }

   public QPath getQPath()
   {
      return path;
   }

}
