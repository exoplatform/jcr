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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import org.exoplatform.services.jcr.impl.util.io.SpoolFile;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:natasha.vakulenko@gmail.com">Natasha Vakulenko</a>
 * @version $Id$  
 */

public class TestSpoolFile extends TestCase
{
   private static final String dirName = "../";

   private static final String fileName = "testSpoolFile";

   public void testCreateTempFile() throws IOException
   {
      // This method creates a file on disk space.
      // When calling a method delete() it should be removed.
      SpoolFile sf = SpoolFile.createTempFile("prefix", "suffics", new File(dirName));
      assertNotNull("File should be created.", sf);
      assertTrue("File should be deleted.", sf.delete());
   }

   public void testAcquireFile() throws FileNotFoundException
   {
      SpoolFile sf = new SpoolFile(dirName + fileName);

      // Add new holder of file, now file must be in use.
      sf.acquire("holder");
      assertTrue("File must be in use.", sf.inUse());

      sf.release("holder");
      sf.delete();

      // Use non-existent file.
      try
      {
         sf.acquire("anotherHolder");
         fail("FileNotFoundException should have been thrown.");
      }
      catch (FileNotFoundException e)
      {
         // Ok.
      }
   }

   public void testReleaseFile() throws FileNotFoundException
   {
      SpoolFile sf = new SpoolFile(dirName + fileName);

      // Add new holder of file.
      sf.acquire("holder");

      // Release file from holder.
      sf.release("holder");

      assertFalse("File should not have holders.", sf.inUse());
      sf.delete();

      // Use non-existent file.
      try
      {
         sf.release("someHolder");
         fail("FileNotFoundException should have been thrown.");
      }
      catch (FileNotFoundException e)
      {
         // Ok.
      }
   }

   public void testFileInUse() throws FileNotFoundException
   {
      SpoolFile sf = new SpoolFile(dirName + fileName);

      sf.acquire("holder");
      assertTrue("The file has holder. It must be in use.", sf.inUse());

      sf.release("holder");
      assertFalse("The file has no holder. It should not be in use.", sf.inUse());

      sf.delete();

      // Work with non-existent file.
      try
      {
         sf.inUse();
         fail("FileNotFoundException should have been thrown.");
      }
      catch (FileNotFoundException e)
      {
         // Ok.
      }
   }

   public void testDeleteFile() throws FileNotFoundException
   {
      // This method not creates a file on disk space.
      SpoolFile sf = new SpoolFile(dirName + fileName);

      // Add and release new holder of file.
      sf.acquire("holder");
      sf.release("holder");

      // Now file is free. It can be deleted.
      // File on disk does not exist. It will not be removed from disk space.
      assertFalse("Deleted file was not created on the disk.", sf.delete());
   }
}
