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

/**
 * Created by The eXo Platform SAS .<br/>
 * SessionProviderService implementation where SessionProviders are stored in Thread Local. In this
 * implementation the KEY make no sense, null value can be passed as a key.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ThreadLocalSessionProviderService.java 13869 2008-05-05 08:40:10Z pnedonosko $
 */

public class ThreadLocalSessionProviderService implements SessionProviderService
{

   private static final ThreadLocal<SessionProvider> sessionProviderKeeper = new ThreadLocal<SessionProvider>();
   private static final ThreadLocal<SessionProvider> systemSessionProviderKeeper = new ThreadLocal<SessionProvider>();

   /**
    * {@inheritDoc}
    */
   public SessionProvider getSessionProvider(Object key)
   {
      return sessionProviderKeeper.get();
   }

   /**
    * {@inheritDoc}
    */
   public SessionProvider getSystemSessionProvider(Object key)
   {
      if (systemSessionProviderKeeper.get() != null)
      {
         return systemSessionProviderKeeper.get();
      }
      else
      {
         final SessionProvider ssp = SessionProvider.createSystemProvider();
         systemSessionProviderKeeper.set(ssp);
         return ssp;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setSessionProvider(Object key, SessionProvider sessionProvider)
   {
      sessionProviderKeeper.set(sessionProvider);
   }

   /**
    * {@inheritDoc}
    */
   public void removeSessionProvider(Object key)
   {
      if (sessionProviderKeeper.get() != null)
      {
         sessionProviderKeeper.get().close();
         sessionProviderKeeper.set(null);
      }

      if (systemSessionProviderKeeper.get() != null)
      {
         systemSessionProviderKeeper.get().close();
         systemSessionProviderKeeper.set(null);
      }
   }

}
