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
package org.exoplatform.services.jcr.access;

import org.exoplatform.services.security.ConversationState;

import javax.jcr.Credentials;
import javax.jcr.LoginException;

/**
 * Created by The eXo Platform SAS.<br/> Authentication policy for
 * 
 * 
 * @author <a href="mailto:lautarul@gmail.com">Roman Pedchenko</a>
 * @version $Id: AuthenticationPolicy.java 14100 2008-05-12 10:53:47Z gazarenkov $
 * @LevelAPI Platform
 */

public interface AuthenticationPolicy
{

   /**
    * Authenticates getting credentials.
    * 
    * @param credentials
    *          credentials
    * @return credentials took part in authentication (could be not the same a incoming one)
    * @throws LoginException
    */
   ConversationState authenticate(Credentials credentials) throws LoginException;

   /**
    * Authenticates using some external mechanism.
    * 
    * @return credentials took part in authentication
    * @throws LoginException
    */
   ConversationState authenticate() throws LoginException;

}
