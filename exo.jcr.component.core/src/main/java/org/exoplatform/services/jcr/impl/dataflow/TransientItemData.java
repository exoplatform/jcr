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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.MutableItemData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: TransientItemData.java 11907 2008-03-13 15:36:21Z ksm $
 */
public abstract class TransientItemData implements MutableItemData, Externalizable
{

   protected static final Log LOG = ExoLogger.getLogger("jcr.TransientItemData");

   protected String identifier;

   protected String parentIdentifier;

   protected int persistedVersion;

   protected QPath qpath;

   private int NOT_NULL_VALUE = 1;

   private int NULL_VALUE = -1;

   // serializable --------------
   TransientItemData()
   {
   }

   /**
    * @param path QPath
    * @param identifier id
    * @param version persisted version
    * @param parentIdentifier parentId
    */
   TransientItemData(QPath path, String identifier, int version, String parentIdentifier)
   {
      this.parentIdentifier = parentIdentifier != null ? parentIdentifier : null;
      this.identifier = identifier;
      this.qpath = path;
      this.persistedVersion = version;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;

      if (obj == null)
         return false;

      if (obj instanceof ItemData)
      {
         // TODO use String.equals, but check performance!
         return getIdentifier().hashCode() == ((ItemData)obj).getIdentifier().hashCode();
      }

      return false;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getIdentifier()
    */
   public String getIdentifier()
   {
      return identifier;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getParentUUID()
    */
   public String getParentIdentifier()
   {
      return parentIdentifier;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getPersistedVersion()
    */
   public int getPersistedVersion()
   {
      return persistedVersion;
   }

   /**
    * @return Qname - shortcut for getQPath().getName();
    */
   public InternalQName getQName()
   {
      return qpath.getName();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.datamodel.ItemData#getQPath()
    */
   public QPath getQPath()
   {
      return qpath;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.datamodel.MutableItemData#increasePersistedVersion()
    */
   public void increasePersistedVersion()
   {
      this.persistedVersion++;
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
      identifier = new String(buf);

      int isNull = in.readInt();
      if (isNull == NOT_NULL_VALUE)
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         parentIdentifier = new String(buf);
      }

      persistedVersion = in.readInt();
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf = qpath.getAsString().getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      out.writeInt(identifier.getBytes().length);
      out.write(identifier.getBytes());

      if (parentIdentifier != null)
      {
         out.writeInt(NOT_NULL_VALUE);
         out.writeInt(parentIdentifier.getBytes().length);
         out.write(parentIdentifier.getBytes());
      }
      else
         out.writeInt(NULL_VALUE);

      out.writeInt(persistedVersion);
   }

}
