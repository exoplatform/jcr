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
package org.exoplatform.services.jcr.impl.storage;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS. <br/> Prerequisites: <value-storages> <value-storage
 * class="org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage"> <properties>
 * <property name="path" value="target/temp/values"/> </properties> <filters> <filter
 * property-type="Binary"/> </filters> </value-storage> </value-storages>
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueStoragePluginTest.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ValueStoragePluginTest extends BaseStandaloneTest
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.ValueStoragePluginTest");

   // protected String sourceName = "jdbc/basic";
   // JDBCWorkspaceDataContainer container;

   @Override
   public String getRepositoryName()
   {
      return repository.getName();
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      List<WorkspaceEntry> wsList = repository.getConfiguration().getWorkspaceEntries();
      for (WorkspaceEntry ws : wsList)
      {
         log.info("---- Value STORAGE of " + ws.getName() + " = " + ws.getContainer().getValueStorages());
         if (ws.getName().equals(workspace.getName()))
         {
            if (ws.getContainer().getValueStorages() == null)
               throw new Exception("No value storage plugins configured for workspace " + ws.getName());
            List<ValueStorageEntry> wssEntries = ws.getContainer().getValueStorages();
            for (ValueStorageEntry we : wssEntries)
            {
               for (ValueStorageFilterEntry vef : we.getFilters())
               {
                  if (PropertyType.valueFromName(vef.getPropertyType()) == PropertyType.BINARY)
                     return;
               }
            }
            throw new Exception("No BINARY type filter found for workspace " + ws.getName());
         }
      }
   }

   // public void testConfig() throws Exception {
   // }

   public void testShortBinary() throws Exception
   {
      Node n = root.addNode("binaryTestNode", "nt:unstructured");
      // add property
      n.setProperty("binaryTestProp", "Binary content", PropertyType.BINARY);
      root.save();
      // log.info("CONTENT '"+n.getProperty("binaryTestProp").getString()+"'");
      assertEquals("Binary content", n.getProperty("binaryTestProp").getString());
      // update property
      n.setProperty("binaryTestProp", "NEW Binary content", PropertyType.BINARY);
      root.save();
      assertEquals("NEW Binary content", n.getProperty("binaryTestProp").getString());
      // delete property
      n.getProperty("binaryTestProp").remove();
      root.save();
   }

   public void testLongBinary() throws Exception
   {
      // Node n = root.getNode("binaryTestNode");
      Node n = root.addNode("binaryTestNode1", "nt:unstructured");

      // add property
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 90000; i++)
      {
         sb.append("Binary content");
      }
      String s = new String(sb);
      n.setProperty("binaryTestProp", s, PropertyType.BINARY);
      root.save();
      // System.out.println("CONTENT
      // '"+n.getProperty("binaryTestProp").getString().length()+" "+s.length());
      assertEquals(s, n.getProperty("binaryTestProp").getString());
      // update property
      s += "NEW";
      n.setProperty("binaryTestProp", s, PropertyType.BINARY);
      root.save();
      assertEquals(s, n.getProperty("binaryTestProp").getString());
      // delete property
      n.getProperty("binaryTestProp").remove();
      root.save();
   }

   public void testAddValuesInDifferentVs() throws Exception
   {
      int WORKSPACE_COUNT = 3;
      int NODES_COUNT = 5;
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep = null;

      defRep = (RepositoryImpl)service.getDefaultRepository();
      Random random = new Random();
      for (int i = 0; i < WORKSPACE_COUNT; i++)
      {
         String currwsName = createWs();
         Session currenSession = defRep.getSystemSession(currwsName);
         Node currentRoot = currenSession.getRootNode();
         assertNotNull(currentRoot);
         Node testLocalBigFiles = currentRoot.addNode("testVs");
         List<String> blobFiles = new ArrayList<String>();

         // add file to repository
         long startTime, endTime;

         // add
         for (int j = 0; j < NODES_COUNT; j++)
         {
            startTime = System.currentTimeMillis(); // to get the time of start
            String TEST_FILE = createBLOBTempFile(random.nextInt(1024 * 1024 * 2)).getAbsolutePath();
            blobFiles.add(TEST_FILE);
            Node localBigFile = testLocalBigFiles.addNode("bigFile" + j, "nt:file");
            Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
            // contentNode.setProperty("jcr:encoding", "UTF-8");
            InputStream is = new FileInputStream(TEST_FILE);
            contentNode.setProperty("jcr:data", is);
            contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
            is.close();
            System.out.println("Data is set: " + TEST_FILE);
            // contentNode.setProperty("jcr:mimeType", "video/avi");
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
            currenSession.save();

            System.out.println("Saved: " + TEST_FILE + " " + Runtime.getRuntime().freeMemory());
            endTime = System.currentTimeMillis();
            log.info("Execution time after adding and saving (local big):" + ((endTime - startTime) / 1000) + "s");
         }
         // load
         Node n1 = currentRoot.getNode("testVs");

         for (int j = 0; j < NODES_COUNT; j++)
         {
            Node lbf = n1.getNode("bigFile" + j);
            Node content = lbf.getNode("jcr:content");

            // comparing with source file
            compareStream(new BufferedInputStream(new FileInputStream(blobFiles.get(j))), content.getProperty(
               "jcr:data").getStream());
         }
         n1.remove();
         currenSession.save();
      }

   }

   private String createWs() throws Exception
   {
      TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();
      WorkspaceEntry wsEntry = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);
      boolean isDefaultWsMultiDb = false;
      if ("true".equals(wsEntry.getContainer().getParameterValue("multi-db")))
      {
         isDefaultWsMultiDb = true;
      }
      WorkspaceEntry workspaceEntry =
         helper.createWorkspaceEntry(isDefaultWsMultiDb,
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME));

      return workspaceEntry.getName();
   }

   @Override
   protected File createBLOBTempFile(int sizeInb) throws IOException
   {
      // create test file
      byte[] data = new byte[1024]; // 1Kb

      File testFile = File.createTempFile(IdGenerator.generate(), ".tmp");
      FileOutputStream tempOut = new FileOutputStream(testFile);
      Random random = new Random();

      for (int i = 0; i < sizeInb; i += 1024)
      {
         if (i + 1024 > sizeInb)
         {
            byte[] rest = new byte[(sizeInb - i)];
            random.nextBytes(rest);
            tempOut.write(rest);
            continue;
         }
         random.nextBytes(data);
         tempOut.write(data);
      }
      tempOut.close();
      testFile.deleteOnExit(); // delete on test exit
      log.info("Temp file created: " + testFile.getAbsolutePath() + " size: " + testFile.length());
      return testFile;
   }

}
