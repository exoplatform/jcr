/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.infinispan;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This class is needed to be able to implement our own {@link JdbcStringBasedCacheStore} but also
 * to enforce a {@link TableManipulation} that uses our own way to check if a table exists.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class JdbcStringBasedCacheStoreConfig extends
   org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStoreConfig
{

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = -581911437872977991L;

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JdbcStringBasedCacheStoreConfig");

   public JdbcStringBasedCacheStoreConfig()
   {
      this.cacheLoaderClassName = JdbcStringBasedCacheStore.class.getName();
      this.tableManipulation = new TableManipulation();
      super.setConnectionFactoryClass(ManagedConnectionFactory.class.getName());
   }

   /**
    * @see org.infinispan.loaders.jdbc.AbstractJdbcCacheStoreConfig#setConnectionFactoryClass(java.lang.String)
    */
   @Override
   public void setConnectionFactoryClass(String connectionFactoryClass)
   {
      if (!ManagedConnectionFactory.class.getName().equals(connectionFactoryClass))
      {
         LOG.debug("The class " + connectionFactoryClass + " is not allowed for this Cache Store only "
            + ManagedConnectionFactory.class.getName() + " is supported");
         return;
      }
      super.setConnectionFactoryClass(connectionFactoryClass);
   }
}
