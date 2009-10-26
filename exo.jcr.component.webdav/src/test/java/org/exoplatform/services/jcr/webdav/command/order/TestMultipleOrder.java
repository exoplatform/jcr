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
package org.exoplatform.services.jcr.webdav.command.order;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.rest.ext.provider.HierarchicalPropertyEntityProvider;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

import javax.jcr.Node;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class TestMultipleOrder extends OrderPatchTest
{

   protected Node multipleOrderNode;

   public void setUp() throws Exception
   {
      super.setUp();
      session.refresh(false);
      if (multipleOrderNode == null)
      {
         multipleOrderNode = orderPatchNode.addNode("multipleOrderNode", ORDERABLE_NODETYPE);
         session.save();
         for (int i = 1; i <= 5; i++)
         {
            multipleOrderNode.addNode("n" + i, ORDERABLE_NODETYPE);
         }
         session.save();
      }
   }

   public void testMultipleOrder() throws Exception
   {
      assertOrder(multipleOrderNode, new String[]{"n1", "n2", "n3", "n4", "n5"});

      String path = multipleOrderNode.getPath();
      // 1 2 3 4 5 1 2 3 4 5
      // 1 before 4 2 3 1 4 5
      // 4 before 3 2 4 3 1 5
      // 2 last 4 3 1 5 2
      // 5 first 5 4 3 1 2
      // 3 after 2 5 4 1 2 3
      // 5 after 1 4 1 5 2 3

      String xml =
         "" + "<D:orderpatch xmlns:D=\"DAV:\">" +

         "<D:order-member>" + "<D:segment>n1</D:segment>" + "<D:position>"
            + "<D:before><D:segment>n4</D:segment></D:before>" + "</D:position>" + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n4</D:segment>" + "<D:position>"
            + "<D:before><D:segment>n3</D:segment></D:before>" + "</D:position>" + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n2</D:segment>" + "<D:position><D:last/></D:position>"
            + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n5</D:segment>" + "<D:position><D:first/></D:position>"
            + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n3</D:segment>" + "<D:position>"
            + "<D:after><D:segment>n2</D:segment></D:after>" + "</D:position>" + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n5</D:segment>" + "<D:position>"
            + "<D:after><D:segment>n1</D:segment></D:after>" + "</D:position>" + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n0</D:segment>" + "<D:position><D:first/></D:position>"
            + "</D:order-member>" +

            "</D:orderpatch>";

      // HierarchicalProperty body = body(xml);
      ContainerResponse response =
         service(WebDAVMethods.ORDERPATCH, getPathWS() + URLEncoder.encode(path, "UTF-8"), "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      OrderPatchResponseEntity entity = (OrderPatchResponseEntity)response.getEntity();
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      entity.write(outStream);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString();
      HierarchicalPropertyEntityProvider entityProvider = new HierarchicalPropertyEntityProvider();
      HierarchicalProperty multistatus =
         entityProvider.readFrom(null, null, null, null, null, new ByteArrayInputStream(resp.getBytes()));
      assertEquals(new QName("DAV:", "multistatus"), multistatus.getName());

      for (int i = 0; i < 6; i++)
      {
         HierarchicalProperty respProperty = multistatus.getChild(i);
         String okStatus = WebDavConst.getStatusDescription(HTTPStatus.OK);
         assertEquals(okStatus, respProperty.getChild(new QName("DAV:", "status")).getValue());
      }
      HierarchicalProperty badResp = multistatus.getChild(6);
      String forbiddenStatus = WebDavConst.getStatusDescription(HTTPStatus.FORBIDDEN);
      assertEquals(forbiddenStatus, badResp.getChild(new QName("DAV:", "status")).getValue());
      //    
      // // 4 1 5 2 3
      assertOrder(multipleOrderNode, new String[]{"n4", "n1", "n5", "n2", "n3"});
   }

}
