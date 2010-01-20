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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jbosscache;

import org.jboss.cache.Fqn;
import org.jboss.cache.Modification;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.AbstractCacheLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 06.11.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: AbstractWriteOnlyCacheLoader.java 480 2009-11-06 10:17:07Z pnedonosko $
 */
public abstract class AbstractWriteOnlyCacheLoader
   extends AbstractCacheLoader
{
   private IndividualCacheLoaderConfig config;

   /**
    * {@inheritDoc}
    */
   public boolean exists(Fqn arg0) throws Exception
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public Map<Object, Object> get(Fqn arg0) throws Exception
   {
      return Collections.emptyMap();
   }

   /**
    * {@inheritDoc}
    */
   public Set<Object> getChildrenNames(Fqn arg0) throws Exception
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public IndividualCacheLoaderConfig getConfig()
   {
      return config;
   }

   /**
    * {@inheritDoc}
    */
   public void prepare(Object tx, List<Modification> modifications, boolean onePhase) throws Exception
   {
      throw new WriteOnlyCacheLoaderException("The method 'prepare(Object tx, List<Modification> modifications, boolean onePhase)' should not be called.");
   }

   /**
    * {@inheritDoc}
    */
   public Object remove(Fqn arg0, Object arg1) throws Exception
   {
      throw new WriteOnlyCacheLoaderException("The method 'remove(Fqn arg0, Object arg1)' should not be called.");
   }

   /**
    * {@inheritDoc}
    */
   public void removeData(Fqn arg0) throws Exception
   {
      throw new WriteOnlyCacheLoaderException("The method 'removeData(Fqn arg0)' should not be called.");
   }

   /**
    * {@inheritDoc}
    */
   public void setConfig(IndividualCacheLoaderConfig config)
   {
     this.config = config;
   }
   
   /**
    * {@inheritDoc}
    */
   public abstract void put(List<Modification> modifications) throws Exception ;

}
