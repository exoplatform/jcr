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
package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.access.AccessControlList;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: NodeData.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface NodeData extends ItemData
{

   /**
    * @return this node order number
    */
   int getOrderNumber();

   /**
    * @return name of primary node type of this node. The jcr:primaryType property is loaded twice
    *         here and lazy initialized like others.
    */
   InternalQName getPrimaryTypeName();

   /**
    * @return names of mixin node types. The jcr:mixinTypes property is loaded twice here and lazy
    *         initialized like others. return empty array if no mixin types found
    */
   InternalQName[] getMixinTypeNames();

   /**
    * @return access control list either this node's data or nearest ancestor's data
    */
   AccessControlList getACL();

   /**
    * @param acl
    */
   void setACL(AccessControlList acl);

}
