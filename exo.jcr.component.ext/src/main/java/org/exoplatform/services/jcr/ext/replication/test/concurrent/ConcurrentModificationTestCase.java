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
package org.exoplatform.services.jcr.ext.replication.test.concurrent;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.replication.test.BaseReplicationTestCase;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ConcurrentModificationTestCase.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class ConcurrentModificationTestCase extends BaseReplicationTestCase
{

   /**
    * The apache logger.
    */
   private static final Log log = ExoLogger.getLogger(ConcurrentModificationTestCase.class);

   /**
    * ConcurrentModificationTestCase constructor.
    * 
    * @param repositoryService
    *          the RepositoryService.
    * @param reposytoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    */
   public ConcurrentModificationTestCase(RepositoryService repositoryService, String reposytoryName,
      String workspaceName, String userName, String password)
   {
      super(repositoryService, reposytoryName, workspaceName, userName, password);
   }

   /**
    * createContent.
    * 
    * @param repoPath
    *          the repository path
    * @param fileName
    *          the file name
    * @param iterations
    *          how many iterations for simple content
    * @param simpleContent
    *          the simple content
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer createContent(String repoPath, String fileName, Long iterations, String simpleContent)
   {
      StringBuffer sb = new StringBuffer();

      log.info("ReplicationTestService.createContent run");
      long start, end;

      File tempFile = null;
      try
      {
         tempFile = File.createTempFile("tempF", "_");
         FileOutputStream fos = new FileOutputStream(tempFile);

         for (long i = 0; i < iterations; i++)
            fos.write(simpleContent.getBytes());
         fos.close();

         start = System.currentTimeMillis(); // to get the time of start

         Node cool = addNodePath(repoPath).addNode(fileName, "nt:file");
         Node contentNode = cool.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:encoding", "UTF-8");
         contentNode.setProperty("jcr:data", new FileInputStream(tempFile));
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
         contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

         session.save();

         end = System.currentTimeMillis();

         log.info("The time of the adding of nt:file : " + ((end - start) / BaseReplicationTestCase.ONE_SECONDS)
            + " sec");
         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't save nt:file : ", e);
         sb.append("fail");
      }
      finally
      {
         tempFile.delete();
      }

      return sb;
   }

   /**
    * compareData.
    * 
    * @param srcRepoPath
    *          the source repository path
    * @param srcFileName
    *          the source file name
    * @param destRepoPath
    *          the destination repository path
    * @param destFileName
    *          the destination file name
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer compareData(String srcRepoPath, String srcFileName, String destRepoPath, String destFileName)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         Node srcNode = ((Node)session.getItem(getNormalizePath(srcRepoPath))).getNode(srcFileName);
         Node destNode = ((Node)session.getItem(getNormalizePath(destRepoPath))).getNode(destFileName);

         InputStream srcStream = srcNode.getNode("jcr:content").getProperty("jcr:data").getStream();
         InputStream destStream = destNode.getNode("jcr:content").getProperty("jcr:data").getStream();

         compareStream(srcStream, destStream);

         log.info("ReplicationTestService.startThread run");
         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't compare the data : ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * startThreadUpdater.
    * 
    * * @param srcRepoPath the source repository path
    * 
    * @param srcFileName
    *          the source file name
    * @param destRepoPath
    *          the destination repository path
    * @param destFileName
    *          the destination file name
    * @param iterations
    *          how many iterations the thread
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer startThreadUpdater(String srcRepoPath, String srcFileName, String destRepoPath,
      String destFileName, Long iterations)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         Node srcNode = ((Node)session.getItem(getNormalizePath(srcRepoPath))).getNode(srcFileName);
         Node destNode = ((Node)session.getItem(getNormalizePath(destRepoPath))).getNode(destFileName);

         DataUpdaterThread updaterThread = new DataUpdaterThread(srcNode, destNode, iterations);
         updaterThread.start();

         log.info("ReplicationTestService.startThread run");
         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't start the thread : ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * The DataUpdaterThread will be update the specific property.
    * 
    */
   class DataUpdaterThread extends Thread
   {
      /**
       * The source node.
       */
      private final Node srcNode;

      /**
       * The destination node.
       */
      private final Node destNode;

      /**
       * How many iterations the update property.
       */
      private final Long iterations;

      /**
       * DataUpdaterThread constructor.
       * 
       * @param srcNode
       *          the source node
       * @param destNode
       *          the destination node
       * @param iterations
       *          the iteration value
       */
      public DataUpdaterThread(Node srcNode, Node destNode, Long iterations)
      {
         this.srcNode = srcNode;
         this.destNode = destNode;
         this.iterations = iterations;
      }

      /**
       * {@inheritDoc}
       */
      public void run()
      {
         String destPath = null;
         try
         {
            destPath = destNode.getPath();
            for (int i = 0; i < iterations; i++)
            {
               InputStream srcStream = srcNode.getNode("jcr:content").getProperty("jcr:data").getStream();

               destNode.getNode("jcr:content").setProperty("jcr:data", srcStream);
               session.save();

               log.info(Calendar.getInstance().getTime().toGMTString() + " : ");
               log
                  .info(this.getName() + " : " + "has been updated the 'nt:file' " + destPath + " : iterations == " + i);
            }
         }
         catch (RepositoryException e)
         {
            log.error("Can't update the 'nt:file' " + destPath + " : ", e);
         }
      }
   }
}
