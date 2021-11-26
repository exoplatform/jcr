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

package org.exoplatform.services.jcr.impl.util.io;

import org.exoplatform.container.ExoContainerContext;
import org.picocontainer.Startable;


/**
 * Created by The eXo Platform SAS. <br> per workspace container file cleaner holder object
 * 
 * @author Gennady Azarenkov
 * @version $Id: WorkspaceFileCleanerHolder.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class FileCleanerHolder implements Startable
{

   //   private static final FileCleaner defaultFileCleaner = new Fi;

   private final FileCleaner fileCleaner;

   private static final FileCleaner defaultFileCleaner = new FileCleaner(null);

   public FileCleanerHolder()
   {
      this(null);
   }

   public FileCleanerHolder(ExoContainerContext ctx)
   {
      this.fileCleaner = new FileCleaner(ctx);
   }

   public FileCleaner getFileCleaner()
   {
      return fileCleaner;
   }

   public static FileCleaner getDefaultFileCleaner()
   {
      return defaultFileCleaner;
   }

   /**
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
   }

   /**
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
      fileCleaner.halt();
   }

}

