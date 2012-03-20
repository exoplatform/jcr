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
package org.exoplatform.services.jcr.api.namespaces;

import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.search.BooleanQuery;
import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.ExtendedNamespaceRegistry;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.RepositoryIndexSearcherHolder;

import java.util.Arrays;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady
 *         Azarenkov</a>
 * @version $Id: TestNamespaceRegistry.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestNamespaceRegistry extends JcrAPIBaseTest
{

   protected ExtendedNamespaceRegistry namespaceRegistry;
   private RepositoryIndexSearcherHolder indexSearcherHolder;

   public void initRepository() throws RepositoryException
   {
      workspace = session.getWorkspace();
      namespaceRegistry = (ExtendedNamespaceRegistry)workspace.getNamespaceRegistry();
      try
      {
         namespaceRegistry.getURI("newMapping");
      }
      catch (NamespaceException e)
      {
         // not found
         namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");
      }
   }

   @Override
   public void setUp() throws Exception
   {
      // TODO Auto-generated method stub
      super.setUp();
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer(session.getWorkspace().getName());

      indexSearcherHolder = (RepositoryIndexSearcherHolder)wsc.getComponent(RepositoryIndexSearcherHolder.class);
      
      //indexSearcherHolder = (RepositoryIndexSearcherHolder)container.getComponentInstanceOfType(RepositoryIndexSearcherHolder.class);

   }

   public void testGetPrefixes() throws RepositoryException
   {
      // namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");
      String[] namespaces = {"jcr", "nt", "mix", "", "sv", "exo", "newMapping"};

      String[] prefixes = namespaceRegistry.getPrefixes();

      for (int i = 0; i < namespaces.length; i++)
      {

         String namespace = namespaces[i];
         assertTrue("not found " + namespace, ArrayUtils.contains(prefixes, namespace));
      }
      assertTrue(prefixes.length >= 7);

      assertTrue(Arrays.asList(session.getWorkspace().getNamespaceRegistry().getPrefixes()).containsAll(
         Arrays.asList(namespaceRegistry.getPrefixes())));
   }

   public void testGetURIs() throws RepositoryException
   {
      // namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");
      String[] namespacesURIs =
         {"http://www.jcp.org/jcr/1.0", "http://www.jcp.org/jcr/nt/1.0", "http://www.jcp.org/jcr/mix/1.0", "",
            "http://www.jcp.org/jcr/sv/1.0", "http://www.exoplatform.com/jcr/exo/1.0", "http://dumb.uri/jcr"};

      String[] uris = namespaceRegistry.getURIs();
      for (int i = 0; i < namespacesURIs.length; i++)
      {
         String namespacesURI = namespacesURIs[i];
         assertTrue("not found " + namespacesURI, ArrayUtils.contains(uris, namespacesURI));
      }
   }

   public void testGetURI() throws RepositoryException
   {
      // namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");

      assertNotNull(namespaceRegistry.getURI("mix"));
      assertNotNull(namespaceRegistry.getURI("newMapping"));
   }

   public void testGetPrefix() throws RepositoryException
   {
      // namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");

      assertNotNull(namespaceRegistry.getPrefix("http://www.jcp.org/jcr/mix/1.0"));
      assertEquals("mix", namespaceRegistry.getPrefix("http://www.jcp.org/jcr/mix/1.0"));
      assertNotNull(namespaceRegistry.getPrefix("http://dumb.uri/jcr"));

      try
      {
         namespaceRegistry.getPrefix("http://dumb.uri/jcr2");
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

   }

   // ///////////////// LEVEL 2

   public void testBuiltInNamespace() throws RepositoryException
   {
      try
      {
         namespaceRegistry.registerNamespace("jcr", null);
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }
      try
      {
         namespaceRegistry.registerNamespace("nt", null);
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }
      try
      {
         namespaceRegistry.registerNamespace("mix", null);
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }

      try
      {
         namespaceRegistry.registerNamespace("sv", null);
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }

      try
      {
         namespaceRegistry.registerNamespace("jcr", "http://dumb.uri/jcr");
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }

      try
      {
         namespaceRegistry.registerNamespace("xml-started", "http://dumb.uri/jcr");
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }

      try
      {
         namespaceRegistry.unregisterNamespace("jcr");
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }
   }

   public void testRegisterNamespace() throws RepositoryException
   {
      // namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");
      assertNotNull(namespaceRegistry.getURI("newMapping"));
      assertEquals("http://dumb.uri/jcr", namespaceRegistry.getURI("newMapping"));

      NodeImpl n = (NodeImpl)root.addNode("newMapping:test", "nt:unstructured");
      System.out.println("Node before save" + n);
      root.save();
      System.out.println("Node after save" + n);
      n = (NodeImpl)root.getNode("newMapping:test");
      n.remove();
      System.out.println("Node after remove" + n);
      root.save();

      // [PN] Unregisteration of node types its not supported in eXo JCR.
      // (see http://jira.exoplatform.org/browse/JCR-43)
      namespaceRegistry.unregisterNamespace("newMapping");
      try
      {
         root.addNode("newMapping:test1", "nt:unstructured");
         root.save();
         fail("exception should have been thrown");
      }
      catch (RepositoryException e)
      {
      }

      try
      {
         assertNull(namespaceRegistry.getURI("newMapping"));
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }

   }

   public void testReRegiterNamespace() throws RepositoryException
   {

      // (see http://jira.exoplatform.org/browse/JCR-43)

      namespaceRegistry.registerNamespace("newMapping", "http://dumb.uri/jcr");
      namespaceRegistry.registerNamespace("newMapping2", "http://dumb.uri/jcr");
      try
      {
         assertNull(namespaceRegistry.getURI("newMapping"));
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }
      assertNotNull(namespaceRegistry.getURI("newMapping2"));
      assertEquals("http://dumb.uri/jcr", namespaceRegistry.getURI("newMapping2"));
   }

   public void testQueryNsPropName() throws Exception
   {
      namespaceRegistry.registerNamespace("testuri", "http://testquery.uri/www");
      namespaceRegistry.registerNamespace("blahtesturi", "http://blahtesturi.uri/www");
      Node test1 = root.addNode("NodeName1");
      test1.setProperty("testuriprop", "v1");
      Node test2 = root.addNode("nodeName2");
      test2.setProperty("testuri:prop", "v2");
      Node test3 = root.addNode("nodeName3");
      test3.setProperty("blahtesturi:prop", "v2");
      test3.setProperty("blahtesturi", "v2");
      session.save();

      Set<String> nodes = indexSearcherHolder.getNodesByUri("http://testquery.uri/www"); //((NamespaceRegistryImpl)namespaceRegistry).getNodes("testuri");
      assertEquals(1, nodes.size());
      assertFalse(nodes.contains(((NodeImpl)test1).getData().getIdentifier()));
      assertFalse(nodes.contains(((NodeImpl)test3).getData().getIdentifier()));
      assertTrue(nodes.contains(((NodeImpl)test2).getData().getIdentifier()));
   }

   public void testQueryNsNodeName() throws Exception
   {
      Node test1 = root.addNode("testuri:testNodeName");
      Node test2 = root.addNode("testuriNodeName1");
      Node test3 = root.addNode("blahtesturiNodeName1");
      session.save();

      Set<String> nodes = indexSearcherHolder.getNodesByUri("http://testquery.uri/www"); //((NamespaceRegistryImpl)namespaceRegistry).getNodes("testuri");
      assertEquals(1, nodes.size());
      assertTrue(nodes.contains(((NodeImpl)test1).getData().getIdentifier()));
      assertFalse(nodes.contains(((NodeImpl)test2).getData().getIdentifier()));
      assertFalse(nodes.contains(((NodeImpl)test3).getData().getIdentifier()));

   }

   public void testQueryNsNodePathValue() throws Exception
   {
      Node test1 = root.addNode("NodeName1");
      test1.setProperty("tprop", valueFactory.createValue("/rr/testuri:node/", PropertyType.PATH));
      Node test2 = root.addNode("nodeName2");
      test2.setProperty("prop", "v2");
      session.save();

      Set<String> nodes = indexSearcherHolder.getNodesByUri("http://testquery.uri/www"); //((NamespaceRegistryImpl)namespaceRegistry).getNodes("testuri");
      assertEquals(1, nodes.size());
      assertTrue(nodes.contains(((NodeImpl)test1).getData().getIdentifier()));
      assertFalse(nodes.contains(((NodeImpl)test2).getData().getIdentifier()));
   }

   public void testQueryNsNodeNameValue() throws Exception
   {
      Node test1 = root.addNode("NodeName1");
      test1.setProperty("tprop", valueFactory.createValue("testuri:node", PropertyType.NAME));
      Node test2 = root.addNode("nodeName2");
      test2.setProperty("prop", "v2");

      Node test3 = root.addNode("nodeName2");
      test3.setProperty("prop", "blablatesturi:v2");
      session.save();
      
      Set<String> nodes = indexSearcherHolder.getNodesByUri("http://testquery.uri/www"); //((NamespaceRegistryImpl)namespaceRegistry).getNodes("testuri");
      assertEquals(1, nodes.size());
      assertTrue(nodes.contains(((NodeImpl)test1).getData().getIdentifier()));
      assertFalse(nodes.contains(((NodeImpl)test2).getData().getIdentifier()));
      assertFalse(nodes.contains(((NodeImpl)test3).getData().getIdentifier()));
   }

   public void testTooManyFields()
   {
      try
      {
         namespaceRegistry.registerNamespace("tmf", "http://www.tmf.org/jcr");

         int defClausesCount = BooleanQuery.getMaxClauseCount();
         Node tr = root.addNode("testRoot");
         for (int i = 0; i < defClausesCount + 10; i++)
         {
            tr.setProperty("prop" + i, i);
         }
         session.save();
         // ok
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }
      try
      {
         namespaceRegistry.unregisterNamespace("tmf");
      }
      catch (NamespaceException e)
      {
         e.printStackTrace();
         fail();
      }
      catch (RepositoryException e)
      {
         e.printStackTrace();
         fail();
      }
   }

   public void testIsDefaultPrefix()
   {
      assertTrue(((NamespaceRegistryImpl)namespaceRegistry).isDefaultPrefix("nt"));
      assertFalse(((NamespaceRegistryImpl)namespaceRegistry).isDefaultPrefix("somePrefix"));
   }

   public void testIsDefaultNamespace() throws NamespaceException, RepositoryException
   {      
      NamespaceRegistryImpl nameSpace = (NamespaceRegistryImpl)namespaceRegistry;
      String uri = workspace.getNamespaceRegistry().getURI("nt");
      
      assertTrue(nameSpace.isDefaultNamespace(uri));
      assertFalse(nameSpace.isDefaultNamespace(" "));
   }

   public void testValidateNamespace() throws RepositoryException
   {
      try
      {
         ((NamespaceRegistryImpl)namespaceRegistry).validateNamespace("some:text", "");
         fail();
      }
      catch (RepositoryException e)
      {
      }

      try
      {
         ((NamespaceRegistryImpl)namespaceRegistry).validateNamespace("nt", null);
         fail();
      }
      catch (NamespaceException e)
      {
      }
   }
}