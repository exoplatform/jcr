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
package org.exoplatform.services.jcr.ext.replication.priority;

import org.exoplatform.services.jcr.ext.replication.Packet;
import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;
import org.exoplatform.services.jcr.ext.replication.transport.MemberAddress;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: PublisherPriorityChecker.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class DynamicPriorityChecker extends AbstractPriorityChecker
{

   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.DynamicPriorityChecker");

   /**
    * The previous participants counter.
    */
   private int previousParticipantsCount;

   /**
    * The previous max priority value.
    */
   private int previousMaxPriority;

   /**
    * DynamicPriorityChecker constructor.
    * 
    * @param channelManager
    *          the ChannelManager
    * @param ownPriority
    *          the own priority
    * @param ownName
    *          the own name
    * @param otherParticipants
    *          the list of names to other participants
    */
   public DynamicPriorityChecker(ChannelManager channelManager, int ownPriority, String ownName,
      List<String> otherParticipants)
   {
      super(channelManager, ownPriority, ownName, otherParticipants);
   }

   /**
    * {@inheritDoc}
    */
   public void receive(AbstractPacket p, MemberAddress sourceAddress)
   {
      Packet packet = (Packet)p;

      if (log.isDebugEnabled())
         log.info(" ------->>> MessageListener.receive(), byte == " + packet.getByteArray());

      try
      {

         if (!ownName.equals(packet.getOwnerName()))
            switch (packet.getPacketType())
            {
               case Packet.PacketType.GET_ALL_PRIORITY :
                  Packet pktMyPriority =
                     new Packet(Packet.PacketType.OWN_PRIORITY, ownName, (long)ownPriority, packet.getIdentifier());
                  super.waitView();
                  channelManager.sendPacket(pktMyPriority);
                  break;

               case Packet.PacketType.OWN_PRIORITY :
                  if (identifier != null && identifier.equals(packet.getIdentifier()))
                  {
                     currentParticipants.put(packet.getOwnerName(), Integer.valueOf((int)packet.getSize()));

                     if (log.isDebugEnabled())
                     {
                        log.info(channelManager.getChannel().getClusterName() + " : " + identifier
                           + " : added member :");
                        log.info("   +" + packet.getOwnerName() + ":" + currentParticipants.get(packet.getOwnerName()));
                     }
                  }

                  if (log.isDebugEnabled())
                     printOnlineMembers();

                  break;
               default :
                  break;
            }
      }
      catch (Exception e)
      {
         log.error("An error in processing packet : ", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void informAll()
   {
      previousParticipantsCount = currentParticipants.size() + 1;
      previousMaxPriority = getCurrentMaxPriority();

      super.informAll();
   }

   /**
    * getCurrentMaxPriority.
    * 
    * @return int return the current max value
    */
   private int getCurrentMaxPriority()
   {
      int max = Integer.MIN_VALUE;

      for (String nodeName : currentParticipants.keySet())
         if (currentParticipants.get(nodeName) > max)
            max = currentParticipants.get(nodeName);

      if (ownPriority > max)
         max = ownPriority;

      return max == Integer.MIN_VALUE ? ownPriority : max;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMaxPriority()
   {
      if (otherParticipants.size() == 1)
         return ownPriority == MAX_PRIORITY;
      else if (otherParticipants.size() > 1 && currentParticipants.size() == 0 && ownPriority == MAX_PRIORITY)
         return false;
      else if (otherParticipants.size() > 1 && currentParticipants.size() == 0 && previousMaxPriority != ownPriority
         && previousParticipantsCount > 1)
         return false;
      else
         return true;
   }

   /**
    * {@inheritDoc}
    */
   public void onError(MemberAddress sourceAddress)
   {
   }
}
