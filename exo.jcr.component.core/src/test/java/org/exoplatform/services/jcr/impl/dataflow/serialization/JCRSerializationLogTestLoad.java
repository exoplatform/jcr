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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;

/**
 * Created by The eXo Platform SAS. <br>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: JCRSerializationLogTest.java 111 2008-11-11 11:11:11Z serg $
 */
public class JCRSerializationLogTestLoad extends JcrImplSerializationBaseTest
{

   private final int iter = 10000;

   public void testWriteLog() throws Exception
   {
      TesterItemsPersistenceListener pl = new TesterItemsPersistenceListener(this.session);

      File file = this.createBLOBTempFile(310);
      FileInputStream fis = new FileInputStream(file);

      NodeImpl node = (NodeImpl)root.addNode("file", "nt:file");
      NodeImpl cont = (NodeImpl)node.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:encoding", "UTF-8");

      cont.setProperty("jcr:data", fis);
      root.save();

      fis.close();

      file.delete();

      List<TransactionChangesLog> logs = pl.pushChanges();

      TransactionChangesLog log = logs.get(0);
      TransactionChangesLogWriter wr = new TransactionChangesLogWriter();

      // Serialize with JCR
      long jcrwrite = 0;
      long jcrread = 0;

      File jcrfile = File.createTempFile("jcr", "test");
      ObjectWriterImpl jcrout = new ObjectWriterImpl(new FileOutputStream(jcrfile));

      System.out.println(" WRITE START");
      long t1 = System.currentTimeMillis();
      for (int i = 0; i < iter; i++)
      {
         wr.write(jcrout, logs.get(0));
      }
      jcrwrite = System.currentTimeMillis() - t1;
      jcrout.close();

      System.out.println(" READ START");
      // deserialize
      ObjectReaderImpl jcrin = new ObjectReaderImpl(new FileInputStream(jcrfile));

      //  List<TransactionChangesLog> readed = new ArrayList<TransactionChangesLog>();
      long t3 = System.currentTimeMillis();

      for (int i = 0; i < iter; i++)
      {
         TransactionChangesLogReader rdr = new TransactionChangesLogReader(SpoolConfig.getDefaultSpoolConfig(), holder);
         TransactionChangesLog obj = rdr.read(jcrin);

         //  readed.add(obj); 
      }

      jcrread = System.currentTimeMillis() - t3;
      jcrin.close();

      //check it
      //Iterator<TransactionChangesLog> it = readed.iterator();
      //while(it.hasNext()){
      //  checkIterator(logs.get(i).getAllStates().iterator(), obj.getAllStates().iterator());
      //}

      /*  // java
        long javaWrite = 0;
        long javaRead = 0;

        File jfile = File.createTempFile("java", "test");
        ObjectOutputStream jout = new ObjectOutputStream(new FileOutputStream(jfile));

        it = logs.iterator();
        
        System.out.println(" WRITE START");
        long t2 = System.currentTimeMillis();
        while (it.hasNext()) {
          jout.writeObject(it.next());
        }
        javaWrite = System.currentTimeMillis() - t2;
        jout.close();

        // deserialize
        System.out.println(" READ START");
        ObjectInputStream jin = new ObjectInputStream(new FileInputStream(jfile));

        long t4 = System.currentTimeMillis();

        for(int i=0; i<iter; i++){
          TransactionChangesLog obj = (TransactionChangesLog) jin.readObject();
          assertNotNull(obj);
          
        }
        javaRead = System.currentTimeMillis() - t4;
        jin.close();*/

      System.out.println(" JCR s- " + (jcrwrite));
      //    System.out.println(" Java s- " + (javaWrite));
      System.out.println(" JCR file size - " + jcrfile.length());
      //    System.out.println(" Java file size - " + jfile.length());
      System.out.println(" JCR des- " + (jcrread));
      //    System.out.println(" Java des- " + (javaRead));

   }
}
