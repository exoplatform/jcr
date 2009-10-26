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
package org.exoplatform.services.jcr.impl.tools.tree.generator;

import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: RandomValueNodeGenerator.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class RandomValueNodeGenerator extends WeightNodeGenerator
{
   private static final Random random = new Random();

   protected static Log log = ExoLogger.getLogger(RandomValueNodeGenerator.class);

   private final int maxBinarySize;

   private final int maxPropertiesCount;

   private final int maxValuesCount;

   private final boolean isAddMultivalued;

   private final ValueFactory valueFactory;

   public RandomValueNodeGenerator(ValueFactory valueFactory, int maxDepth, int maxWidth, int maxPropertiesCount,
      int maxValuesCount, int maxBinarySize)
   {
      super(maxDepth, maxWidth);
      this.valueFactory = valueFactory;
      this.maxPropertiesCount = maxPropertiesCount;
      this.maxValuesCount = maxValuesCount;
      this.maxBinarySize = maxBinarySize;
      this.isAddMultivalued = maxValuesCount > 1;

   }

   private Value getNewValue(int propType) throws FileNotFoundException, IOException, IllegalStateException,
      RepositoryException
   {
      Value val = null;
      byte[] buffer;
      switch (propType)
      {
         case PropertyType.STRING :
            val = valueFactory.createValue(IdGenerator.generate(), propType);
            break;
         case PropertyType.BINARY :
            int size = random.nextInt(maxBinarySize);
            InputStream inputStream = null;
            File file = null;
            if (size < 1024 * 200)
            {// 200K
               buffer = new byte[random.nextInt(maxBinarySize)];
               random.nextBytes(buffer);
               inputStream = new ByteArrayInputStream(buffer);
            }
            else
            {
               file = createBLOBTempFile(size);
               inputStream = new BufferedInputStream(new FileInputStream(file));
            }

            val = valueFactory.createValue(inputStream);
            val.getStream();// to spool data;
            inputStream.close();
            if (file != null)
               file.delete();
            break;
         case PropertyType.BOOLEAN :
            val = valueFactory.createValue(random.nextBoolean());
            break;
         case PropertyType.DATE :
            val = valueFactory.createValue(Calendar.getInstance());
            break;
         case PropertyType.DOUBLE :
            val = valueFactory.createValue(random.nextDouble());
            break;
         case PropertyType.LONG :
            val = valueFactory.createValue(random.nextLong());
            break;
         case PropertyType.NAME :
            val = valueFactory.createValue(IdGenerator.generate(), propType);
            break;
         case PropertyType.PATH :
            val = valueFactory.createValue(currentNode.getPath(), propType);
            break;
         default :
            break;
      }
      return val;
   }

   @Override
   protected void addProperties() throws RepositoryException
   {

      int propCount = random.nextInt(maxPropertiesCount);
      log.info("add " + propCount + " properties to the node " + currentNode.getPath());
      for (int i = 0; i < propCount; i++)
      {
         log.info("add property number " + i);
         int valueType = random.nextInt(8) + 1;
         try
         {
            if (this.isAddMultivalued && random.nextBoolean())
            {
               int valueCount = random.nextInt(maxValuesCount) + 1;
               Value[] values = new Value[valueCount];
               for (int j = 0; j < valueCount; j++)
               {
                  values[j] = getNewValue(valueType);
               }
               log.info("add " + valueCount + " values " + " type " + valueType);
               currentNode.setProperty(IdGenerator.generate(), values);
            }
            else
            {
               log.info("add " + " type " + valueType);
               currentNode.setProperty(IdGenerator.generate(), getNewValue(valueType));
            }
         }
         catch (FileNotFoundException e)
         {
            throw new RepositoryException(e);
         }
         catch (IllegalStateException e)
         {
            throw new RepositoryException(e);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
      }
   }

   public static File createBLOBTempFile(int sizeInb) throws IOException
   {
      // create test file
      byte[] data = new byte[1024]; // 1Kb

      File testFile = File.createTempFile("randomsizefile", ".tmp");
      FileOutputStream tempOut = new FileOutputStream(testFile);

      for (int i = 0; i < sizeInb; i += 1024)
      {
         if (i + 1024 > sizeInb)
         {
            byte[] rest = new byte[sizeInb - i];
            random.nextBytes(rest);
            tempOut.write(rest);
            continue;
         }
         random.nextBytes(data);
         tempOut.write(data);
      }
      tempOut.close();
      testFile.deleteOnExit(); // delete on test exit
      log.info("Temp file created: " + testFile.getAbsolutePath() + " size: " + testFile.length());
      return testFile;
   }

}
