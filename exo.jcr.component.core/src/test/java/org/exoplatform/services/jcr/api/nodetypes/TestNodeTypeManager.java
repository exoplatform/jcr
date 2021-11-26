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

package org.exoplatform.services.jcr.api.nodetypes;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestNodeTypeManager.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestNodeTypeManager extends JcrAPIBaseTest
{

   public void testGetNodeType() throws Exception
   {

      final NodeTypeManager ntManager = this.session.getWorkspace().getNodeTypeManager();
      final NodeType type = ntManager.getNodeType("nt:unstructured");
      assertEquals("nt:unstructured", type.getName());

      try
      {
         ntManager.getNodeType("nt:not-found");
         fail("exception should have been thrown");
      }
      catch (final NoSuchNodeTypeException e)
      {
      }

   }

   public void testGetNodeTypes() throws Exception
   {
      final NodeTypeManager ntManager = this.session.getWorkspace().getNodeTypeManager();
      assertTrue(ntManager.getAllNodeTypes().getSize() > 0);
      assertTrue(ntManager.getPrimaryNodeTypes().getSize() > 0);
      assertTrue(ntManager.getMixinNodeTypes().getSize() > 0);
   }

   //   public void testNtQueryNtBase() throws Exception
   //   {
   //      final NodeTypeDataManagerImpl ntManager =
   //         (NodeTypeDataManagerImpl)this.session.getWorkspace().getNodeTypesHolder();
   //
   //      assertTrue(ntManager.getNodeSearcher().getNodesByNodeType(Constants.MIX_VERSIONABLE).size() == 0);
   //      final Node t = this.root.addNode("tt");
   //      t.addMixin("mix:versionable");
   //      this.session.save();
   //      assertTrue(ntManager.getNodeSearcher().getNodesByNodeType(Constants.MIX_VERSIONABLE).size() != 0);
   //   }
}
