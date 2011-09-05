/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * TODO this class contains unfixed tests from RemoveSameNameSiblingTest.
 * Please move back fixed tests.
 * 
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 9.06.2011 skarpenko $
 */
public class RemoveSameNameSiblingUnfixedTest extends BaseUsecasesTest
{
   public void testRemoveSameNameSiblings() throws Exception
   {
      Node testRoot = root.addNode("snsRemoveTest");
      session.save();

      try
      {
         Node node1 = testRoot.addNode("_node");
         node1.setProperty("prop", "_data1");
         Node node2 = testRoot.addNode("_node");
         node2.setProperty("prop", "_data2");
         Node node3 = node2.addNode("node3");
         testRoot.save();

         try
         {
            assertEquals("/snsRemoveTest/_node[2]/node3", node2.getNode("node3").getPath());
            node1.remove(); // /snsRemoveTest/_node[2] -> /snsRemoveTest/_node[1]

            // check
            String n2p = node2.getProperty("prop").getString();
            assertEquals("A property must be same ", "_data2", n2p);

            // TODO there is a problem, we can't see deep subtree of reindexed same-name-siblings now.
            // after save it will be ok.
            // See http://jira.exoplatform.org/browse/JCR-340
            // Also, this test do not fails with disabled cache.
            assertEquals("/snsRemoveTest/_node/node3", node2.getNode("node3").getPath());

         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
            fail("A property must exists on the node /snsRemoveTest/_node[1] " + e);
         }
      }
      finally
      {
         testRoot.remove();
         session.save();
      }
   }
}
