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
package org.exoplatform.services.jcr.impl.storage.fs;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.session.SessionChangesLog;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS 10.07.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestJCRVSReadWrite.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestJCRVSReadWrite extends JcrImplBaseTest
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.TestJCRVSReadWrite");

   public static final int FILES_COUNT = 1000;

   // public static final int FILE1_SIZE_KB = 1;
   // public static final int FILE1_SIZE = FILE1_SIZE_KB * 1024;
   public static int FILE1_SIZE = 0;

   // public static final int FILE2_SIZE_KB = 2;
   // public static final int FILE2_SIZE = FILE2_SIZE_KB * 1024;
   public static int FILE2_SIZE = 0;

   protected Node testRoot = null;

   protected List<String> properties = null;

   protected BufferedInputStream fBLOB1 = null;

   protected BufferedInputStream fBLOB2 = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      if (fBLOB1 == null)
      {
         // fBLOB1 = createBLOBTempFile("treeVSTest_", FILE1_SIZE_KB);
         // fBLOB1.deleteOnExit();
         fBLOB1 = new BufferedInputStream(new ByteArrayInputStream("qazws".getBytes()));
         FILE1_SIZE = fBLOB1.available();
      }

      if (fBLOB2 == null)
      {
         // fBLOB2 = createBLOBTempFile("treeVSTest_", FILE2_SIZE_KB);
         // fBLOB2.deleteOnExit();
         fBLOB2 = new BufferedInputStream(new ByteArrayInputStream("qazwsxedcr".getBytes()));
         FILE2_SIZE = fBLOB2.available();
      }

      testRoot = root.addNode("tree_vs_test");
      root.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      long time = System.currentTimeMillis();

      if (root.hasNode(testRoot.getName()))
      {
         testRoot.remove();
         root.save();
      }

      if (log.isDebugEnabled())
      {
         log.debug("Tear down of " + getName() + ",\t" + (System.currentTimeMillis() - time));
      }

      super.tearDown();
   }

   protected List<String> createJCRAPICase() throws Exception
   {
      List<String> props = new ArrayList<String>();
      String rootPath = testRoot.getPath();
      fBLOB1.mark(FILE1_SIZE);
      fBLOB2.mark(FILE2_SIZE);
      for (int i = 0; i < FILES_COUNT; i++)
      {
         try
         {
            Node resource = testRoot.addNode("blob" + i, "nt:file").addNode("jcr:content", "nt:unstructured"); // ,
            // "nt:resource"
            String path = "";
            if (i % 10 == 0)
            {
               Value[] vals =
                  new Value[]{session.getValueFactory().createValue(fBLOB1),
                     session.getValueFactory().createValue(fBLOB2),};
               path = resource.setProperty("jcr:data", vals).getPath();
            }
            else
            {
               path = resource.setProperty("jcr:data", fBLOB1).getPath();
            }
            resource.setProperty("jcr:mimeType", "application/x-octet-stream");
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
            testRoot.save();

            props.add(path.substring(rootPath.length() + 1));
         }
         catch (RepositoryException e)
         {
            log.warn("Can't create test case, " + e);
            throw new Exception("Can't create test case, " + e, e);
         }
         finally
         {
            fBLOB1.reset();
            fBLOB2.reset();
         }
      }
      return props;
   }

   protected void deleteJCRAPICase() throws RepositoryException
   {
      for (NodeIterator iter = testRoot.getNodes(); iter.hasNext();)
      {
         iter.nextNode().remove();
      }

      testRoot.save();
   }

   protected List<QPathEntry[]> createInternalAPICase() throws Exception
   {
      DataManager dm =
         ((NodeImpl)testRoot).getSession().getTransientNodesManager().getTransactManager().getStorageDataManager();

      List<QPathEntry[]> props = new ArrayList<QPathEntry[]>();
      NodeData rootData = (NodeData)((NodeImpl)testRoot).getData();
      fBLOB1.mark(FILE1_SIZE);
      fBLOB2.mark(FILE2_SIZE);
      for (int i = 0; i < FILES_COUNT; i++)
      {
         try
         {
            SessionChangesLog changes = new SessionChangesLog(((NodeImpl)testRoot).getSession());

            TransientNodeData ntfile =
               TransientNodeData.createNodeData(rootData, InternalQName.parse("[]blob" + i), Constants.NT_FILE);
            changes.add(ItemState.createAddedState(ntfile));

            TransientPropertyData ntfilePrimaryType =
               TransientPropertyData.createPropertyData(ntfile, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
                  new TransientValueData(Constants.NT_FILE));
            changes.add(ItemState.createAddedState(ntfilePrimaryType));

            TransientNodeData res =
               TransientNodeData.createNodeData(ntfile, Constants.JCR_CONTENT, Constants.NT_UNSTRUCTURED);
            changes.add(ItemState.createAddedState(res));

            TransientPropertyData resPrimaryType =
               TransientPropertyData.createPropertyData(res, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false,
                  new TransientValueData(Constants.NT_UNSTRUCTURED));
            changes.add(ItemState.createAddedState(resPrimaryType));

            List<ValueData> data = new ArrayList<ValueData>();
            if (i % 10 == 0)
            {
               data.add(new TransientValueData(fBLOB1, SpoolConfig.getDefaultSpoolConfig()));
               data.add(new TransientValueData(fBLOB2, SpoolConfig.getDefaultSpoolConfig()));
            }
            else
            {
               data.add(new TransientValueData(fBLOB1, SpoolConfig.getDefaultSpoolConfig()));
            }

            TransientPropertyData resData =
               TransientPropertyData.createPropertyData(res, Constants.JCR_DATA, PropertyType.BINARY, data.size() > 1,
                  data);
            changes.add(ItemState.createAddedState(resData));

            TransientPropertyData resMimeType =
               TransientPropertyData.createPropertyData(res, Constants.JCR_MIMETYPE, PropertyType.STRING, false,
                  new TransientValueData("application/x-octet-stream"));
            changes.add(ItemState.createAddedState(resMimeType));

            TransientPropertyData resLastModified =
               TransientPropertyData.createPropertyData(res, Constants.JCR_LASTMODIFIED, PropertyType.DATE, false,
                  new TransientValueData(Calendar.getInstance()));
            changes.add(ItemState.createAddedState(resLastModified));

            QPath path = resData.getQPath();

            dm.save(new TransactionChangesLog(changes));

            props.add(path.getRelPath(path.getEntries().length - rootData.getQPath().getEntries().length));
         }
         catch (RepositoryException e)
         {
            log.warn("Can't create test case, " + e);
            throw new Exception("Can't create test case, " + e, e);
         }
         finally
         {
            fBLOB1.reset();
            fBLOB2.reset();
         }
      }
      return props;
   }

   protected void deleteInternalAPICase() throws RepositoryException
   {
      final DataManager dm =
         ((NodeImpl)testRoot).getSession().getTransientNodesManager().getTransactManager().getStorageDataManager();
      final SessionChangesLog changes = new SessionChangesLog(((NodeImpl)testRoot).getSession());

      class Remover
      {
         void delete(NodeData node) throws RepositoryException
         {
            for (NodeData nd : dm.getChildNodesData(node))
            {
               new Remover().delete(nd);
            }
            for (PropertyData pd : dm.getChildPropertiesData(node))
            {
               changes.add(ItemState.createDeletedState(pd));
            }
            changes.add(ItemState.createDeletedState(node));
         }
      }

      NodeData rootData = (NodeData)((NodeImpl)testRoot).getData();

      new Remover().delete(rootData);

      dm.save(new TransactionChangesLog(changes));
   }

   // copied from SessionDataManager
   protected ItemData getItemData(DataManager manager, NodeData parent, QPathEntry[] relPathEntries, ItemType itemType)
      throws RepositoryException
   {
      ItemData item = parent;
      for (int i = 0; i < relPathEntries.length; i++)
      {
         if (i == relPathEntries.length - 1)
         {
            item = manager.getItemData(parent, relPathEntries[i], itemType);
         }
         else
         {
            item = manager.getItemData(parent, relPathEntries[i], ItemType.UNKNOWN);
         }

         if (item == null)
         {
            break;
         }

         if (item.isNode())
         {
            parent = (NodeData)item;
         }
         else if (i < relPathEntries.length - 1)
         {
            throw new IllegalPathException("Path can not contains a property as the intermediate element");
         }
      }
      return item;
   }

   public void testname() throws Exception
   {

   }

   public void _testReadWriteJCRAPI() throws Exception
   {
      long time = System.currentTimeMillis();
      List<String> props = createJCRAPICase();
      log.info(getName() + " ADD -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      // read randomize
      Set<String> caseProps = new HashSet<String>(props);
      for (String prop : caseProps)
      {
         try
         {
            InputStream stream = testRoot.getProperty(prop).getStream();
            assertEquals("Value has wrong length", FILE1_SIZE, stream.available());
         }
         catch (ValueFormatException e)
         {
            Value[] vs = testRoot.getProperty(prop).getValues();
            for (int i = 0; i < vs.length; i++)
            {
               assertEquals("Value has wrong length", i == 0 ? FILE1_SIZE : FILE2_SIZE, vs[i].getStream().available());
            }
         }
      }
      log.info(getName() + " READ -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      deleteJCRAPICase();
      log.info(getName() + " DELETE -- " + (System.currentTimeMillis() - time));
   }

   public void _testReadWriteInternalAPI() throws Exception
   {

      DataManager manager =
         ((NodeImpl)testRoot).getSession().getTransientNodesManager().getTransactManager().getStorageDataManager();
      NodeData parent = (NodeData)((NodeImpl)testRoot).getData();

      long time = System.currentTimeMillis();
      List<QPathEntry[]> props = createInternalAPICase();
      log.info(getName() + " ADD -- " + (System.currentTimeMillis() - time));

      time = System.currentTimeMillis();
      // read randomize
      Set<QPathEntry[]> caseProps = new HashSet<QPathEntry[]>(props);
      for (QPathEntry[] prop : caseProps)
      {
         PropertyData p = (PropertyData)getItemData(manager, parent, prop, ItemType.PROPERTY);
         List<ValueData> vals = p.getValues();
         for (int i = 0; i < vals.size(); i++)
         {
            assertEquals("Value has wrong length", i == 0 ? FILE1_SIZE : FILE2_SIZE, vals.get(i).getAsStream()
               .available());
         }
      }
      log.info(getName() + " READ -- " + (System.currentTimeMillis() - time));

      // time = System.currentTimeMillis();
      // deleteInternalAPICase();
      // log.info(getName() + " DELETE -- " + (System.currentTimeMillis() - time));
   }

}
