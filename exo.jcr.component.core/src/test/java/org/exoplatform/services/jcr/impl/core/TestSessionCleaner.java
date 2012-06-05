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

import java.lang.ref.WeakReference;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestSessionCleaner.java 14508 2008-05-20 10:07:45Z ksm $
 */
public class TestSessionCleaner extends JcrImplBaseTest
{
   private final static int AGENT_COUNT = 10;

   private final static int TEST_SESSION_TIMEOUT = 1; // seconds

   private SessionRegistry sessionRegistry;

   private long oldTimeOut;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      sessionRegistry = (SessionRegistry)session.getContainer().getComponentInstanceOfType(SessionRegistry.class);
      oldTimeOut = sessionRegistry.getTimeOut();
      sessionRegistry.setTimeOut(TEST_SESSION_TIMEOUT);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      sessionRegistry.setTimeOut(oldTimeOut);
   }

   public void testSessionRemove() throws LoginException, NoSuchWorkspaceException, RepositoryException,
      InterruptedException
   {
      SessionImpl session2 = (SessionImpl)repository.login(credentials, "ws");
      assertTrue(session2.isLive());

      // Create a weak reference to the session
      WeakReference<SessionImpl> ref = new WeakReference<SessionImpl>(session2);

      Thread.sleep(5000);

      sessionRegistry.runCleanup();

      assertFalse(session2.isLive());

      // Dereference the session explicitely
      session2 = null;

      // Make a GC
      forceGC();

      // The weak reference must now be null
      assertNull(ref.get());
   }
}
