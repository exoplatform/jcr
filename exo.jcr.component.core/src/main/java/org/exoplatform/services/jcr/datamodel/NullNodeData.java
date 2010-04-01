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
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;

import javax.jcr.RepositoryException;

/**
 * This class is used to represent <code>null</code> value, it is designed to be used 
 * into the cache to represent missing value.
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 31 mars 2010  
 */
public class NullNodeData implements NodeData
{
   private final String id;
   private final String parentId;
   private final QPath path;
   
   public NullNodeData(String id)
   {
      this.id = id;
      this.path = new QPath(new QPathEntry[]{new QPathEntry(null, null, 0)});
      this.parentId = null;
   }
   
   public NullNodeData(NodeData parentData, QPathEntry name)
   {
      this.parentId = parentData.getIdentifier();
      this.path = QPath.makeChildPath(parentData.getQPath(), name);
      this.id = parentId + "$" + name.asString();
   }
   
   /**
    * {@inheritDoc}
    */
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
   }

   /**
    * {@inheritDoc}
    */
   public String getIdentifier()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   public String getParentIdentifier()
   {
      return parentId;
   }

   /**
    * {@inheritDoc}
    */
   public int getPersistedVersion()
   {
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public QPath getQPath()
   {
      return path;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public AccessControlList getACL()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName[] getMixinTypeNames()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getPrimaryTypeName()
   {
      return null;
   }      
}