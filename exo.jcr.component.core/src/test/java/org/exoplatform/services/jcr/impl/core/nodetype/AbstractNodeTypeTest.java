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
