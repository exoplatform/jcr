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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.util.UUID;
import org.junit.Test;

import org.exoplatform.services.ispn.ClusterJavaSerializationMarshaller;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.jcr.impl.quota.ChangesItems;
import org.exoplatform.services.rpc.RPCException;

/**
 * One request and one reply of each command the JCR core registers, built as its callers and its execution build
 * them, through the marshaller of the cluster; then the failures its execution throws.
 */
public class TestRPCCommandPayloads
{
   private static final String WS = "repository_workspace";

   private final ClusterJavaSerializationMarshaller marshaller = new ClusterJavaSerializationMarshaller();

   private final Address member = UUID.randomUUID();

   @Test
   public void testEachCommandMakesTheRoundTrip() throws Exception
   {
      String suffix = WS + "-true";
      List<Object[]> commands = List.of(
         command("org.exoplatform.services.jcr.impl.WorkspaceResumer-requestForResponsibilityForResuming-" + WS,
                 Boolean.TRUE),
         command("exo.jcr.component.core.RepositoryCreationSynchronizer-shouldIWaitCommand", Boolean.FALSE),
         command("exo.jcr.component.core.RepositoryCreationSynchronizer-needToInitWorkspaceCommand", null),
         command("exo.jcr.component.core.RepositoryCreationSynchronizer-releaseCommand", null),
         command("org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager-suspend-" + WS, null),
         command("org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager-resume-" + WS, null),
         command("org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager"
            + "-requestForResponsibilityForResuming-" + WS, Boolean.TRUE),
         command("org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl-registerNamespace-repository", Boolean.TRUE,
                 "registry-id", "exo", "http://www.exoplatform.com/jcr/exo/1.0"),
         command("org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl-unregisterNamespace-repository",
                 Boolean.TRUE, "registry-id", "exo"),
         command("ChangesListener-" + WS + "-applyPersistedChangesTask", null, ChangesItems.sample()),
         command("org.exoplatform.services.jcr.impl.core.query.SearchManager-suspend-" + suffix, null),
         command("org.exoplatform.services.jcr.impl.core.query.SearchManager-resume-" + suffix, null),
         command("org.exoplatform.services.jcr.impl.core.query.SearchManager-requestForResponsibilityForResuming-"
            + suffix, Boolean.FALSE),
         command("org.exoplatform.services.jcr.impl.core.query.SearchManager-changeIndexerState-" + suffix, null,
                 Boolean.FALSE, Boolean.FALSE),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-changeIndexMode-" + suffix, null,
                 Boolean.TRUE),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-getIndexList-" + suffix,
                 new ArrayList<>(List.of("1_0/_0.cfs", "1_0/segments_1"))),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-getIndexFile-" + suffix,
                 new byte[] {1, 2, 3}, "1_0/_0.cfs", Long.valueOf(4096)),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-prepareIndexesForCopy-" + suffix,
                 "indexes1234.zip"),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-cleanupIndexesAfterCopy-" + suffix,
                 Boolean.TRUE, "indexes1234.zip"),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-getAllIndexes-" + suffix, new byte[0],
                 "indexes1234.zip", Long.valueOf(0)),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-getCoordinatorAddress-" + suffix,
                 "127.0.0.1"),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-requestForResponsibleToSetIndexOnline-"
            + suffix, Boolean.TRUE),
         command("org.exoplatform.services.jcr.impl.core.query.IndexRecoveryImpl-checkIndexIsReady-" + suffix,
                 Boolean.TRUE),
         command("org.exoplatform.services.jcr.impl.core.query.lucene.DocNumberRecoveryFilter-getDocNum-" + suffix,
                 Integer.valueOf(42)),
         command("org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDataManagerImpl-registerNodeTypes-repository",
                 Boolean.TRUE, "registry-id", new String[] {"exo:article", "exo:folder"}),
         command("org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDataManagerImpl-unregisterNodeType-repository",
                 Boolean.TRUE, "registry-id", "exo:article"));

      for (Object[] command : commands)
      {
         String id = (String)command[0];
         Serializable[] args = (Serializable[])command[2];
         AbstractRPCService.MessageBody request =
            (AbstractRPCService.MessageBody)marshaller.objectFromByteBuffer(AbstractRPCService.toRequest(marshaller,
               new AbstractRPCService.MessageBody(null, id, args)));
         assertEquals(id, request.getCommandId());
         assertEquals(id, args.length, request.getArgs().length);
         for (int i = 0; i < args.length; i++)
         {
            assertSame(id, args[i], request.getArgs()[i]);
         }
         assertSame(id, command[1], AbstractRPCService.fromReply(marshaller,
            AbstractRPCService.toReply(marshaller, command[1], id), member));
      }
   }

   @Test
   public void testEachFailureOfACommandReachesTheCallerWithItsType() throws Exception
   {
      for (Throwable cause : List.of(new SuspendException("The workspace cannot be suspended"),
                                     new ResumeException("The workspace cannot be resumed"),
                                     new RepositoryConfigurationException("The configuration is refused"),
                                     new IOException("The index file cannot be read"),
                                     // A cause of the JDK runtime
                                     new IllegalStateException("The index is not ready")))
      {
         RPCException failure = new RPCException("Cannot execute the command command-id", cause);

         Object received =
            AbstractRPCService.fromReply(marshaller, AbstractRPCService.toReply(marshaller, failure, "command-id"),
                                         member);

         assertTrue(String.valueOf(received), received instanceof RPCException);
         Throwable receivedCause = ((Throwable)received).getCause();
         assertEquals(cause.getClass(), receivedCause.getClass());
         assertEquals(cause.getMessage(), receivedCause.getMessage());
      }
   }

   private static Object[] command(String id, Serializable reply, Serializable... args)
   {
      return new Object[] {id, reply, args};
   }

   /**
    * The same value, compared by content: arrays by their elements, the quota changes by the size they carry.
    */
   private static void assertSame(String id, Object expected, Object actual)
   {
      if (expected instanceof byte[])
      {
         assertArrayEquals(id, (byte[])expected, (byte[])actual);
      }
      else if (expected instanceof Object[])
      {
         assertArrayEquals(id, (Object[])expected, (Object[])actual);
      }
      else if (expected != null && expected.getClass().getName().endsWith(".ChangesItem"))
      {
         assertEquals(id, expected.getClass(), actual.getClass());
         assertEquals(id, ChangesItems.workspaceChangedSize((Serializable)expected),
                      ChangesItems.workspaceChangedSize((Serializable)actual));
      }
      else
      {
         assertEquals(id, expected, actual);
      }
   }
}
