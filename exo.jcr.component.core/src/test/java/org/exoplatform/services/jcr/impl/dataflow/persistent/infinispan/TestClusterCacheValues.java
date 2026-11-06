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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Payload;
import org.exoplatform.services.ispn.SharedJGroupsChannelConfigurator;
import org.exoplatform.services.ispn.SharedJGroupsTransport;
import org.exoplatform.services.ispn.Utils;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.PatternQPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.LockData;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.SingletonTokenStream;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The keys and values the replicated caches of the JCR carry, put on a replicated cache of one node and read on the
 * other, with the platform marshaller: the items, the child lists and patterns of the workspace cache, the locks and
 * the index changes, whose documents carry Lucene payloads.
 */
public class TestClusterCacheValues
{
   private static final String STACK = "/conf/jgroups/jgroups-service-tcp.xml";

   private static final String PROPERTY_PREFIX = "exo.service.cluster.jgroups.";

   private static DefaultCacheManager node1;

   private static DefaultCacheManager node2;

   private static Cache<Object, Object> cache1;

   private static Cache<Object, Object> cache2;

   @BeforeClass
   public static void startNodes() throws Exception
   {
      int[] ports = freePorts(2);
      node1 = startNode(stack(ports[0], ports), "node1");
      node2 = startNode(stack(ports[1], ports), "node2");
      cache1 = node1.getCache("replicated");
      cache2 = node2.getCache("replicated");
      for (int i = 0; i < 100 && cache1.getAdvancedCache().getDistributionManager().getCacheTopology().getMembers()
                                       .size() < 2; i++)
      {
         Thread.sleep(100);
      }
      assertEquals(2, cache1.getAdvancedCache().getDistributionManager().getCacheTopology().getMembers().size());
   }

   @AfterClass
   public static void stopNodes()
   {
      if (node2 != null)
      {
         node2.stop();
      }
      if (node1 != null)
      {
         node1.stop();
      }
      for (String name : System.getProperties().stringPropertyNames())
      {
         if (name.startsWith(PROPERTY_PREFIX))
         {
            System.clearProperty(name);
         }
      }
   }

   @Test
   public void testItemArrivesUnderItsKey() throws Exception
   {
      CacheId key = new CacheId("workspace", "node-id");
      PersistedNodeData node = new PersistedNodeData("node-id", QPath.parse("[]:1[]node:1"), Constants.ROOT_UUID, 1, 1,
         Constants.NT_UNSTRUCTURED, new InternalQName[] {Constants.MIX_REFERENCEABLE}, new AccessControlList());

      cache1.put(key, node);
      PersistedNodeData copy = (PersistedNodeData)cache2.get(new CacheId("workspace", "node-id"));
      assertEquals(node.getIdentifier(), copy.getIdentifier());
      assertEquals(node.getQPath(), copy.getQPath());
      assertEquals(node.getPrimaryTypeName(), copy.getPrimaryTypeName());
      assertArrayEquals(node.getMixinTypeNames(), copy.getMixinTypeNames());
   }

   @Test
   public void testChildListsArriveAsTheyAreBuilt()
   {
      Set<Object> children = new HashSet<>(Set.of("child1", "child2"));
      Map<QPathEntryFilter, Set<String>> patterns = new HashMap<>();
      patterns.put(new PatternQPathEntryFilter(new QPathEntry("", "child*", 1)), new HashSet<>(Set.of("child1")));
      Map<Integer, Set<String>> pages = new HashMap<>(Map.of(0, new HashSet<>(Set.of("child1"))));

      for (Object value : List.of(children, patterns, pages))
      {
         cache1.put(value.getClass().getName() + value.hashCode(), value);
         Object copy = cache2.get(value.getClass().getName() + value.hashCode());
         assertEquals(value, copy);
         assertEquals(value.getClass(), copy.getClass());
      }
   }

   @Test
   public void testLockArrives()
   {
      LockData lock = new LockData("node-id", "token-hash", true, false, "owner", 30000);

      cache1.put("node-id", lock);
      assertEquals(lock, cache2.get("node-id"));
   }

   @Test
   public void testIndexChangesArriveWithTheirPayloads() throws Exception
   {
      Document document = new Document();
      document.add(new Field(FieldNames.PROPERTIES, new SingletonTokenStream("value", new Payload(new byte[] {1, 2}))));
      ChangesHolder changes = new ChangesHolder(List.of("removed-id"), List.of(document));
      ChangesFilterListsWrapper wrapper = new ChangesFilterListsWrapper(changes, null);

      cache1.put("changes", wrapper);
      ChangesFilterListsWrapper copy = (ChangesFilterListsWrapper)cache2.get("changes");
      assertEquals(List.of("removed-id"), List.copyOf(copy.getChanges().getRemove()));
      SingletonTokenStream stream =
         (SingletonTokenStream)copy.getChanges().getAdd().iterator().next().getFieldable(FieldNames.PROPERTIES)
                                   .tokenStreamValue();
      assertTrue(stream.incrementToken());
      assertArrayEquals(new byte[] {1, 2}, stream.getAttribute(PayloadAttribute.class).getPayload().getData());
   }

   private static DefaultCacheManager startNode(ProtocolStackConfigurator stack, String nodeName) throws Exception
   {
      ConfigurationBuilderHolder holder = new ConfigurationBuilderHolder();
      GlobalConfigurationBuilder global = holder.getGlobalConfigurationBuilder();
      global.cacheManagerName(nodeName).jmx().enabled(false);
      global.transport()
            .defaultTransport()
            .clusterName("jcr-values-test")
            .nodeName(nodeName)
            .addProperty(JGroupsTransport.CHANNEL_CONFIGURATOR, new SharedJGroupsChannelConfigurator(stack, nodeName));
      Utils.configureSerialization(global, "test");
      holder.newConfigurationBuilder("replicated").clustering().cacheMode(CacheMode.REPL_SYNC);
      return new DefaultCacheManager(holder, true);
   }

   /**
    * The TCP stack the platform ships, its variables resolved for a node bound to the given port and finding its peers
    * on the others.
    */
   private static ProtocolStackConfigurator stack(int port, int[] ports) throws IOException
   {
      StringBuilder hosts = new StringBuilder();
      for (int peer : ports)
      {
         hosts.append(hosts.length() == 0 ? "" : ",").append("127.0.0.1[").append(peer).append(']');
      }
      Map<String, String> properties = Map.of("tcp.bind_addr", "127.0.0.1",
                                              "tcp.bind_port", String.valueOf(port),
                                              "tcpping.initial_hosts", hosts.toString(),
                                              "fd_sock.start_port", "0");
      properties.forEach((name, value) -> System.setProperty(PROPERTY_PREFIX + name, value));
      try (InputStream input = TestClusterCacheValues.class.getResourceAsStream(STACK))
      {
         return SharedJGroupsTransport.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }
   }

   private static int[] freePorts(int count) throws IOException
   {
      int[] ports = new int[count];
      ServerSocket[] sockets = new ServerSocket[count];
      try
      {
         for (int i = 0; i < count; i++)
         {
            sockets[i] = new ServerSocket(0);
            ports[i] = sockets[i].getLocalPort();
         }
      }
      finally
      {
         for (ServerSocket socket : sockets)
         {
            if (socket != null)
            {
               socket.close();
            }
         }
      }
      return ports;
   }
}
