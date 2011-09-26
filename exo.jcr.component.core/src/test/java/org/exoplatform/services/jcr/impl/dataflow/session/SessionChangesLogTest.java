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
package org.exoplatform.services.jcr.impl.dataflow.session;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * Jun 13, 2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SessionChangesLogTest.java 11962 2008-03-16 16:31:14Z gazarenkov $
 */
public class SessionChangesLogTest extends JcrImplBaseTest
{

   // for makeNotThisThreadLog() method, contains SessionChangesLog and its sessionId
   private class SessionChangesLogInfo
   {

      private final SessionChangesLog slog;

      private final String sessionId;

      SessionChangesLogInfo(SessionChangesLog slog, String sessionId)
      {
         this.slog = slog;
         this.sessionId = sessionId;
      }

      public SessionChangesLog getLog()
      {
         return slog;
      }

      public String getSessionId()
      {
         return sessionId;
      }
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      NodeImpl testRoot = (NodeImpl)root.addNode("sessionLinkRoot");
      session.save();
   }

   @Override
   protected void tearDown() throws Exception
   {

      if (root.hasNode("sessionLinkRoot"))
      {
         NodeImpl testRoot = (NodeImpl)root.getNode("sessionLinkRoot");
         testRoot.remove();
         session.save();
      }

      super.tearDown();
   }

   private SessionChangesLogInfo makeNotThisThreadLog() throws Exception
   {

      final SessionChangesLogInfo[] chlog = new SessionChangesLogInfo[1];

      final Repository frepository = repository;
      final Credentials fcredentials = this.credentials /* session.getCredentials() */;

      Thread thread = new Thread()
      {

         @Override
         public void run()
         {

            SessionImpl userSession;
            try
            {
               userSession = (SessionImpl)frepository.login(fcredentials, "ws");
               chlog[0] =
                  new SessionChangesLogInfo(new SessionChangesLog(userSession), userSession
                     .getId());
            }
            catch (RepositoryException e)
            {
               throw new RuntimeException("testSessionLinkGCedSession(), " + e, e);
            }

            userSession = null;
         }
      };

      thread.start();
      thread.join();

      return chlog[0];
   }

   private SessionImpl getRegisteredSession(String sessionId)
   {

      SessionRegistry sreg = (SessionRegistry)session.getContainer().getComponentInstanceOfType(SessionRegistry.class);

      return sreg.getSession(sessionId);
   }

   public void testSameSession()
   {

      SessionChangesLog chlog = new SessionChangesLog(session);
      assertEquals("Session must be same as given id owns", session, getRegisteredSession(chlog.getSessionId()));
   }

   public void testWithoutSession() throws Exception
   {

      String id = IdGenerator.generate();
      SessionChangesLog chlog = new SessionChangesLog(id);

      assertNull("No session should be linked to the log", getRegisteredSession(chlog.getSessionId()));
   }

   public void testAnotherThreadSession() throws Exception
   {

      SessionChangesLogInfo chlog = makeNotThisThreadLog();
      assertEquals("Session must be same as given id owns", chlog.getSessionId(), getRegisteredSession(
         chlog.getLog().getSessionId()).getId());
   }

   public void testSessionLogout() throws Exception
   {

      // test if WeakHashMap does remove at logout

      SessionChangesLogInfo chlog = makeNotThisThreadLog();

      getRegisteredSession(chlog.getLog().getSessionId()).logout();

      // gc and wait
      System.gc();
      Thread.currentThread().sleep(5000);

      assertNull("No session should be linked to the log", getRegisteredSession(chlog.getLog().getSessionId()));
   }

   public void testMultipleSessionsLogout() throws Exception
   {

      List<SessionChangesLogInfo> logs = new ArrayList<SessionChangesLogInfo>();

      NodeImpl testRoot = (NodeImpl)root.getNode("sessionLinkRoot");

      NodeData parent = (NodeData)testRoot.getData();

      for (int i = 1; i <= 1000; i++)
      {
         SessionChangesLogInfo logInfo = makeNotThisThreadLog();

         SessionChangesLog slog = logInfo.getLog();

         InternalQName qname = InternalQName.parse("[]node" + i);

         TransientNodeData ndata = TransientNodeData.createNodeData(parent, qname, Constants.NT_UNSTRUCTURED);

         // jcr:primaryType
         TransientPropertyData ndpt =
            TransientPropertyData.createPropertyData(ndata, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
               new TransientValueData(ndata.getPrimaryTypeName()));

         slog.add(ItemState.createAddedState(ndata));
         slog.add(ItemState.createAddedState(ndpt));

         SessionDataManager sdm = getRegisteredSession(logInfo.getLog().getSessionId()).getTransientNodesManager();
         sdm.getTransactManager().save(slog); // persist changes

         logs.add(logInfo);
      }

      List<String> sessions = new ArrayList<String>();

      for (Iterator<SessionChangesLogInfo> iter = logs.iterator(); iter.hasNext();)
      {
         SessionChangesLogInfo slog = iter.next();
         iter.remove();

         getRegisteredSession(slog.getLog().getSessionId()).logout();
         // sessions.add(new String(slog.getSessionId()));
      }

      // gc and wait
      System.gc();
      Thread.sleep(5000);

      // ItemDataChangesLogHood registry = new ItemDataChangesLogHood();

      // int linked = 0;
      // for (String sessionId: sessions) {
      // if (registry.isSessionLinked(sessionId))
      // linked++;
      // }

      // assertEquals("No session should be linked ", 0, registry.linkedSessionsCount());

      // assertEquals("No session should be linked ", 0, linked);
   }

   public void testAddRootChanges() throws Exception
   {
      SessionChangesLog changesLog = new SessionChangesLog(session);
      try
      {
         changesLog.add(new ItemState(new TransientPropertyData(Constants.ROOT_PATH, Constants.ROOT_UUID, 0,
            PropertyType.STRING, null, false), ItemState.ADDED, false, Constants.ROOT_PATH));
      }
      catch (Exception e)
      {
         fail("Exception should not be thrown");
      }
   }
}
