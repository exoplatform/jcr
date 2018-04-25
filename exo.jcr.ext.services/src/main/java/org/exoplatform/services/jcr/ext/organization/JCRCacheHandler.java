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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.organization.CacheHandler;

import java.io.Serializable;

import javax.jcr.RepositoryException;

/**
 * Cache handler for JCR implementation of organization service. Contains method for
 * hierarchically removing groups and related membership records. To remove node and its 
 * children in JCR need to remove only parent node, but cache has not hierarchy structrue.
 * We need dedicated method for this. 
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: JCRCacheHandler.java 79575 2012-02-17 13:23:37Z aplotnikov $
 */
public class JCRCacheHandler extends CacheHandler
{
   private static char DELIMITER = ':';

   private final JCROrganizationServiceImpl jcrOrganizationServiceImpl;

   protected boolean enabled;

   /**
    * JCRCacheHandler constructor.
    */
   public JCRCacheHandler(CacheService cservice, JCROrganizationServiceImpl jcrOrganizationServiceImpl, boolean enabled)
   {
      super(cservice);
      this.jcrOrganizationServiceImpl = jcrOrganizationServiceImpl;
      this.enabled = enabled;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Serializable createCacheKey(Serializable orgServiceKey)
   {
      // Safe check
      if (orgServiceKey instanceof String)
      {
         try
         {
            // add "repository:" to OrgSerivce Key
            return jcrOrganizationServiceImpl.getWorkingRepository().getConfiguration().getName() + DELIMITER
               + orgServiceKey;
         }
         catch (RepositoryException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
         catch (RepositoryConfigurationException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
      }
      else
      {
         return orgServiceKey;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean matchKey(Serializable cacheKey)
   {
      if (cacheKey instanceof String)
      {
         try
         {
            // check is prefix equals to "repository:"
            String prefix = jcrOrganizationServiceImpl.getWorkingRepository().getConfiguration().getName() + DELIMITER;
            return ((String)cacheKey).startsWith(prefix);
         }
         catch (RepositoryException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
         catch (RepositoryConfigurationException e)
         {
            throw new IllegalStateException(e.getMessage(), e);
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected Serializable createOrgServiceKey(Serializable cacheKey)
   {
      if (cacheKey instanceof String)
      {
         // trim "repository:" from Cache Key
         int indexOfDelimiter = ((String)cacheKey).indexOf(DELIMITER);
         if (indexOfDelimiter >= 0)
         {
            return ((String)cacheKey).substring(indexOfDelimiter + 1);
         }
      }
      return cacheKey;
   }

   /**
    * {@inheritDoc}
    */
   public void put(Serializable key, Object value, CacheType cacheType)
   {
      if (enabled)
      {
         super.put(key, value, cacheType);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Object get(Serializable key, CacheType cacheType)
   {
      if (enabled)
      {
         return super.get(key, cacheType);
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void remove(Serializable key, CacheType cacheType)
   {
      if (enabled)
      {
         super.remove(key, cacheType);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void move(Serializable oldKey, Serializable newKey, CacheType cacheType)
   {
      if (enabled)
      {
         super.move(oldKey, newKey, cacheType);
      }
   }
}
