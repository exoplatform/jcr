/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;

import javax.jcr.RepositoryException;

/**
 * This class is used to represent <code>null</code> value, it is designed to be used  
 * into the cache to represent missing value.
 * 
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: NullNodeData.java 111 2010-11-11 11:11:11Z tolusha $
 */
public class NullNodeData implements NodeData
{

   private final String id;

   private final String parentId;

   private final QPath path;

   public NullNodeData(NodeData parentData, QPathEntry name)
   {
      this.parentId = parentData.getIdentifier();
      this.path = QPath.makeChildPath(parentData.getQPath(), name);
      this.id = parentId + "$" + name.asString();
   }

   public NullNodeData(String id)
   {
      this.parentId = null;
      this.path = new QPath(new QPathEntry[]{new QPathEntry(null, null, 0)});
      this.id = id;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public AccessControlList getACL()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InternalQName[] getMixinTypeNames()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getOrderNumber()
   {
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InternalQName getPrimaryTypeName()
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getIdentifier()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getParentIdentifier()
   {
      return parentId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getPersistedVersion()
   {
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public QPath getQPath()
   {
      return path;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isNode()
   {
      return true;
   }

}
