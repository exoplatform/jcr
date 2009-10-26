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
package org.exoplatform.services.jcr.ext.replication.test;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BaseReplicationTestCase.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public abstract class BaseReplicationTestCase
{
   /**
    * The apache logger.
    */
   private static final Log log = ExoLogger.getLogger("ext.BaseReplicationTestCase");

   /**
    * Definition the size of buffer.
    */
   protected static final int BUFFER_SIZE = 1024;

   /**
    * Definition the divider.
    */
   public static final int DIVIDER = 255;

   /**
    * Definition the constants to on seconds.
    */
   public static final double ONE_SECONDS = 1000;

   /**
    * The JCR session.
    */
   protected Session session;

   /**
    * The root node in workspace.
    */
   protected Node rootNode;

   /**
    * The Credentials to workspace.
    */
   private Credentials credentials;

   /**
    * The JCR repository.
    */
   protected Repository repository;

   /**
    * BaseReplicationTestCase constructor.
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
   public BaseReplicationTestCase(RepositoryService repositoryService, String reposytoryName, String workspaceName,
      String userName, String password)
   {
      try
      {
         credentials = new CredentialsImpl(userName, password.toCharArray());

         repository = repositoryService.getRepository(reposytoryName);

         session = repository.login(credentials, workspaceName);

         rootNode = session.getRootNode();

      }
      catch (RepositoryException e)
      {
         log.error("Can't start BaseReplicationTestCase", e);
      }
      catch (RepositoryConfigurationException e)
      {
         log.error("Can't start BaseReplicationTestCase", e);
      }
   }

   /**
    * addNodePath.
    * 
    * @param repoPath
    *          the repository path
    * @return Node the Node
    * @throws RepositoryException
    *           will be generated the RepositoryException.
    */
   protected Node addNodePath(String repoPath) throws RepositoryException
   {
      Node resultNode = rootNode;
      String[] sArray = repoPath.split("[::]");

      for (String nodeName : sArray)
         if(!nodeName.equals(""))
            if (resultNode.hasNode(nodeName))
               resultNode = resultNode.getNode(nodeName);
            else
               resultNode = resultNode.addNode(nodeName, "nt:unstructured");

      return resultNode;
   }

   /**
    * getNormalizePath.
    * 
    * @param repoPath
    *          the repository path split '::'
    * @return String return the repository path split '/'
    */
   protected String getNormalizePath(String repoPath)
   {
      // return repoPath;
      // TODO remove it ?
      return repoPath.replaceAll("[:][:]", "/");
   }

   /**
    * compareStream.
    * 
    * @param etalon
    *          the eatalon stream
    * @param data
    *          the testing stream
    * @throws Exception
    *           will be generated the Exception.
    */
   protected void compareStream(InputStream etalon, InputStream data) throws Exception
   {
      compareStream(etalon, data, 0, 0, -1);
   }

   /**
    * Compare etalon stream with data stream begining from the offset in etalon and position in data.
    * Length bytes will be readed and compared. if length is lower 0 then compare streams till one of
    * them will be read.
    * 
    * @param etalon
    *          the etalon stream
    * @param data
    *          testing stream
    * @param etalonPos
    *          etalon position
    * @param dataPos
    *          testing position
    * @param length
    *          the length
    * @throws Exception
    *           will be generated the Exception
    */
   protected void compareStream(InputStream etalon, InputStream data, long etalonPos, long dataPos, long length)
      throws Exception
   {

      int dindex = 0;

      byte[] ebuff = new byte[BUFFER_SIZE];
      int eread = 0;

      while ((eread = etalon.read(ebuff)) > 0)
      {

         byte[] dbuff = new byte[eread];
         int erindex = 0;
         while (erindex < eread)
         {
            int dread = -1;
            try
            {
               dread = data.read(dbuff);
            }
            catch (IOException e)
            {
               throw new Exception("Streams is not equals by length or data stream is unreadable. Cause: "
                  + e.getMessage());
            }

            if (dread == -1)
               throw new Exception("Streams is not equals by length. Data end-of-stream reached at position " + dindex);

            for (int i = 0; i < dread; i++)
            {
               byte eb = ebuff[i];
               byte db = dbuff[i];
               if (eb != db)
                  throw new Exception("Streams is not equals. Wrong byte stored at position " + dindex
                     + " of data stream. Expected 0x" + Integer.toHexString(eb) + " '" + new String(new byte[]{eb})
                     + "' but found 0x" + Integer.toHexString(db) + " '" + new String(new byte[]{db}) + "'");

               erindex++;
               dindex++;
               if (length > 0 && dindex >= length)
                  return;
            }

            if (dread < eread)
               dbuff = new byte[eread - dread];
         }
      }

      if (data.available() > 0)
         throw new Exception("Streams is not equals by length. Data stream contains more data. Were read " + dindex);
   }
}
