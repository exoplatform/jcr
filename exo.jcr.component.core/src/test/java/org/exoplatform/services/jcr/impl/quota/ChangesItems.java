/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import java.io.Serializable;

/**
 * Builds the changes the quota manager of a node sends to the coordinator, whose class is package-private.
 */
public final class ChangesItems
{
   private ChangesItems()
   {
   }

   /**
    * @return changes of the workspace size, of the size of a node, and of paths whose size is unknown or updated later
    */
   public static Serializable sample()
   {
      ChangesItem changes = new ChangesItem();
      changes.updateWorkspaceChangedSize(1024);
      changes.updateNodeChangedSize("/node", 512);
      changes.addPathWithUnknownChangedSize("/unknown");
      changes.addPathWithAsyncUpdate("/async");
      return changes;
   }

   /**
    * @param changes changes built by {@link #sample()}, or received
    * @return the workspace size change they carry
    */
   public static long workspaceChangedSize(Serializable changes)
   {
      return ((ChangesItem)changes).getWorkspaceChangedSize();
   }
}
