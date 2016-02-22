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

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS. <br>Date: 16.02.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: JcrImplSerializationBaseTest.java 111 2008-11-11 11:11:11Z
 *          rainf0x $
 */
public abstract class JcrImplSerializationBaseTest extends JcrImplBaseTest
{

   protected void checkIterator(Iterator<ItemState> expected, Iterator<ItemState> changes) throws Exception
   {

      while (expected.hasNext())
      {

         assertTrue(changes.hasNext());
         ItemState expect = expected.next();
         ItemState elem = changes.next();

         assertEquals(expect.getState(), elem.getState());
         // assertEquals(expect.getAncestorToSave(), elem.getAncestorToSave());
         ItemData expData = expect.getData();
         ItemData elemData = elem.getData();
         assertEquals(expData.getQPath(), elemData.getQPath());
         assertEquals(expData.isNode(), elemData.isNode());
         assertEquals(expData.getIdentifier(), elemData.getIdentifier());
         assertEquals(expData.getParentIdentifier(), elemData.getParentIdentifier());

         if (!expData.isNode())
         {
            PropertyData expProp = (PropertyData)expData;
            PropertyData elemProp = (PropertyData)elemData;
            assertEquals(expProp.getType(), elemProp.getType());
            assertEquals(expProp.isMultiValued(), elemProp.isMultiValued());

            List<ValueData> expValDat = expProp.getValues();
            List<ValueData> elemValDat = elemProp.getValues();
            // Both value data are null 
            if (expValDat != null && elemValDat != null)
            {
               assertEquals(expValDat.size(), elemValDat.size());
               for (int j = 0; j < expValDat.size(); j++)
               {

                  // are of the same class
                  assertEquals(expValDat.get(j).getClass(), elemValDat.get(j).getClass());
                  //
                  if (expValDat.get(j) instanceof FilePersistedValueData)
                  {
                     // if files in both instances are null
                     Boolean nullFiles =
                        ((FilePersistedValueData)expValDat.get(j)).getFile() == null
                           && ((FilePersistedValueData)elemValDat.get(j)).getFile() == null;
                     // if there are instances of StreamPersistedValueData
                     if (expValDat.get(j) instanceof StreamPersistedValueData)
                     {
                        // and they both have null spool files
                        nullFiles &=
                           ((StreamPersistedValueData)expValDat.get(j)).getTempFile() == null
                              && ((StreamPersistedValueData)elemValDat.get(j)).getTempFile() == null;
                     }
                     if (nullFiles)
                     {
                        // Both value data are equals
                        continue;
                     }
                  }

                  assertTrue(java.util.Arrays.equals(expValDat.get(j).getAsByteArray(), elemValDat.get(j)
                     .getAsByteArray()));

                  // check is received property values ReplicableValueData
                  // assertTrue(elemValDat.get(j) instanceof ReplicableValueData);
               }
            }
         }
      }
      assertFalse(changes.hasNext());

   }

   protected void checkResults(List<TransactionChangesLog> srcLog) throws Exception
   {
      File jcrfile = serializeLogs(srcLog);

      List<TransactionChangesLog> destLog = deSerializeLogs(jcrfile);

      assertEquals(srcLog.size(), destLog.size());

      for (int i = 0; i < srcLog.size(); i++)
      {
         checkIterator(srcLog.get(i).getAllStates().iterator(), destLog.get(i).getAllStates().iterator());
      }
   }

   protected File serializeLogs(List<TransactionChangesLog> logs) throws IOException, UnknownClassIdException
   {
      File jcrfile = File.createTempFile("jcr", "test");
      ObjectWriterImpl jcrout = new ObjectWriterImpl(new FileOutputStream(jcrfile));

      TransactionChangesLogWriter wr = new TransactionChangesLogWriter();
      for (TransactionChangesLog tcl : logs)
      {
         wr.write(jcrout, tcl);
      }

      jcrout.flush();
      jcrout.close();

      return jcrfile;
   }

   protected List<TransactionChangesLog> deSerializeLogs(File jcrfile) throws IOException, UnknownClassIdException
   {
      ObjectReaderImpl jcrin = new ObjectReaderImpl(new FileInputStream(jcrfile));

      List<TransactionChangesLog> readed = new ArrayList<TransactionChangesLog>();

      try
      {
         while (true)
         {
            TransactionChangesLog obj =
               (new TransactionChangesLogReader(SpoolConfig.getDefaultSpoolConfig(), holder)).read(jcrin);
            // TransactionChangesLog obj = new TransactionChangesLog();
            // obj.readObject(jcrin);
            readed.add(obj);
         }
      }
      catch (EOFException e)
      {
         // ok
      }

      //Imitation of save.
      imitationSave(readed);

      return readed;
   }

   /**
    * Imitation of JCR save
    * 
    * @param readed
    * @return
    * @throws IOException 
    */
   private void imitationSave(List<TransactionChangesLog> readed) throws IOException
   {
      for (TransactionChangesLog tLog : readed)
      {
         ChangesLogIterator it = tLog.getLogIterator();

         while (it.hasNextLog())
         {
            PlainChangesLog pLog = it.nextLog();

            for (ItemState state : pLog.getAllStates())
            {
               ItemData itemData = state.getData();

               if (!itemData.isNode())
               {
                  PropertyData propData = (PropertyData)itemData;
                  if (propData.getValues() != null)
                  {
                     for (ValueData valueData : propData.getValues())
                     {
                        if (valueData instanceof StreamPersistedValueData)
                        {
                           // imitation of JCR save
                           if (((StreamPersistedValueData)valueData).getTempFile() != null)
                           {
                              ((StreamPersistedValueData)valueData)
                                 .setPersistedFile(((StreamPersistedValueData)valueData).getTempFile());
                           }
                           else
                           {
                              File file = File.createTempFile("tempFile", "tmp");
                              file.deleteOnExit();
                              if (((StreamPersistedValueData)valueData).getStream() != null)
                              {
                                 copy(((StreamPersistedValueData)valueData).getStream(), new FileOutputStream(file));
                                 ((StreamPersistedValueData)valueData).setPersistedFile(file);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected long copy(InputStream in, OutputStream out) throws IOException
   {
      // compare classes as in Java6 Channels.newChannel(), Java5 has a bug in newChannel().
      boolean inFile = in instanceof FileInputStream && FileInputStream.class.equals(in.getClass());
      boolean outFile = out instanceof FileOutputStream && FileOutputStream.class.equals(out.getClass());
      if (inFile && outFile)
      {
         // it's user file
         FileChannel infch = ((FileInputStream)in).getChannel();
         FileChannel outfch = ((FileOutputStream)out).getChannel();

         long size = 0;
         long r = 0;
         do
         {
            r = outfch.transferFrom(infch, r, infch.size());
            size += r;
         }
         while (r < infch.size());
         return size;
      }
      else
      {
         // it's user stream (not a file)
         ReadableByteChannel inch = inFile ? ((FileInputStream)in).getChannel() : Channels.newChannel(in);
         WritableByteChannel outch = outFile ? ((FileOutputStream)out).getChannel() : Channels.newChannel(out);

         long size = 0;
         int r = 0;
         ByteBuffer buff = ByteBuffer.allocate(32 * 1024);
         buff.clear();
         while ((r = inch.read(buff)) >= 0)
         {
            buff.flip();

            // copy all
            do
            {
               outch.write(buff);
            }
            while (buff.hasRemaining());

            buff.clear();
            size += r;
         }

         if (outFile)
         {
            ((FileChannel)outch).force(true); // force all data to FS
         }

         return size;
      }
   }
}
