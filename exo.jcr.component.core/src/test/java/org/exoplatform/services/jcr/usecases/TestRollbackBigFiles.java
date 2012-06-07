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
package org.exoplatform.services.jcr.usecases;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TestRollbackBigFiles.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestRollbackBigFiles extends JcrImplBaseTest
{

   public void testRollback() throws Exception
   {

      List<ItemState> list = new ArrayList<ItemState>();

      // create ADD node itemState
      NodeData parent = (NodeData)((NodeImpl)this.root).getData();
      QPath ancestorToSave = parent.getQPath();

      JCRPath nodePath = session.getLocationFactory().parseRelPath("testNode");
      InternalQName primaryType = session.getLocationFactory().parseJCRName("nt:unstructured").getInternalName();

      String id = IdGenerator.generate();
      TransientNodeData newNode =
         new TransientNodeData(nodePath.getInternalPath(), id, -1, primaryType, new InternalQName[0], 0,
            ((NodeImpl)root).getInternalIdentifier(), ((NodeImpl)root).getACL());

      ItemState state = new ItemState(newNode, ItemState.ADDED, false, parent.getQPath());
      list.add(state);

      // add property

      // added big file property
      JCRPath propPath = session.getLocationFactory().parseRelPath("bigProp");

      File f = this.createBLOBTempFile(1024);
      TransientPropertyData newProperty =
         new TransientPropertyData(propPath.getInternalPath(), IdGenerator.generate(), -1, PropertyType.BINARY,
            newNode.getIdentifier(), false, new TransientValueData(new FileInputStream(f),
               SpoolConfig.getDefaultSpoolConfig()));

      list.add(new ItemState(newProperty, ItemState.ADDED, false, parent.getQPath()));

      // crate broken node;
      nodePath = session.getLocationFactory().parseRelPath("testNodeBroken");
      newNode =
         new TransientNodeData(nodePath.getInternalPath(), id, -1, primaryType, new InternalQName[0], 0,
            ((NodeImpl)root).getInternalIdentifier(), ((NodeImpl)root).getACL());

      list.add(new ItemState(newNode, ItemState.ADDED, false, parent.getQPath()));

      PlainChangesLog log = new PlainChangesLogImpl();
      log.addAll(list);

      TransactionChangesLog tlog = new TransactionChangesLog();
      tlog.addLog(log);

      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer(workspace.getName());

      PersistentDataManager dm = (PersistentDataManager)wsc.getComponent(PersistentDataManager.class);
      try
      {
         dm.save(tlog);
         fail("JCRInvalidItemStateException expected");
      }
      catch (JCRInvalidItemStateException e)
      {
         // ok
      }

      assertNull("Item should not be found", dm.getItemData(id));
   }
}
