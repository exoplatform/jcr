/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.query.jbosscache;

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;

/**
 * This cache loader replaces Indexer IO Mode handling with constant ReadWrite state.
 * This is required for indexing in cluster, when each instance has it's own index stack,
 * having local FileSystem with LuceneDirectories.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: LocalIndexCacheLoader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class LocalIndexCacheLoader extends IndexerCacheLoader
{
   public LocalIndexCacheLoader()
   {
      super();
      modeHandler = new IndexerIoModeHandler(IndexerIoMode.READ_WRITE); // initialize mode handler
   }

   @Override
   IndexerIoModeHandler getModeHandler()
   {
      return modeHandler;
   }

   @Override
   void setMode(IndexerIoMode ioMode)
   {
      // can't set RO on this cache loader
      if (ioMode == IndexerIoMode.READ_ONLY)
      {
         throw new UnsupportedOperationException(
            "Can't set ReadOnly on this type of CacheLoader. It is designed to provide local index for each cluster instance. Make sure you are using Index properly.");
      }
   }

}
