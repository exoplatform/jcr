/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.api.writing;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchul</a>
 * @version $Id: TestSameNameSiblingsSessionMove 34360 2011-07-20 18:58:59Z nzamosenchuk $
 *
 */
public class TestSameNameSiblingsWorkspaceMove extends AbstractSameNameSiblingsMoveTest
{

   /**
   * {@inheritDoc}
   */
   @Override
   void move(Node testRoot, String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException, RepositoryException
   {
      testRoot.getSession().getWorkspace().move(srcAbsPath, destAbsPath);
   }

}
