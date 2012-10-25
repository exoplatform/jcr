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

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.cas.RecordNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.cas.ValueContentAddressStorage;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.CASableDeleteValues;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.CASableWriteValue;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: CASableSimpleFileIOChannel.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class CASableSimpleFileIOChannel extends SimpleFileIOChannel
{

   static private final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CASableSimpleFileIOChannel");

   private final CASableIOSupport cas;

   private final ValueContentAddressStorage vcas;

   public CASableSimpleFileIOChannel(File rootDir, FileCleaner cleaner, String storageId,
      ValueDataResourceHolder resources, ValueContentAddressStorage vcas, String digestAlgo)
   {
      super(rootDir, cleaner, storageId, resources);

      this.vcas = vcas;
      this.cas = new CASableIOSupport(this, digestAlgo);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void write(String propertyId, ValueData value, ChangedSizeHandler sizeHandler) throws IOException
   {
      CASableWriteValue o =
         new CASableWriteValue(value, resources, cleaner, tempDir, propertyId, vcas, cas, sizeHandler);
      o.execute();
      changes.add(o);
   }

   /**
    * Delete given property value.<br/>
    * Special logic implemented for Values CAS. As the storage may have one file (same hash) for
    * multiple properties/values.<br/>
    * The implementation assumes that delete operations based on {@link getFiles()} method result.
    * 
    * @see getFiles()
    * @param propertyId
    *          - property id to be deleted
    */
   @Override
   public void delete(String propertyId) throws IOException
   {
      File[] files;
      try
      {
         files = getFiles(propertyId);
      }
      catch (RecordNotFoundException e)
      {
         // This is workaround for CAS VS. No records found for this value at the moment.
         // CASableDeleteValues saves VCAS record on commit, but it's possible the Property just
         // added in this transaction and not commited.

         files = new File[0];
      }
      CASableDeleteValues o = new CASableDeleteValues(files, resources, cleaner, tempDir, propertyId, vcas);
      o.execute();
      changes.add(o);
   }

   @Override
   protected File getFile(String propertyId, int orderNumber) throws IOException
   {
      return super.getFile(vcas.getIdentifier(propertyId, orderNumber), CASableIOSupport.HASHFILE_ORDERNUMBER);
   }

   /**
    * Returns storage files list by propertyId.<br/>
    * NOTE: Files list used for <strong>delete</strong> operation. The list will not contains files
    * shared with other properties!
    * 
    * @see ValueContentAddressStorage.getIdentifiers()
    * @param propertyId
    * @return actual files on file system related to given propertyId
    */
   @Override
   protected File[] getFiles(String propertyId) throws IOException
   {
      List<String> hids = vcas.getIdentifiers(propertyId, true); // return only
      // own ids
      File[] files = new File[hids.size()];
      for (int i = 0; i < hids.size(); i++)
         files[i] = super.getFile(hids.get(i), CASableIOSupport.HASHFILE_ORDERNUMBER);

      return files;
   }
}
