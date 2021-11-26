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

package org.exoplatform.services.jcr.api.exporting;

import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.NamespaceContext;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: JcrNamespaceContext.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class JcrNamespaceContext implements NamespaceContext
{
   private final Session session;

   public JcrNamespaceContext(Session session)
   {
      super();
      this.session = session;
   }

   public String getNamespaceURI(String prefix)
   {

      try
      {
         return session.getNamespaceURI(prefix);
      }
      catch (NamespaceException e)
      {
      }
      catch (RepositoryException e)
      {
      }
      return null;
   }

   public String getPrefix(String namespaceURI)
   {
      try
      {
         return session.getNamespacePrefix(namespaceURI);
      }
      catch (NamespaceException e)
      {
      }
      catch (RepositoryException e)
      {
      }
      return null;
   }

   public Iterator getPrefixes(String namespaceURI)
   {
      return Arrays.asList(getPrefix(namespaceURI)).iterator();
   }

}
