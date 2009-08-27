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
package org.exoplatform.services.jcr.api.nodetypes;

import javax.jcr.Node;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestNodeTypeConstraints.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestNodeTypeConstraints
   extends JcrAPIBaseTest
{

   public void testRemoveProtectedProperty() throws Exception
   {

      Node node1 = root.addNode("test", "nt:base");
      // log.debug(">>> node "+node1.getPrimaryNodeType().canRemoveItem("jct:primaryType"));
      try
      {
         node1.getProperty("jcr:primaryType").remove();
         fail("exception should have been thrown");
      }
      catch (ConstraintViolationException e)
      {
         log.debug("Exception FOUND: " + e);
      }
   }
   /*
    * public void testRemoveProtectedNode() throws Exception { NodeImpl node1 =
    * (NodeImpl)root.addNode("test", "nt:unstructured"); node1.addMixin("exo:accessControllable");
    * node1.createChildNode("exo:permissions", "exo:userPermission", true);
    * log.debug(">>> node "+node1.getMixinNodeTypes()[0].canRemoveItem("exo:permissions")); try {
    * node1.getNode("exo:permissions").remove(); fail("exception should have been thrown"); } catch
    * (ConstraintViolationException e) { log.debug("Exception FOUND: "+e); } }
    */
}
