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
package org.exoplatform.frameworks.jcr.web;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.StateKey;
import org.exoplatform.services.security.web.HttpSessionStateKey;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by The eXo Platform SAS . <br/> Checks out if there are SessionProvider instance in
 * current thread using ThreadLocalSessionProviderService, if no, initializes it getting current
 * credentials from AuthenticationService and initializing ThreadLocalSessionProviderService with
 * newly created SessionProvider
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */
public class ThreadLocalSessionProviderInitializedFilter extends AbstractFilter
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.ThreadLocalSessionProviderInitializedFilter");

   /*
    * (non-Javadoc)
    * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
    * javax.servlet.FilterChain)
    */
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException
   {
      ExoContainer container = getContainer();

      SessionProviderService providerService = (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);
      ConversationRegistry stateRegistry = (ConversationRegistry)container.getComponentInstanceOfType(ConversationRegistry.class);

      HttpServletRequest httpRequest = (HttpServletRequest)request;

      ConversationState state = ConversationState.getCurrent();
      SessionProvider provider = null;

      // NOTE not create new HTTP session, if session is not created yet
      // this means some settings is incorrect, see web.xml for filter
      // org.exoplatform.services.security.web.SetCurrentIdentityFilter
      HttpSession httpSession = httpRequest.getSession(false);
      if (state == null)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Current conversation state is not set");
         }

         if (httpSession != null)
         {
            StateKey stateKey = new HttpSessionStateKey(httpSession);
            // initialize thread local SessionProvider
            state = stateRegistry.getState(stateKey);
            if (state != null)
            {
               provider = new SessionProvider(state);
            }
            else if (LOG.isDebugEnabled())
            {
               LOG.debug("WARN: Conversation State is null, id  " + httpSession.getId());
            }
         }
      }
      else
      {
         provider = new SessionProvider(state);
      }

      if (provider == null)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Create SessionProvider for anonymous.");
         }
         provider = SessionProvider.createAnonimProvider();
      }
      try
      {
         if (ConversationState.getCurrent() != null)
         {
            ConversationState.getCurrent().setAttribute(SessionProvider.SESSION_PROVIDER, provider);
         }

         providerService.setSessionProvider(null, provider);

         chain.doFilter(request, response);

      }
      finally
      {
         if (ConversationState.getCurrent() != null)
         {
            try
            {
               ConversationState.getCurrent().removeAttribute(SessionProvider.SESSION_PROVIDER);
            }
            catch (Exception e)
            {
               LOG.warn("An error occured while removing the session provider from the conversation state", e);
            }
         }
         if (providerService.getSessionProvider(null) != null)
         {
            try
            {
               // remove SessionProvider
               providerService.removeSessionProvider(null);
            }
            catch (Exception e)
            {
               LOG.warn("An error occured while cleaning the ThreadLocal", e);
            }
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see javax.servlet.Filter#destroy()
    */
   public void destroy()
   {
   }

}
