/*
 * Copyright (C) 2012 eXo Platform SAS.
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

import org.exoplatform.services.jcr.impl.core.RepositoryImpl;

import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

/**
 * @author <a href="dvishinskiy@exoplatform.com">Dmitriy Vishinskiy</a>
 * @version $Id:$
 */
public class TestMigrationTool extends AbstractOrganizationServiceTest
{

   private TesterJCROrgService testService;

   /**
    * Method that responsible for loading of old organization service structure from dump.
    * @param sess is a valid Session object.
    * @param dumpName is a name of file where dump is stored.
    * @throws Exception if error occurs.
    */
   private void loadDataFromDump(Session sess, String dumpName) throws Exception
   {
      if (sess.itemExists(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT))
      {
         sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT).remove();
         sess.save();
      }

      InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(dumpName);
      sess.importXML("/", in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      sess.save();
   }

   /**
    * Method that prepares test structure.
    * @throws Exception if error occurs.
    */
   private void createTestStructure() throws Exception
   {
      repository = (RepositoryImpl)repositoryService.getDefaultRepository();
      testService = (TesterJCROrgService)container.getComponentInstanceOfType(TesterJCROrgService.class);
      testService.saveStorageWorkspaceName();
      testService.setStorageWorkspace("ws6");
      Session sess = testService.getStorageSession();
      try
      {
         MigrationTool migrationTool = new MigrationTool(testService);

         if (!sess.itemExists("/exo:organization"))
         {
            loadDataFromDump(sess, "jcrorgservice114dump.xml");
            assertTrue(migrationTool.migrationRequired());
            migrationTool.migrate();
         }
      }
      finally
      {
         sess.logout();
      }

   }

   /**
    * Method that removes test structure.
    * @throws Exception if error occurs.
    */
   private void removeTestStructure() throws Exception
   {
      repository = (RepositoryImpl)repositoryService.getDefaultRepository();
      Session sess = testService.getStorageSession();

      try
      {
         if (sess.itemExists("/exo:organization"))
         {
            sess.getItem("/exo:organization").remove();
            sess.save();
         }

         testService.restoreStorageWorkspaceName();
      }
      finally
      {
         sess.logout();
      }
   }

   /**
    * Method that checks correct user node migration.
    * @throws Exception if error occurs.
    */
   public void testUserMigration() throws Exception
   {
      createTestStructure();
      Session sess = testService.getStorageSession();

      try
      {
         Node marryUserNode =
            (Node)sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT + "/"
               + JCROrganizationServiceImpl.STORAGE_JOS_USERS + "/marry");

         assertTrue(marryUserNode.getProperty("jos:firstName").getString().equals("Marry"));
         assertTrue(marryUserNode.getProperty("jos:lastName").getString().equals("Kelly"));
         assertTrue(marryUserNode.isNodeType(JCROrganizationServiceImpl.JOS_USERS_NODETYPE));
         assertTrue(marryUserNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE).isNodeType("jos:userProfile-v2"));

         PropertyIterator iterator = marryUserNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE).getProperties();
         while (iterator.hasNext())
         {
            Property prop = iterator.nextProperty();
            if (prop.getName().equals("jcr:primaryType"))
            {
               continue;
            }
            assertTrue(prop.getName().startsWith("attr."));
         }
      }
      finally
      {
         sess.logout();
      }
      removeTestStructure();
   }

   /**
    * Method that checks correct group node migration.
    * @throws Exception if error occurs.
    */
   public void testGroupMigration() throws Exception
   {
      createTestStructure();
      Session sess = testService.getStorageSession();

      try
      {

         Node administratorsGroupNode =
            (Node)sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT + "/"
               + JCROrganizationServiceImpl.STORAGE_JOS_GROUPS + "/platform/administrators");

         assertTrue(administratorsGroupNode.isNodeType(JCROrganizationServiceImpl.JOS_HIERARCHY_GROUP_NODETYPE));

         NodeIterator iterat = administratorsGroupNode.getNodes();
         while (iterat.hasNext())
         {
            Node nd = iterat.nextNode();
            assertTrue(nd.isNodeType("jos:memberships-v2"));
         }
      }
      finally
      {
         sess.logout();
      }
      removeTestStructure();
   }

   /**
    * Method that checks correct userMembership node migration.
    * @throws Exception if error occurs.
    */
   public void testMembershipsMigration() throws Exception
   {
      createTestStructure();
      Session sess = testService.getStorageSession();

      try
      {
         Node administratorsGroupNode =
            (Node)sess.getItem(JCROrganizationServiceImpl.STORAGE_PATH_DEFAULT + "/"
               + JCROrganizationServiceImpl.STORAGE_JOS_GROUPS + "/platform/administrators");

         assertTrue(administratorsGroupNode.isNodeType(JCROrganizationServiceImpl.JOS_HIERARCHY_GROUP_NODETYPE));
         assertEquals(2, testService.getUserHandler().findUsersByGroup("/platform/administrators").getAll().size());
      }
      finally
      {
         sess.logout();
      }
      removeTestStructure();
   }

   /**
    * Method that checks correct membershipType node migration.
    * @throws Exception if error occurs.
    */
   public void testMembershipTypesMigration() throws Exception
   {
      createTestStructure();
      Session sess = testService.getStorageSession();

      try
      {
         assertEquals(4, ((Node)sess.getItem("/exo:organization/jos:membershipTypes")).getNodes().getSize());
      }
      finally
      {
         sess.logout();
      }
      removeTestStructure();
   }
}
