/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestGetPropertiesByPattern.java 111 4.05.2011 serg $
 */
public class TestGetPropertiesByPattern extends BaseUsecasesTest
{
   public void testPatternWithEscapedSymbols() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.setProperty("property.txt", "prop1Value");
      node.setProperty("property_txt", "prop2Value");
      node.setProperty("property3txt", "prop3Value");
      root.save();

      PropertyIterator iterator = node.getProperties("property.tx*");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextProperty().getName(), "property.txt");
      assertFalse(iterator.hasNext());

      iterator = node.getProperties("property_tx*");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextProperty().getName(), "property_txt");
      assertFalse(iterator.hasNext());

      iterator = node.getProperties("property_tx* | boo");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextProperty().getName(), "property_txt");
      assertFalse(iterator.hasNext());

      iterator = node.getProperties("property.txt");
      assertTrue(iterator.hasNext());
      assertEquals(iterator.nextProperty().getName(), "property.txt");
      assertFalse(iterator.hasNext());

      iterator = node.getProperties("nodata.txt");
      assertFalse(iterator.hasNext());

      iterator = node.getProperties("no*.txt");
      assertFalse(iterator.hasNext());
   }

   public void testCaching() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.setProperty("cassiopeia", "cassiopeiaValue");
      node.setProperty("casio", "casioValue");
      node.setProperty("cassandra", "cassandraValue");
      node.setProperty("libra", "libraValue");
      node.setProperty("equilibrium", "equilibriumValue");
      node.setProperty("equality", "cassandraValue");
      root.save();
      for (int i = 0; i < 100; i++)
      {
         String name = "property" + i;
         node.setProperty(name, name);
      }
      root.save();

      long executionTime = System.currentTimeMillis();
      PropertyIterator iterator = node.getProperties("cass* | *lib*");
      executionTime = System.currentTimeMillis() - executionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassiopeia", "cassandra", "libra", "equilibrium"});

      long nextExecutionTime = System.currentTimeMillis();
      iterator = node.getProperties("cass* | *lib*");
      nextExecutionTime = System.currentTimeMillis() - nextExecutionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"cassiopeia", "cassandra", "libra", "equilibrium"});

      if (nextExecutionTime * 1.1 < executionTime)
      {
         log.warn("Fetching data from DataBase takes less time than from cache - " + executionTime + " "
            + nextExecutionTime);
      }
   }

   public void testNamespaces() throws Exception
   {
      session = (SessionImpl)repository.login(credentials, WORKSPACE);
      Node root = session.getRootNode();
      Node node = root.getNode("childNode").addNode("testNode", "nt:unstructured");

      node.setProperty("exo:cassiopeia", "cassiopeiaValue");
      node.setProperty("jcr:casio", "casioValue");
      node.setProperty("nt:cassandra", "cassandraValue");
      node.setProperty("exo:libra", "libraValue");
      node.setProperty("jcr:equilibrium", "equilibriumValue");
      node.setProperty("exo:equality", "cassandraValue");
      root.save();
      for (int i = 0; i < 100; i++)
      {
         String name = "property" + i;
         node.setProperty(name, name);
      }
      root.save();

      long executionTime = System.currentTimeMillis();
      PropertyIterator iterator = node.getProperties("*:cass* | *:*lib*");
      executionTime = System.currentTimeMillis() - executionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"exo:cassiopeia", "nt:cassandra", "exo:libra", "jcr:equilibrium"});

      long nextExecutionTime = System.currentTimeMillis();
      iterator = node.getProperties("*:cass* | *:*lib*");
      nextExecutionTime = System.currentTimeMillis() - nextExecutionTime;
      assertTrue(iterator.hasNext());
      testNames(iterator, new String[]{"exo:cassiopeia", "nt:cassandra", "exo:libra", "jcr:equilibrium"});
      if (nextExecutionTime * 1.1 < executionTime)
      {
         log.warn("Fetching data from DataBase takes less time than from cache - " + executionTime + " "
            + nextExecutionTime);
      }
   }

   public void setUp() throws Exception
   {
      super.setUp();
      Node root = session.getRootNode();
      root.addNode("childNode");
      root.save();
   }

   public void tearDown() throws Exception
   {
      Node root = session.getRootNode();
      Node node = root.getNode("childNode");
      node.remove();
      session.save();

      super.tearDown();
   }
}
