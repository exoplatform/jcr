/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.cas.ValueContentAddressStorage;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;

import java.io.File;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: CASableDeleteValues.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class CASableDeleteValues extends DeleteValues
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
   public void prepare() throws IOException
   {
      try
      {
         super.prepare();
      }
      finally
      {
         vcas.deleteProperty(propertyId);
      }
   }

}
