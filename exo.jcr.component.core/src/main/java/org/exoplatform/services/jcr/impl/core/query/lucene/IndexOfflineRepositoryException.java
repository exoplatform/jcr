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

package org.exoplatform.services.jcr.impl.core.query.lucene;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: IndexOfflineRepositoryException.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class IndexOfflineRepositoryException extends RepositoryException
{

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = 6605682968923297623L;

   /**
    * Constructs a new instance of this class with the specified detail
    * message and root cause.
    *
    * @param message   the detail message. The detail message is saved for
    *                  later retrieval by the {@link #getMessage()} method.
    * @param rootCause root failure cause
    */
   public IndexOfflineRepositoryException(String message, Throwable rootCause)
   {
      super(message);
      this.rootCause = rootCause;
   }

}
