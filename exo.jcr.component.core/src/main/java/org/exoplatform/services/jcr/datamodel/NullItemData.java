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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: NullItemData.java 111 2008-11-11 11:11:11Z serg $
 */
public abstract class NullItemData implements ItemData, Externalizable
{

   public static final String NULL_ID = "_null_id";

   private String id;

   private String parentId;

   private QPathEntry name;

   private QPath path;

   public NullItemData(NodeData parent, QPathEntry name)
   {
      this.parentId = parent.getIdentifier();
      this.path = QPath.makeChildPath(parent.getQPath(), name);
      this.name = name;
      this.id = NULL_ID;
   }

   public NullItemData(String id)
   {
      this.id = id;
   }

   public NullItemData()
   {
      this(NULL_ID);
   }

   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   public String getIdentifier()
   {
      return id;
   }

   public String getParentIdentifier()
   {
      return parentId;
   }

   public int getPersistedVersion()
   {
      throw new UnsupportedOperationException("Method is not supported");
   }

   public QPath getQPath()
   {
      return path;
   }

   public QPathEntry getName()
   {
      return name;
   }
   /**
    * {@inheritDoc}
    * 
    * We need to make it serializable mostly for distributed cache in case we
    * don't allow local caching
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      id = new String(buf, Constants.DEFAULT_ENCODING);
      int length = in.readInt();
      if (length > 0)
      {
         buf = new byte[length];
         in.readFully(buf);
         parentId = new String(buf, Constants.DEFAULT_ENCODING);
      }
      length = in.readInt();
      if (length > 0)
      {
         buf = new byte[length];
         in.readFully(buf);
         try
         {
            name = QPathEntry.parse(new String(buf, Constants.DEFAULT_ENCODING));
         }
         catch (Exception e)
         {
            throw new IOException("Deserialization error, could not parse the name. ", e);
         }
      }
      length = in.readInt();
      if (length > 0)
      {
         buf = new byte[length];
         in.readFully(buf);
         try
         {
            path = QPath.parse(new String(buf, Constants.DEFAULT_ENCODING));
         }
         catch (Exception e)
         {
            throw new IOException("Deserialization error, could not parse the path. ", e);
         }
      }
   }

   /**
    * {@inheritDoc}
    * 
    * We need to make it serializable mostly for distributed cache in case we
    * don't allow local caching
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf = id.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);
      
      if (parentId == null)
      {
         out.writeInt(-1);
      }
      else
      {
         buf = parentId.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }
      if (name == null)
      {
         out.writeInt(-1);
      }
      else
      {
         buf = name.getAsString(true).getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }
      if (path == null)
      {
         out.writeInt(-1);
      }
      else
      {
         buf = path.getAsString().getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(buf.length);
         out.write(buf);
      }
   }
}
