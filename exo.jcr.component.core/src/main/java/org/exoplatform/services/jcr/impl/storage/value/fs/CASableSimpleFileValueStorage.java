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

package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.storage.value.cas.ValueContentAddressStorage;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: CASableSimpleFileValueStorage.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public class CASableSimpleFileValueStorage extends FileValueStorage
{

   private ValueContentAddressStorage vcas;

   private String digestAlgo;

   /**
    * CASableSimpleFileValueStorage constructor.
    */
   public CASableSimpleFileValueStorage(FileCleaner fileCleaner)
   {
      super(fileCleaner);
   }

   @Override
   public void init(Properties props, ValueDataResourceHolder resources) throws IOException,
      RepositoryConfigurationException
   {
      super.init(props, resources);

      this.digestAlgo = props.getProperty(ValueContentAddressStorage.DIGEST_ALGO_PARAM);
      String vcasType = props.getProperty(ValueContentAddressStorage.VCAS_TYPE_PARAM);

      // get other vcas specific props and make VCAS
      try
      {
         vcas = (ValueContentAddressStorage)ClassLoading.forName(vcasType, this).newInstance();
      }
      catch (ClassNotFoundException e)
      {
         throw new RepositoryConfigurationException("VCAS Storage class load error " + e, e);
      }
      catch (InstantiationException e)
      {
         throw new RepositoryConfigurationException("VCAS Storage class load error " + e, e);
      }
      catch (IllegalAccessException e)
      {
         throw new RepositoryConfigurationException("VCAS Storage class load error " + e, e);
      }
      vcas.init(props);
   }

   @Override
   public ValueIOChannel openIOChannel() throws IOException
   {
      return new CASableSimpleFileIOChannel(rootDir, cleaner, id, resources, vcas, digestAlgo);
   }

}
