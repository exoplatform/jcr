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
package org.exoplatform.services.jcr.impl.dataflow.serialization;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.PrivilegedAction;

/**
 * Created by The eXo Platform SAS. <br>
 * Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: SerializationSpoolFile.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class SerializationSpoolFile extends SpoolFile
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SerializationSpoolFile");

   /**
    * SpoolFileHolder.
    */
   private final ReaderSpoolFileHolder holder;

   /**
    * Read SpoolFile id.
    */
   private final String id;

   /**
    * Constructor.
    * 
    * @param parent
    *          parent directory.
    * @param id
    *          ReadedSpoolFiel id.
    * @param holder
    *          ReaderSpoolFileHolder.
    */
   public SerializationSpoolFile(File parent, String id, ReaderSpoolFileHolder holder)
   {
      super(parent, id);
      this.holder = holder;
      this.id = id;
   }

   /**
    * Return spoolFile id.
    * 
    * @return spoolFile id
    */
   public String getId()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public synchronized boolean delete()
   {
      try
      {
         if (!inUse())
         {
            PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>()
            {
               public Boolean run()
               {
                  return SerializationSpoolFile.super.delete();
               }
            };
            boolean result = SecurityHelper.doPrivilegedAction(action);

            if (result)
            {
               holder.remove(id);
            }
            return result;
         }
      }
      catch (FileNotFoundException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return false;
   }

}
