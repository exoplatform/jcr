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
package org.exoplatform.services.rpc.jgv3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.jgroups.Address;
import org.jgroups.BytesMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.ispn.ClusterJavaSerializationMarshaller;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.services.rpc.impl.AbstractRPCService.MessageBody;

/**
 * Two nodes in one JVM, each on the shipped JCR TCP stack bound to its own port: the commands, their results and
 * their failures cross the wire as they do between the nodes of a cluster.
 */
public class RPCServiceImplTest
{
   private static final String STACK = "jar:/conf/jcr/jgroups/jgroups-jcr-tcp.xml";

   private static final String PROPERTY_PREFIX = "exo.jcr.cluster.jgroups.";

   private static final String NODE_NAME = "exo.cluster.node.name";

   private final Map<String, String> formerProperties = new HashMap<>();

   private final List<Node> nodes = new ArrayList<>();

   private Node node1;

   private Node node2;

   @Before
   public void setUp() throws Exception
   {
      int[] ports = freePorts(2);
      node1 = new Node("node1", ports[0], ports);
      node2 = new Node("node2", ports[1], ports);
   }

   @After
   public void tearDown()
   {
      for (int i = nodes.size() - 1; i >= 0; i--)
      {
         nodes.get(i).stop();
      }
      formerProperties.forEach((name, value) -> {
         if (value == null)
         {
            System.clearProperty(name);
         }
         else
         {
            System.setProperty(name, value);
         }
      });
      PropertyManager.refresh();
   }

   @Test
   public void testResultsOfEveryNodeReachTheCaller() throws Exception
   {
      RemoteCommand bytes = node1.register("bytes", (node, args) -> (node + ":" + args[0]).getBytes(StandardCharsets.UTF_8));
      node2.register("bytes", (node, args) -> (node + ":" + args[0]).getBytes(StandardCharsets.UTF_8));
      RemoteCommand list = node1.register("list", (node, args) -> new ArrayList<>(List.of(node, args[0])));
      node2.register("list", (node, args) -> new ArrayList<>(List.of(node, args[0])));
      startNodes();

      Set<String> replies = new HashSet<>();
      for (Object reply : node1.executeCommandOnAllNodes(bytes, true, "arg"))
      {
         replies.add(new String((byte[])reply, StandardCharsets.UTF_8));
      }
      assertEquals(Set.of("node1:arg", "node2:arg"), replies);
      assertEquals(Set.of(List.of("node1", "arg"), List.of("node2", "arg")),
                   new HashSet<>(node1.executeCommandOnAllNodes(list, true, "arg")));
   }

   @Test
   public void testFailureWithACauseOffTheAllowListReachesTheCallerDescribed() throws Exception
   {
      RemoteCommand failing = node1.register("failing", (node, args) -> {
         throw new RepositoryException("The item of " + node + " is gone");
      });
      node2.register("failing", (node, args) -> {
         throw new RepositoryException("The item of " + node + " is gone");
      });
      startNodes();

      List<Object> replies = node1.executeCommandOnAllNodes(failing, true);

      assertEquals(2, replies.size());
      Set<String> causes = new HashSet<>();
      for (Object reply : replies)
      {
         // The cause, a javax.jcr class, cannot be read by the caller: the reply describes it
         RPCException failure = (RPCException)reply;
         assertTrue(failure.getMessage(), failure.getMessage().startsWith("Cannot execute the command failing, caused by "));
         causes.add(failure.getMessage().substring(failure.getMessage().indexOf("caused by ") + 10));
         assertEquals(RPCServiceImplTest.class.getName(), failure.getStackTrace()[0].getClassName().replaceAll("\\$.*", ""));
      }
      assertEquals(Set.of("javax.jcr.RepositoryException: The item of node1 is gone",
                          "javax.jcr.RepositoryException: The item of node2 is gone"),
                   causes);
   }

   @Test
   public void testCommandUnknownToANodeIsAnsweredByAFailure() throws Exception
   {
      RemoteCommand localOnly = node1.register("localOnly", (node, args) -> "done by " + node);
      startNodes();

      List<Object> replies = node1.executeCommandOnAllNodes(localOnly, true);

      assertEquals(2, replies.size());
      assertTrue(replies.toString(), replies.contains("done by node1"));
      Object failure = replies.get(replies.indexOf("done by node1") == 0 ? 1 : 0);
      assertEquals("Command localOnly unkown, please register your command first", ((RPCException)failure).getMessage());
   }

   @Test
   public void testCommandReadFromThePartOfABufferItIsIn() throws Exception
   {
      node2.register("bytes", (node, args) -> (node + ":" + args[0]).getBytes(StandardCharsets.UTF_8));
      startNodes();
      ClusterJavaSerializationMarshaller marshaller = new ClusterJavaSerializationMarshaller();
      byte[] body = marshaller.objectToByteBuffer(new MessageBody(null, "bytes", new Serializable[]{"arg"}));
      // The message of a batch shares the buffer of the others
      byte[] buffer = new byte[body.length + 10];
      System.arraycopy(body, 0, buffer, 7, body.length);

      Object reply = node2.handle(new BytesMessage(null, buffer, 7, body.length));

      assertArrayEquals("node2:arg".getBytes(StandardCharsets.UTF_8),
                        (byte[])marshaller.objectFromByteBuffer((byte[])reply));
   }

   private void startNodes() throws InterruptedException
   {
      node1.startNode();
      node2.startNode();
      for (int i = 0; i < 100 && (node1.memberCount() < 2 || node2.memberCount() < 2); i++)
      {
         Thread.sleep(100);
      }
      assertEquals(2, node1.memberCount());
      assertEquals(2, node2.memberCount());
   }

   private void setProperty(String name, String value)
   {
      formerProperties.putIfAbsent(name, System.getProperty(name));
      PropertyManager.setProperty(name, value);
   }

   private static int[] freePorts(int count) throws IOException
   {
      int[] ports = new int[count];
      List<ServerSocket> sockets = new ArrayList<>();
      try
      {
         for (int i = 0; i < count; i++)
         {
            ServerSocket socket = new ServerSocket(0);
            sockets.add(socket);
            ports[i] = socket.getLocalPort();
         }
      }
      finally
      {
         for (ServerSocket socket : sockets)
         {
            socket.close();
         }
      }
      return ports;
   }

   private interface Body
   {
      Serializable run(String node, Serializable[] args) throws Throwable;
   }

   /**
    * A node of the cluster: its stack is read when it is created, with the port it binds and the ports of its peers.
    */
   private class Node extends RPCServiceImpl
   {
      private final String name;

      private Node(String name, int port, int[] ports)
      {
         super(new ExoContainerContext(null, "portal"), stackParameters(name, port, ports), new ConfigurationManagerImpl());
         this.name = name;
      }

      private RemoteCommand register(String id, Body body)
      {
         return registerCommand(new RemoteCommand()
         {
            public String getId()
            {
               return id;
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               return body.run(name, args);
            }
         });
      }

      private void startNode()
      {
         setProperty(NODE_NAME, name);
         nodes.add(this);
         start();
      }

      private int memberCount()
      {
         List<Address> current = members;
         return current == null ? 0 : current.size();
      }
   }

   private InitParams stackParameters(String name, int port, int[] ports)
   {
      StringBuilder hosts = new StringBuilder();
      for (int peer : ports)
      {
         hosts.append(hosts.length() == 0 ? "" : ",").append("127.0.0.1[").append(peer).append(']');
      }
      setProperty(PROPERTY_PREFIX + "tcp.bind_addr", "127.0.0.1");
      setProperty(PROPERTY_PREFIX + "tcp.bind_port", String.valueOf(port));
      setProperty(PROPERTY_PREFIX + "tcpping.initial_hosts", hosts.toString());
      InitParams params = new InitParams();
      params.addParameter(valueParam("jgroups-configuration", STACK));
      params.addParameter(valueParam("jgroups-cluster-name", "RPCServiceImplTest"));
      params.addParameter(valueParam("jgroups-default-timeout", "10000"));
      return params;
   }

   private static ValueParam valueParam(String name, String value)
   {
      ValueParam param = new ValueParam();
      param.setName(name);
      param.setValue(value);
      return param;
   }
}
