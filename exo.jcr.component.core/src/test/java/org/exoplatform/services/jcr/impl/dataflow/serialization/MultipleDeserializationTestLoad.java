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

package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by The eXo Platform SAS. <br>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: TestMultipleDeserialization.java 111 2008-11-11 11:11:11Z serg $
 */
public class MultipleDeserializationTestLoad extends JcrImplSerializationBaseTest
{

   private final static int nodes = 50;

   private final static int iterations = 50;

   public class TesterItemsPersistenceListener implements ItemsPersistenceListener
   {

      private final List<TransactionChangesLog> logsList = new ArrayList<TransactionChangesLog>();

      private final PersistentDataManager dataManager;

      public TesterItemsPersistenceListener(SessionImpl session)
      {
         this.dataManager =
            (PersistentDataManager)((ManageableRepository)session.getRepository()).getWorkspaceContainer(
               session.getWorkspace().getName()).getComponent(PersistentDataManager.class);
         this.dataManager.addItemPersistenceListener(this);
      }

      /**
       * {@inheritDoc}
       */
      public void onSaveItems(ItemStateChangesLog itemStates)
      {
         logsList.add((TransactionChangesLog)itemStates);
      }

      /**
       * Unregister the listener and return collected changes.
       * 
       * @return List of TransactionChangesLog
       */
      public List<TransactionChangesLog> pushChanges()
      {
         dataManager.removeItemPersistenceListener(this);
         return logsList;
      }

      public List<TransactionChangesLog> getCurrentLogList()
      {
         return logsList;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void finalize() throws Throwable
      {
         logsList.clear();
      }

      public boolean isTXAware()
      {
         return true;
      }
   }

   public void testSerialization() throws Exception
   {

      PersistentDataManager dataManager =
         (PersistentDataManager)((ManageableRepository)session.getRepository()).getWorkspaceContainer(
            session.getWorkspace().getName()).getComponent(PersistentDataManager.class);

      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      for (int i = 0; i < nodes; i++)
      {
         NodeImpl node = (NodeImpl)root.addNode("fileName" + i, "nt:file");
         NodeImpl cont = (NodeImpl)node.addNode("jcr:content", "nt:resource");
         cont.setProperty("jcr:mimeType", "text/plain");
         cont.setProperty("jcr:lastModified", Calendar.getInstance());
         cont.setProperty("jcr:encoding", "UTF-8");

         cont.setProperty("jcr:data", new ByteArrayInputStream(createBLOBTempData(300)));
      }
      root.save();

      // Serialize with JCR
      File jcrfile = File.createTempFile("jcr", "test");
      ObjectWriterImpl jcrout = new ObjectWriterImpl(new FileOutputStream(jcrfile));
      TransactionChangesLog l = pl.pushChanges().get(0);
      TransactionChangesLogWriter wr = new TransactionChangesLogWriter();
      wr.write(jcrout, l);

      jcrout.close();

      ObjectReaderImpl jcrin = new ObjectReaderImpl(new FileInputStream(jcrfile));
      long jcrfread = System.currentTimeMillis();
      TransactionChangesLog mlog =
         (new TransactionChangesLogReader(SpoolConfig.getDefaultSpoolConfig(), holder)).read(jcrin);
      //TransactionChangesLog mlog = new TransactionChangesLog();
      //mlog.readObject(jcrin);
      jcrfread = System.currentTimeMillis() - jcrfread;
      jcrin.close();

      long jcrread = 0;

      TransactionChangesLogReader rdr = new TransactionChangesLogReader(SpoolConfig.getDefaultSpoolConfig(), holder);

      for (int j = 0; j < iterations; j++)
      {
         // deserialize
         jcrin = new ObjectReaderImpl(new FileInputStream(jcrfile));
         long t3 = System.currentTimeMillis();
         TransactionChangesLog log = rdr.read(jcrin);

         t3 = System.currentTimeMillis() - t3;
         jcrread += t3;
         jcrin.close();
      }
      jcrfile.delete();

      System.out.println(" JCR first des - " + (jcrfread));
      System.out.println(" JCR des- " + (jcrread / iterations));
   }

   protected byte[] createBLOBTempData(int size) throws IOException
   {
      byte[] data = new byte[size * 1024]; // 1Kb
      Random random = new Random();
      random.nextBytes(data);
      return data;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }
}
