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

package org.exoplatform.services.jcr.api.nodetypes;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import javax.jcr.Node;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestNodeTypeConstraints.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestNodeTypeConstraints extends JcrAPIBaseTest
{

   public void testRemoveProtectedProperty() throws Exception
   {

      Node node1 = root.addNode("test");
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
