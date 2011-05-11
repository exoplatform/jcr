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
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import java.io.ByteArrayInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestNodeReference.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestNodeReference extends JcrAPIBaseTest
{

   public void testGetReferences() throws RepositoryException
   {

      Node root = session.getRootNode();

      Node testNode = root.addNode("testGetReferences", "nt:unstructured");

      Node testNode1 = root.addNode("testGetReferences1", "nt:unstructured");

      testNode.addMixin("mix:referenceable");
      testNode1.addMixin("mix:referenceable");

      // Should be saved first
      root.save();

      Node n1 = root.addNode("n1", "nt:unstructured");
      Node n2 = root.addNode("n2", "nt:unstructured");
      Node n11 = root.addNode("n11", "nt:unstructured");

      n1.setProperty("p1", new String[]{((NodeImpl)testNode).getInternalIdentifier(),
         ((NodeImpl)testNode1).getInternalIdentifier()}, PropertyType.REFERENCE);

      //      n1.setProperty("p1", testNode);
      // n2.setProperty("p1", testNode);
      //      n11.setProperty("p1", testNode1);

      root.save();

      PropertyIterator refs = testNode.getReferences();
      if (log.isDebugEnabled())
      {
         while (refs.hasNext())
         {
            log.debug("ref >>>" + refs.nextProperty());
         }
      }
      assertEquals(1, testNode.getReferences().getSize());
      assertEquals(1, testNode1.getReferences().getSize());

      testNode.remove();

      try
      {
         root.save();
         fail("ReferentialIntegrityException");
      }
      catch (ReferentialIntegrityException e)
      {
      }
   }

   public void testGetReferences2() throws RepositoryException
   {

      Node root = session.getRootNode();

      Node testNode = root.addNode("testGetReferences", "nt:unstructured");
      Node testNode1 = root.addNode("testGetReferences1", "nt:unstructured");
      Node testNode2 = root.addNode("testGetReferences2", "nt:unstructured");

      Node n1 = root.addNode("n1", "nt:unstructured");
      Node n2 = root.addNode("n2", "nt:unstructured");
      Node n3 = root.addNode("n3", "nt:unstructured");
      Node n4 = root.addNode("n4", "nt:unstructured");

      try
      {
         testNode.addMixin("mix:referenceable");
         testNode1.addMixin("mix:referenceable");

         root.save();

         testNode2.addMixin("mix:versionable");

         // Should be saved first
         root.save();

         // version stuff
         testNode2.checkin();
         testNode2.checkout();
         testNode2.addNode("Any node").setProperty("Any prop", true);
         root.save();
         testNode2.checkin();
         testNode2.checkout();

         n1.setProperty("p1", testNode);
         n2.setProperty("p1", testNode1);
         n3.setProperty("p1", testNode2);

         n3.setProperty("p0", testNode);
         n3.setProperty("p1", testNode1); // !!! instead testNode2
         n3.setProperty("p2", testNode2);

         ValueFactory vFactory = n2.getSession().getValueFactory();

         n2.setProperty("p2_multiple", new Value[]{vFactory.createValue(testNode), vFactory.createValue(testNode1),
            vFactory.createValue(testNode2)});

         n4.setProperty("p1_multiple", new Value[]{vFactory.createValue(testNode1), vFactory.createValue(testNode1),
            vFactory.createValue(testNode)});

         // i.e. REFERENCEs
         // n1/p1 -> testNode
         // n2/p1 -> testNode1
         // n3/p0 -> testNode
         // n3/p1 -> testNode1
         // n3/p2 -> testNode2
         // n2/p2_multiple -> testNode, testNode1, testNode2
         // n4/p1_multiple -> testNode1, testNode1, testNode
         root.save();

         PropertyIterator refs = testNode.getReferences();
         if (log.isDebugEnabled())
         {
            while (refs.hasNext())
            {
               Property p = refs.nextProperty();

               log.debug(testNode.getPath() + " ref >>> " + p.getPath());
            }
         }
         assertEquals(4, testNode.getReferences().getSize());

         refs = testNode1.getReferences();
         if (log.isDebugEnabled())
         {
            while (refs.hasNext())
            {
               Property p = refs.nextProperty();

               log.debug(testNode1.getPath() + " ref >>> " + p.getPath());
            }
         }
         assertEquals(5, testNode1.getReferences().getSize());

         refs = testNode2.getReferences();
         if (log.isDebugEnabled())
         {
            while (refs.hasNext())
            {
               Property p = refs.nextProperty();

               log.debug(testNode2.getPath() + " ref >>> " + p.getPath());
            }
         }
         assertEquals(2, testNode2.getReferences().getSize());

         testNode.remove();
         try
         {
            root.save();
            fail("ReferentialIntegrityException must be");
         }
         catch (ReferentialIntegrityException e)
         {
            root.refresh(false); // rollback remove in session (7.1.1.7 Item)
         }

         testNode1.remove();
         try
         {
            root.save();
            fail("ReferentialIntegrityException must be");
         }
         catch (ReferentialIntegrityException e)
         {
            root.refresh(false); // rollback remove in session (7.1.1.7 Item)
         }

         testNode2.remove();
         try
         {
            root.save();
            fail("ReferentialIntegrityException must be");
         }
         catch (ReferentialIntegrityException e)
         {
            root.refresh(false); // rollback remove in session (7.1.1.7 Item)
         }

         // remove some props
         n1.getProperty("p1").remove();

         n3.getProperty("p0").remove();

         n2.setProperty("p2_multiple", new Value[]{vFactory.createValue(testNode1), vFactory.createValue(testNode2)});

         n4.setProperty("p1_multiple", new Value[]{vFactory.createValue(testNode1), vFactory.createValue(testNode1)});

         // i.e. REFERENCEs
         // ....n1/p1 -> testNode (Removed)
         // n2/p1 -> testNode1
         // ....n3/p0 -> testNode (Removed)
         // n3/p1 -> testNode1
         // n3/p2 -> testNode2
         // n2/p2_multiple -> ....testNode (Removed), testNode1, testNode2
         // n4/p1_multiple -> testNode1, testNode1, ....testNode (Removed)
         root.save();

         refs = testNode.getReferences();
         if (log.isDebugEnabled())
         {
            while (refs.hasNext())
            {
               Property p = refs.nextProperty();

               log.debug(testNode.getPath() + " ref >>> " + p.getPath());
            }
         }
         assertEquals(0, testNode.getReferences().getSize());

         refs = testNode1.getReferences();
         if (log.isDebugEnabled())
         {
            while (refs.hasNext())
            {
               Property p = refs.nextProperty();

               log.debug(testNode1.getPath() + " ref >>> " + p.getPath());
            }
         }
         assertEquals(5, testNode1.getReferences().getSize());

         refs = testNode2.getReferences();
         if (log.isDebugEnabled())
         {
            while (refs.hasNext())
            {
               Property p = refs.nextProperty();

               log.debug(testNode2.getPath() + " ref >>> " + p.getPath());
            }
         }
         assertEquals(2, testNode2.getReferences().getSize());

         testNode.remove();
         try
         {
            root.save();
         }
         catch (ReferentialIntegrityException e)
         {
            fail("ReferentialIntegrityException must has no place here");
         }

         testNode1.remove();
         try
         {
            root.save();
            fail("ReferentialIntegrityException must be");
         }
         catch (ReferentialIntegrityException e)
         {
         }

         testNode2.remove();
         try
         {
            root.save();
            fail("ReferentialIntegrityException must be");
         }
         catch (ReferentialIntegrityException e)
         {
         }

      }
      catch (Exception e)
      {

         e.printStackTrace();
         fail("An exception occurs. " + e.getMessage());

      }
      finally
      {

         // finalization
         n2.getProperty("p1").remove();
         n3.getProperty("p1").remove();
         n3.getProperty("p2").remove();
         n2.getProperty("p2_multiple").remove();
         n4.getProperty("p1_multiple").remove();
         root.save();
      }
   }

   public void testGetRegerencesOnChangedNode() throws Exception
   {

      Node testNode = root.addNode("testGetReferences", "nt:unstructured");
      Node refNode = root.addNode("refNode", "nt:unstructured");
      assertTrue(testNode.canAddMixin("mix:referenceable"));

      testNode.addMixin("mix:referenceable");
      refNode.setProperty("p", refNode.getSession().getValueFactory().createValue(testNode));
      session.save();

      // change node
      assertTrue(testNode.canAddMixin("mix:lockable"));
      testNode.addMixin("mix:lockable");

      PropertyIterator refs = testNode.getReferences();
      assertEquals(1, refs.getSize());
   }

   public void testGetTransientDeletedReferences() throws Exception
   {
      Node file = root.addNode("n", "nt:file");
      Node content = file.addNode("jcr:content", "nt:resource");
      content.setProperty("jcr:data", new ByteArrayInputStream(new byte[0]));
      content.setProperty("jcr:mimeType", "");
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node link = root.addNode("link", "nt:linkedFile");
      link.setProperty("jcr:content", content);
      root.save();

      // remove linked file but don't save it
      content.getReferences().nextProperty().getParent().remove();

      try
      {
         PropertyIterator it = content.getReferences();
         assertEquals(0, it.getSize());

         root.refresh(false);

         it = content.getReferences();
         assertEquals(1, it.getSize());
      }
      catch (Exception e)
      {
         fail(e.getMessage());
      }
   }

   public void testGetTransientDeletedReferences_AnotherSession() throws Exception
   {
      Node file = root.addNode("n", "nt:file");
      Node content = file.addNode("jcr:content", "nt:resource");
      content.setProperty("jcr:data", new ByteArrayInputStream(new byte[0]));
      content.setProperty("jcr:mimeType", "");
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node link = root.addNode("link", "nt:linkedFile");
      link.setProperty("jcr:content", content);
      root.save();

      // remove linked file but don't save it
      content.getReferences().nextProperty().getParent().remove();

      Session anotherSession = repository.login(credentials, root.getSession().getWorkspace().getName());
      try
      {
         // but another session still see the link as a reference
         PropertyIterator it = ((Node)anotherSession.getItem(content.getPath())).getReferences();
         assertEquals(1, it.getSize());
      }
      catch (Exception e)
      {
         fail(e.getMessage());
      }
      finally
      {
         anotherSession.logout();
      }
   }

   public void testGetDeletedReferences() throws Exception
   {
      Node file = root.addNode("n", "nt:file");
      Node content = file.addNode("jcr:content", "nt:resource");
      content.setProperty("jcr:data", new ByteArrayInputStream(new byte[0]));
      content.setProperty("jcr:mimeType", "");
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node link = root.addNode("link", "nt:linkedFile");
      link.setProperty("jcr:content", content);
      root.save();

      // remove linked file but and save it
      content.getReferences().nextProperty().getParent().remove();
      root.save();

      try
      {
         PropertyIterator it = content.getReferences();
         assertEquals(0, it.getSize());
      }
      catch (Exception e)
      {
         fail(e.getMessage());
      }
   }
}
