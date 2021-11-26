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

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: DefaultAccessManagerImpl.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */

public class DefaultAccessManagerImpl extends AccessManager
{

   public DefaultAccessManagerImpl(RepositoryEntry config, WorkspaceEntry wsConfig) throws RepositoryException
   {
      super(config, wsConfig);
   }

   public DefaultAccessManagerImpl(RepositoryEntry config) throws RepositoryException
   {
      super(config, null);
   }

}
