/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.infinispan.persistence.spi.InitializationContext;

/**
 * Implements Cache Store that designed to be used when each cluster node has it's own local index
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: LocalIndexerCacheStore.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class LocalIndexCacheStore extends AbstractIndexerCacheStore
{

   protected volatile IndexerIoModeHandler modeHandler;

   public LocalIndexCacheStore()
   {
      super();
      this.modeHandler = new IndexerIoModeHandler(IndexerIoMode.READ_WRITE); // initialize mode handler
   }

   @Override
   public IndexerIoModeHandler getModeHandler()
   {
      return modeHandler;
   }

   @Override
   public void init(InitializationContext ctx)
   {
      super.init(ctx);
   }
}
