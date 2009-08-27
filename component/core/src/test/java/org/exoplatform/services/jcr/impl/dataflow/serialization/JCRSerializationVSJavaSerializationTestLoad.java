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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: JCRSerializationVSJavaSerializationTest.java 111 2008-11-11
 *          11:11:11Z serg $
 */
public class JCRSerializationVSJavaSerializationTestLoad
   extends JcrImplSerializationBaseTest
{

   private final static int nodes = 1000;

   private final static int iterations = 50;

   public void testSerialization() throws Exception
   {

      List<TransientValueData> list = new ArrayList<TransientValueData>();
      // Random random = new Random();

      ByteArrayInputStream bin;

      for (int i = 0; i < nodes; i++)
      {
         bin = new ByteArrayInputStream(createBLOBTempData(20));
         list.add(new TransientValueData(bin));
      }

      Iterator<TransientValueData> it;
      // Serialize with JCR

      long jcrwrite = 0;
      long jcrread = 0;

      for (int j = 0; j < iterations; j++)
      {

         it = list.iterator();

         File jcrfile = File.createTempFile("jcr", "test");
         ObjectWriterImpl jcrout = new ObjectWriterImpl(new FileOutputStream(jcrfile));

         long t1 = System.currentTimeMillis();
         TransientValueDataWriter wr = new TransientValueDataWriter();
         while (it.hasNext())
         {
            wr.write(jcrout, it.next());
         }
         t1 = System.currentTimeMillis() - t1;

         jcrwrite += t1;
         jcrout.close();

         // deserialize
         ObjectReaderImpl jcrin = new ObjectReaderImpl(new FileInputStream(jcrfile));

         long t3 = System.currentTimeMillis();

         TransientValueDataReader rdr = new TransientValueDataReader(fileCleaner, maxBufferSize, holder);
         for (int i = 0; i < nodes; i++)
         {
            TransientValueData obj = rdr.read(jcrin);
         }
         t3 = System.currentTimeMillis() - t3;
         jcrread += t3;
         jcrin.close();

         jcrfile.delete();

      }

      // java
      long javaWrite = 0;
      long javaRead = 0;
      for (int j = 0; j < iterations; j++)
      {
         File jfile = File.createTempFile("java", "test");
         ObjectOutputStream jout = new ObjectOutputStream(new FileOutputStream(jfile));

         it = list.iterator();
         long t2 = System.currentTimeMillis();
         while (it.hasNext())
         {
            jout.writeObject(it.next());
         }
         t2 = System.currentTimeMillis() - t2;
         javaWrite += t2;
         jout.close();

         // deserialize
         ObjectInputStream jin = new ObjectInputStream(new FileInputStream(jfile));

         it = list.iterator();
         long t4 = System.currentTimeMillis();
         for (int i = 0; i < nodes; i++)
         {
            TransientValueData obj = (TransientValueData) jin.readObject();
         }
         t4 = System.currentTimeMillis() - t4;
         javaRead += t4;
         jin.close();

         jfile.delete();
      }

      System.out.println(" JCR s- " + (jcrwrite / iterations));
      System.out.println(" Java s- " + (javaWrite / iterations));
      // System.out.println(" JCR file size - " + jcrfile.length());
      // System.out.println(" Java file size - " + jfile.length());
      System.out.println(" JCR des- " + (jcrread / iterations));
      System.out.println(" Java des- " + (javaRead / iterations));

   }

   protected byte[] createBLOBTempData(int size) throws IOException
   {
      byte[] data = new byte[size * 1024]; // 1Kb
      Random random = new Random();
      random.nextBytes(data);
      return data;
   }

}
