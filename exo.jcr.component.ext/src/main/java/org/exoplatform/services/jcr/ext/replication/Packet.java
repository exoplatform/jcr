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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: Packet.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class Packet extends AbstractPacket implements Externalizable
{

   /**
    * serialVersionUID.
    */
   private static final long serialVersionUID = -238898618077133064L;

   /**
    * PacketType. Definition of Packet types
    */
   public final class PacketType
   {
      /**
       * CHANGESLOG. the pocket type for ChangesLog without stream
       */
      public static final int CHANGESLOG = 1;

      /**
       * FIRST_CHANGESLOG_WITH_STREAM. the pocket type for first packet of ChangesLog with stream
       */
      public static final int FIRST_CHANGESLOG_WITH_STREAM = 2;

      /**
       * FIRST_PACKET_OF_STREAM. the pocket type for first packet of stream
       */
      public static final int FIRST_PACKET_OF_STREAM = 3;

      /**
       * PACKET_OF_STREAM. the pocket type for middle packet of stream
       */
      public static final int PACKET_OF_STREAM = 4;

      /**
       * LAST_PACKET_OF_STREAM. the pocket type for last packet of stream
       */
      public static final int LAST_PACKET_OF_STREAM = 5;

      /**
       * LAST_CHANGESLOG_WITH_STREAM. the pocket type for last packet of ChangesLog with stream
       */
      public static final int LAST_CHANGESLOG_WITH_STREAM = 6;

      /**
       * CHANGESLOG_FIRST_PACKET. the pocket type for first packet of ChangesLog without stream
       */
      public static final int CHANGESLOG_FIRST_PACKET = 7;

      /**
       * CHANGESLOG_MIDDLE_PACKET. the pocket type for middle packet of ChangesLog without stream
       */
      public static final int CHANGESLOG_MIDDLE_PACKET = 8;

      /**
       * CHANGESLOG_LAST_PACKET. the pocket type for last packet of ChangesLog without stream
       */
      public static final int CHANGESLOG_LAST_PACKET = 9;

      /**
       * CHANGESLOG_WITH_STREAM_FIRST_PACKET. the pocket type for first packet of ChangesLog with
       * stream (MAX_PACKET_SIZE < ChangesLog only)
       */
      public static final int CHANGESLOG_WITH_STREAM_FIRST_PACKET = 10;

      /**
       * CHANGESLOG_WITH_STREAM_MIDDLE_PACKET. the pocket type for middle packet of ChangesLog with
       * stream (MAX_PACKET_SIZE < ChangesLog only)
       */
      public static final int CHANGESLOG_WITH_STREAM_MIDDLE_PACKET = 11;

      /**
       * CHANGESLOG_WITH_STREAM_LAST_PACKET. the pocket type for last packet of ChangesLog with stream
       * (MAX_PACKET_SIZE < ChangesLog only)
       */
      public static final int CHANGESLOG_WITH_STREAM_LAST_PACKET = 12;

      /**
       * ADD_OK. the pocket type for information of successful save
       */
      public static final int ADD_OK = 13;

      /**
       * GET_CHANGESLOG_UP_TO_DATE. the pocket type for initialize synchronization mechanism
       */
      public static final int GET_CHANGESLOG_UP_TO_DATE = 14;

      /**
       * BINARY_FILE_PACKET. the packet type for packet to binary file
       */
      public static final int BINARY_FILE_PACKET = 15;

      /**
       * ALL_BINARY_FILE_TRANSFERRED_OK. the pocket type for information of all files was transferred
       */
      public static final int ALL_BINARY_FILE_TRANSFERRED_OK = 18;

      /**
       * ALL_CHANGESLOG_SAVED_OK. the pocket type for information of all ChangesLogs was saved
       */
      public static final int ALL_CHANGESLOG_SAVED_OK = 19;

      /**
       * SYNCHRONIZED_OK. the pocket type for information of synchronized well
       */
      public static final int SYNCHRONIZED_OK = 20;

      /**
       * INITED_IN_CLUSTER. the pocket type for information of member was initialized
       */
      public static final int INITED_IN_CLUSTER = 21;

      /**
       * ALL_INITED. the pocket type for information of all members was initialized
       */
      public static final int ALL_INITED = 22;

      /**
       * OLD_CHANGESLOG_REMOVED_OK. the pocket type for information of old ChangesLogs was removed
       */
      public static final int OLD_CHANGESLOG_REMOVED_OK = 23;

      /**
       * NEED_TRANSFER_COUNTER. the pocket type for information of how much ChangesLogs will be
       * transfered
       */
      public static final int NEED_TRANSFER_COUNTER = 24;

      /**
       * REMOVED_OLD_CHANGESLOG_COUNTER. the pocket type for information of how much ChangesLogs will
       * be removed
       */
      public static final int REMOVED_OLD_CHANGESLOG_COUNTER = 25;

      /**
       * MEMBER_STARTED. the pocket type for information other of member started
       */
      public static final int MEMBER_STARTED = 26;

      /**
       * BIG_PACKET. the pocket type for packet to binary Changeslog (using for recovery)
       */
      public static final int BIG_PACKET = 27;

      /**
       * BIG_PACKET_FIRST. the pocket type for first packet to binary Changeslog (using for recovery)
       */

      /**
       * GET_ALL_PRIORITY. the pocket type for request to other their priorities
       */
      public static final int GET_ALL_PRIORITY = 30;

      /**
       * OWN_PRIORITY. the pocket type for information other of own priority
       */
      public static final int OWN_PRIORITY = 31;

      /**
       * BINARY_CHANGESLOG_PACKET. the pocket type for packet to binary Changeslog
       */
      public static final int BINARY_CHANGESLOG_PACKET = 32;

      /**
       * Private PacketType constructor.
       */
      private PacketType()
      {
      }
   }

   /**
    * The definition of max packet size.
    */
   public static final int MAX_PACKET_SIZE = 1024 * 16;

   /**
    * Array of byte to binary data.
    */
   private byte[] buffer;

   /**
    * Field to size.
    */
   private long size;

   /**
    * Packet type.
    */
   private int type;

   /**
    * Offset to large file.
    */
   private long offset;

   /**
    * The packet identifier.
    */
   private String identifier;

   /**
    * Owner name.
    */
   private String ownName = new String(" ");

   /**
    * Time stamp.
    */
   private Calendar timeStamp = Calendar.getInstance();

   /**
    * Name of file.
    */
   private String fileName = new String(" ");

   /**
    * The system identifier.
    */
   private String systemId = new String(" ");

   /**
    * The names of files .
    */
   private List<String> fileNameList = new ArrayList<String>();

   /**
    * The total packets count.
    */
   private long totalPacketCount = 1;

   /**
    * Packet constructor. The empty constructor need for Externalizable
    */
   public Packet()
   {
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param identifier
    *          packet identifier
    * @param totalPacketCount
    *          toral packets count
    * @param size
    *          size value
    * @param offset
    *          offset to data
    * @param buf
    *          binary data
    */
   public Packet(int type, String identifier, long totalPacketCount, long size, long offset, byte[] buf)
   {
      this.identifier = identifier;
      this.type = type;
      this.totalPacketCount = totalPacketCount;
      this.size = size;
      this.offset = offset;

      this.buffer = buf;
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param identifier
    *          packet identifier
    */
   public Packet(int type, String identifier)
   {
      this.type = type;
      this.identifier = identifier;
      buffer = new byte[1];
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param identifier
    *          packet identifier
    * @param ownName
    *          owner name
    */
   public Packet(int type, String identifier, String ownName)
   {
      this.type = type;
      this.identifier = identifier;
      this.buffer = new byte[1];
      this.ownName = ownName;
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param systemId
    *          the system id
    * @param identifier
    *          packet identifier
    * @param ownName
    *          owner name
    * @param fileName
    *          file name
    * @param totalPacketCount
    *          total packets count
    * @param offset
    *          offset in data file
    * @param data
    *          piece of data
    */
   public Packet(int type, String systemId, String identifier, String ownName, String fileName, long totalPacketCount,
      long offset, byte[] data)
   {
      this(type, identifier, ownName);
      this.fileName = fileName;
      this.systemId = systemId;
      this.totalPacketCount = totalPacketCount;
      this.offset = offset;
      this.buffer = data;
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param identifier
    *          packet identifier
    * @param ownName
    *          owner name
    * @param fileNameList
    *          the list with files name
    */
   public Packet(int type, String identifier, String ownName, List<String> fileNameList)
   {
      this(type, identifier, ownName);
      this.fileNameList = fileNameList;
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param identifier
    *          packet identifier
    * @param ownName
    *          owner name
    * @param timeStamp
    *          the Calendar object with "time"
    */
   public Packet(int type, String identifier, String ownName, Calendar timeStamp)
   {
      this(type, identifier, ownName);
      this.timeStamp = timeStamp;
   }

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param ownName
    *          owner name
    * @param size
    *          the size value
    * @param identifier
    *          packet identifier
    */
   public Packet(int type, String ownName, long size, String identifier)
   {
      this(type, identifier, ownName);
      this.size = size;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(buffer.length);
      out.write(buffer);
      out.writeLong(size);
      out.writeInt(type);
      out.writeLong(offset);
      out.writeLong(totalPacketCount);

      byte[] id = identifier.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(id.length);
      out.write(id);

      byte[] own = ownName.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(own.length);
      out.write(own);

      // write timeStamp
      out.writeLong(timeStamp.getTimeInMillis());

      byte[] fn = fileName.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(fn.length);
      out.write(fn);

      // write list
      out.writeInt(fileNameList.size());
      for (String fName : fileNameList)
      {
         fn = fName.getBytes(Constants.DEFAULT_ENCODING);
         out.writeInt(fn.length);
         out.write(fn);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      int bufSize = in.readInt();
      buffer = new byte[bufSize];
      in.readFully(buffer);

      size = in.readLong();
      type = in.readInt();
      offset = in.readLong();
      this.totalPacketCount = in.readLong();

      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      identifier = new String(buf, Constants.DEFAULT_ENCODING);

      buf = new byte[in.readInt()];
      in.readFully(buf);
      ownName = new String(buf, Constants.DEFAULT_ENCODING);

      // set timeStamp
      timeStamp.setTimeInMillis(in.readLong());

      buf = new byte[in.readInt()];
      in.readFully(buf);
      fileName = new String(buf, Constants.DEFAULT_ENCODING);

      // read list
      int listSize = in.readInt();
      for (int i = 0; i < listSize; i++)
      {
         buf = new byte[in.readInt()];
         in.readFully(buf);
         fileNameList.add(new String(buf, Constants.DEFAULT_ENCODING));
      }
   }

   /**
    * getIdentifier.
    * 
    * @return String the packet identifier
    */
   public String getIdentifier()
   {
      return identifier;
   }

   /**
    * getByteArray.
    * 
    * @return byte[] the binary data
    */
   public byte[] getByteArray()
   {
      return buffer;
   }

   /**
    * getSize.
    * 
    * @return long the size value
    */
   public long getSize()
   {
      return size;
   }

   /**
    * setSize.
    * 
    * @param size
    *          size value
    */
   public void setSize(long size)
   {
      this.size = size;
   }

   /**
    * getPacketType.
    * 
    * @return integer the packet type
    */
   public int getPacketType()
   {
      return type;
   }

   /**
    * getOffset.
    * 
    * @return long the offset value
    */
   public long getOffset()
   {
      return offset;
   }

   /**
    * getTotalPacketCount.
    * 
    * @return long the TotalPacketCount
    */
   public long getTotalPacketCount()
   {
      return totalPacketCount;
   }

   /**
    * getAsByteArray.
    * 
    * @param packet
    *          Packet object
    * @return byte[] the binary value
    * @throws IOException
    *           generate the IOExaption
    */
   public static byte[] getAsByteArray(Packet packet) throws IOException
   {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);
      oos.writeObject(packet);

      byte[] bArray = os.toByteArray();
      return bArray;
   }

   /**
    * getAsPacket.
    * 
    * @param byteArray
    *          binary data
    * @return Packet the Packet object from bytes
    * @throws IOException
    *           generate the IOExeption
    * @throws ClassNotFoundException
    *           generate the ClassNotFoundException
    */
   public static Packet getAsPacket(byte[] byteArray) throws IOException, ClassNotFoundException
   {
      ByteArrayInputStream is = new ByteArrayInputStream(byteArray);
      ObjectInputStream ois = new ObjectInputStream(is);
      Packet objRead = (Packet)ois.readObject();

      return objRead;
   }

   /**
    * getOwnerName.
    * 
    * @return String the owner name
    */
   public String getOwnerName()
   {
      return ownName;
   }

   /**
    * setOwnName.
    * 
    * @param ownName
    *          owner name
    */
   public void setOwnName(String ownName)
   {
      this.ownName = ownName;
   }

   /**
    * getTimeStamp.
    * 
    * @return Calendar the timeStamp
    */
   public Calendar getTimeStamp()
   {
      return timeStamp;
   }

   /**
    * setTimeStamp.
    * 
    * @param timeStamp
    *          set the timeStamp (Calendar)
    */
   public void setTimeStamp(Calendar timeStamp)
   {
      this.timeStamp = timeStamp;
   }

   /**
    * getFileName.
    * 
    * @return String the file name
    */
   public String getFileName()
   {
      return fileName;
   }

   /**
    * setFileName.
    * 
    * @param fileName
    *          the file name
    */
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
   }

   /**
    * getFileNameList.
    * 
    * @return List the list of fileNames
    */
   public List<String> getFileNameList()
   {
      return fileNameList;
   }

   /**
    * getSystemId.
    * 
    * @return String the systemId
    */
   public String getSystemId()
   {
      return systemId;
   }

   /**
    * setSystemId.
    * 
    * @param systemId
    *          the systemId
    */
   public void setSystemId(String systemId)
   {
      this.systemId = systemId;
   }
}
