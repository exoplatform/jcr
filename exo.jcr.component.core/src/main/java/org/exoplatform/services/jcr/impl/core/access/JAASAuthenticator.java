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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.access;

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.security.jaas.BasicCallbackHandler;

import java.util.HashSet;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

/**
 * Created by The eXo Platform SAS .<br>
 * 
 * Implements JAAS based authentication using LoginContext configured
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class JAASAuthenticator extends BaseAuthenticator
{

   public JAASAuthenticator(RepositoryEntry config, IdentityRegistry identityRegistry)
   {
      super(config, identityRegistry);
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.access.AuthenticationPolicy#authenticate(javax.jcr.Credentials)
    */
   public final ConversationState authenticate(Credentials credentials) throws LoginException
   {

      CredentialsImpl thisCredentials;
      if (credentials instanceof CredentialsImpl)
      {
         thisCredentials = (CredentialsImpl)credentials;
      }
      else if (credentials instanceof SimpleCredentials)
      {
         String name = ((SimpleCredentials)credentials).getUserID();
         char[] pswd = ((SimpleCredentials)credentials).getPassword();
         thisCredentials = new CredentialsImpl(name, pswd);
      }
      else
         throw new LoginException(
            "Credentials for the authentication should be CredentialsImpl or SimpleCredentials type");

      // SYSTEM
      if (thisCredentials.getUserID().equals(IdentityConstants.SYSTEM))
      {
         Identity sid = new Identity(IdentityConstants.SYSTEM, new HashSet<MembershipEntry>());
         return new ConversationState(sid);
      }

      // prepare to new login
      // uses BasicCallbackHandler
      CallbackHandler handler = new BasicCallbackHandler(thisCredentials.getUserID(), thisCredentials.getPassword());

      // and try to login
      try
      {

         LoginContext loginContext = new LoginContext(config.getSecurityDomain(), handler);
         loginContext.login();

      }
      catch (javax.security.auth.login.LoginException e)
      {
         throw new LoginException("Login failed for " + thisCredentials.getUserID(), e);
      }

      if (log.isDebugEnabled())
         log.debug("Logged " + thisCredentials.getUserID());

      // supposed to be set
      Identity identity = identityRegistry.getIdentity(thisCredentials.getUserID());
      if (identity == null)
      {
         throw new LoginException("Identity not found, check Loginmodule, userId " + thisCredentials.getUserID());
      }
      ConversationState state = new ConversationState(identity);
      String[] aNames = thisCredentials.getAttributeNames();
      for (String name : aNames)
      {
         state.setAttribute(name, thisCredentials.getAttribute(name));
      }

      ConversationState.setCurrent(state);
      return state;

   }
}
