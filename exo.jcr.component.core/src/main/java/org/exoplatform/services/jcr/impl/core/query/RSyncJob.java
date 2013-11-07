/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 6, 2012  
 */
/**
 * Wrapper of native process calling RSYNC utility 
 */
public class RSyncJob
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RsyncJob");

   private final static String RSYNC_USER_SYSTEM_PROPERTY = "USER";

   private final static String RSYNC_PASSWORD_SYSTEM_PROPERTY = "RSYNC_PASSWORD";

   private Process process;

   private final String src;

   private final String dst;

   private String userName;

   private String password;

   public RSyncJob(String src, String dst, String userName, String password)
   {
      this.src = src.endsWith(File.separator) ? src : src + File.separator;
      this.dst = dst;
      this.userName = userName;
      this.password = password;
   }

   /**
    * Executes RSYNC synchronization job 
    * 
    * @throws IOException
    */
   public void execute() throws IOException
   {
      // Future todo: Use JNI and librsync library?
      Runtime run = Runtime.getRuntime();
      try
      {
         String command = "rsync -rv --delete " + src + " " + dst;
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Rsync job started: " + command);
         }
         if (userName != null && password != null)
         {
            String[] envProperties =
               new String[]{RSYNC_USER_SYSTEM_PROPERTY + "=" + userName,
                  RSYNC_PASSWORD_SYSTEM_PROPERTY + "=" + password};
            process = run.exec(command, envProperties);
         }
         else
         {
            process = run.exec(command);
         }

         // Handle process Standard and Error output
         InputStream stderr = process.getErrorStream();
         InputStreamReader isrErr = new InputStreamReader(stderr);
         BufferedReader brErr = new BufferedReader(isrErr);

         InputStream stdout = process.getInputStream();
         InputStreamReader isrStd = new InputStreamReader(stdout);
         BufferedReader brStd = new BufferedReader(isrStd);

         String val = null;
         StringBuilder stringBuilderErr = new StringBuilder();
         StringBuilder stringBuilderStd = new StringBuilder();
         while ((val = brStd.readLine()) != null)
         {
            stringBuilderStd.append(val);
            stringBuilderStd.append('\n');
         }

         while ((val = brErr.readLine()) != null)
         {
            stringBuilderErr.append(val);
            stringBuilderErr.append('\n');
         }

         Integer returnCode = null;
         // wait for thread
         while (returnCode == null)
         {
            try
            {
               returnCode = process.waitFor();
            }
            catch (InterruptedException e)
            {
               // oops, this can happen sometimes
            }
         }
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Rsync job finished: " + returnCode + ". Error stream output \n"
               + stringBuilderErr.toString() + " Standard stream output \n" + stringBuilderStd.toString());
         }
         if (returnCode != 0)
         {
            throw new IOException("RSync job finished with exit code is " + returnCode + ". Error stream output: \n"
               + stringBuilderErr.toString());
         }
      }
      finally
      {
         process = null;
      }
   }

   public void forceCancel()
   {
      if (process != null)
      {
         process.destroy();
      }
   }
}