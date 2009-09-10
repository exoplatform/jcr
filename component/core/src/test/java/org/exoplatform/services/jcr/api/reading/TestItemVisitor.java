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
package org.exoplatform.services.jcr.api.reading;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.util.Calendar;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestItemVisitor.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestItemVisitor extends JcrAPIBaseTest
{

   public void testItemVisiting() throws RepositoryException
   {

      Node root = session.getRootNode();
      Node file = root.addNode("childNode", "nt:folder").addNode("childNode2", "nt:file");
      Node contentNode = file.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", session.getValueFactory().createValue("this is the content",
         PropertyType.BINARY));
      contentNode.setProperty("jcr:mimeType", session.getValueFactory().createValue("text/html"));
      contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

      session.save();

      ItemVisitor visitor = new MockVisitor();
      contentNode.accept(visitor);
      contentNode.getProperty("jcr:data").accept(visitor);

      visitor = new MockVisitor2();
      root.getNode("childNode").accept(visitor);
      assertEquals(((MockVisitor2)visitor).getI(), ((MockVisitor2)visitor).getJ());
      assertEquals(3, ((MockVisitor2)visitor).getI());
      log.debug("VISITOR -- " + ((MockVisitor2)visitor).isReached());
      assertTrue(((MockVisitor2)visitor).isReached());

      root.getNode("childNode").remove();
      session.save();
   }

   public class MockVisitor implements ItemVisitor
   {

      public void visit(Property property) throws RepositoryException
      {
         assertEquals(property.getName(), "jcr:data");
      }

      public void visit(Node node) throws RepositoryException
      {
         assertEquals(node.getName(), "jcr:content");
      }

   }

   public class MockVisitor2 extends TraversingItemVisitor
   {

      private boolean reached = false;

      private int i = 0;

      private int j = 0;

      protected void entering(Property property, int level) throws RepositoryException
      {
         if ("jcr:data".equals(property.getName()))
            reached = true;
      }

      protected void entering(Node node, int level) throws RepositoryException
      {
         i++;
         assertTrue(isInList(node.getName()));
      }

      protected void leaving(Property property, int level) throws RepositoryException
      {
      }

      protected void leaving(Node node, int level) throws RepositoryException
      {
         j++;
      }

      private boolean isInList(String name)
      {
         if ("childNode".equals(name) || "childNode2".equals(name) || "jcr:content".equals(name)
            || "jcr:data".equals(name) || "jcr:mimeType".equals(name))
         {
            return true;
         }
         return false;
      }

      public int getI()
      {
         return i;
      }

      public int getJ()
      {
         return j;
      }

      public boolean isReached()
      {
         return reached;
      }
   }

}
