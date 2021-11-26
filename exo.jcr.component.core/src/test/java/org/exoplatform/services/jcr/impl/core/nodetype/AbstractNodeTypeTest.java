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

package org.exoplatform.services.jcr.impl.core.nodetype;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;

public abstract class AbstractNodeTypeTest extends JcrImplBaseTest
{

   protected NodeTypeManagerImpl nodeTypeManager;

   protected NodeTypeDataManager nodeTypeDataManager;

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      nodeTypeManager = (NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager();
      nodeTypeDataManager = session.getWorkspace().getNodeTypesHolder();

   }

   private static int random = 0;

   public AbstractNodeTypeTest()
   {
      super();
   }

   protected String getNewName(String prefix)
   {

      return prefix + (random++);
   }

}
