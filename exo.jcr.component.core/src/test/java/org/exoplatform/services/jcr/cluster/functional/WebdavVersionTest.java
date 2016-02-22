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
import org.exoplatform.services.jcr.cluster.BaseClusteringFunctionalTest;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class WebdavVersionTest
   extends BaseClusteringFunctionalTest
{
   public void testVersioning() throws Exception
   {
      JCRWebdavConnection conn = getConnection();
      
      conn.addNode(nodeName, "v1".getBytes());
      
      conn.addVersionControl(nodeName);
      
      conn.checkIn(nodeName);
      conn.checkOut(nodeName);
      
      // check
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.getNode(nodeName);
         assertEquals(200, response.getStatusCode());
         assertTrue("v1".equals(new String(response.getData())));
      }
      
      // create version
      conn.checkOut(nodeName);
      conn.addNode(nodeName, "v2".getBytes());
      conn.checkIn(nodeName);
      
      // create version      
      conn.checkOut(nodeName);
      conn.addNode(nodeName, "v3".getBytes());
      conn.checkIn(nodeName);
      
      // create version
      conn.checkOut(nodeName);
      conn.addNode(nodeName, "v4".getBytes());
      conn.checkIn(nodeName);
      
      // check
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.getNode(nodeName);
         assertEquals(200, response.getStatusCode());
         assertTrue("v4".equals(new String(response.getData())));
      }
      
      //check v1
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.restore(nodeName, "1");
         assertEquals(200, response.getStatusCode());
         assertTrue("v1".equals(new String(response.getData())));
      }
      
      //check v2
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.restore(nodeName, "2");
         assertEquals(200, response.getStatusCode());
         assertTrue("v2".equals(new String(response.getData())));
      }
      
      //check v2
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.restore(nodeName, "3");
         assertEquals(200, response.getStatusCode());
         assertTrue("v3".equals(new String(response.getData())));
      }
   }
}
