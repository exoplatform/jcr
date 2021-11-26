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

package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.access.AccessControlList;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id$
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
}
