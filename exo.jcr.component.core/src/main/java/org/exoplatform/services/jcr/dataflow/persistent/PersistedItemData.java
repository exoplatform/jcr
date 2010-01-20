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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. </br>
 * 
 * Immutable ItemData from persistent storage
 * 
 * @author Gennady Azarenkov
 * @version $Id$
 */

public abstract class PersistedItemData implements ItemData, Externalizable
{

   /**
    * SerialVersionUID to serialization.
    */
   private static final long serialVersionUID = -3845740801904303663L;

   protected String id;

   protected QPath qpath;

   protected String parentId;

   protected int version;

   private final static int NOT_NULL_VALUE = 1;

   private final static int NULL_VALUE = -1;

   /**
    *  Empty constructor to serialization.
    */
   public PersistedItemData()
   {
   }

   public PersistedItemData(String id, QPath qpath, String parentId, int version)
   {
      this.id = id;
      this.qpath = qpath;
      this.parentId = parentId;
      this.version = version;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getQPath()
    */
   public QPath getQPath()
   {
      return qpath;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getIdentifier()
    */
   public String getIdentifier()
   {
      return id;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getPersistedVersion()
    */
   public int getPersistedVersion()
   {
      return version;
   }

   /**
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getParentIdentifier()
    */
   public String getParentIdentifier()
   {
      return parentId;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;

      if (obj == null)
         return false;

      if (obj instanceof ItemData)
      {
         return getIdentifier().hashCode() == ((ItemData)obj).getIdentifier().hashCode();
      }

      return false;
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      byte[] buf;

      try
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         String sQPath = new String(buf, Constants.DEFAULT_ENCODING);
         qpath = QPath.parse(sQPath);
      }
      catch (final IllegalPathException e)
      {
         throw new IOException("Deserialization error. " + e)
         {

            /**
             * {@inheritDoc}
             */
            @Override
            public Throwable getCause()
            {
               return e;
            }
         };
      }

      buf = new byte[in.readInt()];
      in.readFully(buf);
      id = new String(buf);

      int isNull = in.readInt();
      if (isNull == NOT_NULL_VALUE)
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         parentId = new String(buf);
      }

      version = in.readInt();
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf = qpath.getAsString().getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      out.writeInt(id.getBytes().length);
      out.write(id.getBytes());

      if (parentId != null)
      {
         out.writeInt(NOT_NULL_VALUE);
         out.writeInt(parentId.getBytes().length);
         out.write(parentId.getBytes());
      }
      else
      {
         out.writeInt(NULL_VALUE);
      }

      out.writeInt(version);
   }
}
