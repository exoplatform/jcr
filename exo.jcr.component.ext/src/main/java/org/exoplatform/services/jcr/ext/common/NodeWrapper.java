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

package org.exoplatform.services.jcr.ext.common;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS .<br>
 * Wrapper for jcr node. The idea is to force application to use the node of particular NodeType so
 * the object's client could not change its type in modified method. For example
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public abstract class NodeWrapper
{

   private final Node node;

   protected NodeWrapper(final Node node)
   {
      this.node = node;
   }

   public final Node getNode()
   {
      return node;
   }

}
