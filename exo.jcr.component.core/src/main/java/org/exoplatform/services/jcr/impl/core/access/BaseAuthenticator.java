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
package org.exoplatform.services.jcr.impl.core.access;

import org.exoplatform.services.jcr.access.AuthenticationPolicy;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import java.util.HashSet;

import javax.jcr.LoginException;

/**
 * Created by The eXo Platform SAS.<br/> Abstract implementation of AuthenticationPolicy interface
 * 
 * @author eXo Platform
 * @version $Id: BaseAuthenticator.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */
abstract public class BaseAuthenticator implements AuthenticationPolicy
{

   protected static Log log = ExoLogger.getLogger("jcr.BaseAuthenticator");

   protected RepositoryEntry config;

   protected IdentityRegistry identityRegistry;

   public BaseAuthenticator(RepositoryEntry config, IdentityRegistry identityRegistry)
   {
      this.config = config;
      this.identityRegistry = identityRegistry;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.access.AuthenticationPolicy#authenticate()
    */
   public ConversationState authenticate() throws LoginException
   {

      ConversationState state = ConversationState.getCurrent();

      if (state == null)
      {
         log.debug("No current identity found, ANONYMOUS one will be used");
         return new ConversationState(new Identity(SystemIdentity.ANONIM, new HashSet<MembershipEntry>()));
      }

      ConversationState.setCurrent(state);
      return state;

   }

}
