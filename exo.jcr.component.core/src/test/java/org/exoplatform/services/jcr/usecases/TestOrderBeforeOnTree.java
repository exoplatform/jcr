/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestOrderBeforeOnTree.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestOrderBeforeOnTree extends BaseUsecasesTest
{

   public void testOrderBefore() throws Exception
   {
      final int LAYER_COUNT = 2;

      final int SIBLINGS_COUNT = 5;

      final String baseName = "subnode";

      int[] currentIndexes = new int[LAYER_COUNT];

      String[] uuids = new String[LAYER_COUNT];

      Node testRoot = root.addNode("testRoot");
      session.save();
      Node curParent = testRoot;
      for (int i = 0; i < LAYER_COUNT; i++)
      {
         Node n = null;
         for (int j = 0; j < SIBLINGS_COUNT; j++)
         {
            n = curParent.addNode(baseName + i);
            session.save();
         }
         curParent = n;
         uuids[i] = ((NodeImpl)n).getIdentifier();

         ItemData nd = ((NodeImpl)n).getData();
         currentIndexes[i] = nd.getQPath().getEntries()[nd.getQPath().getEntries().length - 1].getIndex();
      }

      final int timesToCheck = 3;
      final int layer = 0;
      for (int c = 0; c < timesToCheck; c++)
      {

         // reorder             
         int prevIndex = currentIndexes[layer];

         int newIndex = currentIndexes[layer] != 1 ? currentIndexes[layer] - 1 : SIBLINGS_COUNT;

         testRoot.orderBefore(baseName + layer + "[" + prevIndex + "]", (currentIndexes[layer] != 1 ? baseName + layer
            + "[" + newIndex + "]" : null));
         session.save();
         currentIndexes[layer] = newIndex;

         // check
         // make path
         StringBuilder str = new StringBuilder();
         str.append("[]:1[]testRoot:1");
         for (int i = 0; i < LAYER_COUNT; i++)
         {
            str.append("[]subnode" + i + ":" + currentIndexes[i]);
         }

         QPath expectedPath = QPath.parse(str.toString());

         NodeImpl n = (NodeImpl)session.getNodeByIdentifier(uuids[LAYER_COUNT - 1]);
         QPath realPath = n.getInternalPath();
         if (!expectedPath.equals(realPath))
         {
            fail("Expected path " + expectedPath + " but was " + realPath);
         }

         //check properties path
         PropertyIterator pit = n.getProperties();
         while (pit.hasNext())
         {
            PropertyImpl p = (PropertyImpl)pit.nextProperty();
            assertEquals(expectedPath, p.getInternalPath().makeParentPath());
         }
      }
   }
}
