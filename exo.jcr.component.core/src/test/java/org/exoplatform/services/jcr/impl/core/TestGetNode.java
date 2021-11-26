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

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestGetNode.java 111 2009-11-11 11:11:11Z tolusha $
 */
public class TestGetNode extends JcrImplBaseTest
{

   // Reproduces the issue described in http://jira.exoplatform.org/browse/JCR-1094
   public void testGetNode() throws Exception
   {
      try
      {
         root.getNode("..");
         fail("Exception should be thrown");
      }
      catch (PathNotFoundException e)
      {
      }
      catch (Exception e)
      {
         fail("Exception should not be thrown");
      }
   }

   public void testGetNodeAndPropertyWithSameName() throws Exception
   {
      String sameName = "sameName";
      Node rootNode = session.getRootNode();
      Node aNode = rootNode.addNode("a");
      aNode.addNode(sameName);
      aNode.setProperty(sameName, "aa");
      session.save();

      assertNotNull(aNode.getProperty(sameName));
      assertNotNull(aNode.getNode(sameName));
   }

}
