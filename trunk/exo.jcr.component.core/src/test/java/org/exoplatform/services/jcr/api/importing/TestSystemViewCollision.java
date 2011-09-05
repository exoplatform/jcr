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
package org.exoplatform.services.jcr.api.importing;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestSystemViewCollision extends AbstractCollisionTest
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestSystemViewCollisionTest");

   public void testUuidBehaviourIMPORT_UUID_COLLISION_THROW() throws Exception
   {

      XMLReader reader = XMLReaderFactory.createXMLReader();
      root.addNode("testCollision");
      session.save();
      reader.setContentHandler(session.getImportContentHandler("/testCollision",
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW));

      InputSource inputSource =
         new InputSource(new ByteArrayInputStream(TestSystemViewImport.SYSTEM_VIEW_CONTENT.getBytes()));
      reader.parse(inputSource);

      session.save();

      Node node = session.getRootNode().getNode("testCollision/exo:test/uuidNode1");

      Value valueUuid = node.getProperty("jcr:uuid").getValue();

      assertEquals("Uuid must exists [" + valueUuid.getString() + "]", "id_uuidNode1", valueUuid.getString());

      try
      {
         session.getNodeByUUID("id_uuidNode1");
      }
      catch (ItemNotFoundException ex)
      {
         fail("not find node with uuid [id_uuidNode1] " + ex.getMessage());
      }

      Node nodeUuidNode3 = session.getRootNode().getNode("testCollision/exo:test/uuidNode3");

      Value valueRef3ToUuidNode1 = nodeUuidNode3.getProperty("ref_to_1").getValue();

      assertEquals("ref_to_1", "id_uuidNode1", valueRef3ToUuidNode1.getString());
      root.addNode("test2");
      // part 2
      reader.setContentHandler(session
         .getImportContentHandler("/test2", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW));

      inputSource = new InputSource(new ByteArrayInputStream(TestSystemViewImport.SYSTEM_VIEW_CONTENT.getBytes()));
      try
      {
         reader.parse(inputSource);
         fail("Must failed");
      }
      catch (SAXException ex)
      {
         log.debug("Sax exc occure", ex);
      }

   }

   public void testUuidBehaviourIMPORT_UUID_CREATE_NEW() throws Exception
   {

      PlainChangesLog changesLog = new PlainChangesLogImpl();

      TransientNodeData testNodeData =
         TransientNodeData.createNodeData((NodeData)((NodeImpl)root).getData(), new InternalQName("",
            "nodeWithPredefUuid"), Constants.NT_UNSTRUCTURED, "id_uuidNode1");
      changesLog.add(ItemState.createAddedState(testNodeData));
      TransientPropertyData primaryType =
         TransientPropertyData.createPropertyData(testNodeData, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(testNodeData.getPrimaryTypeName()));
      changesLog.add(ItemState.createAddedState(primaryType));

      session.getTransientNodesManager().getTransactManager().save(changesLog);
      root.getNode("nodeWithPredefUuid").addMixin("mix:referenceable");

      session.save();
      XMLReader reader = XMLReaderFactory.createXMLReader();
      root.addNode("test");
      reader.setContentHandler(session.getImportContentHandler("/test", ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW));

      InputSource inputSource =
         new InputSource(new ByteArrayInputStream(TestSystemViewImport.SYSTEM_VIEW_CONTENT.getBytes()));
      reader.parse(inputSource);

      session.save();

      Node nodeUuidNode1 = session.getRootNode().getNode("test/exo:test/uuidNode1");
      Value valueUuidNode1 = nodeUuidNode1.getProperty("jcr:uuid").getValue();

      assertTrue("Uuid must be new [" + valueUuidNode1.getString() + "]", !"id_uuidNode1".equals(valueUuidNode1
         .getString()));

      assertFalse(session.getNodeByUUID("id_uuidNode1").getName().equals("uuidNode1"));

   }

   // ===============================
   public void testUuidCollision_IContentHandler_EContentHandler_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EContentHandler_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, false, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, false, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, false, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, false, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IContentHandler_EStream_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EContentHandler_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, false, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Session_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EStream_Session_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.SESSION, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EStream_Session_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Session_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.SESSION,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Workspace_COLLISION_THROW() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
   }

   public void testUuidCollision_IStream_EStream_Workspace_CREATE_NEW() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.WORKSPACE, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
   }

   public void testUuidCollision_IStream_EStream_Workspace_REMOVE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
   }

   public void testUuidCollision_IStream_EStream_Workspace_REPLACE_EXISTING() throws Exception
   {
      importUuidCollisionTest(true, true, true, XmlSaveType.WORKSPACE,
         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
   }
}
