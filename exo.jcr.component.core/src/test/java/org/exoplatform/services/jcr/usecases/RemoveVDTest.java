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

import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.StreamValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.PersistedValueDataReader;
import org.exoplatform.services.jcr.impl.dataflow.serialization.PersistedValueDataWriter;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: RemoveVDTest.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class RemoveVDTest extends BaseUsecasesTest
{

   public void testRemove() throws IOException
   {

      File f = this.createBLOBTempFile("tempFile", 300);

      FilePersistedValueData vd = new FilePersistedValueData(0, f, SpoolConfig.getDefaultSpoolConfig());
      // vd.setMaxBufferSize(200*1024);
      //      assertNull(vd.getFile()); // not spooling by default until getAsStream() will be call

      File serf = File.createTempFile("serialization", "test");

      ObjectWriter wr = new ObjectWriterImpl(new FileOutputStream(serf));

      PersistedValueDataWriter vdw = new PersistedValueDataWriter();
      vdw.write(wr, vd);
      wr.flush();
      wr.close();

      vd = null;

      // read first time
      ObjectReader or = new ObjectReaderImpl(new FileInputStream(serf));

      FilePersistedValueData vd1 = null;

      PersistedValueDataReader vdr = new PersistedValueDataReader(holder, SpoolConfig.getDefaultSpoolConfig());
      try
      {
         vd1 = (FilePersistedValueData)vdr.read(or, PropertyType.BINARY);
      }
      catch (UnknownClassIdException e)
      {
         fail(e.getMessage());
      }

      or.close();
      // Imitation save
      ((StreamPersistedValueData) vd1).setPersistedFile(((StreamPersistedValueData) vd1).getTempFile());

      // read second time
      or = new ObjectReaderImpl(new FileInputStream(serf));
      FilePersistedValueData vd2 = null;

      try
      {
         vd2 = (FilePersistedValueData)vdr.read(or, PropertyType.BINARY);
      }
      catch (UnknownClassIdException e)
      {
         fail(e.getMessage());
      }
      or.close();
      // Imitation save
      ((StreamPersistedValueData) vd2).setPersistedFile(((StreamPersistedValueData) vd2).getTempFile());

      assertTrue(vd1.getFile().exists());
      assertTrue(vd2.getFile().exists());

      // remove first one
      vd1 = null;
      try
      {
         Thread.sleep(1000);
      }
      catch (InterruptedException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      assertTrue(vd2.getFile().exists());

      f.delete();
   }

   public void testRemoveSpoolFile() throws IOException, InterruptedException
   {

      // StreamPersistedValueData constructor variables
      int orderNum = 0;
      SpoolConfig spoolConfig = SpoolConfig.getDefaultSpoolConfig();
      SpoolFile tempFile = SpoolFile.createTempFile("tempFile", ".tmp", spoolConfig.tempDirectory);
      File file = createBLOBTempFile("file", 300);

      StreamValueData spvd = new StreamPersistedValueData(
            orderNum,
            tempFile,
            file,
            spoolConfig);

      // Update the persisted file reference so that the tempFile is dropped,
      // and add tempFile to file cleaner
      ((StreamPersistedValueData) spvd).setPersistedFile(file);

      //Add tempFile to be delete with the fileCleaner thread
      spoolConfig.fileCleaner.addFile(tempFile);

      //Start the fileCleaner
      spoolConfig.fileCleaner.halt();

      // Check that the temporary file has been removed
      assertFalse(tempFile.exists());

      // Check that the persisted copy is created
      assertTrue((spvd != null) && ((StreamPersistedValueData) spvd).getFile().exists());

      file.deleteOnExit();
   }
}
