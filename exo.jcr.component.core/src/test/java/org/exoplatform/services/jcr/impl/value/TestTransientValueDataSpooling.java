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
package org.exoplatform.services.jcr.impl.value;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TransactionChangesLogWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 2009
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: TestTransientValueDataSpooling.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestTransientValueDataSpooling extends BaseStandaloneTest implements ItemsPersistenceListener
{

   private TransactionChangesLog cLog;

   private final File tmpdir = new File(System.getProperty("java.io.tmpdir"));

   private boolean haveValueStorage = false;

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      for (WorkspaceEntry we : repository.getConfiguration().getWorkspaceEntries())
      {
         if (we.getName().equals(root.getSession().getWorkspace().getName()))
         {
            haveValueStorage = we.getContainer().getValueStorages() != null;
            break;
         }
      }

      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer(session.getWorkspace().getName());
      PersistentDataManager dm = (PersistentDataManager)wsc.getComponent(PersistentDataManager.class);
      dm.addItemPersistenceListener(this);
   }

   /**
    * {@inheritDoc}
    */
   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   /**
    * Write data from stream direct to the storage without spooling.
    * 
    * @throws Exception
    */
   public void testNotSpooling() throws Exception
   {
      File tmpFile = createBLOBTempFile(250);

      System.gc();
      Thread.sleep(2000);

      String[] countBefore = tmpdir.list(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.startsWith("jcrvd");
         }
      });

      NodeImpl node = (NodeImpl)root.addNode("testNode");
      node.setProperty("testProp", new FileInputStream(tmpFile));
      root.save();

      System.gc();
      Thread.sleep(2000);

      String[] countAfter = tmpdir.list(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.startsWith("jcrvd");
         }
      });

      assertFalse(isSpooling(countBefore, countAfter));
   }

   /**
    * Spool steam on get operation.
    * 
    * @throws Exception
    *           if error
    */
   public void testRemoveAfterSet() throws Exception
   {
      File tmpFile = createBLOBTempFile(250);

      System.gc();
      Thread.sleep(2000);

      String[] countBefore = tmpdir.list(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.startsWith("jcrvd");
         }
      });

      Node node = root.addNode("testNode");
      node.setProperty("testProp", new FileInputStream(tmpFile));
      node.getProperty("testProp").getStream().close();
      root.save();

      System.gc();
      Thread.sleep(2000);

      String[] countAfter = tmpdir.list(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.startsWith("jcrvd");
         }
      });

      assertFalse(isSpooling(countBefore, countAfter));
   }

   public void _testSerialization() throws Exception
   {
      File tmpFile = createBLOBTempFile(250);

      Node node = root.addNode("testNode");
      node.setProperty("testProp", new FileInputStream(tmpFile));
      session.save();

      TransactionChangesLog cl = new TransactionChangesLog(cLog.getLogIterator().nextLog());

      node.getProperty("testProp").remove();
      session.save();

      ObjectWriter out = new ObjectWriterImpl(new FileOutputStream(File.createTempFile("out", ".tmp")));
      TransactionChangesLogWriter lw = new TransactionChangesLogWriter();

      lw.write(out, cl);
   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }

   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      cLog = (TransactionChangesLog)itemStates;
   }

   private boolean isSpooling(String[] before, String[] after)
   {
      int newFilecount = 0;

      List<String> lBefore = new ArrayList<String>();
      for (String sBefore : before)
         lBefore.add(sBefore);

      for (String sAfter : after)
      {
         if (!lBefore.contains(sAfter))
         {
            if (haveValueStorage && newFilecount == 0 || newFilecount == 0)
               newFilecount++;
            else
               return true;
         }

      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }
}
