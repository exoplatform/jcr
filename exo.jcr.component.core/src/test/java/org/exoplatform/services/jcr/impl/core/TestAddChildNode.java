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
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:dmitriy.vishinskiy@gmail.com">Dmitriy Vishinskiy</a>
 * @version $Id: TestAddChildNode.java 111 2009-11-11 11:11:11Z tolusha $
 */
public class TestAddChildNode extends JcrImplBaseTest
   /**
    * Test for https://issues.jboss.org/browse/EXOJCR-1866.
    */
{
   public void testAddChildNodeUsingRelativePath() throws Exception
   {
      Node testNodeA = root.addNode("A", "exo:EXOJCR-1866-T1");
      session.save();
      Node testNodeB = testNodeA.addNode("B", "exo:EXOJCR-1866-T2");
      session.save();
      try
      {
         Node testNodeC = testNodeA.addNode("B/C", "exo:EXOJCR-1866-T3");
         session.save();
      }
      catch (ConstraintViolationException e)
      {
         fail("Correct node is not added");
      }
   }


   public void testAddNodeWithForbiddenDefinitionUsingRelativePath() throws Exception
   {
      Node testNodeA = root.addNode("A", "exo:EXOJCR-1866-T1");
      session.save();
      Node testNodeB = testNodeA.addNode("B", "exo:EXOJCR-1866-T2");
      session.save();
      try
      {
         Node testNodeC = testNodeA.addNode("B/C", "exo:EXOJCR-1866-T2");
         session.save();
         fail("Incorrect node added");
      }
      catch (ConstraintViolationException e)
      {

      }
   }
}