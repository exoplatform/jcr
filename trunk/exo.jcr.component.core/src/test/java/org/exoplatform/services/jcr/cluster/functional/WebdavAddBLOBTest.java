/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.cluster.functional;

import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.common.http.client.HttpOutputStream;
import org.exoplatform.services.jcr.cluster.BaseClusteringFunctionalTest;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id$
 */
public class WebdavAddBLOBTest extends BaseClusteringFunctionalTest
{

   /**
    * Since blob stored in memory we can not use really big data.
    */
   private int sizeInMb = 5;

   public void testAddBLOB() throws Exception
   {
      // make a test blob
      byte[] data = new byte[sizeInMb * 1024 * 1024];
      Random random = new Random();
      random.nextBytes(data);

      JCRWebdavConnection conn = getConnection();
      
      // add node with blob data
      HttpOutputStream stream = new HttpOutputStream();
      HTTPResponse response = conn.addNode(nodeName, stream);
      loadStream(stream, new ByteArrayInputStream(data));
      stream.close();
      response.getStatusCode();

      // check results
      for (JCRWebdavConnection connection : getConnections()) 
      {
         response = connection.getNode(nodeName);
         assertEquals(200, response.getStatusCode());
         byte[] respData = response.getData();
         assertEquals(data.length, respData.length);
         assertTrue(java.util.Arrays.equals(data, respData));
      }
   }

   protected void loadStream(HttpOutputStream stream, InputStream in) throws IOException
   {
      byte[] buf = new byte[1024]; // 1Kb

      int readed = 0;
      while ((readed = in.read(buf)) != -1)
      {
         stream.write(buf, 0, readed);
      }
   }
}
