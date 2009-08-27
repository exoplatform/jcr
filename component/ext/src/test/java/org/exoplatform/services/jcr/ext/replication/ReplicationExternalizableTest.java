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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ReplicationExternalizableTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ReplicationExternalizableTest extends BaseReplicationTest
{

   private static final Log log = ExoLogger.getLogger(ReplicationExternalizableTest.class);

   private static final int BUFFER_SIZE = 1024;

   private Node rootNode;

   public void testname() throws Exception
   {
      rootNode = root;
      int folders = 20;
      int files = 10;

      Long size = Long.valueOf(1024 * 1024);

      String[] relPath = new String[folders];

      for (int i = 0; i < folders; i++)
      {
         relPath[i] = createRelPath(5);
         addBinaryPropertyOnly(relPath[i], "n_", size, files);
      }

      Thread.sleep(20000);

   }

   private StringBuffer addBinaryPropertyOnly(String repoPath, String nodeName, Long size, long iterations)
      throws Exception
   {
      StringBuffer sb = new StringBuffer();

      long start, end;
      byte[] buf = new byte[BUFFER_SIZE];

      File tempFile = null;
      try
      {
         tempFile = File.createTempFile("tempF", "_");
         FileOutputStream fos = new FileOutputStream(tempFile);

         for (int i = 0; i < buf.length; i++)
            buf[i] = (byte)(i % 255);

         for (long i = 0; i < size / BUFFER_SIZE; i++)
            fos.write(buf);
         fos.write(buf, 0, (int)(size % BUFFER_SIZE));
         fos.close();

         start = System.currentTimeMillis(); // to get the time of start
         for (int i = 0; i < iterations; i++)
         {
            Node baseNode = addNodePath(repoPath);
            baseNode.setProperty("d", new FileInputStream(tempFile));

            session.save();

            // Thread.sleep(10);
         }

         end = System.currentTimeMillis();

         log.info("The time of the adding of nt:file + " + iterations + "( " + tempFile.length() + " B ) : "
            + ((end - start) / 1000) + " sec");

         sb.append("ok");
      }
      catch (Exception e)
      {
         sb.append("fail");
         throw new Exception("Can't save the binary value : ", e);
      }
      finally
      {
         tempFile.delete();
      }

      return sb;
   }

   protected Node addNodePath(String repoPath) throws RepositoryException
   {
      Node resultNode = rootNode;
      String[] sArray = repoPath.split("[::]");

      for (String nodeName : sArray)
         if (resultNode.hasNode(nodeName))
            resultNode = resultNode.getNode(nodeName);
         else
            resultNode = resultNode.addNode(nodeName, "nt:unstructured");

      return resultNode;
   }

   protected String createRelPath(long fSize)
   {
      String alphabet = "abcdefghijklmnopqrstuvwxyz";
      String relPath = "";
      long pathDepth = (fSize % 7) + 5;

      for (long i = 0; i < pathDepth; i++)
      {
         int index1 = (int)(Math.random() * 1000) % alphabet.length();
         int index2 = (int)(Math.random() * 1000) % alphabet.length();
         String s = alphabet.substring(index1, index1 + 1) + alphabet.substring(index2, index2 + 1);
         // s+=(int) (Math.random() * 100000);

         relPath += ("::" + s);
      }

      return relPath;
   }
}
