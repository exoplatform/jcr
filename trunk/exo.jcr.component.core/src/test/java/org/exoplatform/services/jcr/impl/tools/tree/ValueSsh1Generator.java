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

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ValueSsh1Generator.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ValueSsh1Generator extends ItemDataTraversingVisitor
{
   private final OutputStream ssh1ChecksumStream;

   private final MessageDigest md;

   private final byte[] space = " ".getBytes();

   public ValueSsh1Generator(ItemDataConsumer dataManager, OutputStream ssh1ChecksumStream)
      throws NoSuchAlgorithmException
   {
      super(dataManager);
      this.ssh1ChecksumStream = ssh1ChecksumStream;
      this.md = MessageDigest.getInstance("SHA");
   }

   @Override
   protected void entering(PropertyData property, int arg1) throws RepositoryException
   {
      List<ValueData> vals = property.getValues();
      for (ValueData valueData : vals)
      {
         try
         {
            md.update(valueData.getAsByteArray());
            ssh1ChecksumStream.write(property.getQPath().getAsString().getBytes());
            ssh1ChecksumStream.write(space);
            ssh1ChecksumStream.write(Integer.toString(valueData.getOrderNumber()).getBytes());
            ssh1ChecksumStream.write(space);
            ssh1ChecksumStream.write(Base64.encode(md.digest()).getBytes());
            // ssh1ChecksumStream.write(space);
            // for (byte b : md.digest()) {
            // ssh1ChecksumStream.write(Integer.toHexString(b & 0xff ).getBytes());
            // }
            // ssh1ChecksumStream.write(newLine);
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

}
