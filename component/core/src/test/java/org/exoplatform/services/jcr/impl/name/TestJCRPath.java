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
package org.exoplatform.services.jcr.impl.name;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.JCRName;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: TestJCRPath.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestJCRPath
   extends TestCase
{

   private LocationFactory factory;

   private NamespaceRegistryImpl namespaceRegistry;

   public void setUp() throws Exception
   {
      if (factory == null)
      {
         namespaceRegistry = new NamespaceRegistryImpl();
         factory = new LocationFactory(namespaceRegistry);
      }
   }

   public void testCreateRoot() throws Exception
   {
      JCRPath path = factory.createRootLocation();
      assertEquals("/", path.getAsString(false));
      assertEquals("/", path.getAsString(true));
      assertEquals(1, path.getIndex());
      assertEquals("", path.getName().getName());
   }

   public void testCreateName() throws Exception
   {
      JCRName name = factory.parseJCRName("jcr:test");
      assertEquals("jcr:test", name.getAsString());
      assertEquals("test", name.getName());
      assertEquals(namespaceRegistry.getNamespaceURIByPrefix("jcr"), name.getNamespace());
      assertEquals("jcr", name.getPrefix());
      assertEquals("[" + namespaceRegistry.getNamespaceURIByPrefix("jcr") + "]test", name.getInternalName()
               .getAsString());

      JCRName name1 = factory.createJCRName(name.getInternalName());
      assertTrue(name.equals(name1));
   }

   public void testParsePath() throws Exception
   {

      JCRPath path = factory.parseAbsPath("/jcr:node/node1[2]/exo:node2");
      assertEquals("node2", path.getName().getName());
      assertEquals(1, path.getIndex());
      assertEquals("node2", path.getInternalPath().getName().getName());
      assertEquals(3, path.getDepth());
      assertEquals("/jcr:node/node1[2]/exo:node2", path.getAsString(false));
      assertEquals("/jcr:node[1]/node1[2]/exo:node2[1]", path.getAsString(true));

      // with index
      assertTrue(path.equals(factory.parseAbsPath("/jcr:node/node1[2]/exo:node2[1]")));
      assertFalse(path.equals(factory.parseAbsPath("/jcr:node/node1[1]/exo:node2[1]")));

      JCRPath path1 = factory.parseAbsPath("/jcr:node[3]");
      assertEquals(3, path1.getIndex());

   }

   public void testCreatePath() throws Exception
   {
      JCRPath path = factory.parseAbsPath("/jcr:node/node1[2]/exo:node2");
      JCRPath parent = path.makeParentPath();
      assertEquals("/jcr:node/node1[2]", parent.getAsString(false));
      assertTrue(path.isDescendantOf(parent, true));
      assertTrue(path.isDescendantOf(parent.makeParentPath(), false));

      assertEquals("/jcr:node/node1[2]/exo:node2", factory.createJCRPath(parent, "exo:node2").getAsString(false));
      assertEquals("/jcr:node/node1[2]/exo:node2/node3", factory.createJCRPath(parent, "exo:node2/node3").getAsString(
               false));

      assertTrue(path.equals(factory.createJCRPath(parent, "exo:node2")));
      QPath qpath = path.getInternalPath();
      assertTrue(path.equals(factory.createJCRPath(qpath)));

      JCRPath sibs = factory.parseAbsPath("/jcr:node/node1[2]/exo:node2[2]");
      assertTrue(path.isSameNameSibling(sibs));

      path = factory.parseAbsPath("/jcr:node/node1[2]/exo:node2");
      assertEquals("/jcr:node", path.makeAncestorPath(2).getAsString(false));

   }
}
