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
package org.exoplatform.services.jcr.ext.replication.test.bandwidth;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.replication.test.BaseReplicationTestCase;
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
 * @version $Id: BandwidthAllocationTestCase.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class BandwidthAllocationTestCase extends BaseReplicationTestCase
{

   private static final Log log = ExoLogger.getLogger(BandwidthAllocationTestCase.class);

   /**
    * The alphabet to content.
    */
   private static final String ALPHABET = "qwertyuiop[]asdfghjkl;'zxcvbnm,./1234567890-=!@#$%^&*()_+|:?><";

   /**
    * The random value.
    */
   private static final int RANDOM_VALUE = 1124517;

   /**
    * BandwidthAllocationTestCase constructor.
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
   public BandwidthAllocationTestCase(RepositoryService repositoryService, String reposytoryName, String workspaceName,
      String userName, String password)
   {
      super(repositoryService, reposytoryName, workspaceName, userName, password);
   }

   /**
    * createBaseNode.
    * 
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer createBaseNode(String repoPath, String nodeName)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         Node baseNode = addNodePath(repoPath).addNode(nodeName, "nt:unstructured");
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't locked: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * addEmptyNode.
    * 
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @param iterations
    *          how many iterations adding the empty node
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer addEmptyNode(String repoPath, String nodeName, long iterations)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         for (int i = 0; i < iterations; i++)
         {
            String normalizePath = getNormalizePath(repoPath);
            Node baseNode = (Node)session.getItem(normalizePath);

            Node emptyNode = baseNode.addNode(nodeName + "_" + i, "nt:base");

            session.save();
         }

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't locked: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * addStringPropertyOnly.
    * 
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @param size
    *          the size of string property
    * @param iterations
    *          how many iterations adding the string property
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer addStringPropertyOnly(String repoPath, String nodeName, Long size, long iterations)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         // create random value
         String sValue = "";
         for (int i = 0; i < size; i++)
         {
            int sIndex = (int)(Math.random() * RANDOM_VALUE) % ALPHABET.length();
            sValue += ALPHABET.substring(sIndex, sIndex + 1);
         }

         for (int i = 0; i < iterations; i++)
         {
            String normalizePath = getNormalizePath(repoPath);
            Node baseNode = ((Node)session.getItem(normalizePath)).getNode(nodeName);
            baseNode.setProperty("d", sValue);
            // log.info("ADD propety + " + sValue.length() + " B");
            session.save();
         }

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't add the string propery: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * addBinaryPropertyOnly.
    * 
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @param size
    *          the size of binary property
    * @param iterations
    *          how many iterations adding the binary property
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer addBinaryPropertyOnly(String repoPath, String nodeName, Long size, long iterations)
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
            buf[i] = (byte)(i % BaseReplicationTestCase.DIVIDER);

         for (long i = 0; i < size / BUFFER_SIZE; i++)
            fos.write(buf);
         fos.write(buf, 0, (int)(size % BUFFER_SIZE));
         fos.close();

         start = System.currentTimeMillis(); // to get the time of start
         for (int i = 0; i < iterations; i++)
         {
            String normalizePath = getNormalizePath(repoPath);
            Node baseNode = ((Node)session.getItem(normalizePath)).getNode(nodeName);
            baseNode.setProperty("d", new FileInputStream(tempFile));

            session.save();
         }

         end = System.currentTimeMillis();

         log.info("The time of the adding of nt:file + " + iterations + "( " + tempFile.length() + " B ) : "
            + ((end - start) / BaseReplicationTestCase.ONE_SECONDS) + " sec");

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't save the binary value : ", e);
         sb.append("fail");
      }
      finally
      {
         tempFile.delete();
      }

      return sb;
   }

}
