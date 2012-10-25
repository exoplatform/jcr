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
package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.SpoolConfig;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil.ValueDataWrapper;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.storage.value.cas.RecordAlreadyExistsException;
import org.exoplatform.services.jcr.impl.storage.value.cas.RecordNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.cas.ValueContentAddressStorage;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 19.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public abstract class CASableFileIOChannelTestBase extends JcrImplBaseTest
{//

   private static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CASableFileIOChannelTestBase");

   protected ValueContentAddressStorage vcas;

   protected File rootDir;

   protected String storageId;

   protected File testFile;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      if (vcas == null)
         initVCAS();

      if (rootDir == null)
      {
         rootDir = new File("target/temp/values-test");
         rootDir.mkdirs();

         new File(rootDir, FileValueStorage.TEMP_DIR_NAME).mkdirs();

         if (!rootDir.exists())
            throw new Exception("Folder does not exist " + rootDir.getAbsolutePath());
      }

      if (storageId == null)
         storageId = "#1";

      if (testFile == null)
         testFile = createBLOBTempFile(2048); // 2M
   }

   @Override
   protected void tearDown() throws Exception
   {
      // clean rootDir
      deleteRecursive(rootDir);
      rootDir = null;

      super.tearDown();
   }

   protected abstract void initVCAS() throws Exception;

   protected abstract FileIOChannel openCASChannel(String digestType) throws Exception;

   /**
    * Write value in channel. Check if storage contains appropriate file.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void write(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      String propertyId = IdGenerator.generate();
      ValueData value =
         new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig());

      fch.write(propertyId, value, new SimpleChangedSizeHandler());
      fch.commit();

      File vsfile =
         new File(rootDir, fch.makeFilePath(vcas.getIdentifier(propertyId, 0), CASableIOSupport.HASHFILE_ORDERNUMBER)); // orderNum
      // =0
      assertTrue("File should exists " + vsfile.getAbsolutePath(), vsfile.exists());

      InputStream etalon, tested;
      compareStream(etalon = new FileInputStream(testFile), tested = new FileInputStream(vsfile));
      etalon.close();
      tested.close();
   }

   /**
    * Tries write value already existed in channel.
    * 
    * Check if excpetion RecordAlreadyExistsException will be thrown and storage content will not be
    * changed.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeExisting(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      // prepare
      String propertyId = IdGenerator.generate();
      ValueData value =
         new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig());
      fch.write(propertyId, value, new SimpleChangedSizeHandler());
      fch.commit();

      long initialSize = calcDirSize(rootDir);

      try
      {
         fch = openCASChannel(digestType);
         fch.write(new String(propertyId),
            new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
         fch.commit();

         fail("RecordAlreadyExistsException should be thrown, record exists");
      }
      catch (RecordAlreadyExistsException e)
      {
         // ok
      }

      assertEquals("Storage size must be unchanged ", initialSize, calcDirSize(rootDir));
   }

   /**
    * Tries update (delete/write) just added value in this transaction.
    * 
    * Check if excpetion RecordAlreadyExistsException will be thrown and storage content will not be
    * changed.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeDeleteWriteInSameTransaction(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      // prepare
      String propertyId = IdGenerator.generate();
      try
      {
         ValueData value =
            new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig());
         fch.write(propertyId, value, new SimpleChangedSizeHandler());
         fch.delete(propertyId);
         fch.write(propertyId,
            new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
         fch.commit();

         // long initialSize = calcDirSize(rootDir);
      }
      catch (RecordAlreadyExistsException e)
      {
         fail("RecordAlreadyExistsException should not be thrown, record updated in same transaction");
      }

      // assertEquals("Storage size must be unchanged ", initialSize, calcDirSize(rootDir));
   }

   /**
    * Write and read value in channel. Check if storage contains value equals to the given.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeRead(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      String propertyId = IdGenerator.generate();
      ValueData value =
         new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig());
      fch.write(propertyId, value, new SimpleChangedSizeHandler());
      fch.commit();

      ValueDataWrapper vdWrapper =
         fch.read(propertyId, value.getOrderNumber(), PropertyType.BINARY, SpoolConfig.getDefaultSpoolConfig());

      InputStream etalon, tested;
      compareStream(etalon = new FileInputStream(testFile), tested = vdWrapper.value.getAsStream());
      etalon.close();
      tested.close();
   }

   /**
    * Write and delete value in channel. Checks if value is deleted.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeDelete(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      String propertyId = IdGenerator.generate();
      ValueData value =
         new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig());
      fch.write(propertyId, value, new SimpleChangedSizeHandler());
      fch.commit();

      File vsfile =
         new File(rootDir, fch.makeFilePath(vcas.getIdentifier(propertyId, 0), CASableIOSupport.HASHFILE_ORDERNUMBER)); // orderNum
      // =0

      fch.delete(propertyId);
      fch.commit();

      assertFalse("File should not exists " + vsfile.getAbsolutePath(), vsfile.exists());
   }

   /**
    * Tries delete not existing value in channel.
    * 
    * Check if excpetion RecordNotFoundException will be thrown and storage content will not be
    * changed.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void deleteNotExisting(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);

      try
      {
         FileIOChannel fch = openCASChannel(digestType);
         fch.delete(IdGenerator.generate());
         fch.commit();
         fail("RecordNotFoundException should be thrown, record not found");
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }

      assertEquals("Storage size must be unchanged ", initialSize, calcDirSize(rootDir));
   }

   /**
    * Tries read not existing value in channel.<br/>
    * 
    * Check if excpetion RecordNotFoundException will be thrown and storage content will not be
    * changed.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void readNotExisting(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);

      try
      {
         openCASChannel(digestType).read(IdGenerator.generate(), 1, PropertyType.BINARY,
            SpoolConfig.getDefaultSpoolConfig());
         fail("RecordNotFoundException should be thrown, record not found");
      }
      catch (RecordNotFoundException e)
      {
         // ok
      }

      assertEquals("Storage size must be unchanged ", initialSize, calcDirSize(rootDir));
   }

   /**
    * Write multivalued property with same content address. Check if storage contains only one file.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeSameMultivalued(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      String propertyId = IdGenerator.generate();

      long initialSize = calcDirSize(rootDir);

      for (int i = 0; i < 20; i++)
      {
         fch.write(propertyId,
            new StreamPersistedValueData(i, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
      }
      fch.commit();

      File vsfile =
         new File(rootDir, fch.makeFilePath(vcas.getIdentifier(propertyId, 15), CASableIOSupport.HASHFILE_ORDERNUMBER));
      assertTrue("File should exists " + vsfile.getAbsolutePath(), vsfile.exists());
      assertEquals("Storage size must be increased on size of ONE file ", initialSize + testFile.length(),
         calcDirSize(rootDir));
   }

   /**
    * Write multivalued property with unique content address. Check if storage contains all files.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeUniqueMultivalued(String digestType) throws Exception
   {
      FileIOChannel fch = openCASChannel(digestType);

      String propertyId = IdGenerator.generate();

      long initialSize = calcDirSize(rootDir);
      long addedSize = 0;
      for (int i = 0; i < 20; i++)
      {
         File f = createBLOBTempFile(300);
         addedSize += f.length();
         fch.write(propertyId,
            new StreamPersistedValueData(i, new FileInputStream(f), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
      }
      fch.commit();

      File vsfile =
         new File(rootDir, fch.makeFilePath(vcas.getIdentifier(propertyId, 15), CASableIOSupport.HASHFILE_ORDERNUMBER));
      assertTrue("File should exists " + vsfile.getAbsolutePath(), vsfile.exists());
      assertEquals("Storage size must be increased on size of ALL files ", initialSize + addedSize,
         calcDirSize(rootDir));
   }

   /**
    * Write set of properties with same content address. Check if storage contains only one file.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeSameProperties(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);

      String propertyId = null;
      final int count = 20;
      for (int i = 0; i < count; i++)
      {
         propertyId = IdGenerator.generate();

         FileIOChannel fch = openCASChannel(digestType);
         fch.write(propertyId,
            new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
         fch.commit();
      }

      assertEquals("Storage size must be increased on size of ONE file ", initialSize + testFile.length(),
         calcDirSize(rootDir));
   }

   /**
    * Write set of properties with unique content address. Check if storage contains all file.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void writeUniqueProperties(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);
      long addedSize = 0;

      String propertyId = null;
      final int count = 20;
      for (int i = 0; i < count; i++)
      {
         propertyId = IdGenerator.generate();

         File f = createBLOBTempFile(300);
         addedSize += f.length();

         FileIOChannel fch = openCASChannel(digestType);
         fch.write(propertyId,
            new StreamPersistedValueData(i, new FileInputStream(f), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
         fch.commit();
      }

      assertEquals("Storage size must be increased on size of ALL files ", initialSize + addedSize,
         calcDirSize(rootDir));
   }

   /**
    * Delete one of properties with same content address. Check if storage still contains (only one)
    * file.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void deleteSameProperty(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);

      // add some files
      String propertyId = null;
      final int count = 20;
      for (int i = 0; i < count; i++)
      {

         String pid = IdGenerator.generate();
         if (i == Math.round(count / 2))
            propertyId = pid;

         FileIOChannel fch = openCASChannel(digestType);
         fch.write(pid,
            new StreamPersistedValueData(0, new FileInputStream(testFile), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
         fch.commit();
      }

      // remove mapping in VCAS for one of files
      FileIOChannel fch = openCASChannel(digestType);
      fch.delete(propertyId);
      fch.commit();

      assertEquals("Storage size must be unchanged after the delete ", initialSize + testFile.length(),
         calcDirSize(rootDir));
   }

   /**
    * Delete one of properties with unique content address. Check if storage contains on one file
    * less.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void deleteUniqueProperty(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);

      // add some files
      String propertyId = null;
      final int count = 20;
      final int fileSizeKb = 355;
      long fileSize = 0;
      long addedSize = 0;

      for (int i = 0; i < count; i++)
      {
         String pid = IdGenerator.generate();
         if (i == Math.round(count / 2))
            propertyId = pid;

         File f = createBLOBTempFile(fileSizeKb);
         addedSize += (fileSize = f.length());

         FileIOChannel fch = openCASChannel(digestType);
         fch.write(pid, new StreamPersistedValueData(i, new FileInputStream(f), SpoolConfig.getDefaultSpoolConfig()),
            new SimpleChangedSizeHandler());
         fch.commit();
      }

      // remove mapping in VCAS for one of files
      FileIOChannel fch = openCASChannel(digestType);
      fch.delete(propertyId);
      fch.commit();

      assertEquals("Storage size must be decreased on one file size after the delete ", initialSize
         + (addedSize - fileSize), calcDirSize(rootDir));
   }

   /**
    * Delete one of properties with value shared between some values in few properties.
    * 
    * Check if storage contains only files related to the values.
    * 
    * @param digestType
    * @throws Exception
    */
   protected void addDeleteSharedMultivalued(String digestType) throws Exception
   {
      long initialSize = calcDirSize(rootDir);

      FileIOChannel fch = openCASChannel(digestType);

      final String property1MultivaluedId = IdGenerator.generate();

      StreamPersistedValueData sharedValue = null;

      // add multivaued property
      long m1fileSize = 0;
      long m1filesCount = 0;
      long addedSize = 0;
      for (int i = 0; i < 5; i++)
      {
         File f = createBLOBTempFile(450);
         addedSize += (m1fileSize = f.length());

         StreamPersistedValueData v =
            new StreamPersistedValueData(i, new FileInputStream(f), SpoolConfig.getDefaultSpoolConfig());

         if (i == 1)
            sharedValue = v;
         else
            m1filesCount++;

         fch.write(property1MultivaluedId, v, new SimpleChangedSizeHandler());
      }
      fch.commit();

      // add another multivalued with shared file
      final String property2MultivaluedId = IdGenerator.generate();
      long m2fileSize = 0;
      long m2filesCount = 0;
      fch = openCASChannel(digestType);
      for (int i = 0; i < 4; i++)
      {
         ValueData v;
         if (i == 2)
         {
            // use shared
            sharedValue =
               new StreamPersistedValueData(i, sharedValue.getAsStream(), SpoolConfig.getDefaultSpoolConfig());
            v = sharedValue;
         }
         else
         {
            // new file
            m2filesCount++;
            File f = createBLOBTempFile(350);
            addedSize += (m2fileSize = f.length()); // add size
            v = new StreamPersistedValueData(i, new FileInputStream(f), SpoolConfig.getDefaultSpoolConfig());
         }
         fch.write(property2MultivaluedId, v, new SimpleChangedSizeHandler());
      }
      fch.commit();

      // add some single valued properties, two new property will have shared value too
      String property1Id = null;
      String property2Id = null;
      sharedValue = new StreamPersistedValueData(0, sharedValue.getAsStream(), SpoolConfig.getDefaultSpoolConfig());
      for (int i = 0; i < 10; i++)
      {
         String pid = IdGenerator.generate();
         ValueData v;
         if (i == 1)
         {
            property1Id = pid;
            v = sharedValue;
         }
         else if (i == 5)
         {
            property2Id = pid;
            v = sharedValue;
         }
         else
         {
            File f = createBLOBTempFile(425);
            addedSize += f.length();
            v = new StreamPersistedValueData(i, new FileInputStream(f), SpoolConfig.getDefaultSpoolConfig());
         }
         FileIOChannel vfch = openCASChannel(digestType);
         vfch.write(pid, v, new SimpleChangedSizeHandler());
         vfch.commit();
      }

      // final size
      long finalSize = initialSize + addedSize;

      // remove mapping in VCAS for singlevalued property #2
      fch = openCASChannel(digestType);
      fch.delete(property2Id);
      fch.commit();
      assertEquals("Storage size must be unchanged after the delete of property #2 ", finalSize, calcDirSize(rootDir));

      // remove mapping in VCAS for multivalued property #1
      finalSize -= m1fileSize * m1filesCount;
      fch = openCASChannel(digestType);
      fch.delete(property1MultivaluedId);
      fch.commit();
      assertEquals("Storage size must be unchanged after the delete of multivalue property #1 ", finalSize,
         calcDirSize(rootDir));

      // remove mapping in VCAS for multivalued property #2
      finalSize -= m2fileSize * m2filesCount;
      fch = openCASChannel(digestType);
      fch.delete(property2MultivaluedId);
      fch.commit();
      assertEquals("Storage size must be decreased on " + (m2fileSize * m2filesCount)
         + " bytes after the delete of multivalue property #2 ", finalSize, calcDirSize(rootDir));

      // remove mapping in VCAS for singlevalued property #1
      finalSize -= m1fileSize;
      fch = openCASChannel(digestType);
      fch.delete(property1Id);
      fch.commit();
      assertEquals("Storage size must be decreased on " + m1fileSize + " bytes after the delete of property #1 ",
         finalSize, calcDirSize(rootDir));
   }

   // ----- utilities -----

   private long deleteRecursive(File dir)
   {
      long count = 0;
      for (File sf : dir.listFiles())
      {
         if (sf.isDirectory() && sf.list().length > 0)
            count += deleteRecursive(sf);
         else if (sf.delete())
            count += 1;
         else
            LOG.warn("Can't delete file " + sf.getAbsolutePath());
      }
      count += dir.delete() ? 1 : 0;
      return count;
   }

   private long calcDirSize(File dir)
   {
      long size = 0;
      for (File sf : dir.listFiles())
      {
         if (sf.isDirectory())
            size += calcDirSize(sf);
         else
            size += sf.length();
      }
      return size;
   }

   // ------ tests ------

   // public void testWriteDeleteWriteMD5() throws Exception {
   // writeDeleteWriteInSameTransaction("MD5");
   // }
   //
   // public void testWriteDeleteWriteSHA1() throws Exception {
   // writeDeleteWriteInSameTransaction("SHA1");
   // }

   public void testWriteMD5() throws Exception
   {
      write("MD5");
   }

   public void testWriteSHA1() throws Exception
   {
      write("SHA1");
   }

   public void testReadMD5() throws Exception
   {
      writeRead("MD5");
   }

   public void testReadSHA1() throws Exception
   {
      writeRead("SHA1");
   }

   public void testDeleteMD5() throws Exception
   {
      writeDelete("MD5");
   }

   public void testDeleteSHA1() throws Exception
   {
      writeDelete("SHA1");
   }

   public void testMultivaluedMD5() throws Exception
   {
      writeSameMultivalued("MD5");
   }

   public void testMultivaluedSHA1() throws Exception
   {
      writeSameMultivalued("SHA1");
   }

   public void testUniqueMultivaluedMD5() throws Exception
   {
      writeUniqueMultivalued("MD5");
   }

   public void testUniqueMultivaluedSHA1() throws Exception
   {
      writeUniqueMultivalued("SHA1");
   }

   public void testSamePropertiesMD5() throws Exception
   {
      writeSameProperties("MD5");
   }

   public void testSamePropertiesSHA1() throws Exception
   {
      writeSameProperties("SHA1");
   }

   public void testUniquePropertiesMD5() throws Exception
   {
      writeUniqueProperties("MD5");
   }

   public void testUniquePropertiesSHA1() throws Exception
   {
      writeUniqueProperties("SHA1");
   }

   public void testDeleteSamePropertyMD5() throws Exception
   {
      deleteSameProperty("MD5");
   }

   public void testDeleteSamePropertySHA1() throws Exception
   {
      deleteSameProperty("SHA1");
   }

   public void testDeleteUniquePropertyMD5() throws Exception
   {
      deleteUniqueProperty("MD5");
   }

   public void testDeleteUniquePropertySHA1() throws Exception
   {
      deleteUniqueProperty("SHA1");
   }

   public void testAddDeleteSharedMultivaluedMD5() throws Exception
   {
      addDeleteSharedMultivalued("MD5");
   }

   public void testAddDeleteSharedMultivaluedSHA1() throws Exception
   {
      addDeleteSharedMultivalued("SHA1");
   }

   // failt tests

   public void testAddExistingMD5() throws Exception
   {
      writeExisting("MD5");
   }

   public void testAddExistingSHA1() throws Exception
   {
      writeExisting("SHA1");
   }

   public void testRemoveNotExistingMD5() throws Exception
   {
      deleteNotExisting("MD5");
   }

   public void testRemoveNotExistingSHA1() throws Exception
   {
      deleteNotExisting("SHA1");
   }

   public void testReadNotExistingMD5() throws Exception
   {
      readNotExisting("MD5");
   }

   public void testReadNotExistingSHA1() throws Exception
   {
      readNotExisting("SHA1");
   }

}
