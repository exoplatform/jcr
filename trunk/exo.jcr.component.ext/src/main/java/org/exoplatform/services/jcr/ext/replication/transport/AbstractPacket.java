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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 15.12.2008
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: AsyncPacket.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public abstract class AbstractPacket implements Externalizable
{

   /**
    * Constant will be used for serialization 'null' value.
    */
   protected static final int NULL_VALUE = -1;

   /**
    * Constant will be used for serialization not 'null' value.
    */
   protected static final int NOT_NULL_VALUE = 1;

   /**
    * serialVersionUID.
    */
   private static final long serialVersionUID = -138895618077433063L;

   /**
    * The definition of max packet size.
    */
   public static final int MAX_PACKET_SIZE = 1024 * 16;

   /**
    * Packet type.
    */
   protected int type;

   /**
    * The priority of transmitter.
    */
   protected int priority;

   /**
    * Packet constructor.
    * 
    * @param type
    *          packet type
    * @param priority
    *          the priority value of transmitters
    */
   public AbstractPacket(int type, int priority)
   {
      this.type = type;
      this.priority = priority;
   }

   /**
    * AbstractPacket  constructor.
    *
    */
   public AbstractPacket()
   {
      this.type = -1;
      this.priority = 0;
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(type);
      out.writeInt(priority);
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      type = in.readInt();
      priority = in.readInt();
   }

   /**
    * getType.
    *
    * @return int
    *           return the packet type
    */
   public int getType()
   {
      return type;
   }

   /**
    * getTransmitterPriority.
    *
    * @return int
    *           return the transmitter priority
    */
   public int getTransmitterPriority()
   {
      return priority;
   }
}
