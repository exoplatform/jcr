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
package org.exoplatform.services.jcr.impl.config;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.config.ConfigurationPersister;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
import org.picocontainer.Startable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: RepositoryServiceConfigurationImpl.java 12841 2007-02-16 08:58:38Z peterit $
 */

public class RepositoryServiceConfigurationImpl extends RepositoryServiceConfiguration implements Startable
{

   private ValueParam param;

   private ConfigurationManager configurationService;

   private ConfigurationPersister configurationPersister;

   private final List<String> configExtensionPaths = new CopyOnWriteArrayList<String>();

   public RepositoryServiceConfigurationImpl(InitParams params, ConfigurationManager configurationService,
      InitialContextInitializer initialContextInitializer) throws RepositoryConfigurationException
   {

      param = params.getValueParam("conf-path");

      if (params.getPropertiesParam("working-conf") != null)
      {
         String cn = params.getPropertiesParam("working-conf").getProperty("persister-class-name");
         if (cn == null)
         {
            cn = params.getPropertiesParam("working-conf").getProperty("persisterClassName"); // try old name, pre 1.9
         }

         if (cn != null)
         {
            try
            {
               Class<ConfigurationPersister> configurationPersisterClass =
                  (Class<ConfigurationPersister>)Class.forName(cn);
               configurationPersister = configurationPersisterClass.newInstance();
               configurationPersister.init(params.getPropertiesParam("working-conf"));
            }
            catch (InstantiationException e)
            {
               throw new RepositoryConfigurationException(e.getLocalizedMessage());
            }
            catch (IllegalAccessException e)
            {
               throw new RepositoryConfigurationException(e.getLocalizedMessage());
            }
            catch (ClassNotFoundException e)
            {
               throw new RepositoryConfigurationException(e.getLocalizedMessage());
            }
         }
      }
      this.configurationService = configurationService;
   }

   public RepositoryServiceConfigurationImpl(InputStream is) throws RepositoryConfigurationException
   {
      init(is);
   }

   /**
    * Allows to add new configuration paths
    */
   public void addConfig(RepositoryServiceConfigurationPlugin plugin)
   {
      configExtensionPaths.add(plugin.getConfPath());
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.config.RepositoryServiceConfiguration#isRetainable()
    */
   @Override
   public boolean isRetainable()
   {
      if (configurationPersister != null)
      {
         return true;
      }

      String strfileUri = param.getValue();
      URL fileURL;
      try
      {
         fileURL = configurationService.getURL(strfileUri);
      }
      catch (Exception e)
      {
         return false;
      }
      return fileURL.getProtocol().equals("file");
   }

   /**
    * Retain configuration of JCR If configurationPersister is configured it write data in to the
    * persister otherwise it try to save configuration in file
    * 
    * @throws RepositoryException
    */
   @Override
   public synchronized void retain() throws RepositoryException
   {
      try
      {

         if (!isRetainable())
            throw new RepositoryException("Unsupported  configuration place "
               + configurationService.getURL(param.getValue())
               + " If you want to save configuration, start repository from standalone file."
               + " Or persister-class-name not configured");

         OutputStream saveStream = null;

         if (configurationPersister != null)
         {
            saveStream = new ByteArrayOutputStream();
         }
         else
         {
            URL filePath = configurationService.getURL(param.getValue());
            File sourceConfig = new File(filePath.toURI());
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
            File backUp = new File(sourceConfig.getAbsoluteFile() + "_" + format.format(new Date()));
            if (!PrivilegedFileHelper.renameTo(sourceConfig, backUp))
               throw new RepositoryException("Can't back up configuration on path "
                  + PrivilegedFileHelper.getAbsolutePath(sourceConfig));
            saveStream = PrivilegedFileHelper.fileOutputStream(sourceConfig);
         }

         IBindingFactory bfact = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         IMarshallingContext mctx = bfact.createMarshallingContext();

         mctx.marshalDocument(this, "ISO-8859-1", null, saveStream);
         saveStream.close();

         // writing configuration in to the persister
         if (configurationPersister != null)
         {
            // TODO file output stream
            configurationPersister.write(new ByteArrayInputStream(((ByteArrayOutputStream)saveStream).toByteArray()));
         }

      }
      catch (JiBXException e)
      {
         throw new RepositoryException(e);
      }
      catch (FileNotFoundException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }
      catch (Exception e)
      {
         throw new RepositoryException(e);
      }

   }

   private void initFromStream(InputStream jcrConfigurationInputStream) throws RepositoryConfigurationException
   {
      try
      {
         if (configurationPersister != null)
         {
            if (!configurationPersister.hasConfig())
            {
               configurationPersister.write(jcrConfigurationInputStream);
            }
            init(configurationPersister.read());
         }
         else
         {
            init(jcrConfigurationInputStream);
         }
      }
      finally
      {
         try
         {
            jcrConfigurationInputStream.close();
         }
         catch (IOException e)
         {
            // ignore me
         }
      }

   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      try
      {
         if ((configurationPersister != null && configurationPersister.hasConfig()))
         {
            initFromStream(configurationService.getInputStream(param.getValue()));

            // Will be merged extension repository configuration with configuration from persister.  
            if (!configExtensionPaths.isEmpty())
            {
               String[] paths = configExtensionPaths.toArray(new String[configExtensionPaths.size()]);
               for (int i = paths.length - 1; i >= 0; i--)
               {
                  merge(configurationService.getInputStream(paths[i]));
               }
               // Store the merged configuration
               if (configurationPersister != null)
               {
                  retain();
               }
            }
         }
         else
         {

            String[] paths = configExtensionPaths.toArray(new String[configExtensionPaths.size()]);
            for (int i = paths.length - 1; i >= 0; i--)
            {
               // We start from the last one because as it is the one with highest priorityn
               if (i == paths.length - 1)
               {
                  init(configurationService.getInputStream(paths[i]));
               }
               else
               {
                  merge(configurationService.getInputStream(paths[i]));
               }
            }
            merge(configurationService.getInputStream(param.getValue()));
            // Store the merged configuration
            if (configurationPersister != null && !configurationPersister.hasConfig())
            {
               retain();
            }
         }
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RuntimeException(e);
      }
      catch (Exception e)
      {
         throw new RuntimeException(new RepositoryConfigurationException("Fail to init from xml! Reason: " + e, e));
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }
}
