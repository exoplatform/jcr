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
package org.exoplatform.services.jcr.impl.storage;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.ItemImpl.ItemType;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 30.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: JDBCStorageConnectionTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class JDBCStorageConnectionTest extends JcrImplBaseTest
{

   private DataManager dataManager;

   private NodeData testRoot;

   private NodeData root;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      dataManager =
         (WorkspacePersistentDataManager)session.getContainer().getComponentInstanceOfType(
            WorkspacePersistentDataManager.class); // .
      // getTransientNodesManager
      // (
      // )
      // .
      // getTransactManager
      // (
      // )
      // .
      // getStorageDataManager
      // (
      // )

      root = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

      TransientNodeData troot =
         TransientNodeData.createNodeData(root, InternalQName.parse("[]jdbcStorageConnectionTest"),
            Constants.NT_UNSTRUCTURED);

      TransientPropertyData pt =
         TransientPropertyData.createPropertyData(troot, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
            new TransientValueData(Constants.NT_UNSTRUCTURED));

      PlainChangesLogImpl chlog = new PlainChangesLogImpl();
      chlog.add(ItemState.createAddedState(troot));
      chlog.add(ItemState.createAddedState(pt));

      dataManager.save(new TransactionChangesLog(chlog));

      testRoot =
         (NodeData)dataManager.getItemData(root,
            troot.getQPath().getEntries()[troot.getQPath().getEntries().length - 1], ItemType.NODE);

      assertNotNull("Can't find test root node " + troot.getQPath().getAsString(), testRoot);
   }

   @Override
   protected void tearDown() throws Exception
   {

      PlainChangesLogImpl chlog = new PlainChangesLogImpl();

      List<PropertyData> cps = dataManager.listChildPropertiesData(testRoot);
      for (PropertyData p : cps)
      {
         TransientPropertyData tp =
            new TransientPropertyData(p.getQPath(), p.getIdentifier(), p.getPersistedVersion(), p.getType(), p
               .getParentIdentifier(), p.isMultiValued());
         chlog.add(ItemState.createDeletedState(tp));
      }

      // just a TransientNodeData
      TransientNodeData troot =
         new TransientNodeData(testRoot.getQPath(), testRoot.getIdentifier(), testRoot.getPersistedVersion(), testRoot
            .getPrimaryTypeName(), testRoot.getMixinTypeNames(), testRoot.getOrderNumber(), testRoot
            .getParentIdentifier(), testRoot.getACL());

      chlog.add(ItemState.createDeletedState(troot));

      dataManager.save(new TransactionChangesLog(chlog));

      super.tearDown();
   }

   public void testGetItem_InheritedACL() throws RepositoryException, IllegalNameException
   {

      NodeData troot =
         (NodeData)dataManager.getItemData(root, new QPathEntry(InternalQName.parse("[]jdbcStorageConnectionTest"), 1),
            ItemType.NODE);

      assertEquals("Inherited acl should be here", root.getACL().getOwner(), troot.getACL().getOwner());
   }

   public void testGetItem_MixOwneable() throws RepositoryException, IllegalNameException
   {

      // prepare mixin
      TransientPropertyData mixin =
         TransientPropertyData.createPropertyData(testRoot, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
            new TransientValueData(Constants.EXO_OWNEABLE));

      TransientPropertyData owner =
         TransientPropertyData.createPropertyData(testRoot, Constants.EXO_OWNER, PropertyType.STRING, false,
            new TransientValueData("exo"));

      PlainChangesLogImpl chlog = new PlainChangesLogImpl();
      chlog.add(ItemState.createAddedState(mixin));
      chlog.add(ItemState.createAddedState(owner));

      dataManager.save(new TransactionChangesLog(chlog));

      // test
      NodeData troot =
         (NodeData)dataManager.getItemData(root, new QPathEntry(InternalQName.parse("[]jdbcStorageConnectionTest"), 1),
            ItemType.NODE);

      assertEquals("Owner is not valid", "exo", troot.getACL().getOwner());
   }

   public void testGetItem_MixPrivilegeable() throws RepositoryException, IllegalNameException
   {

      // prepare mixin
      TransientPropertyData mixin =
         TransientPropertyData.createPropertyData(testRoot, Constants.JCR_MIXINTYPES, PropertyType.NAME, true,
            new TransientValueData(Constants.EXO_PRIVILEGEABLE));

      List<ValueData> vd = new ArrayList<ValueData>();
      vd.add(new TransientValueData(SystemIdentity.ANY + AccessControlEntry.DELIMITER + PermissionType.READ));

      vd.add(new TransientValueData("managers" + AccessControlEntry.DELIMITER + PermissionType.SET_PROPERTY));

      vd.add(new TransientValueData("operators" + AccessControlEntry.DELIMITER + PermissionType.SET_PROPERTY));
      vd.add(new TransientValueData("operators" + AccessControlEntry.DELIMITER + PermissionType.ADD_NODE));

      TransientPropertyData permissions =
         TransientPropertyData.createPropertyData(testRoot, Constants.EXO_PERMISSIONS, PropertyType.STRING, false, vd);

      PlainChangesLogImpl chlog = new PlainChangesLogImpl();
      chlog.add(ItemState.createAddedState(mixin));
      chlog.add(ItemState.createAddedState(permissions));

      dataManager.save(new TransactionChangesLog(chlog));

      // test
      NodeData troot =
         (NodeData)dataManager.getItemData(root, new QPathEntry(InternalQName.parse("[]jdbcStorageConnectionTest"), 1),
            ItemType.NODE);

      List<String> iperms = troot.getACL().getPermissions(SystemIdentity.ANY);
      assertEquals("Wrong permission for " + SystemIdentity.ANY, 1, iperms.size());
      assertEquals("Wrong permission for " + SystemIdentity.ANY, PermissionType.READ, iperms.get(0));

      iperms = troot.getACL().getPermissions("managers");
      assertEquals("Wrong permission for managers", 1, iperms.size());
      assertEquals("Wrong permission for managers", PermissionType.SET_PROPERTY, iperms.get(0));

      iperms = troot.getACL().getPermissions("operators");
      assertEquals("Wrong permission for operators", 2, iperms.size());
      assertEquals("Wrong permission for operators", PermissionType.SET_PROPERTY, iperms.get(0));
      assertEquals("Wrong permission for operators", PermissionType.ADD_NODE, iperms.get(1));
   }

   public void testGetItem_MixOwneableMixPrivilegeable() throws RepositoryException, IllegalNameException
   {

      // prepare mixin
      List<ValueData> mixvd = new ArrayList<ValueData>();
      mixvd.add(new TransientValueData(Constants.EXO_OWNEABLE));
      mixvd.add(new TransientValueData(Constants.EXO_PRIVILEGEABLE));
      TransientPropertyData mixin =
         TransientPropertyData.createPropertyData(testRoot, Constants.JCR_MIXINTYPES, PropertyType.NAME, true, mixvd);

      List<ValueData> vd = new ArrayList<ValueData>();
      vd.add(new TransientValueData(SystemIdentity.ANY + AccessControlEntry.DELIMITER + PermissionType.READ));

      vd.add(new TransientValueData("managers" + AccessControlEntry.DELIMITER + PermissionType.SET_PROPERTY));

      vd.add(new TransientValueData("operators" + AccessControlEntry.DELIMITER + PermissionType.SET_PROPERTY));
      vd.add(new TransientValueData("operators" + AccessControlEntry.DELIMITER + PermissionType.ADD_NODE));

      TransientPropertyData permissions =
         TransientPropertyData.createPropertyData(testRoot, Constants.EXO_PERMISSIONS, PropertyType.STRING, false, vd);

      TransientPropertyData owner =
         TransientPropertyData.createPropertyData(testRoot, Constants.EXO_OWNER, PropertyType.STRING, false,
            new TransientValueData("exo"));

      PlainChangesLogImpl chlog = new PlainChangesLogImpl();
      chlog.add(ItemState.createAddedState(mixin));
      chlog.add(ItemState.createAddedState(owner));
      chlog.add(ItemState.createAddedState(permissions));

      dataManager.save(new TransactionChangesLog(chlog));

      // test
      NodeData troot =
         (NodeData)dataManager.getItemData(root, new QPathEntry(InternalQName.parse("[]jdbcStorageConnectionTest"), 1),
            ItemType.NODE);

      assertEquals("Owner is not valid", "exo", troot.getACL().getOwner());

      List<String> iperms = troot.getACL().getPermissions(SystemIdentity.ANY);
      assertEquals("Wrong permission for " + SystemIdentity.ANY, 1, iperms.size());
      assertEquals("Wrong permission for " + SystemIdentity.ANY, PermissionType.READ, iperms.get(0));

      iperms = troot.getACL().getPermissions("managers");
      assertEquals("Wrong permission for managers", 1, iperms.size());
      assertEquals("Wrong permission for managers", PermissionType.SET_PROPERTY, iperms.get(0));

      iperms = troot.getACL().getPermissions("operators");
      assertEquals("Wrong permission for operators", 2, iperms.size());
      assertEquals("Wrong permission for operators", PermissionType.SET_PROPERTY, iperms.get(0));
      assertEquals("Wrong permission for operators", PermissionType.ADD_NODE, iperms.get(1));
   }

}
