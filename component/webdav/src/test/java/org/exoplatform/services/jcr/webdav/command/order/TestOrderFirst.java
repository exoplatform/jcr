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
import org.exoplatform.services.jcr.webdav.util.TextUtil;
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

public class TestOrderFirst extends OrderPatchTest
{

   protected Node orderFirstNode;

   public void setUp() throws Exception
   {
      super.setUp();
      session.refresh(false);
      if (orderFirstNode == null)
      {
         orderFirstNode = orderPatchNode.addNode("orderFirstNode", ORDERABLE_NODETYPE);
         session.save();
         for (int i = 1; i <= 5; i++)
         {
            orderFirstNode.addNode("n" + i, ORDERABLE_NODETYPE);
         }
         session.save();
      }
   }

   public void testOrderFirst1() throws Exception
   {
      assertOrder(orderFirstNode, new String[]{"n1", "n2", "n3", "n4", "n5"});

      String path = orderFirstNode.getPath();
      String xml =
         "" + "<D:orderpatch xmlns:D=\"DAV:\">" + "<D:order-member>" + "<D:segment>n3</D:segment>"
            + "<D:position><D:first/></D:position>" + "</D:order-member>" + "</D:orderpatch>";
      ContainerResponse response =
         service(WebDAVMethods.ORDERPATCH, getPathWS() + URLEncoder.encode(path, "UTF-8"), "", null, xml.getBytes());
      assertEquals(HTTPStatus.OK, response.getStatus());
      assertOrder(orderFirstNode, new String[]{"n3", "n1", "n2", "n4", "n5"});
   }

   public void testOrderFirst2() throws Exception
   {
      assertOrder(orderFirstNode, new String[]{"n1", "n2", "n3", "n4", "n5"});

      String path = orderFirstNode.getPath();
      String xml =
         "" + "<D:orderpatch xmlns:D=\"DAV:\">" + "<D:order-member>" + "<D:segment>n1</D:segment>"
            + "<D:position><D:first/></D:position>" + "</D:order-member>" + "</D:orderpatch>";
      ContainerResponse response =
         service(WebDAVMethods.ORDERPATCH, getPathWS() + URLEncoder.encode(path, "UTF-8"), "", null, xml.getBytes());
      assertEquals(HTTPStatus.OK, response.getStatus());
      assertOrder(orderFirstNode, new String[]{"n1", "n2", "n3", "n4", "n5"});
   }

   public void testOrderFirst3() throws Exception
   {
      assertOrder(orderFirstNode, new String[]{"n1", "n2", "n3", "n4", "n5"});

      String path = orderFirstNode.getPath();

      String xml =
         "" + "<D:orderpatch xmlns:D=\"DAV:\">" + "<D:order-member>" + "<D:segment>n0</D:segment>"
            + "<D:position><D:first/></D:position>" + "</D:order-member>" + "</D:orderpatch>";

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

      assertEquals(1, multistatus.getChildren().size());
      assertEquals(new QName("DAV:", "response"), multistatus.getChild(0).getName());

      String hrefMustBe = TextUtil.escape(getPathWS() + orderFirstNode.getPath() + "/n0", '%', true);
      HierarchicalProperty r = multistatus.getChild(0);
      String href = r.getChild(new QName("DAV:", "href")).getValue();
      assertEquals(hrefMustBe, href);

      String statusMustBe = WebDavConst.getStatusDescription(HTTPStatus.FORBIDDEN);
      String status = r.getChild(new QName("DAV:", "status")).getValue();
      assertEquals(statusMustBe, status);

      assertOrder(orderFirstNode, new String[]{"n1", "n2", "n3", "n4", "n5"});
   }

}
