/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.version;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import javax.jcr.Node;
import javax.jcr.version.Version;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id$
 */
public class TestOnParentVersionIgnore extends JcrImplBaseTest
{
   public void testFrozenInitialized_OnParentVersion_IGNORE_EXOJCR1413() throws Exception
   {
      Node testNode = session.getRootNode().addNode("test");
      testNode.addMixin("mix:versionable");
      testNode.addNode("page", "exo:page").addNode("page", "exo:page");
      testNode.addNode("hello");
      session.save();

      Version ver = testNode.checkin();
      testNode.checkout();

      try
      {
         ver.getNode("jcr:frozenNode").getNode("page");
      }
      catch (Exception e)
      {
         fail("Node ../test/page should be accessable");
      }

      try
      {
         ver.getNode("jcr:frozenNode").getNode("hello");
      }
      catch (Exception e)
      {
         fail("Node ../test/hello should be accessable");
      }

      try
      {
         ver.getNode("jcr:frozenNode").getNode("page").getNode("page");
         fail("Node ../test/page/page should not be accessable");
      }
      catch (Exception e)
      {
      }

      testNode.remove();
      session.save();
   }
}
