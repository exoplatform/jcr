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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 24.10.2006
 * 
 * @version $Id: DeleteThread.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class DeleteThread extends UserThread
{

   public DeleteThread(Session threadSession)
   {
      super(threadSession);
   }

   public void testAction()
   {
      while (process || TestConcurrentItems.consumedNodes.size() > 0)
      {
         deleteAction();
         try
         {
            sleep(2500);
         }
         catch (InterruptedException e)
         {
            threadLog.error("Sleep error: " + e.getMessage(), e);
         }
      }
   }

   public void deleteAction()
   {

      final String[] nodes =
         TestConcurrentItems.consumedNodes.toArray(new String[TestConcurrentItems.consumedNodes.size()]);
      try
      {
         threadSession.refresh(false);
      }
      catch (RepositoryException th)
      {
         threadLog.error("Refresh before delete error: " + th.getMessage(), th);
      }
      for (String nodePath : nodes)
      {
         String nodeInfo = "";
         try
         {
            Node node = (Node)threadSession.getItem(nodePath);
            PropertyImpl data = (PropertyImpl)node.getProperty("jcr:content/jcr:data");
            nodeInfo = "node: " + node.getPath() + ", data: " + data.getInternalIdentifier();
            node.remove();
            threadSession.save();
            if (threadLog.isDebugEnabled())
               threadLog.debug("Delete " + nodeInfo);
         }
         catch (PathNotFoundException e)
         {
            threadLog.warn(e.getMessage());
         }
         catch (RepositoryException e)
         {
            try
            {
               threadSession.refresh(false);
            }
            catch (RepositoryException e1)
            {
               threadLog.error("Rollback repository error: " + e1.getMessage() + ". Root exception " + e, e);
            }
         }
         catch (Throwable th)
         {
            threadLog.error("Delete error: " + th.getMessage() + ". " + nodeInfo, th);
         }
         finally
         {
            TestConcurrentItems.consumedNodes.remove(nodePath);
         }
      }
   }
}
