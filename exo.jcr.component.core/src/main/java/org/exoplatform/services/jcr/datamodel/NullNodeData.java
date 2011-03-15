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

/**
 * This class is used to represent <code>null</code> value, it is designed to be used  
 * into the cache to represent missing value.
 * 
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: NullNodeData.java 111 2010-11-11 11:11:11Z tolusha $
 */
public class NullNodeData extends NullItemData implements NodeData
{

   public NullNodeData(NodeData parent, QPathEntry name)
   {
      super(parent, name);
   }

   public NullNodeData(String id)
   {
      super(id);
   }

   public NullNodeData()
   {
      super();
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
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName[] getMixinTypeNames()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getPrimaryTypeName()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

}
