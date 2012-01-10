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
package org.exoplatform.services.jcr.ext.backup.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: FixupStream.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class FixupStream implements Externalizable
{

   /**
    * serialVersionUID. Determinate the version of serialization.
    */
   private static final long serialVersionUID = 6453641729031051616L;

   /**
    * iItemStateId. Index of ItemState in ChangesLog.
    */
   private int iItemStateId = -1;

   /**
    * iValueDataId. Index of ValueData in ItemState.
    */
   private int iValueDataId = -1;

   /**
    * FixupStream constructor. Empty constructor is necessary to Externalizable.
    */
   public FixupStream()
   {
   }

   /**
    * FixupStream constructor.
    * 
    * @param itemState
    *          index of ItemState in ChangesLog
    * @param valueData
    *          index of ValueData in ItemState
    */
   public FixupStream(int itemState, int valueData)
   {
      this.iItemStateId = itemState;
      this.iValueDataId = valueData;
   }

   /**
    * getItemSateId.
    * 
    * @return int return the iItemStateId
    */
   public int getItemSateId()
   {
      return iItemStateId;
   }

   /**
    * getValueDataId.
    * 
    * @return int return the iValueDataId
    */
   public int getValueDataId()
   {
      return iValueDataId;
   }

   /**
    * compare.
    * 
    * @param fs
    *          FixupStream.
    * @return boolean return 'true' if this == fs
    */
   public boolean compare(FixupStream fs)
   {
      boolean b = true;
      if (fs.getItemSateId() != this.getItemSateId())
         b = false;
      if (fs.getValueDataId() != this.getValueDataId())
         b = false;
      return b;
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      iItemStateId = in.readInt();
      iValueDataId = in.readInt();
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(iItemStateId);
      out.writeInt(iValueDataId);
   }
}
