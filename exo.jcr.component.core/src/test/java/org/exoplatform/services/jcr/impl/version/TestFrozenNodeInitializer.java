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
package org.exoplatform.services.jcr.impl.version;

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestFrozenNodeInitializer.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestFrozenNodeInitializer extends BaseVersionImplTest
{

   public void testFrozenCreated() throws Exception
   {

      createVersionable(TESTCASE_ONPARENT_COPY);

      // going to test
      versionable.accept(visitor);

      // ask for nt:frozenNode
      List<ItemState> versionableChanges = new ArrayList<ItemState>();
      versionableChanges.addAll(versionableLog.getChildrenChanges(versionable.getIdentifier(), true));
      versionableChanges.addAll(versionableLog.getChildrenChanges(versionable.getIdentifier(), false));

      List<ItemState> testChanges = new ArrayList<ItemState>();
      testChanges.addAll(changesLog.getChildrenChanges(frozenRoot.getIdentifier(), true));
      testChanges.addAll(changesLog.getChildrenChanges(frozenRoot.getIdentifier(), false));

      next : for (ItemState state : versionableChanges)
      {
         if (versionable.equals(state.getData()))
            continue next; // we have no interest for this item

         log.info("versionable change " + state.getData().getQPath().getAsString() + ", "
            + state.getData().getIdentifier() + "... ");
         for (ItemState result : testChanges)
         {
            ItemData resultData = result.getData();
            // log.info("frozen change found " + resultData.getQPath().getAsString() + ", " +
            // resultData.getUUID() + "... ");
            if (resultData.getQPath().getName().equals(state.getData().getQPath().getName()))
            {
               // if (resultData.isNode() && resultData.getUUID().equals(state.getData().getUUID()))
               log.info("...found in frozen changes " + result.getData().getQPath().getAsString());
               continue next;
            }
         }
         fail("Change is not stored in frozen state: " + state.getData().getQPath().getAsString());
      }
   }

   public void testFrozenInitialized_OnParentVersion_COPY() throws Exception
   {

      createVersionable(TESTCASE_ONPARENT_COPY);

      // going to test
      versionable.accept(visitor);

      List<ItemState> versionableChanges = versionableLog.getDescendantsChanges(versionable.getQPath());
      List<ItemState> testChanges = changesLog.getDescendantsChanges(frozenRoot.getQPath());

      next : for (ItemState state : versionableChanges)
      {
         if (versionable.equals(state.getData()))
            continue next; // we have no interest for this item

         log.info("versionable change " + state.getData().getQPath().getAsString() + ", "
            + state.getData().getIdentifier() + "... ");
         for (ItemState result : testChanges)
         {
            ItemData resultData = result.getData();
            // log.info("frozen change found " + resultData.getQPath().getAsString() + ", " +
            // resultData.getUUID() + "... ");
            if (resultData.getQPath().getName().equals(state.getData().getQPath().getName()))
            {
               // if (resultData.isNode() && resultData.getUUID().equals(state.getData().getUUID()))
               log.info("...found in frozen changes " + result.getData().getQPath().getAsString());
               continue next;
            }
         }
         fail("Change is not stored in frozen state: " + state.getData().getQPath().getAsString());
      }
   }

   public void testFrozenInitialized_OnParentVersion_ABORT() throws Exception
   {

      createVersionable(TESTCASE_ONPARENT_ABORT);

      try
      {
         versionable.accept(visitor);
         fail("A VersionException must be throwed on OnParentVersion=ABORT");
      }
      catch (VersionException e)
      {
         // ok
      }
   }

   public void testFrozenInitialized_OnParentVersion_IGNORE() throws Exception
   {

      createVersionable(TESTCASE_ONPARENT_IGNORE);

      versionable.accept(visitor);

      List<ItemState> versionableChanges = versionableLog.getDescendantsChanges(versionable.getQPath());
      List<ItemState> testChanges = changesLog.getDescendantsChanges(frozenRoot.getQPath());

      next : for (ItemState state : versionableChanges)
      {
         if (versionable.equals(state.getData()))
            continue next; // we have no interest for this item

         log.info("versionable change " + state.getData().getQPath().getAsString() + ", "
            + state.getData().getIdentifier() + "... ");
         for (ItemState result : testChanges)
         {
            ItemData resultData = result.getData();
            // log.info("frozen change found " + resultData.getQPath().getAsString() + ", " +
            // resultData.getUUID() + "... ");
            if (resultData.getQPath().getName().equals(state.getData().getQPath().getName()))
            {
               if (resultData.getQPath().getName().equals(PROPERTY_IGNORED))
                  fail("Ignored property can't be stored in frozen state: " + resultData.getQPath().getAsString());
               if (resultData.getQPath().getName().equals(NODE_IGNORED))
                  fail("Ignored node can't be stored in frozen state: " + resultData.getQPath().getAsString());
               log.info("...found in frozen changes " + resultData.getQPath().getAsString());
               continue next;
            }
         }
         if (!(state.getData().getQPath().getName().equals(PROPERTY_IGNORED) || state.getData().getQPath().getName()
            .equals(NODE_IGNORED)))
            fail("Change is not stored in frozen state: " + state.getData().getQPath().getAsString());
      }
   }

   public void testFrozenInitialized_OnParentVersion_VERSION() throws Exception
   {

      createVersionable(TESTCASE_ONPARENT_VERSION);

      versionable.accept(visitor);

      List<ItemState> versionableChanges = versionableLog.getDescendantsChanges(versionable.getQPath());
      List<ItemState> testChanges = changesLog.getDescendantsChanges(frozenRoot.getQPath());

      next : for (ItemState state : versionableChanges)
      {
         if (versionable.equals(state.getData()))
            continue next; // we have no interest for this item

         if (state.getData().getQPath().getName().equals(Constants.JCR_VERSIONHISTORY))
            continue next; // we have no interest for jcr:versionHistory on versionable (as it will be
         // saved as jcr:childVersionHistory)

         log.info("versionable change " + state.getData().getQPath().getAsString() + ", "
            + state.getData().getIdentifier() + "... ");
         for (ItemState result : testChanges)
         {
            ItemData resultData = result.getData();
            if (resultData.getQPath().getName().equals(state.getData().getQPath().getName()))
            {
               if (resultData.getQPath().getName().equals(NODE_VERSIONED))
               {
                  InternalQName ntName = null;
                  if (resultData instanceof TransientNodeData)
                  {
                     ntName = ((TransientNodeData)resultData).getPrimaryTypeName();
                  }
                  else if (resultData instanceof PersistedNodeData)
                  {
                     ntName = ((PersistedNodeData)resultData).getPrimaryTypeName();
                  }
                  else
                  {
                     fail("Unknown (for test) node data instance type: " + resultData);
                  }

                  assertEquals("Versioned node must be stored in frozen state as node of type nt:versionedChild: "
                     + resultData.getQPath().getAsString(), Constants.NT_VERSIONEDCHILD, ntName);

                  // QPath versionHistoryPropertyPath = QPath.makeChildPath(state.getData().getQPath(),
                  // Constants.JCR_VERSIONHISTORY);
                  // PropertyData vh = (PropertyData)
                  // session.getTransientNodesManager().getItemData(versionHistoryPropertyPath);

                  PropertyData vh =
                     (PropertyData)session.getTransientNodesManager().getItemData((NodeData)state.getData(),
                        new QPathEntry(Constants.JCR_VERSIONHISTORY, 0), ItemType.PROPERTY);

                  String vhUuid = new String(vh.getValues().get(0).getAsByteArray());

                  QPath childVersionHistoryPropertyPath =
                     QPath.makeChildPath(resultData.getQPath(), Constants.JCR_CHILDVERSIONHISTORY);
                  ItemState cvhState = changesLog.getItemState(childVersionHistoryPropertyPath);
                  assertNotNull(
                     "Frozen state of node of type nt:versionedChild hasn't jcr:childVersionHistory property: "
                        + resultData.getQPath().getAsString(), cvhState);

                  String cvhUuid = new String(((PropertyData)cvhState.getData()).getValues().get(0).getAsByteArray());
                  assertEquals("jcr:childVersionHistory property in frozen state contains wrong uuid", vhUuid, cvhUuid);

               }
               else if (resultData.getQPath().getName().equals(PROPERTY_VERSIONED))
               {
                  // behaviour of COPY...
               }
               log.info("...found in frozen changes " + resultData.getQPath().getAsString());
               continue next;
            }
         }
         fail("Change is not stored in frozen state: " + state.getData().getQPath().getAsString());
      }
   }
}
