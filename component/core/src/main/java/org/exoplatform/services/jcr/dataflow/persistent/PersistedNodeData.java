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
package org.exoplatform.services.jcr.dataflow.persistent;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.</br>
 * 
 * Immutable NodeData from persistense storage
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: PersistedNodeData.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class PersistedNodeData extends PersistedItemData implements NodeData
{

   protected final int orderNumber;

   protected final InternalQName primaryTypeName;

   protected final InternalQName[] mixinTypeNames;

   protected AccessControlList acl;

   public PersistedNodeData(String id, QPath qpath, String parentId, int version, int orderNumber,
      InternalQName primaryTypeName, InternalQName[] mixinTypeNames, AccessControlList acl)
   {
      super(id, qpath, parentId, version);
      this.primaryTypeName = primaryTypeName;
      this.mixinTypeNames = mixinTypeNames;
      this.orderNumber = orderNumber;
      this.acl = acl;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getOrderNumber()
    */
   public int getOrderNumber()
   {
      return orderNumber;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getPrimaryTypeName()
    */
   public InternalQName getPrimaryTypeName()
   {
      return primaryTypeName;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getMixinTypeNames()
    */
   public InternalQName[] getMixinTypeNames()
   {
      return mixinTypeNames;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.NodeData#getACL()
    */
   public AccessControlList getACL()
   {
      return acl;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.datamodel.NodeData#setACL(org.exoplatform.services.jcr.access.
    * AccessControlList)
    */
   public void setACL(AccessControlList acl)
   {
      this.acl = acl;
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.datamodel.ItemData#accept(org.exoplatform.services.jcr.dataflow
    * .ItemDataVisitor)
    */
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
      visitor.visit(this);
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.datamodel.ItemData#isNode()
    */
   public boolean isNode()
   {
      return true;
   }

}
