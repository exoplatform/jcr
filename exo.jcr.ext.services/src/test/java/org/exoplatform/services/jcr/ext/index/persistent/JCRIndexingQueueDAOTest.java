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
package org.exoplatform.services.jcr.ext.index.persistent;

import java.util.Calendar;
import java.util.UUID;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.jcr.ext.index.persistent.entity.JCRIndexQueueEntity;
import org.exoplatform.services.jcr.ext.index.persistent.impl.JCRIndexingQueueDAOImpl;

/**
 * The indexing queue as the other nodes of a cluster apply it: the removal of a node deletes its pending operations,
 * from a listener no transaction surrounds.
 */
public class JCRIndexingQueueDAOTest extends BaseStandaloneTest
{
   private JCRIndexingQueueDAOImpl indexingQueueDAO;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      indexingQueueDAO = ExoContainerContext.getService(JCRIndexingQueueDAOImpl.class);
      RequestLifeCycle.begin(container);
   }

   @Override
   protected void tearDown() throws Exception
   {
      RequestLifeCycle.end();
      super.tearDown();
   }

   public void testOperationsOfARemovedNodeAreDeletedWithoutATransactionOfTheCaller()
   {
      String jcrUUID = UUID.randomUUID().toString();
      indexingQueueDAO.create(new JCRIndexQueueEntity(jcrUUID,
                                                      "ws",
                                                      JCRIndexingOperationType.CREATE,
                                                      Calendar.getInstance(),
                                                      false,
                                                      "node"));
      long count = indexingQueueDAO.count();

      indexingQueueDAO.deleteOperationsByJCRUUID(jcrUUID);

      assertEquals(count - 1, indexingQueueDAO.count().longValue());
   }
}
