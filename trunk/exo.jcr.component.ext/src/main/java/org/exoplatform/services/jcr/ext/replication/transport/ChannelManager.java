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
package org.exoplatform.services.jcr.ext.replication.transport;

import org.exoplatform.services.jcr.ext.replication.ReplicationException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 12.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ChannelManager.java 31925 2009-05-19 07:40:04Z rainf0x $
 */
public class ChannelManager implements RequestHandler, MembershipListener
{

   /**
    * The initialized state.
    */
   public static final int INITIALIZED = 1;

   /**
    * The connected state.
    */
   public static final int CONNECTED = 2;

   /**
    * The disconnected state.
    */
   public static final int DISCONNECTED = 3;

   /**
    * log. the apache logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.AsyncChannelManager");

   /**
    * State of async channel manager {INITIALIZED, CONNECTED, DISCONNECTED}.
    */
   protected int state;

   /**
    * The JChanel.
    */
   protected JChannel channel;

   /**
    * dispatcher. The MessageDispatcher will be transmitted the Massage.
    */
   protected MessageDispatcher dispatcher;

   /**
    * channelConfig. The configuration to JChannel.
    */
   protected final String channelConfig;

   /**
    * channelName. The name to channel.
    */
   protected final String channelName;

   /**
    * Members count according the configuration (other-participants-priority).
    */
   private final int confMembersCount;

   /**
    * Packet listeners.
    */
   private List<PacketListener> packetListeners;

   /**
    * Channel state listeners.
    */
   private List<StateListener> stateListeners;

   /**
    * Channel connection sate listeners.
    */
   private final List<ConnectionListener> connectionListeners;

   /**
    * Packets handler.
    */
   protected final PacketHandler packetsHandler;

   /**
    * This latch will be used for sending pocket after successful connection.
    */
   private CountDownLatch latch;

   /**
    * MemberPacket.
    *
    */
   class MemberPacket
   {
      /**
       * packet.
       */
      final AbstractPacket packet;

      /**
       * member.
       */
      final MemberAddress member;

      /**
       * MemberPacket  constructor.
       *
       * @param packet
       *          AbstractPacket, the packet
       * @param member
       *          MemebrAddress, the member address
       */
      MemberPacket(AbstractPacket packet, MemberAddress member)
      {
         this.packet = packet;
         this.member = member;
      }
   }

   /**
    * PacketHandler.
    *
    */
   protected class PacketHandler extends Thread
   {

      /**
       * Wait lock.
       */
      private final Object lock = new Object();

      /**
       * Packets queue.
       */
      private final ConcurrentLinkedQueue<MemberPacket> queue = new ConcurrentLinkedQueue<MemberPacket>();

      /**
       * User flag.
       */
      private MemberPacket current;

      /**
       * {@inheritDoc}
       */
      @Override
      public void run()
      {
         while (true)
         {
            try
            {
               synchronized (lock)
               {
                  current = queue.poll();
                  while (current != null)
                  {
                     PacketListener[] pl = packetListeners.toArray(new PacketListener[packetListeners.size()]);
                     for (PacketListener handler : pl)
                        handler.receive(current.packet, current.member);

                     current = queue.poll();
                  }

                  lock.wait();
               }
            }
            catch (InterruptedException e)
            {
               LOG.error("Cannot handle the queue. Wait lock failed " + e, e);
            }
            catch (Throwable e)
            {
               LOG.error("Cannot handle the queue now. Error " + e, e);
               try
               {
                  sleep(5000);
               }
               catch (Throwable e1)
               {
                  LOG.error("Sleep error " + e1);
               }
            }
         }
      }

      /**
       * Add packet to the queue.
       * 
       * @param packet
       *          AbstractPacket
       * @param member
       *          Member
       */
      public void add(AbstractPacket packet, MemberAddress member)
      {
         queue.add(new MemberPacket(packet, member));
      }

      /**
       * Run handler if channel is ready.
       * 
       */
      public void handle()
      {

         if (current == null)
         {
            synchronized (lock)
            {
               lock.notify();
            }

            // JCR-886: let other threads work
            Thread.yield();
         }
         else if (LOG.isDebugEnabled())
            LOG.debug("Handler already active, queue size : " + queue.size());
      }
   }

   /**
    * ChannelManager constructor.
    * 
    * @param channelConfig
    *          channel configuration
    * @param channelName
    *          name of channel
    * @param confMembersCount
    *          the how many members was configured
    */
   public ChannelManager(String channelConfig, String channelName, int confMembersCount)
   {
      this.state = INITIALIZED;
      this.channelConfig = channelConfig;
      this.channelName = channelName;
      this.confMembersCount = confMembersCount;

      this.packetListeners = new ArrayList<PacketListener>();
      this.stateListeners = new ArrayList<StateListener>();
      this.connectionListeners = new ArrayList<ConnectionListener>();

      this.packetsHandler = new PacketHandler();
      this.packetsHandler.start();
   }

   /**
    * Tell if manager is connected to the channel and ready to work.
    * 
    * @return boolean, true if connected
    */
   public boolean isConnected()
   {
      return channel != null;
   }

   /**
    * Connect to channel.
    * 
    * @throws ReplicationException
    *           Will be generated the ReplicationException.
    */
   public void connect() throws ReplicationException
   {

      try
      {
         if (channel == null)
         {
            latch = new CountDownLatch(1);

            channel = new JChannel(channelConfig);

            channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
            channel.setOpt(Channel.AUTO_GETSTATE, Boolean.TRUE);

            dispatcher = new MessageDispatcher(channel, null, null, null);

            dispatcher.setRequestHandler(this);
            dispatcher.setMembershipListener(this);
         }
      }
      catch (ChannelException e)
      {
         throw new ReplicationException("Can't create JGroups channel", e);
      }

      LOG.info("Channel name : " + channelName);

      try
      {
         channel.connect(channelName);
         this.state = CONNECTED;
      }
      catch (ChannelException e)
      {
         throw new ReplicationException("Can't connect to JGroups channel", e);
      }
      finally
      {
         latch.countDown();
      }
   }

   /**
    * closeChannel. Close the channel.
    */
   public synchronized void disconnect()
   {
      this.state = DISCONNECTED;

      if (dispatcher != null)
      {
         dispatcher.setRequestHandler(null);
         dispatcher.setMembershipListener(null);
         dispatcher.stop();
         dispatcher = null;

         if (LOG.isDebugEnabled())
            LOG.debug("dispatcher stopped");
         try
         {
            Thread.sleep(3000);
         }
         catch (InterruptedException e)
         {
            LOG.error("The interapted on disconnect : " + e, e);
         }
      }

      if (channel != null)
      {
         channel.disconnect();

         if (LOG.isDebugEnabled())
            LOG.debug("channel disconnected");
         try
         {
            Thread.sleep(5000);
         }
         catch (InterruptedException e)
         {
            LOG.error("The interapted on disconnect : " + e, e);
         }

         channel.close();
         channel = null;

         if (LOG.isDebugEnabled())
            LOG.debug("Disconnect done, fire connection listeners");

         for (ConnectionListener cl : connectionListeners)
         {
            cl.onDisconnect();
         }
      }
   }

   /**
    * addPacketListener.
    * 
    * @param packetListener
    *          add the PacketListener
    */
   public void addPacketListener(PacketListener packetListener)
   {
      this.packetListeners.add(packetListener);
   }

   /**
    * Remove PacketListener.
    * 
    * @param packetListener
    *          add the PacketListener
    */
   public void removePacketListener(PacketListener packetListener)
   {
      this.packetListeners.remove(packetListener);
   }

   /**
    * Add channel state listener (AsynInitializer).
    * 
    * @param listener
    *          StateListener
    */
   public void addStateListener(StateListener listener)
   {
      this.stateListeners.add(listener);
   }

   /**
    * Remove SatateListener.
    *
    * @param listener
    *          StateListener
    */
   public void removeStateListener(StateListener listener)
   {
      this.stateListeners.remove(listener);
   }

   /**
    * Add connection sate listener.
    * 
    * @param listener
    *          ConnectionListener
    */
   public void addConnectionListener(ConnectionListener listener)
   {
      this.connectionListeners.add(listener);
   }

   /**
    * Remove connection listener.
    *
    * @param listener
    *          ConnectionListener
    */
   public void removeConnectionListener(ConnectionListener listener)
   {
      this.connectionListeners.remove(listener);
   }

   /**
    * getDispatcher.
    * 
    * @return MessageDispatcher return the MessageDispatcher object
    */
   public MessageDispatcher getDispatcher()
   {
      return dispatcher;
   }

   /**
    * getOtherMembers.
    * 
    * @return List list of other members.
    */
   public List<MemberAddress> getOtherMembers()
   {
      List<Address> list = new ArrayList<Address>(channel.getView().getMembers());
      list.remove(channel.getLocalAddress());

      List<MemberAddress> members = new ArrayList<MemberAddress>();

      for (Address address : list)
         members.add(new MemberAddress(address));

      return members;
   }

   /**
    * sendPacket.
    * 
    * @param packet
    *          the Packet with content
    * @param destinations
    *          the destination addresses
    * @throws IOException
    *           will be generated Exception
    */
   public void sendPacket(AbstractPacket packet, MemberAddress... destinations) throws IOException
   {
      if (latch != null && latch.getCount() != 0)
      {
         try
         {
            latch.await();
         }
         catch (InterruptedException e)
         {
            throw new RuntimeException(e);
         }
      }

      if (state == CONNECTED)
      {
         Vector<Address> dest = new Vector<Address>();
         for (MemberAddress address : destinations)
            dest.add(address.getAddress());

         sendPacket(packet, dest);
      }
      else if (state == INITIALIZED)
         throw new ChannelNotConnectedException("The channel is not connected.");
      else
         throw new ChannelWasDisconnectedException("The channel was disconnected.");
   }

   /**
    * Send packet using Vector of dests.
    * 
    * @param packet
    *          AbstractPacket
    * @param dest
    *          Vector of Address
    * @throws IOException
    *           if error
    */
   private void sendPacket(AbstractPacket packet, Vector<Address> dest) throws IOException
   {
      if (state == CONNECTED)
      {
         byte[] buffer = PacketTransformer.getAsByteArray(packet);

         Message msg = new Message(null, null, buffer);

         if (state == DISCONNECTED || dispatcher == null)
            throw new ChannelWasDisconnectedException("The channel was disconnected.");

         dispatcher.castMessage(dest, msg, GroupRequest.GET_NONE, 0);
      }
   }

   /**
    * Send packet to all members.
    * 
    * @param packet
    *          the Packet with contents
    * @throws IOException
    *           will be generated Exception
    */
   public void sendPacket(AbstractPacket packet) throws IOException
   {
      if (latch != null && latch.getCount() != 0)
      {
         try
         {
            latch.await();
         }
         catch (InterruptedException e)
         {
            throw new RuntimeException(e);
         }
      }

      if (state == CONNECTED)
      {
         Vector<Address> dest = new Vector<Address>(channel.getView().getMembers());
         dest.remove(channel.getLocalAddress());

         sendPacket(packet, dest);
      }
      else if (state == INITIALIZED)
         throw new ChannelNotConnectedException("The channel is not connected.");
      else
         throw new ChannelWasDisconnectedException("The channel was disconnected.");
   }

   /**
    * getChannel.
    * 
    * @return JChannel return the JChannel object
    */
   public JChannel getChannel()
   {
      return channel;
   }

   // ************ RequestHandler **********

   /**
    * {@inheritDoc}
    */
   public Object handle(final Message message)
   {
      if (isConnected())
      {
         try
         {
            packetsHandler.add(PacketTransformer.getAsPacket(message.getBuffer()), new MemberAddress(message.getSrc()));

            if (channel.getView() != null)
            {
               if (channel.getView().getMembers().size() == confMembersCount)
                  // TODO run without one (few) members will not work, see LastMemberWaiter in initializer
                  packetsHandler.handle();
               else
                  LOG.warn("Not all members connected to the channel " + +channel.getView().getMembers().size()
                     + " != " + confMembersCount + ", queue message " + message);
            }
            else
               LOG.warn("No members found or channel closed, queue message " + message);

            return new String("Success");
         }
         catch (IOException e)
         {
            LOG.error("Message handler error " + e, e);
            return e.getMessage();
         }
         catch (ClassNotFoundException e)
         {
            LOG.error("Message handler error " + e, e);
            return e.getMessage();
         }
      }
      else
      {
         LOG.warn("Channel is closed but message received " + message);
         return new String("Disconnected");
      }
   }

   // ******** MembershipListener ***********

   /**
    * {@inheritDoc}
    */
   public void viewAccepted(View view)
   {
      if (isConnected())
      {
         LOG.info("View accepted " + view.printDetails());

         ArrayList<MemberAddress> members = new ArrayList<MemberAddress>();

         for (Address address : view.getMembers())
            members.add(new MemberAddress(address));

         StateEvent event = new StateEvent(new MemberAddress(channel.getLocalAddress()), members);

         for (StateListener listener : stateListeners)
            listener.onStateChanged(event);

         // check if we have data to be propagated to the synchronization
         packetsHandler.handle();
      }
      else
         LOG.warn("Channel is closed but View accepted " + view.printDetails());
   }

   /**
    * {@inheritDoc}
    */
   public void block()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void suspect(Address arg0)
   {
   }

   // *****************************************

}
