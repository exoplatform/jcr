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

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.services.jcr.cluster.BaseClusteringFunctionalTest;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class WebdavLockNodeTest
   extends BaseClusteringFunctionalTest
{
   public void testLock() throws Exception
   {
      JCRWebdavConnection conn = getConnection();
      
      // prepare
      String lockedNodeName = "locked_node";
      
      conn.addDir(nodeName);
      conn.addNode(nodeName + "/" + lockedNodeName, "".getBytes());
      
      // lock
      String lockToken =  conn.lock(nodeName + "/" + lockedNodeName);
      
      // check
      for (JCRWebdavConnection connection : getConnections()) 
      {
         HTTPResponse response = connection.removeNode(nodeName + "/" + lockedNodeName);
         assertEquals(HTTPStatus.LOCKED, response.getStatusCode());
      }
      
      // unloc
      conn.unlock(nodeName + "/" + lockedNodeName, lockToken);
      
      HTTPResponse resp = getConnection().removeNode(nodeName + "/" + lockedNodeName);
      assertEquals(HTTPStatus.NO_CONTENT, resp.getStatusCode());
      
      // ckeck
      for (JCRWebdavConnection connection : getConnections()) 
      {
         HTTPResponse response = connection.getNode(nodeName + "/" + lockedNodeName);
         assertEquals(HTTPStatus.NOT_FOUND, response.getStatusCode());
      }
   }
}
