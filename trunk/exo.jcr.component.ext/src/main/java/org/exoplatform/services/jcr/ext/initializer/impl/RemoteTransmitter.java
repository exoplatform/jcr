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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;
import org.exoplatform.services.jcr.ext.replication.transport.MemberAddress;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 17.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RemoteTransmitter.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class RemoteTransmitter
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.RemoteTransmitter");

   /**
    * The ChannelManager will be send data.
    */
   private final ChannelManager channelManager;

   /**
    * RemoteTransmitter constructor.
    * 
    * @param channelManager
    *          the ChannelManager.
    */
   public RemoteTransmitter(ChannelManager channelManager)
   {
      this.channelManager = channelManager;

   }

   /**
    * sendChangesLogFile.
    * 
    * @param destinationAddress
    *          MemberAddress, the destination address
    * @param file
    *          File, the data file
    * @param checkSum
    *          byte[], the checksum for data file
    * @throws IOException
    *           will be generated IOException
    */
   protected void sendChangesLogFile(MemberAddress destinationAddress, File file, byte[] checkSum) throws IOException
   {
      if (log.isDebugEnabled())
         log.debug("Begin send : " + PrivilegedFileHelper.length(file));

      InputStream in = PrivilegedFileHelper.fileInputStream(file);
      long totalPacketCount = getPacketCount(PrivilegedFileHelper.length(file), AbstractPacket.MAX_PACKET_SIZE);

      try
      {
         byte[] buff = new byte[AbstractPacket.MAX_PACKET_SIZE];
         int len;
         long offset = 0;
         AbstractPacket packet;

         // Send first packet in all cases. If InputStream is empty too.
         len = in.read(buff);
         if (len < AbstractPacket.MAX_PACKET_SIZE)
         {
            // cut buffer to original size;
            byte[] b = new byte[len];
            System.arraycopy(buff, 0, b, 0, len);
            buff = b;
         }

         packet =
            new WorkspaceDataPacket(WorkspaceDataPacket.WORKSPACE_DATA_PACKET, totalPacketCount, checkSum, offset, buff);

         channelManager.sendPacket(packet, destinationAddress);

         offset += len;

         while ((len = in.read(buff)) > 0)
         {

            if (len < AbstractPacket.MAX_PACKET_SIZE)
            {
               byte[] b = new byte[len];
               // cut buffer to original size;
               System.arraycopy(buff, 0, b, 0, len);
               buff = b;
            }

            packet =
               new WorkspaceDataPacket(WorkspaceDataPacket.WORKSPACE_DATA_PACKET, totalPacketCount, checkSum, offset,
                  buff);

            channelManager.sendPacket(packet, destinationAddress);

            offset += len;
         }

      }
      finally
      {
         try
         {
            in.close();
         }
         catch (IOException e)
         {
            log.error("Error fo input data stream close. " + e, e);
         }
      }
   }

   /**
    * getPacketCount.
    * 
    * @param contentLength
    *          long, content length
    * @param packetSize
    *          long, the packet size
    * @return long how many packets needs for content
    */
   private long getPacketCount(long contentLength, long packetSize)
   {
      long count = contentLength / packetSize;
      count += ((count * packetSize - contentLength) != 0) ? 1 : 0;
      return count;
   }

}
