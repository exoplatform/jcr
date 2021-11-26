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

package org.exoplatform.services.jcr.api.lock;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 21.09.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestLock.java 11908 2008-03-13 16:00:12Z ksm $
 */
public class TestLock extends JcrAPIBaseTest
{

   public void testUnLockNodeByOwner() throws Exception
   {

      Credentials credentialsMary = new CredentialsImpl("mary", "exo".toCharArray());
      Session sessionMary = repository.login(credentialsMary, WORKSPACE);

      Node testNode = sessionMary.getRootNode().addNode("testNode");
      sessionMary.save();

      testNode.addMixin("mix:lockable");
      testNode.addMixin("exo:privilegeable");
      sessionMary.save();

      testNode = sessionMary.getRootNode().getNode("testNode");
      testNode.lock(false, false);
      sessionMary.logout();

      Credentials credentialsJohn = new CredentialsImpl("john", "exo".toCharArray());
      Session sessionJohn = repository.login(credentialsJohn, WORKSPACE);

      try
      {
         sessionJohn.getRootNode().getNode("testNode").unlock();
         sessionJohn.save();
         fail();
      }
      catch (LockException e)
      {
         // it is OK, john has no right. Node is locked by mary
      }

      sessionJohn.logout();

      Session systemSession = repository.getSystemSession(WORKSPACE);
      systemSession.getRootNode().getNode("testNode").unlock();
      systemSession.logout();
   }
}
