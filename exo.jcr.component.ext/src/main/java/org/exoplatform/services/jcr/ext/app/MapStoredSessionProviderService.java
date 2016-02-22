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
package org.exoplatform.services.jcr.ext.app;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

import java.util.HashMap;

/**
 * Created by The eXo Platform SAS .<br>
 * SessionProviderService implementation where SessionProviders are stored as key-value pairs. where
 * key is any object, for instance user's credentials
 * 
 * @author Gennady Azarenkov
 * @version $Id: MapStoredSessionProviderService.java 13869 2008-05-05 08:40:10Z pnedonosko $
 */

public class MapStoredSessionProviderService implements SessionProviderService
{

   private HashMap<Object, SessionProvider> providers;

   private HashMap<Object, SessionProvider> systemProviders;

   public MapStoredSessionProviderService()
   {
      providers = new HashMap<Object, SessionProvider>();
      systemProviders = new HashMap<Object, SessionProvider>();
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.ext.app.SessionProviderService#getSessionProvider(java.lang.Object
    * )
    */
   public SessionProvider getSessionProvider(Object key)
   {
      if (providers.containsKey(key))
      {
         return providers.get(key);
      }
      else
      {
         throw new IllegalArgumentException("SessionProvider is not initialized");
      }
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.ext.app.SessionProviderService#getSystemSessionProvider(java.lang
    * .Object)
    */
   public SessionProvider getSystemSessionProvider(Object key)
   {
      if (systemProviders.containsKey(key))
      {
         return systemProviders.get(key);
      }
      else
      {
         final SessionProvider ssp = SessionProvider.createSystemProvider();
         systemProviders.put(key, ssp);
         return ssp;
      }
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.ext.app.SessionProviderService#setSessionProvider(java.lang.Object
    * , org.exoplatform.services.jcr.ext.common.SessionProvider)
    */
   public void setSessionProvider(Object key, SessionProvider sessionProvider)
   {
      providers.put(key, sessionProvider);
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.ext.app.SessionProviderService#removeSessionProvider(java.lang
    * .Object)
    */
   public void removeSessionProvider(Object key)
   {
      if (providers.containsKey(key))
      {
         getSessionProvider(key).close();
         providers.remove(key);
      }

      if (systemProviders.containsKey(key))
      {
         systemProviders.get(key).close();
         systemProviders.remove(key);
      }
   }

}
