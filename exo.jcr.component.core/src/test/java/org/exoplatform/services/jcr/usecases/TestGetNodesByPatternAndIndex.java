/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestGetNodesByPatternAndIndex.java 111 13 ����. 2011 serg $
 */
public class TestGetNodesByPatternAndIndex extends BaseUsecasesTest
{
   public void testPatternWithEscapedSymbols() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.addNode("node.txt");
      node.addNode("node.txt");
      node.addNode("node.txt");
      node.addNode("node_txt");
      node.addNode("node3txt");
      root.save();

      NodeIterator iterator = node.getNodes("node.tx*");
      assertTrue(iterator.hasNext());
      testNamesWithIndex(iterator, new String[]{"[]node.txt:1", "[]node.txt:2", "[]node.txt:3"});
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("node.txt");
      assertTrue(iterator.hasNext());
      testNamesWithIndex(iterator, new String[]{"[]node.txt:1", "[]node.txt:2", "[]node.txt:3"});
      assertFalse(iterator.hasNext());

      iterator = node.getNodes("node.txt[1] | node.txt[3] | london[6]");
      assertTrue(iterator.hasNext());
      testNamesWithIndex(iterator, new String[]{"[]node.txt:1", "[]node.txt:3"});
      assertFalse(iterator.hasNext());
   }

   public void setUp() throws Exception
   {
      super.setUp();
      Node root = session.getRootNode();
      root.addNode("childNode");
      root.save();
   }

   public void tearDown() throws Exception
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      node.remove();
      session.save();

      super.tearDown();
   }

   protected void testNamesWithIndex(Iterator iterator, String[] expectedNames) throws RepositoryException
   {

      List<String> names = new ArrayList<String>();
      while (iterator.hasNext())
      {
         ItemImpl item = (ItemImpl)iterator.next();
         names.add(item.getInternalPath().getEntries()[item.getInternalPath().getDepth()].getAsString(true));
      }

      //compare names
      assertEquals(expectedNames.length, names.size());

      for (String expectedName : expectedNames)
      {
         boolean finded = false;
         for (String name : names)
         {
            if (expectedName.equals(name))
            {
               finded = true;
               break;
            }
         }
         assertTrue(finded);
      }
   }

}
