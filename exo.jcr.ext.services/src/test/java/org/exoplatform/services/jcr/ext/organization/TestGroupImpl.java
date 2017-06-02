/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.organization;


/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: TestGroupImpl.java 73771 2011-09-20 10:26:40Z tolusha $
 */
public class TestGroupImpl extends AbstractOrganizationServiceTest
{

   /**
    * Test reusing entity during adding new child group.
    */
   public void testReuseEntity() throws Exception
   {
      GroupImpl child1 = (GroupImpl)gHandler.createGroupInstance();
      child1.setGroupName(groupName1);
      child1.setLabel("label");

      gHandler.addChild(null, child1, true);
      assertNotNull(child1.getInternalId());
      assertNull(child1.getParentId());
      assertEquals("/" + groupName1, child1.getId());
      assertEquals(groupName1, child1.getGroupName());
      
      GroupImpl child2 = (GroupImpl)gHandler.createGroupInstance();
      child2.setGroupName(groupName2);
      child2.setLabel("label");

      gHandler.addChild(child1, child2, true);
      assertNotNull(child2.getInternalId());
      assertEquals("/" + groupName1, child2.getParentId());
      assertEquals("/" + groupName1 + "/" + groupName2, child2.getId());
      assertEquals(groupName2, child2.getGroupName());
   }
}
