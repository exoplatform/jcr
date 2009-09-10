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
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 24.10.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CreateThread.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class CreateThread extends UserThread
{

   public CreateThread(Session threadSession)
   {
      super(threadSession);
   }

   public void testAction()
   {
      while (process)
      {
         createAction();
         try
         {
            sleep(1500);
         }
         catch (InterruptedException e)
         {
            threadLog.error("Sleep error: " + e.getMessage(), e);
         }
      }
   }

   public void createAction()
   {

      String nodeName = IdGenerator.generate();
      InputStream dataStream = null;
      try
      {
         Node testRoot = threadSession.getRootNode().getNode(TestConcurrentItems.TEST_ROOT);
         Node ntFile = testRoot.addNode(nodeName, "nt:file");
         Node contentNode = ntFile.addNode("jcr:content", "nt:resource");
         // dataStream = new URL(TestSwap.URL_BIG_MEDIA_FILE).openStream();
         dataStream = new FileInputStream(TestConcurrentItems.TEST_FILE);
         PropertyImpl data = (PropertyImpl)contentNode.setProperty("jcr:data", dataStream);
         contentNode.setProperty("jcr:mimeType", "video/avi");
         contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
         this.threadSession.save();
         if (threadLog.isDebugEnabled())
            threadLog.debug("Create node: " + ntFile.getPath() + ", data: " + data.getInternalIdentifier());
      }
      catch (Throwable th)
      {
         threadLog.error("Create error: " + th.getMessage(), th);
      }
      finally
      {
         if (dataStream != null)
            try
            {
               dataStream.close();
            }
            catch (IOException e)
            {
               threadLog.error("Stream read error: " + e.getMessage(), e);
            }
         try
         {
            this.threadSession.refresh(false);
         }
         catch (Throwable th)
         {
            threadLog.error("Session refresh error: " + th.getMessage());
         }
      }
   }
}
