/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.distribution.impl;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * It will generate from the data id an hash code thanks to an hashing function
 * then with this hash code it will be able to generate a hierarchy of sub-nodes 
 * with <code>n</code> levels of depth for example with <code>n = 4</code> and
 * MD5 as hashing function:
 * For "john.smith" with MD5, the hash code in base 32 is 12spjkm4fhsrl151pva3f7mf1r, 
 * so the path would be "1/2/s/john.smith"
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class DataDistributionByHash extends AbstractDataDistributionType
{
   /**
    * The level of depth used by the algorithm
    */
   private int depth = 4;

   /**
    * The name of the hash algorithm to use
    */
   private String hashAlgorithm = "MD5";

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> getAncestors(String dataId)
   {
      List<String> result = new ArrayList<String>(depth);
      String hash = hash(dataId);
      int length = hash.length();
      for (int i = 0; i < depth - 1 && i < length; i++)
      {
         result.add(hash.substring(i, i + 1));
      }
      result.add(dataId);
      return result;
   }

   /**
    * Gives the hash code of the given data id
    * @param dataId The id of the data to hash
    * @return The hash code of the value of the given data id in base 32
    */
   private String hash(String dataId)
   {
      try
      {
         MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
         digest.update(dataId.getBytes("UTF-8"));
         return new BigInteger(1, digest.digest()).toString(32);
      }
      catch (NumberFormatException e)
      {
         throw new RuntimeException("Could not generate the hash code of '" + dataId + "' with the algorithm '"
            + hashAlgorithm + "'", e);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException("Could not generate the hash code of '" + dataId + "' with the algorithm '"
            + hashAlgorithm + "'", e);
      }
      catch (NoSuchAlgorithmException e)
      {
         throw new RuntimeException("Could not generate the hash code of '" + dataId + "' with the algorithm '"
            + hashAlgorithm + "'", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean useParametersOnLeafOnly()
   {
      return true;
   }
}
