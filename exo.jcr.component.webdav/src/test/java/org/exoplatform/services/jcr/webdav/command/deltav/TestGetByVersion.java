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
package org.exoplatform.services.jcr.webdav.command.deltav;

import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.ws.rs.core.MediaType;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class TestGetByVersion extends BaseStandaloneTest
{

   private Node getByVersionNode;

   private void assertResponseContent(String path, String versionName, String content) throws IOException,
      VersionException, UnsupportedRepositoryOperationException, PathNotFoundException, RepositoryException
   {
      Version version =
         session.getRootNode().getNode(TextUtil.relativizePath(path)).getVersionHistory().getVersion(versionName);
      String receivedContent =
         version.getNode("jcr:frozenNode").getNode("jcr:content").getProperty("jcr:data").getString();
      assertEquals(content, receivedContent);
   }

   public void testGetByVersion1() throws Exception
   {
      String CONTENT1 = "TEST BASE FILE CONTENT..";
      String CONTENT2 = "TEST CONTENT FOR SECOND VERSION..";
      String CONTENT3 = "TEST CONTENT FOR THIRD VERSION OF THE FILE..";

      // NullResourceLocksHolder lockHolder = new NullResourceLocksHolder();

      String path = TestUtils.getFileName();
      Node node =
         TestUtils.addContent(session, path, new ByteArrayInputStream(CONTENT1.getBytes()), defaultFileNodeType,
            MediaType.TEXT_PLAIN);
      createVersion(node, new ByteArrayInputStream(CONTENT2.getBytes()), MediaType.TEXT_PLAIN, new ArrayList<String>());
      createVersion(node, new ByteArrayInputStream(CONTENT3.getBytes()), MediaType.TEXT_PLAIN, new ArrayList<String>());
      assertResponseContent(path, "1", CONTENT1);
      assertResponseContent(path, "2", CONTENT2);
      assertResponseContent(path, "3", CONTENT3);
   }

   private final void createVersion(Node fileNode, InputStream inputStream, String mimeType, List<String> mixins)
      throws RepositoryException
   {
      if (!fileNode.isNodeType("mix:versionable"))
      {
         if (fileNode.canAddMixin("mix:versionable"))
         {
            fileNode.addMixin("mix:versionable");
            fileNode.getSession().save();
         }
         fileNode.checkin();
         fileNode.getSession().save();
      }

      if (!fileNode.isCheckedOut())
      {
         fileNode.checkout();
         fileNode.getSession().save();
      }

      updateContent(fileNode, inputStream, mimeType, mixins);
      fileNode.getSession().save();
      fileNode.checkin();
      fileNode.getSession().save();
   }

   private final void updateContent(Node node, InputStream inputStream, String mimeType, List<String> mixins)
      throws RepositoryException
   {

      Node content = node.getNode("jcr:content");
      content.setProperty("jcr:mimeType", mimeType);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", inputStream);

      while (mixins.iterator().hasNext())
      {
         content.addMixin(mixins.iterator().next());
      }
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
