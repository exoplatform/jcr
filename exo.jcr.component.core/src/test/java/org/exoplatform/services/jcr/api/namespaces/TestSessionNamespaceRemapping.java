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

package org.exoplatform.services.jcr.api.namespaces;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: TestSessionNamespaceRemapping.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestSessionNamespaceRemapping extends JcrAPIBaseTest
{

   private NamespaceRegistry namespaceRegistry;

   public void init() throws Exception
   {
      workspace = session.getWorkspace();
      namespaceRegistry = workspace.getNamespaceRegistry();
   }

   public void testSetNamespacePrefix() throws Exception
   {
      try
      {
         session.setNamespacePrefix("exo2", "http://dummy.com");
         fail("exception should have been thrown as http://dummy.com is not mapped in reg");
      }
      catch (NamespaceException e)
      {
      }

      try
      {
         session.setNamespacePrefix("exo", "http://www.jcp.org/jcr/1.0");
         fail("exception should have been thrown");
      }
      catch (NamespaceException e)
      {
      }

      session.setNamespacePrefix("exo2", "http://www.exoplatform.com/jcr/exo/1.0");
      assertEquals("http://www.exoplatform.com/jcr/exo/1.0", session.getNamespaceURI("exo2"));
      // assertNull(session.getNamespaceURI("exo"));

      assertEquals("http://www.jcp.org/jcr/1.0", session.getNamespaceURI("jcr"));
   }

   public void testGetNamespacePrefixes() throws Exception
   {
      String[] protectedNamespaces = {"jcr", "nt", "mix", "", "sv", "exo2"};
      session.setNamespacePrefix("exo2", "http://www.exoplatform.com/jcr/exo/1.0");
      String[] prefixes = session.getNamespacePrefixes();
      assertTrue(protectedNamespaces.length <= prefixes.length);
   }

   public void testGetNamespaceURI() throws Exception
   {
      session.setNamespacePrefix("exo2", "http://www.exoplatform.com/jcr/exo/1.0");
      assertEquals("http://www.exoplatform.com/jcr/exo/1.0", session.getNamespaceURI("exo2"));
      // assertNull(session.getNamespaceURI("exo"));
      assertEquals("http://www.jcp.org/jcr/1.0", session.getNamespaceURI("jcr"));
   }

   public void testGetNamespacePrefix() throws Exception
   {
      assertEquals("exo", session.getNamespacePrefix("http://www.exoplatform.com/jcr/exo/1.0"));
      session.setNamespacePrefix("exo2", "http://www.exoplatform.com/jcr/exo/1.0");
      assertEquals("exo2", session.getNamespacePrefix("http://www.exoplatform.com/jcr/exo/1.0"));
   }

}
