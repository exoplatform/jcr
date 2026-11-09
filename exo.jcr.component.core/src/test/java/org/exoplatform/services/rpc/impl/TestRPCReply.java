/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.rpc.impl;

import junit.framework.TestCase;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jgroups.Address;
import org.jgroups.util.UUID;

import org.exoplatform.services.ispn.ClusterJavaSerializationMarshaller;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.rpc.RPCException;

/**
 * The reply of a failed command keeps the types of its chain the caller rethrows, a cause off the cluster
 * serialization allow-list described instead.
 */
public class TestRPCReply extends TestCase
{
   private final ClusterJavaSerializationMarshaller marshaller = new ClusterJavaSerializationMarshaller();

   public void testAReplyThisNodeRefusesIsAFailureOfItsMember() throws Exception
   {
      // A member whose allow-list differs replies with a class this node refuses
      ByteArrayOutputStream reply = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(reply))
      {
         out.writeObject(new Point(1, 2));
      }
      Address member = UUID.randomUUID();

      Object received = AbstractRPCService.fromReply(marshaller, reply.toByteArray(), member);

      assertTrue(String.valueOf(received), received instanceof RPCException);
      String message = ((Throwable)received).getMessage();
      assertTrue(message, message.contains(member.toString()));
      assertTrue(message, message.contains(Point.class.getName()));
   }

   public void testARequestAndAReplyNameTheirCommandInTheSizeWarning() throws Exception
   {
      List<String> sources = new ArrayList<>();
      ClusterJavaSerializationMarshaller recording = new ClusterJavaSerializationMarshaller()
      {
         @Override
         protected boolean warnIfOversized(int size, Supplier<String> source)
         {
            sources.add(source.get());
            return super.warnIfOversized(size, source);
         }
      };

      AbstractRPCService.toRequest(recording, new AbstractRPCService.MessageBody(null, "create",
                                                                                 new Serializable[] {"workspace"}));
      AbstractRPCService.toReply(recording, "created", "create");

      assertEquals(Arrays.asList("the request of the RPC command create", "the reply of the RPC command create"),
                   sources);
   }

   public void testAFailureKeepsTheTypesOfItsChainOnTheList() throws Exception
   {
      RPCException failure = new RPCException("Cannot execute the command create",
         new RepositoryConfigurationException("configuration refused", new javax.jcr.RepositoryException("jcr failure")));

      Object received = marshaller.objectFromByteBuffer(AbstractRPCService.toReply(marshaller, failure, "create"));

      assertTrue(received instanceof RPCException);
      Throwable cause = ((Throwable)received).getCause();
      assertTrue(cause instanceof RepositoryConfigurationException);
      assertEquals("configuration refused", cause.getMessage());
      assertTrue(cause.getCause() instanceof RPCException);
      assertTrue(cause.getCause().getMessage().contains("jcr failure"));
   }

   public void testTheCausesBelowAThrowableOffTheListTravelToo() throws Exception
   {
      RPCException failure = new RPCException("Cannot execute the command create",
         new javax.jcr.RepositoryException("jcr failure", new RepositoryConfigurationException("bad config")));

      Object received = marshaller.objectFromByteBuffer(AbstractRPCService.toReply(marshaller, failure, "create"));

      Throwable described = ((Throwable)received).getCause();
      assertTrue(described instanceof RPCException);
      assertTrue(described.getMessage().contains("jcr failure"));
      assertTrue(String.valueOf(described.getCause()), described.getCause() instanceof RepositoryConfigurationException);
      assertEquals("bad config", described.getCause().getMessage());
   }

   public void testAReplyOffTheListWhoseChainLoopsIsDescribedOnce() throws Exception
   {
      CarryingException failure = new CarryingException("Cannot execute the command create");
      IllegalStateException inner = new IllegalStateException("inner failure");
      failure.initCause(inner);
      inner.initCause(failure);

      ExecutorService executor = Executors.newSingleThreadExecutor();
      try
      {
         Future<byte[]> reply = executor.submit(() -> AbstractRPCService.toReply(marshaller, failure, "create"));
         Object received = marshaller.objectFromByteBuffer(reply.get(30, TimeUnit.SECONDS));

         assertTrue(received instanceof RPCException);
         assertEquals("Cannot execute the command create, caused by " + inner, ((Throwable)received).getMessage());
         assertTrue(Arrays.equals(failure.getStackTrace(), ((Throwable)received).getStackTrace()));
      }
      finally
      {
         executor.shutdownNow();
      }
   }

   /**
    * A failure on the list that holds a value off it: the reply cannot travel with its types.
    */
   static class CarryingException extends Exception
   {
      private static final long serialVersionUID = 1L;

      private final Vector<String> payload = new Vector<>();

      CarryingException(String message)
      {
         super(message);
      }
   }
}
