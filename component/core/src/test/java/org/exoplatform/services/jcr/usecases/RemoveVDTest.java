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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.exoplatform.services.jcr.dataflow.serialization.ObjectWriter;
import org.exoplatform.services.jcr.dataflow.serialization.ObjectReader;
import org.exoplatform.services.jcr.dataflow.serialization.UnknownClassIdException;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectReaderImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.ObjectWriterImpl;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TransientValueDataReader;
import org.exoplatform.services.jcr.impl.dataflow.serialization.TransientValueDataWriter;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: RemoveVDTest.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class RemoveVDTest
   extends BaseUsecasesTest
{

   public void testRemove() throws IOException
   {

      File f = this.createBLOBTempFile("tempFile", 300);

      TransientValueData vd = new TransientValueData(new FileInputStream(f));
      // vd.setMaxBufferSize(200*1024);

      FileCleaner cleaner = new FileCleaner();

      vd.setFileCleaner(cleaner);

      assertNull(vd.getSpoolFile()); // not spooling by default until getAsStream() will be call

      File serf = File.createTempFile("serialization", "test");

      ObjectWriter wr = new ObjectWriterImpl(new FileOutputStream(serf));

      TransientValueDataWriter vdw = new TransientValueDataWriter();
      vdw.write(wr, vd);
      wr.flush();
      wr.close();

      vd = null;

      // read first time
      ObjectReader or = new ObjectReaderImpl(new FileInputStream(serf));

      TransientValueData vd1 = new TransientValueData();

      TransientValueDataReader vdr = new TransientValueDataReader(fileCleaner, maxBufferSize, holder);
      try
      {
         vd1 = vdr.read(or);
      }
      catch (UnknownClassIdException e)
      {
         fail(e.getMessage());
      }

      or.close();

      // read second time
      or = new ObjectReaderImpl(new FileInputStream(serf));
      TransientValueData vd2 = new TransientValueData();

      try
      {
         vd2 = vdr.read(or);
      }
      catch (UnknownClassIdException e)
      {
         fail(e.getMessage());
      }
      or.close();

      assertTrue(vd1.getSpoolFile().exists());
      assertTrue(vd2.getSpoolFile().exists());

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
      assertTrue(vd2.getSpoolFile().exists());

      // f.delete();
   }

}
