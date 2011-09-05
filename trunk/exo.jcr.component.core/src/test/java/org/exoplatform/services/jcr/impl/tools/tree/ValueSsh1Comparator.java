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
package org.exoplatform.services.jcr.impl.tools.tree;

import org.apache.ws.commons.util.Base64;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataTraversingVisitor;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ValueSsh1Comparator.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ValueSsh1Comparator extends ItemDataTraversingVisitor
{
   private Map<String, HashMap<Integer, byte[]>> propertysCheckSum = new HashMap<String, HashMap<Integer, byte[]>>();

   private final MessageDigest md;

   protected Log log = ExoLogger.getLogger("exo.jcr.component.core.ValueSsh1Comparator");

   public ValueSsh1Comparator(ItemDataConsumer dataManager, InputStream ssh1ChecksumStream) throws IOException,
      NoSuchAlgorithmException
   {
      super(dataManager);
      this.md = MessageDigest.getInstance("SHA");
      loadCheckSum(ssh1ChecksumStream);
   }

   @Override
   protected void entering(PropertyData property, int arg1) throws RepositoryException
   {
      List<ValueData> vals = property.getValues();
      for (ValueData valueData : vals)
      {
         HashMap<Integer, byte[]> propSums = propertysCheckSum.get(property.getQPath().getAsString());
         if (propSums == null)
         {
            log.info("Property " + property.getQPath().getAsString() + " check sum not found");
            continue;
         }
         try
         {
            byte[] checkSum = propSums.get(valueData.getOrderNumber());
            if (checkSum == null)
            {
               log.info("Property " + property.getQPath().getAsString() + " check sum not found");
               continue;
            }
            md.update(valueData.getAsByteArray());
            if (!Arrays.equals(checkSum, md.digest()))
               throw new RepositoryException("Ssh1 not equals " + property.getQPath().getAsString());

         }
         catch (IllegalStateException e)
         {
            throw new RepositoryException(e);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e);
         }
         log.info("Property " + property.getQPath().getAsString() + " check ok");
      }

   }

   @Override
   protected void entering(NodeData arg0, int arg1) throws RepositoryException
   {

   }

   @Override
   protected void leaving(PropertyData arg0, int arg1) throws RepositoryException
   {

   }

   @Override
   protected void leaving(NodeData arg0, int arg1) throws RepositoryException
   {

   }

   private void loadCheckSum(InputStream ssh1ChecksumStream) throws IOException
   {
      InputStreamReader eisr = new InputStreamReader(ssh1ChecksumStream, "UTF-8");
      LineNumberReader lineNumberReader = new LineNumberReader(eisr);
      String line;
      while ((line = lineNumberReader.readLine()) != null)
      {
         StringTokenizer stringTokenizer = new StringTokenizer(line);

         if (stringTokenizer.countTokens() != 3)
            throw new IOException("Invalid file");

         String path = stringTokenizer.nextToken();
         Integer orderNumber = Integer.parseInt(stringTokenizer.nextToken());
         byte[] checkSum = Base64.decode(stringTokenizer.nextToken());

         log.info(path + " " + orderNumber + " " + checkSum);
         HashMap<Integer, byte[]> propMap = propertysCheckSum.get(path);
         if (propMap == null)
         {
            propMap = new HashMap<Integer, byte[]>();
            propertysCheckSum.put(path, propMap);
         }
         propMap.put(orderNumber, checkSum);
      }

   };

}
