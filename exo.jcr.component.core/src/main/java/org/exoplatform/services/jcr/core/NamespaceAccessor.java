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

package org.exoplatform.services.jcr.core;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br>
 * 
 * Interface for namespaces holder objects: Session and NamespaceRegistry.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: NamespaceAccessor.java 11907 2008-03-13 15:36:21Z ksm $
 */
public interface NamespaceAccessor
{

   /**
    * @param prefix
    * @return URI by mapped prefix
    * @throws NamespaceException
    * @throws RepositoryException
    */
   String getNamespaceURIByPrefix(String prefix) throws NamespaceException, RepositoryException;

   /**
    * @param uri
    * @return prefix by mapped URI
    * @throws NamespaceException
    * @throws RepositoryException
    */
   String getNamespacePrefixByURI(String uri) throws NamespaceException, RepositoryException;

   /**
    * @return all prefixes registered
    * @throws RepositoryException
    */
   String[] getAllNamespacePrefixes() throws RepositoryException;
}
