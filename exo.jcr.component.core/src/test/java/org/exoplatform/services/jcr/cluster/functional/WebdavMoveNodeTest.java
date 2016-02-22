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
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id$
 */
public class WebdavMoveNodeTest extends BaseClusteringFunctionalTest
{
   public void testMoveNode() throws Exception
   {
      JCRWebdavConnection conn = getConnection();
      
      String newNodeName = nodeName + "new";

      // add node 
      conn.addNode(nodeName, "".getBytes());

      // check is node exist
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.getNode(nodeName);
         assertEquals(200, response.getStatusCode());
      }

      // move node
      conn = getConnection();
      
      HTTPResponse response = conn.moveNode(nodeName, newNodeName);
      assertEquals(201, response.getStatusCode());

      // check is node not exist
      for (JCRWebdavConnection connection : getConnections()) 
      {
         response = connection.getNode(nodeName);
         assertEquals(404, response.getStatusCode());
      }

      // check is node exist
      for (JCRWebdavConnection connection : getConnections()) 
      {
         response = connection.getNode(newNodeName);
         assertEquals(200, response.getStatusCode());
      }
   }
}
