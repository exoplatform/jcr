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

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.config.ConfigurationPersister;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
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
import java.io.OutputStream;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
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

   protected ValueParam param;

   protected ConfigurationManager configurationService;

   private ConfigurationPersister configurationPersister;

   private final List<String> configExtensionPaths = new CopyOnWriteArrayList<String>();

   /**
    * Max number of backup files
    */
   private final int maxBackupFiles;

   /**
    * Current backup file index
    */
   private int indexBackupFile = 1;

   /**
    * Default number of max backup files
    */
   public static final int DEFAULT_MAX_BACKUP_FILES = 3;

   public RepositoryServiceConfigurationImpl(InitParams params, ConfigurationManager configurationService,
      InitialContextInitializer initialContextInitializer) throws RepositoryConfigurationException
   {
      param = params.getValueParam("conf-path");
      ValueParam valueBackupFiles = params.getValueParam("max-backup-files");
      maxBackupFiles =
         valueBackupFiles == null ? DEFAULT_MAX_BACKUP_FILES : Integer.valueOf(valueBackupFiles.getValue());

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
                  (Class<ConfigurationPersister>)ClassLoading.forName(cn, this);
               configurationPersister = configurationPersisterClass.newInstance();
               configurationPersister.init(params.getPropertiesParam("working-conf"));
            }
            catch (InstantiationException e)
            {
               throw new RepositoryConfigurationException(e.getLocalizedMessage(), e);
            }
            catch (IllegalAccessException e)
            {
               throw new RepositoryConfigurationException(e.getLocalizedMessage(), e);
            }
            catch (ClassNotFoundException e)
            {
               throw new RepositoryConfigurationException(e.getLocalizedMessage(), e);
            }
         }
      }
      this.configurationService = configurationService;
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
            final File sourceConfig = new File(filePath.toURI());
            final File backUp = new File(sourceConfig.getAbsoluteFile() + "." + indexBackupFile++);

            if (indexBackupFile > maxBackupFiles)
            {
               indexBackupFile = 1;
            }

            try
            {
               SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Void>()
               {
                  public Void run() throws IOException
                  {
                     DirectoryHelper.deleteDstAndRename(sourceConfig, backUp);
                     return null;
                  }
               });
            }
            catch (IOException ioe)
            {
               throw new RepositoryException("Can't back up configuration on path "
                  + PrivilegedFileHelper.getAbsolutePath(sourceConfig), ioe);
            }

            saveStream = PrivilegedFileHelper.fileOutputStream(sourceConfig);
         }

         IBindingFactory bfact;
         try
         {
            bfact = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<IBindingFactory>()
            {
               public IBindingFactory run() throws Exception
               {
                  return BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof JiBXException)
            {
               throw (JiBXException)cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException)cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         }
         
         IMarshallingContext mctx = bfact.createMarshallingContext();

         mctx.marshalDocument(this, "ISO-8859-1", null, saveStream);
         saveStream.close();

         // writing configuration in to the persister
         if (configurationPersister != null)
         {
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

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      try
      {
         // Start from extensions first
         String[] paths = configExtensionPaths.toArray(new String[configExtensionPaths.size()]);
         for (int i = paths.length - 1; i >= 0; i--)
         {
            // We start from the last one because as it is the one with highest priority
            if (i == paths.length - 1)
            {
               init(configurationService.getInputStream(paths[i]));
            }
            else
            {
               merge(configurationService.getInputStream(paths[i]));
            }
         }

         // Then from normal config
         if (configExtensionPaths.isEmpty())
         {
            init(configurationService.getInputStream(param.getValue()));
         }
         else
         {
            merge(configurationService.getInputStream(param.getValue()));
         }

         // Then from config from persister
         if (configurationPersister != null)
         {
            if (configurationPersister.hasConfig())
            {
               merge(configurationPersister.read());
            }

            // Store the merged configuration
            retain();
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
