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
package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestNtAutorcreating extends JcrImplBaseTest
{
   /**
    * Class logger.
    */
   private static boolean registred = false;

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TestNtAutorcreating");

   public void testAutocreate() throws Exception
   {
      registerNodetypes();
      Node myParentNode = root.addNode("testNode");
      Node myNode = myParentNode.addNode("myNodeName", "exo:myTypeJCR805");
      Node myChildNode = myNode.getNode("exo:myChildNode");
      myChildNode.setProperty("exo:myValue", "myTestValue");
      session.save();
   }

   public void testAutocreateChildNodes() throws Exception
   {
      registerNodetypes();
      Node myParentNode = root.addNode("testNode");
      Node myNode = myParentNode.addNode("myNodeName", "exo:myTypeJCR806");
      assertEquals(1, myNode.getNodes().getSize());

      Node myChildNode = myNode.getNode("exo:myChildNode");
      session.save();
      assertEquals(1, myNode.getNodes().getSize());
   }

   public void test2() throws Exception
   {
      registerNodetypes();
      Node myParentNode = root.addNode("testNode");
      Node myNode = myParentNode.addNode("myNodeName", "exo:myTypeJCR805");
      Node myChildNode = myNode.getNode("exo:myChildNode");
      try
      {
         session.save();
         fail();
      }
      catch (ConstraintViolationException e)
      {
         // e.printStackTrace();
      }
   }

   private void registerNodetypes() throws Exception
   {
      if (!registred)
      {
         InputStream xml =
            this.getClass().getResourceAsStream("/org/exoplatform/services/jcr/impl/core/nodetype/test-nodetypes.xml");
         repositoryService.getCurrentRepository().getNodeTypeManager().registerNodeTypes(xml,
            ExtendedNodeTypeManager.FAIL_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
         registred = true;
      }
   }
}
