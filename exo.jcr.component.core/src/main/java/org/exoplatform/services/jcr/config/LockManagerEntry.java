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
package org.exoplatform.services.jcr.config;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: LockManagerEntry.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class LockManagerEntry extends MappedParametrizedObjectEntry
{

   private long timeout = -1;

   private LockPersisterEntry persister;

   private String cacheConfig;

   public long getTimeout()
   {
      return timeout;
   }

   public void setTimeout(long timeout)
   {
      this.timeout = timeout;
   }

   public LockPersisterEntry getPersister()
   {
      return persister;
   }

   public void setPersister(LockPersisterEntry persister)
   {
      this.persister = persister;
   }

   public String getCacheConfig()
   {
      return cacheConfig;
   }

   public void steCacheConfig(String cacheConfig)
   {
      this.cacheConfig = cacheConfig;
   }

}
