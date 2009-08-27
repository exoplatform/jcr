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
package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import java.io.File;
import java.io.IOException;

import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.cas.ValueContentAddressStorage;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CASableDeleteValues.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class CASableDeleteValues
   extends DeleteValues
{

   /**
    * CAS manager.
    */
   protected final ValueContentAddressStorage vcas;

   /**
    * Affected Property Id.
    */
   protected final String propertyId;

   /**
    * CASableDeleteValues constructor.
    * 
    * @param files
    *          Files to be deleted
    * @param resources
    *          ValueDataResourceHolder
    * @param cleaner
    *          FileCleaner
    * @param tempDir
    *          File, temp dir
    * @param propertyId
    *          Property Id
    * @param vcas
    *          ValueContentAddressStorage CAS manager
    */
   /**
    * CASableDeleteValues constructor.
    * 
    * 
    */
   public CASableDeleteValues(File[] files, ValueDataResourceHolder resources, FileCleaner cleaner, File tempDir,
            String propertyId, ValueContentAddressStorage vcas)
   {
      super(files, resources, cleaner, tempDir);

      this.vcas = vcas;
      this.propertyId = propertyId;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void commit() throws IOException
   {
      try
      {
         super.commit();
      }
      finally
      {
         vcas.deleteProperty(propertyId);
      }
   }

}
