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

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 13.02.2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ObjectZipReaderImpl 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ZipObjectReader extends ObjectReaderImpl
{

   /**
    * The ZipInputStream
    */
   private final ZipInputStream in;

   /**
    * ObjectZipReaderImpl constructor.
    *
    * @param in
    *          original InputStream
    */
   public ZipObjectReader(ZipInputStream in)
   {
      super(in);
      this.in = in;
   }

   /**
    * Get next ZipEntry.
    *
    * @return
    * @throws IOException
    */
   public ZipEntry getNextEntry() throws IOException
   {
      return this.in.getNextEntry();
   }
}
