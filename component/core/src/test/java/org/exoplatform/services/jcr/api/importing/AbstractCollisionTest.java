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
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.TransientNodesManagerUtil;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.transform.TransformerConfigurationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: BaseXmlImporter.java 12649 2008-04-02 12:46:37Z ksm $
 */
public class AbstractCollisionTest extends AbstractImportTest
{

   /**
    * Import root node name.
    */
   private static final String EXPORT_ROOT_NODE_NAME = "exportRoot";

   /**
    * First child node name.
    */
   private static final String FAMILY_A_FIRST_CHILD_NODE_NAME = "afc";

   /**
    * Mix referenceable node name.
    */
   private static final String FAMILY_A_PARENT = "aparent";

   /**
    * First child node name.
    */
   private static final String FAMILY_A_SECOND_CHILD_NODE_NAME = "asc";

   /**
    * First child node name.
    */
   private static final String FAMILY_B_FIRST_CHILD_NODE_NAME = "bfc";

   /**
    * First child node name.
    */
   private static final String FAMILY_B_FIRST_GRAND_CHILD_NODE_NAME = "bfgc";

   /**
    * Mix referenceable node name.
    */
   private static final String FAMILY_B_PARENT = "bparent";

   /**
    * First child node name.
    */
   private static final String FAMILY_B_SECOND_CHILD_NODE_NAME = "bsc";

   /**
    * First child node name.
    */
   private static final String FAMILY_C_FIRST_CHILD_NODE_NAME = "cfc";

   /**
    * Mix referenceable node name.
    */
   private static final String FAMILY_C_PARENT = "cparent";

   /**
    * First child node name.
    */
   private static final String FAMILY_C_SECOND_CHILD_NODE_NAME = "csc";

   /**
    * Import root node name.
    */
   private static final String IMPORT_ROOT_NODE_NAME = "importRoot";

   /**
    * Root node name.
    */
   private static final String ROOT_NODE_NAME = "testRoot";

   /**
    * Uuid of <code>FAMILY_A_PARENT</code> node.
    */
   private String familyAuuid;

   /**
    * Uuid of <code>FAMILY_B_PARENT</code> node.
    */
   private String familyBuuid;

   /**
    * Uuid of <code>FAMILY_C_PARENT</code> node.
    */
   private String familyCuuid;

   /**
    * @param isSystemView
    * @param isExportedByStream
    * @param isImportedByStream
    * @param saveType
    * @param testedBehavior
    * @throws RepositoryException
    * @throws TransformerConfigurationException
    * @throws IOException
    * @throws SAXException
    */
   protected void importUuidCollisionTest(boolean isSystemView, boolean isExportedByStream, boolean isImportedByStream,
      XmlSaveType saveType, int testedBehavior) throws RepositoryException, TransformerConfigurationException,
      IOException, SAXException
   {
      Exception result = null;
      try
      {
         preformImport(isSystemView, isExportedByStream, isImportedByStream, saveType, testedBehavior);

         session.save();

      }
      catch (Exception e)
      {
         result = e;
      }
      Node imporRoot = null;
      if (testedBehavior != ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW)
      {
         imporRoot =
            session.getRootNode().getNode(ROOT_NODE_NAME).getNode(IMPORT_ROOT_NODE_NAME).getNode(EXPORT_ROOT_NODE_NAME);
      }
      else
      {
         imporRoot = session.getRootNode().getNode(ROOT_NODE_NAME).getNode(IMPORT_ROOT_NODE_NAME);
      }

      checkResult(imporRoot, session.getRootNode().getNode(ROOT_NODE_NAME).getNode(EXPORT_ROOT_NODE_NAME), result,
         isImportedByStream, testedBehavior);

      session.refresh(false);

      if (session.getRootNode().hasNode(ROOT_NODE_NAME))
      {
         session.getRootNode().getNode(ROOT_NODE_NAME).remove();
         session.save();
      }

      if ((testedBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING || testedBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
         && saveType == XmlSaveType.SESSION)
      {
         preformImport(isSystemView, isExportedByStream, isImportedByStream, saveType, testedBehavior);
         SessionChangesLog changesLog = TransientNodesManagerUtil.getChangesLog(session.getTransientNodesManager());

         for (ItemState state : changesLog.getAllStates())
         {
            assertTrue(state.getAncestorToSave().equals(
               ((NodeImpl)session.getRootNode().getNode(ROOT_NODE_NAME)).getData().getQPath()));
         }
      }

   }

   /**
    * Checks correctness of an arrangement of members of family in a family tree beginning from the
    * <code>checkRoot</code> node.
    * 
    * 
    * @param checkRoot
    *          - parent node of family tree.
    * @param family
    *          - family name.
    * @param referenceableUuid
    *          - uuid of famali parent node.
    * @throws RepositoryException
    *           - RepositoryException.
    */
   private void checkFamilyTree(final Node checkRoot, final Family family, final String referenceableUuid)
      throws RepositoryException
   {
      String parentName = null;
      String child1Name = null;
      String child2Name = null;
      switch (family)
      {
         case A :
            parentName = FAMILY_A_PARENT;
            child1Name = FAMILY_A_FIRST_CHILD_NODE_NAME;
            child2Name = FAMILY_A_SECOND_CHILD_NODE_NAME;
            break;
         case B :
            parentName = FAMILY_B_PARENT;
            child1Name = FAMILY_B_FIRST_CHILD_NODE_NAME;
            child2Name = FAMILY_B_SECOND_CHILD_NODE_NAME;

            break;
         case C :
            parentName = FAMILY_C_PARENT;
            child1Name = FAMILY_C_FIRST_CHILD_NODE_NAME;
            child2Name = FAMILY_C_SECOND_CHILD_NODE_NAME;
            break;
         default :
            throw new RepositoryException("Unknown famaly " + family.name());
      }

      // check family root
      assertTrue(checkRoot.hasNode(parentName));
      Node familyRoot = checkRoot.getNode(parentName);
      assertNotNull(familyRoot);
      // check is mix:referenceable
      assertTrue(familyRoot.isNodeType("mix:referenceable"));
      // compare uuids
      if (referenceableUuid != null)
         assertEquals(referenceableUuid, familyRoot.getProperty("jcr:uuid").getString());
      // check childs

      assertTrue(familyRoot.hasNode(child1Name));
      assertTrue(familyRoot.hasNode(child2Name));
      // check child count
      assertEquals(2, familyRoot.getNodes().getSize());

   }

   /**
    * @param importRoot
    * @param exportRootNode
    * @param result
    * @param isImportedByStream
    * @param testedBehavior
    * @throws RepositoryException
    */
   private void checkResult(Node importRoot, Node exportRootNode, Exception result, boolean isImportedByStream,
      int testedBehavior) throws RepositoryException
   {
      assertNotNull(exportRootNode);
      assertNotNull(importRoot);
      // Exception should be thrown
      if (testedBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW)
      {

         assertNotNull("Exception should have been thrown", result);

         // Check correct type of exception
         if (isImportedByStream)
         {
            assertTrue("Exception should be the ItemExistsException type", result instanceof ItemExistsException);
         }
         else
         {

            assertTrue("Exception should be the SAXException type", result instanceof SAXException);
         }
      }
      else if (testedBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW)
      {

         assertNull("An exception should not have been thrown", result);

         // Old referenceable node should stay on same position with same uuid.
         checkFamilyTree(exportRootNode, Family.C, familyCuuid);
         checkFamilyTree(exportRootNode, Family.B, familyBuuid);

         assertTrue(exportRootNode.hasNode(FAMILY_B_PARENT + "/" + FAMILY_B_FIRST_CHILD_NODE_NAME + "/"
            + FAMILY_B_FIRST_GRAND_CHILD_NODE_NAME));

         // all content of document imported to the importRoot
         checkFamilyTree(importRoot, Family.A, null);
         checkFamilyTree(importRoot, Family.B, null);

      }
      else if (testedBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING)
      {

         assertNull("An exception should not have been thrown", result);
         // Old referenceable node and all its subtree should be removed
         checkFamilyTree(exportRootNode, Family.C, familyCuuid);
         assertFalse(exportRootNode.hasNode(FAMILY_B_PARENT));
         assertFalse(exportRootNode.hasNode(FAMILY_A_PARENT));

         // Check correct importing of document to new location
         checkFamilyTree(importRoot, Family.A, familyAuuid);
         checkFamilyTree(importRoot, Family.B, familyBuuid);

      }
      else if (testedBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
      {
         checkFamilyTree(exportRootNode, Family.C, familyCuuid);
         checkFamilyTree(exportRootNode, Family.B, familyBuuid);

         assertFalse(exportRootNode.hasNode(FAMILY_B_PARENT + "/" + FAMILY_B_FIRST_CHILD_NODE_NAME + "/"
            + FAMILY_B_FIRST_GRAND_CHILD_NODE_NAME));

         checkFamilyTree(importRoot, Family.A, familyAuuid);
         assertFalse(importRoot.hasNode(FAMILY_B_PARENT));

      }

   }

   /**
    * @param isSystemView
    * @param isExportedByStream
    * @param isImportedByStream
    * @param saveType
    * @param testedBehavior
    * @throws RepositoryException
    * @throws TransformerConfigurationException
    * @throws IOException
    * @throws SAXException
    */
   private void preformImport(boolean isSystemView, boolean isExportedByStream, boolean isImportedByStream,
      XmlSaveType saveType, int testedBehavior) throws RepositoryException, TransformerConfigurationException,
      IOException, SAXException
   {
      NodeImpl testRoot = (NodeImpl)prepareForExport(session);

      Node exportRoot = testRoot.getNode(EXPORT_ROOT_NODE_NAME);
      byte[] content = serialize(exportRoot, isSystemView, isExportedByStream);

      Node importRoot = prepareForImport(testRoot);

      deserialize(importRoot, saveType, isImportedByStream, testedBehavior, new ByteArrayInputStream(content));
   }

   /**
    * Prepare repository for uuid collisions tests.
    * 
    * @param exportSession
    *          - working session.
    * @return - prepared root node.
    * @throws RepositoryException
    */
   private Node prepareForExport(final Session exportSession) throws RepositoryException
   {
      exportSession.refresh(false);
      if (exportSession.getRootNode().hasNode(ROOT_NODE_NAME))
      {
         exportSession.getRootNode().getNode(ROOT_NODE_NAME).remove();
         exportSession.save();
      }
      Node testRootNode = exportSession.getRootNode().addNode(ROOT_NODE_NAME);
      Node exportRootNode = testRootNode.addNode(EXPORT_ROOT_NODE_NAME);

      Node familyAParentNode = exportRootNode.addNode(FAMILY_A_PARENT);
      familyAParentNode.addMixin("mix:referenceable");
      familyAParentNode.addNode(FAMILY_A_FIRST_CHILD_NODE_NAME);
      familyAParentNode.addNode(FAMILY_A_SECOND_CHILD_NODE_NAME);

      Node familyBParentNode = exportRootNode.addNode(FAMILY_B_PARENT);
      familyBParentNode.addMixin("mix:referenceable");
      familyBParentNode.addNode(FAMILY_B_FIRST_CHILD_NODE_NAME);
      familyBParentNode.addNode(FAMILY_B_SECOND_CHILD_NODE_NAME);

      exportSession.save();
      familyAuuid = familyAParentNode.getProperty("jcr:uuid").getString();
      familyBuuid = familyBParentNode.getProperty("jcr:uuid").getString();

      // check correct state
      checkFamilyTree(exportRootNode, Family.A, familyAuuid);
      checkFamilyTree(exportRootNode, Family.B, familyBuuid);

      return testRootNode;
   }

   /**
    * Prepare repository for import content for uuid collisions tests.
    * <ol>
    * <li>Remove FIRST_CHILD_NODE_NAME and all subnodes</li>
    * <li>Add to MIX_REFERENCEABLE_NODE_NAME node SECOND_CHILD_NODE_NAME</li>
    * <li>Add to testRoot new node IMPORT_ROOT_NODE_NAME</li>
    * </ol>
    * 
    * @param testRootNode
    * @return
    * @throws RepositoryException
    */
   private Node prepareForImport(Node testRootNode) throws RepositoryException
   {
      Node exportRootNode = testRootNode.getNode(EXPORT_ROOT_NODE_NAME);
      assertTrue(exportRootNode.hasNode(FAMILY_A_PARENT));
      exportRootNode.getNode(FAMILY_A_PARENT).remove();

      exportRootNode.getNode(FAMILY_B_PARENT).getNode(FAMILY_B_FIRST_CHILD_NODE_NAME).addNode(
         FAMILY_B_FIRST_GRAND_CHILD_NODE_NAME);

      Node familyCParentNode = exportRootNode.addNode(FAMILY_C_PARENT);
      familyCParentNode.addMixin("mix:referenceable");
      familyCParentNode.addNode(FAMILY_C_FIRST_CHILD_NODE_NAME);
      familyCParentNode.addNode(FAMILY_C_SECOND_CHILD_NODE_NAME);

      Node importRoot = testRootNode.addNode(IMPORT_ROOT_NODE_NAME);
      testRootNode.getSession().save();

      familyCuuid = familyCParentNode.getProperty("jcr:uuid").getString();

      assertFalse(exportRootNode.hasNode(FAMILY_A_PARENT));
      assertTrue(exportRootNode.hasNode(FAMILY_C_PARENT));

      checkFamilyTree(exportRootNode, Family.C, familyCuuid);
      return importRoot;
   }

   /**
    * 
    * 
    *
    */
   enum Family {
      A, B, C
   };
}
