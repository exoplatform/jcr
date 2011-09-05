/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.jcr.usecases.query;

import org.exoplatform.services.jcr.usecases.BaseUsecasesTest;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: TestUpdateProperty.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TestUpdateProperty extends BaseUsecasesTest
{

   public void testAddUpdateRemoveProperty() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      node.setProperty("prop2", "value2");
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value2')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      // the main issue that the last state is property deleting
      node.setProperty("prop1", "value1-2");
      node.setProperty("prop3", "value3");
      node.getProperty("prop2").remove();
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1-2')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value2')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value3')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      // remove node
      node.remove();
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1-2')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value3')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());
   }

   public void testMoveNode() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      node.setProperty("prop2", "value2");

      node = node.addNode("subNode");
      node.setProperty("prop21", "value21");
      node.setProperty("prop22", "value22");
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value2')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value21')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value22')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      session.move("/testNode", "/testNode2");
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value2')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value21')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value22')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());
   }

   public void testSetRemovePropertyImmediatly() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      node.getProperty("prop1").remove();
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());
   }

   public void testSetRemoveProperty() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      node.getProperty("prop1").remove();
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());
   }

   public void testRemoveNodeUpdateProperty() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      node.setProperty("prop1", "value2");
      node.remove();
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value2')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());
   }

   public void testRemoveNodeSetProperty() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      node.setProperty("prop2", "valu2");
      node.remove();
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value2')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());
   }

   public void testRemoveNodeRemoveProperty() throws Exception
   {

      Node node = root.addNode("testNode");
      node.setProperty("prop1", "value1");
      root.save();

      QueryManager manager = session.getWorkspace().getQueryManager();
      Query query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(1, query.execute().getNodes().getSize());

      node.getProperty("prop1").remove();
      node.remove();
      root.save();

      query = manager.createQuery("SELECT * FROM nt:base " + " WHERE CONTAINS(., 'value1')", Query.SQL);
      assertEquals(0, query.execute().getNodes().getSize());
   }

}
