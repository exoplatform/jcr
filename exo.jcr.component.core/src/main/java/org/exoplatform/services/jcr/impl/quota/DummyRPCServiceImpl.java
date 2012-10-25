/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.TopologyChangeListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dummy implementation. Executes all commands synchronously and locally.
 * The main idea to have such kind of implementation is in avoiding
 * devided logic with and without existence {@link RPCService} instance.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: DummyRPCServiceImpl.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DummyRPCServiceImpl implements RPCService
{

   /**
    * {@inheritDoc}
    */
   public List<Object> executeCommandOnAllNodes(RemoteCommand command, boolean synchronous, Serializable... args)
      throws RPCException, SecurityException
   {
      List<Object> result = new ArrayList<Object>(1);
      result.add(executeCommand(command, args));

      return result;
   }

   /**
    * {@inheritDoc}
    */
   public List<Object> executeCommandOnAllNodes(RemoteCommand command, long timeout, Serializable... args)
      throws RPCException, SecurityException
   {
      List<Object> result = new ArrayList<Object>(1);
      result.add(executeCommand(command, args));

      return result;
   }

   /**
    * {@inheritDoc}
    */
   public Object executeCommandOnCoordinator(RemoteCommand command, boolean synchronous, Serializable... args)
      throws RPCException, SecurityException
   {
      return executeCommand(command, args);
   }

   /**
    * {@inheritDoc}
    */
   public Object executeCommandOnCoordinator(RemoteCommand command, long timeout, Serializable... args)
      throws RPCException, SecurityException
   {
      return executeCommand(command, args);
   }

   /**
    * {@inheritDoc}
    */
   public RemoteCommand registerCommand(RemoteCommand command) throws SecurityException
   {
      return command;
   }

   /**
    * {@inheritDoc}
    */
   public void unregisterCommand(RemoteCommand command) throws SecurityException
   {
   }

   /**
    * {@inheritDoc}
    */
   public boolean isCoordinator() throws RPCException
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void registerTopologyChangeListener(TopologyChangeListener listener) throws SecurityException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void unregisterTopologyChangeListener(TopologyChangeListener listener) throws SecurityException
   {
   }

   /**
    * Command executing.
    */
   private Serializable executeCommand(RemoteCommand command, Serializable... args) throws RPCException
   {
      try
      {
         return command.execute(args);
      }
      catch (Throwable e)
      {
         throw new RPCException(e.getMessage(), e);
      }
   }
}
