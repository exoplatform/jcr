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

import org.apache.commons.chain.Context;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JCRAppContext.java 5800 2006-05-28 18:03:31Z geaz $
 */

public interface JCRAppContext extends Context
{

   /**
    * Change current workspace name.
    * 
    * @param workspaceName
    */
   void setCurrentWorkspace(String workspaceName);

   /**
    * Return Session of the context.
    * 
    * @return the session
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
    */
   Session getSession() throws LoginException, NoSuchWorkspaceException, RepositoryException;
}
