/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: RemoveNodeUseCaseTest.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 * 
 */

public class RemoveNodeUseCaseTest extends BaseUsecasesTest
{

   /**
    * [BM] so looks like that when I have Parent/ child1/ prop1 child2/ If I remove child2, then it
    * can not get the prop1 anymore (that is probably on the same session object)
    * 
    * @throws Exception
    */
   public void testIfPropertyFromSiblingReachableAfterRemove() throws Exception
   {
      // make sub-root with unique name;
      Node subRootNode = root.addNode("testIfPropertyFromSiblingReachableAfterRemove");

      Node child1 = subRootNode.addNode("child1");
      child1.setProperty("prop1", "test");
      Node child2 = subRootNode.addNode("child2");

      child2.remove();

      session.save();

      // and test on current session
      assertNotNull(child1.getProperty("prop1"));

      // test on another session
      Session session2 = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), workspace.getName());

      child1 = session2.getRootNode().getNode("testIfPropertyFromSiblingReachableAfterRemove/child1");
      // there should be 2 child props: jcr:primaryType and prop1

      assertEquals(2, child1.getProperties().getSize());
      assertNotNull(child1.getProperty("prop1"));

      // clean
      subRootNode.remove();
      session.save();

   }

}
