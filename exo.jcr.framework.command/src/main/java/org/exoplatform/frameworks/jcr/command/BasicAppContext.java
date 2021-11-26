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

package org.exoplatform.frameworks.jcr.command;

import org.apache.commons.chain.impl.ContextBase;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.NamingException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: BasicAppContext.java 10160 2006-11-08 09:14:24Z geaz $
 */

public class BasicAppContext extends ContextBase implements JCRAppContext
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.framework.command.BasicAppContext");

   protected final SessionProvider sessionProvider;

   protected final ManageableRepository repository;

   protected String currentWorkspace;

   public BasicAppContext(ManageableRepository rep) throws NamingException
   {
      this.sessionProvider = new SessionProvider(ConversationState.getCurrent());
      this.repository = rep;
      this.currentWorkspace = rep.getConfiguration().getDefaultWorkspaceName();
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.frameworks.jcr.command.JCRAppContext#getSession()
    */
   public Session getSession() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      return sessionProvider.getSession(currentWorkspace, repository);
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.frameworks.jcr.command.JCRAppContext#setCurrentWorkspace(java.lang.String)
    */
   public void setCurrentWorkspace(String workspaceName)
   {
      this.currentWorkspace = workspaceName;
   }

}
