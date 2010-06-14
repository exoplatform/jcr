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
package org.exoplatform.services.jcr.load.blob;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * NOTE: Make sure you have the files pointed below!
 */

public class TestBinaryValueMultiThreading extends JcrAPIBaseTest
{

   private int FILES_COUNT = 3;

   private int CLIENTS_COUNT = 3;

   private String LOCAL_BIG_FILE = null;

   private String LOCAL_SMALL_FILE = "src/test/resources/index/test_index.doc";

   private static String REMOTE_BIG_FILE = "\\\\Exooffice\\public\\Tmp\\resources\\BigFile.zip";

   private static String REMOTE_SMALL_FILE = "\\\\Exooffice\\public\\Tmp\\resources\\SmallFile.zip";

   private static String URL_BIG_FILE = "http://localhost:8080/ecm/jcr?workspace=production&path=/BigFile.zip";

   private static String URL_SMALL_FILE = "http://localhost:8080/ecm/jcr?workspace=production&path=/SmallFile.zip";

   // private static String LOCAL_VERYBIG_MEDIA_FILE = "D:\\films\\test.avi";
   // private static String URL_BIG_FILE =
   // "http://localhost:8080/ecm/jcr?workspace=production&path=/testBinaryValue/BigFile.zip";
   // private static String URL_BIG_FILE =
   // "http://exooffice:8080/jcr-webdav/repository/production/Lost.Season3.Preview.rus.avi";
   // private static String URL_BIG_MEDIA_FILE=
   // "ftp://exoua.dnsalias.net/pub/video/Lost.Season3.Preview.rus.avi";
   // private static String URL_VERYBIG_MEDIA_FILE =
   // "ftp://exoua.dnsalias.net/pub/video/9_11_xchcoin.avi";
   // private static String URL_SMALL_FILE =
   // "http://localhost:8080/ecm/jcr?workspace=production&path=/testBinaryValue/SmallFile.zip";
   // private static String URL_SMALL_FILE =
   // "http://exooffice:8080/jcr-webdav/repository/production/SmallFile.zip";
   // private static String URL_SMALL_FILE = "ftp://exoua.dnsalias.net/jcr/test/SmallFile.zip";

   protected class TestJCRClient extends Thread
   {

      @Override
      public void run()
      {
         log.info("Client started...");
         SessionImpl clientSession = null;
         try
         {
            clientSession = (SessionImpl)repository.login(new CredentialsImpl("exo", "exo".toCharArray()), "ws");
            Node testLocalSmallFiles = clientSession.getRootNode().getNode("testLocalSmallFiles");
            Node testLocalBigFiles = clientSession.getRootNode().getNode("testLocalBigFiles");

            /*
             * Node testRemoteSmallFiles = clientSession.getRootNode().getNode("testRemoteSmallFiles");
             * Node testRemoteBigFiles = clientSession.getRootNode().getNode("testRemoteBigFiles"); Node
             * testUrlSmallFiles = clientSession.getRootNode().getNode("testUrlSmallFiles"); Node
             * testUrlBigFiles = clientSession.getRootNode().getNode("testUrlBigFiles");
             */

            for (int i = 0; i < FILES_COUNT; i++)
            {
               // check streams

               compareStream(PrivilegedFileHelper.fileInputStream(LOCAL_SMALL_FILE), testLocalSmallFiles.getProperty(
                  "smallFile" + i + "/jcr:content/jcr:data").getStream());
               compareStream(PrivilegedFileHelper.fileInputStream(LOCAL_BIG_FILE), testLocalBigFiles.getProperty(
                  "bigFile" + i + "/jcr:content/jcr:data").getStream());

               /*
                * compareStream(PrivilegedFileHelper.fileInputStream(REMOTE_SMALL_FILE),
                * testRemoteSmallFiles.getProperty("smallFile" + i +
                * "/jcr:content/jcr:data").getStream()); compareStream(new
                * FileInputStream(REMOTE_BIG_FILE), testRemoteBigFiles.getProperty("bigFile" + i +
                * "/jcr:content/jcr:data").getStream()); compareStream(new
                * URL(URL_SMALL_FILE).openStream(), testUrlSmallFiles.getProperty("smallFile" + i +
                * "/jcr:content/jcr:data").getStream()); compareStream(new
                * URL(URL_BIG_FILE).openStream(), testUrlBigFiles.getProperty("bigFile" + i +
                * "/jcr:content/jcr:data").getStream());
                */

            }
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   /**
    * The test must be runed in preconfigured environment: LOCAL_SMALL_FILE, etc vars
    */
   public void testMultiThreadReading() throws Exception
   {
      // Local small files creating
      Node testLocalSmallFiles = root.addNode("testLocalSmallFiles");
      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node localSmallFile = testLocalSmallFiles.addNode("smallFile" + i, "nt:file");
         Node contentNode = localSmallFile.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(LOCAL_SMALL_FILE));
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      }

      // Local big files
      if (LOCAL_BIG_FILE == null)
      {
         // create test file
         LOCAL_BIG_FILE = createBLOBTempFile(3).getAbsolutePath();
      }
      Node testLocalBigFiles = root.addNode("testLocalBigFiles");
      for (int i = 0; i < FILES_COUNT; i++)
      {
         Node localBigFile = testLocalBigFiles.addNode("bigFile" + i, "nt:file");
         Node contentNode = localBigFile.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(LOCAL_BIG_FILE));
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      }

      // Remote small files
      /*
       * Node testRemoteSmallFiles = root.addNode("testRemoteSmallFiles"); for (int i = 0; i <
       * FILES_COUNT; i++) { Node remoteSmallFile = testRemoteSmallFiles.addNode("smallFile" + i,
       * "nt:file"); Node contentNode = remoteSmallFile.addNode("jcr:content", "nt:resource");
       * contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(REMOTE_SMALL_FILE));
       * contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
       * contentNode.setProperty("jcr:lastModified", Calendar.getInstance()); } //Remote big files
       * Node testRemoteBigFiles = root.addNode("testRemoteBigFiles"); for (int i = 0; i <
       * FILES_COUNT; i++) { Node remoteBigFile = testRemoteBigFiles.addNode("bigFile" + i,
       * "nt:file"); Node contentNode = remoteBigFile.addNode("jcr:content", "nt:resource");
       * contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(REMOTE_BIG_FILE));
       * contentNode.setProperty("jcr:mimeType", "application/octet-stream ");
       * contentNode.setProperty("jcr:lastModified", Calendar.getInstance()); } //Url small files Node
       * testUrlSmallFiles = root.addNode("testUrlSmallFiles"); for (int i = 0; i < FILES_COUNT; i++)
       * { Node urlSmallFile = testUrlSmallFiles.addNode("smallFile" + i, "nt:file"); Node contentNode
       * = urlSmallFile.addNode("jcr:content", "nt:resource"); contentNode.setProperty("jcr:data", new
       * URL(URL_SMALL_FILE).openStream()); contentNode.setProperty("jcr:mimeType",
       * "application/octet-stream "); contentNode.setProperty("jcr:lastModified",
       * Calendar.getInstance()); } //Url big files Node testUrlBigFiles =
       * root.addNode("testUrlBigFiles"); for (int i = 0; i < FILES_COUNT; i++) { Node urlBigFile =
       * testUrlBigFiles.addNode("bigFile" + i, "nt:file"); Node contentNode =
       * urlBigFile.addNode("jcr:content", "nt:resource"); contentNode.setProperty("jcr:data", new
       * URL(URL_BIG_FILE).openStream()); contentNode.setProperty("jcr:mimeType",
       * "application/octet-stream "); contentNode.setProperty("jcr:lastModified",
       * Calendar.getInstance()); }
       */

      session.save();
      // Nodes has been created
      // Run each thread
      ArrayList<TestJCRClient> clients = new ArrayList<TestJCRClient>();
      for (int i = 0; i < CLIENTS_COUNT; i++)
      {
         TestJCRClient jcrClient = new TestJCRClient();
         jcrClient.start();
         clients.add(jcrClient);
      }
      // Next code is waiting for shutting down of all the threads
      boolean isNeedWait = true;
      while (isNeedWait)
      {
         isNeedWait = false;
         for (int i = 0; i < CLIENTS_COUNT; i++)
         {
            TestJCRClient curClient = clients.get(i);
            if (curClient.isAlive())
            {
               isNeedWait = true;
               break;
            }
         }
         Thread.sleep(100);
      }
   }
}
