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
 * Created by The eXo Platform SAS Date: 22.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CASableTreeFileIOChannel.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class CASableTreeFileIOChannel extends TreeFileIOChannel
{

   static private final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CASableTreeFileIOChannel");

   private final ValueContentAddressStorage vcas;

   private final CASableIOSupport cas;

   CASableTreeFileIOChannel(File rootDir, FileCleaner cleaner, String storageId, ValueDataResourceHolder resources,
      ValueContentAddressStorage vcas, String digestAlgo)
   {
      super(rootDir, cleaner, storageId, resources);

      this.vcas = vcas;
      this.cas = new CASableIOSupport(this, digestAlgo);
   }

   @Override
   protected File getFile(final String propertyId, final int orderNumber) throws IOException
   {
      return super.getFile(vcas.getIdentifier(propertyId, orderNumber), CASableIOSupport.HASHFILE_ORDERNUMBER);
   }

   @Override
   protected File[] getFiles(final String propertyId) throws IOException
   {
      List<String> hids = vcas.getIdentifiers(propertyId, true); // return only
      // own ids
      File[] files = new File[hids.size()];
      for (int i = 0; i < hids.size(); i++)
         files[i] = super.getFile(hids.get(i), CASableIOSupport.HASHFILE_ORDERNUMBER);

      return files;
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
   public void write(String propertyId, ValueData value, ChangedSizeHandler sizeHandler) throws IOException
   {
      CASableWriteValue o =
         new CASableWriteValue(value, resources, cleaner, tempDir, propertyId, vcas, cas, sizeHandler);
      o.execute();
      changes.add(o);
   }
}
