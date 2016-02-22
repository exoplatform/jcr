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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.exoplatform.services.jcr.JcrImplBaseTest;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 06.11.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id$
 */
public class TestByteArrayPersistedValueDataSerialization
   extends JcrImplBaseTest
{
   public void testBAPVDSerialization() throws Exception
   {
    
      byte []buf = new byte[124578];
      
      for (int i = 0; i< buf.length; i++)
         buf[i] = (byte) (Math.random()*256);
      
      // Create ValueData instants
      ByteArrayPersistedValueData vd = new ByteArrayPersistedValueData(11, buf);

      File out = File.createTempFile("test", ".data");
      out.deleteOnExit();

      //serialize
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(out));
      oos.writeObject(vd);
      oos.flush();
      oos.close();

      //deserialize
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(out));
      ByteArrayPersistedValueData deserializedValueData = (ByteArrayPersistedValueData) ois.readObject();

      //check
      assertNotNull(deserializedValueData);
      assertEquals(vd.getLength(), deserializedValueData.getLength());
      assertEquals(vd.getOrderNumber(), deserializedValueData.getOrderNumber());
      
      for (int j = 0; j < vd.getAsByteArray().length; j++)
        assertEquals(vd.getAsByteArray()[j], deserializedValueData.getAsByteArray()[j]);
   }
}
