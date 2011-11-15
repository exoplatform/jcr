/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent.cache.jbosscache;

import java.util.HashMap;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.CompressedChangesBuffer;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.JBossCacheWorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache.ChangesContainer;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache.PutObjectContainer;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.BufferedJBossCache.RemoveNodeContainer;
import org.jboss.cache.Fqn;

/**
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z aheritier $
 *
 */
public class TestCompressedChangesBuffer extends TestCase
{

   public void testPutOmit()
   {
      CompressedChangesBuffer buffer = new CompressedChangesBuffer();
      ChangesContainer put1 =
         new PutObjectContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b"),
            new HashMap<String, String>(), null, buffer.getHistoryIndex(), false, false, 0);
      ChangesContainer put2 =
         new PutObjectContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b/c"),
            new HashMap<String, String>(), null, buffer.getHistoryIndex(), false, false, 0);
      ChangesContainer rm1 =
         new RemoveNodeContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b"), null, buffer
            .getHistoryIndex(), false, false, 0);
      buffer.add(put1);
      buffer.add(put2);
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put1));
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put2));
      buffer.add(rm1);
      assertFalse("List still contains put container", buffer.getSortedList().contains(put1));
      assertFalse("List still contains put container", buffer.getSortedList().contains(put2));
      assertTrue("List MUST contain remove container", buffer.getSortedList().contains(rm1));
      buffer.add(put1);
      buffer.add(put2);
      assertTrue("List MUST contain remove container", buffer.getSortedList().contains(rm1));
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put1));
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put2));
   }

   public void testRemoveOmit()
   {
      CompressedChangesBuffer buffer = new CompressedChangesBuffer();
      ChangesContainer put1 =
         new PutObjectContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b"),
            new HashMap<String, String>(), null, buffer.getHistoryIndex(), false, false, 0);

      ChangesContainer put2 =
         new PutObjectContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b/c"),
            new HashMap<String, String>(), null, buffer.getHistoryIndex(), false, false, 0);
      ChangesContainer rm1 =
         new RemoveNodeContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b/c"), null,
            buffer.getHistoryIndex(), false, false, 0);
      ChangesContainer rm2 =
         new RemoveNodeContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES + "/b"), null, buffer
            .getHistoryIndex(), false, false, 0);
      buffer.add(put1);
      buffer.add(put2);
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put1));
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put2));
      buffer.add(rm1);
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put1));
      assertTrue("List MUST contain remove container", buffer.getSortedList().contains(rm1));
      buffer.add(rm2);
      assertTrue("List MUST contain remove container", buffer.getSortedList().contains(rm2));
      assertFalse("List still contains put container", buffer.getSortedList().contains(put1));
      assertFalse("List still contains put container", buffer.getSortedList().contains(put2));
      assertFalse("List still contains put container", buffer.getSortedList().contains(rm1));
   }

   public void testNoChangeOnRegion()
   {
      CompressedChangesBuffer buffer = new CompressedChangesBuffer();
      ChangesContainer put1 =
         new PutObjectContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES_LIST + "/b"),
            new HashMap<String, String>(), null, buffer.getHistoryIndex(), false, false, 0);

      ChangesContainer rm1 =
         new RemoveNodeContainer(Fqn.fromString("/" + JBossCacheWorkspaceStorageCache.CHILD_NODES_LIST + "/b"), null,
            buffer.getHistoryIndex(), false, false, 0);
      buffer.add(put1);
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put1));
      buffer.add(rm1);
      assertTrue("List MUST contain put container", buffer.getSortedList().contains(put1));
      assertTrue("List MUST contain remove container", buffer.getSortedList().contains(rm1));
   }

}
