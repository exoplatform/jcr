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
 * <br/>Date: 2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: WebdavCreatePropertyTest.java 1518 2010-01-20 23:33:30Z sergiykarpenko $
 */
public class WebdavCreatePropertyTest
   extends BaseClusteringFunctionalTest
{

   public void testCreatePropertyTest() throws Exception
   {
      JCRWebdavConnection conn = getConnection();
      
      String property = "D:testProp";
      
      conn.addNode(nodeName, "nt:untstructured","".getBytes());
      conn.addProperty(nodeName, property);
      
      // check is property exist
      for (JCRWebdavConnection connection : getConnections())
      {
         HTTPResponse response = connection.getProperty(nodeName, property);
         assertEquals(207, response.getStatusCode());
         assertEquals("value", getPropertyValue(response.getData(), "D:testProp"));
      }
      
   }
}
