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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.usecases.common;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;

public class TestCopySameNameSibling extends BaseUsecasesTest
{

   public void testCopySameNameSibling() throws RepositoryException
   {
      Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()), WORKSPACE);
      Node root = session.getRootNode();

      Node subRoot = root.addNode("jcrTest");
      Node testNode = subRoot.addNode("testNode");
      root.save();
      session.save();

      testNode = session.getRootNode().getNode("jcrTest/testNode");
      String srcPath = testNode.getPath();
      String destPath = subRoot.getPath() + srcPath.substring(srcPath.lastIndexOf("/"));
      Workspace workspace = session.getWorkspace();
      workspace.copy(srcPath, destPath);
      session.save();
      Node sameNameNode = session.getRootNode().getNode("jcrTest/testNode[2]");
      assertNotNull(sameNameNode);
      // copy same name testNode[2]
      srcPath = sameNameNode.getPath();
      // [VO] 27.07.06 Bug fix. Use old destPath.
      // destPath = subRoot.getPath() + srcPath.substring(srcPath.lastIndexOf("/")) ;
      try
      {
         workspace.copy(srcPath, destPath);
      }
      catch (Exception e)
      {
         fail("\n======>Can not copy. Exception is occur:" + e.getMessage());
      }

      subRoot.remove();
      session.save();
   }

}
