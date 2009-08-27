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
public class JcrNamespaceContext
   implements NamespaceContext
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
