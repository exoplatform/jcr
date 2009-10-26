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
package org.exoplatform.services.jcr.usecases.export;

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerParams;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.config.WorkspaceInitializerEntry;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SysViewWorkspaceInitializer;
import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 06.05.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: ExportWorkspaceSystemViewTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ExportWorkspaceSystemViewTest extends BaseUsecasesTest
{

   public void testTwoRestores() throws Exception
   {
      {
         SessionImpl sessionWS1 = (SessionImpl)repository.login(credentials, "ws1");

         sessionWS1.getRootNode().addNode("asdasdasda", "nt:unstructured").setProperty("data", "data_1");
         sessionWS1.save();

         // 1-st export
         File f1 = new File("target/1.xml");
         sessionWS1.exportWorkspaceSystemView(new FileOutputStream(f1), false, false);

         // 1-st import
         WorkspaceEntry ws1_restore_1 =
            makeWorkspaceEntry("ws1_restore_1", isMultiDB(session) ? "jdbcjcr2export1" : "jdbcjcr", f1);
         repository.configWorkspace(ws1_restore_1);
         repository.createWorkspace(ws1_restore_1.getName());

         // check
         SessionImpl back1 = (SessionImpl)repository.login(credentials, "ws1_restore_1");
         assertNotNull(back1.getRootNode().getNode("asdasdasda").getProperty("data"));

         // add date to restored workspace
         back1.getRootNode().addNode("gdfgrghfhf", "nt:unstructured").setProperty("data", "data_2");
         back1.save();
      }

      {
         // 2-st export
         SessionImpl back1 = (SessionImpl)repository.login(credentials, "ws1_restore_1");
         File f2 = new File("target/2.xml");
         back1.exportWorkspaceSystemView(new FileOutputStream(f2), false, false);

         // 2-st import
         WorkspaceEntry ws1_restore_2 =
            makeWorkspaceEntry("ws1_restore_2", isMultiDB(session) ? "jdbcjcr2export2" : "jdbcjcr", f2);
         repository.configWorkspace(ws1_restore_2);
         repository.createWorkspace(ws1_restore_2.getName());

         // check
         SessionImpl back2 = (SessionImpl)repository.login(credentials, "ws1_restore_2");
         assertNotNull(back2.getRootNode().getNode("gdfgrghfhf").getProperty("data"));
      }
   }

   private WorkspaceEntry makeWorkspaceEntry(String name, String sourceName, File sysViewFile)
   {
      WorkspaceEntry ws1e = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      WorkspaceEntry ws1back = new WorkspaceEntry();
      ws1back.setName(name);
      ws1back.setUniqueName(((RepositoryImpl)session.getRepository()).getName() + "_" + ws1back.getName());

      ws1back.setAccessManager(ws1e.getAccessManager());
      ws1back.setAutoInitializedRootNt(ws1e.getAutoInitializedRootNt());
      ws1back.setAutoInitPermissions(ws1e.getAutoInitPermissions());
      ws1back.setCache(ws1e.getCache());
      ws1back.setContainer(ws1e.getContainer());
      ws1back.setLockManager(ws1e.getLockManager());

      // Initializer
      WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
      wiEntry.setType(SysViewWorkspaceInitializer.class.getCanonicalName());

      List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
      wieParams
         .add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, sysViewFile.getPath()));

      wiEntry.setParameters(wieParams);

      ws1back.setInitializer(wiEntry);

      // Indexer
      ArrayList qParams = new ArrayList();
      qParams.add(new SimpleParameterEntry(QueryHandlerParams.PARAM_INDEX_DIR, "target" + File.separator + name));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      ws1back.setQueryHandler(qEntry);

      ArrayList params = new ArrayList();
      for (Iterator i = ws1back.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry)i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (isMultiDB(session) && newp.getName().equals("source-name"))
            newp.setValue(sourceName);
         else if (newp.getName().equals("swap-directory"))
            newp.setValue("target/temp/swap/" + name);
         else if (isMultiDB(session) && newp.getName().equals("dialect"))
            newp.setValue("hsqldb");

         params.add(newp);
      }

      ContainerEntry ce =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);
      ws1back.setContainer(ce);

      return ws1back;
   }

   private boolean isMultiDB(SessionImpl session)
   {
      WorkspaceEntry ws1e = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);

      for (Iterator i = ws1e.getContainer().getParameters().iterator(); i.hasNext();)
      {
         SimpleParameterEntry p = (SimpleParameterEntry)i.next();
         SimpleParameterEntry newp = new SimpleParameterEntry(p.getName(), p.getValue());

         if (newp.getName().equals("multi-db"))
            return Boolean.valueOf(newp.getValue());
      }

      throw new RuntimeException("Can not get property 'multi-db' in configuration on workspace '" + ws1e.getName()
         + "'");
   }
}
