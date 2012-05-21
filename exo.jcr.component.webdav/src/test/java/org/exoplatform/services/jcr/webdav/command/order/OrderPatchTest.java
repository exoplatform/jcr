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
package org.exoplatform.services.jcr.webdav.command.order;

import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class OrderPatchTest extends BaseStandaloneTest
{

   public static final String ORDERABLE_NODETYPE = "nt:unstructured";

   protected Node orderPatchNode;

   public void setUp() throws Exception
   {
      super.setUp();
      if (orderPatchNode == null)
      {
         orderPatchNode = session.getRootNode().addNode("orderPatchNode", ORDERABLE_NODETYPE);
         session.save();
      }
   }

   public void assertOrder(Node parentNode, String[] nodes) throws RepositoryException
   {
      NodeIterator iterator = parentNode.getNodes();
      int pos = 0;
      while (iterator.hasNext())
      {
         String requiredName = nodes[pos++];
         String nodeName = iterator.nextNode().getName();

         assertEquals(requiredName, nodeName);
      }
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

}
