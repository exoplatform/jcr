/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.quota.infinispan;

import org.exoplatform.services.jcr.impl.Constants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Common class for all keys contained path element.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: PathBasedKey.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class PathBasedKey extends WorkspaceBasedKey
{
   /**
    * Workspace unique name.
    */
   private String path;

   /**
    * Constructor for serialization.
    */
   public PathBasedKey()
   {
      super();
   }

   /**
    * PathBaseKey constructor.
    *
    * @param path
    *          absolute path to a node or pattern path
    */
   public PathBasedKey(String workspaceUniqueName, String path)
   {
      super(workspaceUniqueName, new StringBuilder(workspaceUniqueName).append('/').append(path).toString());
      this.path = path;
   }

   /**
    * Returns path.
    */
   public String getPath()
   {
      return path;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);

      byte[] data = path.getBytes(Constants.DEFAULT_ENCODING);
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
      path = new String(data, Constants.DEFAULT_ENCODING);
   }
}
