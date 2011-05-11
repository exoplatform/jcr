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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.tools.tree.NameTraversingVisitor;
import org.exoplatform.services.jcr.impl.tools.tree.TreeGenerator;
import org.exoplatform.services.jcr.impl.tools.tree.generator.WeightNodeGenerator;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestItemAccess.java 14508 2008-05-20 10:07:45Z ksm $
 */
public class TestItemAccess extends JcrImplBaseTest
{

   private TreeGenerator nGen;

   private QPath[] validNames;

   private String[] validUuids;

   private Node testGetItemNode;

   private static final int TEST_ITEMS_COUNT = 2000;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      testGetItemNode = root.addNode("testGetItemNode");
      root.save();
      // geneteting tree maxDepth = 5 and maxWidth = 12
      nGen = new TreeGenerator(testGetItemNode, new WeightNodeGenerator(5, 5));
      nGen.genereteTree();
      validNames = NameTraversingVisitor.getValidNames(testGetItemNode, NameTraversingVisitor.SCOPE_ALL);

      validUuids = NameTraversingVisitor.getValidUuids(testGetItemNode, NameTraversingVisitor.SCOPE_ALL);
   }

   public void testGetItemTest() throws RepositoryException
   {
      SessionImpl newSession = (SessionImpl)repository.login(this.credentials, session.getWorkspace().getName());

      Random random = new Random();
      SessionDataManager tm = newSession.getTransientNodesManager();
      for (int i = 0; i < TEST_ITEMS_COUNT; i++)
      {
         try
         {
            QPath itemPath = validNames[random.nextInt(validNames.length)];
            assertNotNull(tm.getItem(itemPath, true));
         }
         catch (RuntimeException e)
         {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
         }
      }
      for (int i = 0; i < TEST_ITEMS_COUNT; i++)
      {
         String validUuid = validUuids[random.nextInt(validUuids.length)];
         ItemData data = tm.getItemData((validUuid));
         assertNotNull(data);
      }
      newSession.logout();
   }
}
