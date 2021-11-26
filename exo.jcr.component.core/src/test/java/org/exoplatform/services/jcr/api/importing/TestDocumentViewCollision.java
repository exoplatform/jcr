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

package org.exoplatform.services.jcr.api.importing;

import javax.jcr.ImportUUIDBehavior;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestDocumentViewCollision extends AbstractCollisionTest
{

   public TestDocumentViewCollision()
   {
      super();
   }

   /**
    * (boolean isSystemView, boolean isExportedByStream, boolean isImportedByStream, XmlSaveType
    * saveType, int testedBehavior)
    */
   public void testUuidCollision_IContentHandler_EContentHandler_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, false, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, false, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, false, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, false, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EStream_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EStream_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EStream_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EStream_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(false, true, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

}
