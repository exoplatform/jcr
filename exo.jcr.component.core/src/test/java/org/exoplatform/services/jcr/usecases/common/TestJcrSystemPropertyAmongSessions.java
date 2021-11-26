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
 * @version $Id: TestJcrSystemPropertyAmongSessions.java 11907 2008-03-13 15:36:21Z ksm $
 * 
 *          Items under /jcr:system
 */

public class TestJcrSystemPropertyAmongSessions extends BaseUsecasesTest
{

   /**
    * Check If Jcr System is referenceable from any Workspace NOTE: THIS is an implementation feature
    * and NOT specified by JSR-170!
    * 
    * Sample test. An example how to make it
    * 
    * @throws Exception
    */
   public void testJCRSystemPropertyAmongSessions() throws Exception
   {

      // Session session = repository.getSystemSession(repository.getSystemWorkspaceName()) ;
      Session session = repository.getSystemSession(repository.getSystemWorkspaceName());
      Node testNode = session.getRootNode().addNode("jcr:system/TestNode");
      testNode.setProperty("p", "test");
      session.save();

      Session session2 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), repository.getSystemWorkspaceName());
      testNode = session2.getRootNode().getNode("jcr:system/TestNode");
      String value = testNode.getProperty("p").getValue().getString();
      assertEquals(value, "test");

      // you should change session to see both of session cann't use
      // Session session3 = repository.getSystemSession(repository.getSystemWorkspaceName());
      Session session3 =
         repository.login(new SimpleCredentials("admin", "admin".toCharArray()), repository.getSystemWorkspaceName());
      Node testNode2 = session3.getRootNode().getNode("jcr:system/TestNode");
      testNode2.setProperty("p", "testModify");
      session3.save();
      testNode2 = session3.getRootNode().getNode("jcr:system/TestNode");
      value = testNode2.getProperty("p").getValue().getString();
      assertEquals(value, "testModify");

      session2.refresh(true);
      // session2.refresh(false) ;
      testNode = session2.getRootNode().getNode("jcr:system/TestNode");
      value = testNode.getProperty("p").getValue().getString();
      assertEquals(value, "testModify");
   }
}
