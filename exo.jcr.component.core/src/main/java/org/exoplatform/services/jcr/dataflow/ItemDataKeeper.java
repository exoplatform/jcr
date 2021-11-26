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

package org.exoplatform.services.jcr.dataflow;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ItemDataKeeper.java 11907 2008-03-13 15:36:21Z ksm $
 */

public interface ItemDataKeeper
{
   /**
    * Saves the list of changes from this storage
    * 
    * @param changes
    *          for commit
    * @throws InvalidItemStateException
    * @throws UnsupportedOperationException
    *           if operation is not supported (it is container for level 1)
    * @throws RepositoryException
    *           if some exception occured
    */
   void save(ItemStateChangesLog changes) throws InvalidItemStateException, UnsupportedOperationException,
      RepositoryException;

}
