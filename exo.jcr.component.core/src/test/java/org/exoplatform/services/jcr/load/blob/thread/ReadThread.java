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
package org.exoplatform.services.jcr.load.blob.thread;

import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.load.blob.TestConcurrentItems;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 24.10.2006
 * 
 * @version $Id: ReadThread.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ReadThread extends UserThread
{

   public ReadThread(Session threadSession)
   {
      super(threadSession);
   }

   public void testAction()
   {
      while (process)
      {
         readAction();
         try
         {
            sleep(200);
         }
         catch (InterruptedException e)
         {
            threadLog.error("Sleep error: " + e.getMessage(), e);
         }
      }
   }

   public void readAction()
   {

      final List<String> readedNodes = new ArrayList<String>();
      int dataSizeInfo = 0;
      try
      {
         threadSession.refresh(false);
         Node testRoot = threadSession.getRootNode().getNode(TestConcurrentItems.TEST_ROOT);
         NodeIterator nodes = testRoot.getNodes();
         while (nodes.hasNext())
         {
            Node node = nodes.nextNode();
            Node content = node.getNode("jcr:content");
            InputStream dataStream = null;
            int dataSize = 0;
            try
            {
               PropertyImpl data = (PropertyImpl)content.getProperty("jcr:data");
               dataStream = data.getStream();
               // threadLog.info("Read property " + data.getPath() + ", " + data.getInternalUUID());
               byte[] buff = new byte[1024 * 4];
               int read = 0;
               dataSize = 0;
               while ((read = dataStream.read(buff)) >= 0)
               {
                  dataSize += read;
               }
               if (dataSize != TestConcurrentItems.TEST_FILE_SIZE)
                  threadLog.error("Wrong data size. " + dataSize + " but expected "
                     + TestConcurrentItems.TEST_FILE_SIZE + ". " + dataStream + ". " + data.getPath() + " "
                     + data.getInternalIdentifier());
               else if (threadLog.isDebugEnabled())
                  threadLog.debug("Read node: " + dataStream + ", " + node.getPath() + ", data: "
                     + data.getInternalIdentifier());
            }
            catch (RepositoryException e)
            {
               threadLog.error("Repository error: " + e.getMessage() + ", " + dataSize + " bytes from "
                  + TestConcurrentItems.TEST_FILE_SIZE, e);
            }
            catch (FileNotFoundException e)
            {
               threadLog.error("File not found, stream: " + dataStream + ", " + e.getMessage(), e);
            }
            finally
            {
               if (dataStream != null)
                  dataStream.close();
               dataSizeInfo = dataSize;
            }
            readedNodes.add(node.getPath());
            // threadLog.info("Read node " + node.getPath());
         }
      }
      catch (Throwable th)
      {
         threadLog.error("Read error: " + th.getMessage() + ", " + dataSizeInfo + " bytes from "
            + TestConcurrentItems.TEST_FILE_SIZE, th);
      }
      finally
      {
         TestConcurrentItems.consumedNodes.addAll(readedNodes);
      }
   }
}
