/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.utils.io;

import java.io.File;
import java.io.IOException;

import org.exoplatform.services.jcr.impl.util.io.SwapFile;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:natasha.vakulenko@gmail.com">Natasha Vakulenko</a>
 * @version $Id$  
 */

public class TestSwapFile extends TestCase
{
   private static final String DIR_NAME = "../";

   private static final String FILE_NAME = "childSwapFile";

   public void testCreateTempFile()
   {
      // Not applicable creation.
      try
      {
         SwapFile.createTempFile("prefix", "suffix", new File(DIR_NAME));
         fail("IOException should have been thrown.");
      }
      catch (IOException e)
      {
         // Ok.
      }
   }

   public void testGetSwapFile() throws IOException
   {
      // Get swap file is possible only in this way.
      SwapFile sf = SwapFile.get(new File(DIR_NAME), FILE_NAME);
      assertNotNull("File should be created.", sf);
      sf.spoolDone();
      sf.delete();
   }

   public void testIsSpooled() throws IOException
   {
      SwapFile sf = SwapFile.get(new File(DIR_NAME), FILE_NAME);
      assertFalse("Spool is not over.", sf.isSpooled());
      sf.spoolDone();
      assertTrue("Spool is over.", sf.isSpooled());
      sf.delete();
   }

   public void testSpoolDone() throws IOException
   {
      SwapFile sf = SwapFile.get(new File(DIR_NAME), FILE_NAME);
      sf.spoolDone();
      assertTrue("Spool should be done.", sf.isSpooled());
      sf.delete();
   }

   public void testDeleteSpoolFile() throws IOException
   {
      SwapFile sf = SwapFile.get(new File(DIR_NAME), FILE_NAME);
      sf.spoolDone();

      // File on disk does not exist. It will not be removed from disk space.
      assertFalse("Deleted file was not created on the disk.", sf.delete());
   }
}
