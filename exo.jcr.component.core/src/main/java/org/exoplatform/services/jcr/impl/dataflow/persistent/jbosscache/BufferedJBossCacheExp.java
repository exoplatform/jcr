/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import java.io.Serializable;
import java.util.Map;

import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class BufferedJBossCacheExp extends BufferedJBossCache
{
   /**
    * The expiration timeout.  
    */
   public final static long DEFAULT_EXPIRATION_TIMEOUT = 900000; // 15 minutes. 
   
   public final long expirationTimeOut;

   public BufferedJBossCacheExp(Cache<Serializable, Object> parentCache, long expirationTimeOut)
   {
      super(parentCache);
      this.expirationTimeOut = expirationTimeOut;
   }
   
   public BufferedJBossCacheExp(Cache<Serializable, Object> parentCache)
   {
      super(parentCache);
      this.expirationTimeOut = DEFAULT_EXPIRATION_TIMEOUT;
   }
   
   protected static void putMap(Fqn fqn, Map<? extends Serializable, ? extends Object> data, Cache<Serializable, Object> cache, boolean localMode) 
   {
      putExpiration(fqn, cache, localMode);
      
      cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(localMode);
      cache.put(fqn, data);
      
      cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(localMode);
      cache.put(fqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + DEFAULT_EXPIRATION_TIMEOUT));
   }
   
   protected static void putObject(Fqn fqn, Serializable key, Object value, Cache<Serializable, Object> cache, boolean localMode) 
   {
      cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(localMode);
      cache.put(fqn, key, value);
   }
   
   public static void putExpiration(Fqn efqn, Cache<Serializable, Object> cache, boolean localMode)
   {
      for (int i = 2; i <= efqn.size(); i++)
      {
         Fqn pfqn = efqn.getSubFqn(0, i);
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(localMode);
         cache.put(pfqn, ExpirationAlgorithmConfig.EXPIRATION_KEY, new Long(System.currentTimeMillis() + DEFAULT_EXPIRATION_TIMEOUT));
      }
   }
   
   

}
