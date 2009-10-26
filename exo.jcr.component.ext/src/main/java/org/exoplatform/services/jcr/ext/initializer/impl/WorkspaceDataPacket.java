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
package org.exoplatform.services.jcr.ext.initializer.impl;

import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 17.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: WorkspaceDataPacket.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class WorkspaceDataPacket extends AbstractPacket
{

   /**
    * WORKSPACE_DATA_PACKET. the pocket type for workspace data
    */
   public static final int WORKSPACE_DATA_PACKET = 100;

   /**
    * CRC.
    */
   private byte[] crc;

   /**
    * Count of packets ChangesLog separated to.
    */
   private long totalPacketsCount;

   /**
    * Current data position in total data byte array.
    */
   private long offset;

   /**
    * Current data.
    */
   private byte[] buffer;

   /**
    * Constructor.
    * 
    * @param type
    *          see AsyncPacketTypes
    * @param totalPacketsCount
    *          the total pocket in file
    * @param checksum
    *          the checksum of file
    * @param offset
    *          the offset
    * @param buffer
    *          the binary data
    */
   public WorkspaceDataPacket(int type, long totalPacketsCount, byte[] checksum, long offset, byte[] buffer)
   {
      super(type, -1);
      this.totalPacketsCount = totalPacketsCount;
      this.crc = checksum;
      this.offset = offset;
      this.buffer = buffer;
   }

   /**
    * ChangesPacket constructor.
    */
   public WorkspaceDataPacket()
   {
      super();
   }

   /**
    * getCRC.
    * 
    * @return byte[] return the checksum
    */
   public byte[] getCRC()
   {
      return this.crc;
   }

   /**
    * getPacketsCount.
    * 
    * @return long return total packet count
    */
   public long getPacketsCount()
   {
      return this.totalPacketsCount;
   }

   /**
    * getOffset.
    * 
    * @return long the offset
    */
   public long getOffset()
   {
      return this.offset;
   }

   /**
    * getBuffer.
    * 
    * @return byte[] the binary data
    */
   public byte[] getBuffer()
   {
      return this.buffer;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      super.writeExternal(out);

      out.writeInt(crc.length);
      out.write(crc);

      out.writeLong(totalPacketsCount);
      out.writeLong(offset);

      out.writeInt(buffer.length);
      out.write(buffer);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      super.readExternal(in);

      crc = new byte[in.readInt()];
      in.readFully(crc);

      totalPacketsCount = in.readLong();
      offset = in.readLong();

      int bufSize = in.readInt();
      buffer = new byte[bufSize];
      in.readFully(buffer);
   }

}
