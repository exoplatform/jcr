/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota.infinispan;

import org.exoplatform.services.jcr.impl.Constants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Common class for all workspace based keys.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: WorkspaceBaseKey.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class WorkspaceBasedKey extends QuotaKey
{
   /**
    * Workspace unique name.
    */
   private String workspaceUniqueName;

   /**
    * Constructor for serialization.
    */
   public WorkspaceBasedKey()
   {
      super();
   }

   /**
    * WorkspaceBaseKey constructor.
    *
    * @param wsUniqueName
    *          unique name of workspace in global JCR instance, might
    *          contains repository name as well as workspace name
    */
   public WorkspaceBasedKey(String workspaceUniqueName, String id)
   {
      super(null, id);
      this.workspaceUniqueName = workspaceUniqueName;
   }

   /**
    * Returns workspace unique name.
    */
   public String getWorkspaceUniqueName()
   {
      return workspaceUniqueName;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);

      byte[] data = workspaceUniqueName.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(data.length);
      out.write(data);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);

      byte[] data = new byte[in.readInt()];
      in.readFully(data);
      workspaceUniqueName = new String(data, Constants.DEFAULT_ENCODING);
   }
}
