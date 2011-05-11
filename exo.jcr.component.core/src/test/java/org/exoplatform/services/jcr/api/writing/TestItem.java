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
package org.exoplatform.services.jcr.api.writing;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestItem.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestItem extends JcrAPIBaseTest
{

   public void testSave() throws RepositoryException
   {
      Node root = session.getRootNode();
      try
      {
         root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:propertyDefinition");
         root.save();
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
      }

      // assertEquals("/childNode/childNode2", root.getNode("childNode/childNode2").getPath());
      session.refresh(false);

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      root = session.getRootNode();

      try
      {
         root.getNode("childNode/childNode2");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      Node node1 = root.addNode("testSave1", "exo:mockNodeType");
      node1.setProperty("jcr:nodeTypeName", "test");
      root.save();

      Node node2 = root.addNode("testSave2", "nt:unstructured");
      node2.addNode("node2BRem", "nt:unstructured");
      node2.setProperty("existingProp", "existingValue");
      node2.setProperty("existingProp2", "existingValue2");
      root.save();

      node2.setProperty("prop", "propValue");
      node2.setProperty("existingProp", "existingValueBis");

      node2.getProperty("existingProp2").remove();
      node2.getNode("node2BRem").remove();

      node2.addNode("addedNode", "nt:unstructured");
      // System.out.println(">>>>>>>>>>>>"+node.getProperty("prop"));
      node2.save();

      try
      {
         node2.getProperty("existingProp2");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      try
      {
         node2.getNode("node2BRem");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      // System.out.println(">>>>>>>>>>>>"+session.getTransientNodesManager().dump());

      // System.out.println(">>>>>>>>>>>>"+((Node)session.getItem("/childNode")).getProperty("prop"));

      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node node22 = session.getRootNode().getNode("testSave2");
      root = session.getRootNode();
      try
      {
         node22.getProperty("prop");
      }
      catch (PathNotFoundException e)
      {
         fail("exception should not be thrown");
      }

      assertEquals("existingValueBis", node22.getProperty("existingProp").getString());
      try
      {
         node22.getProperty("existingProp2");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      List<PropertyData> props =
         session.getTransientNodesManager().getChildPropertiesData((NodeData)((NodeImpl)node22).getData());
      for (PropertyData prop : props)
      {
         if (log.isDebugEnabled())
            log.debug("PROPS >>>>>>>>>>>> " + prop.getQPath().getAsString());
      }

      try
      {
         node22.getNode("node2BRem");
         fail("exception should have been thrown");
      }
      catch (PathNotFoundException e)
      {
      }

      root.getNode("testSave1").remove();
      root.getNode("testSave2").remove();
      session.save();
   }
}
