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
package org.exoplatform.services.jcr.webdav.xml;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SARL Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class WebDavNamespaceContext implements NamespaceContext
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.WebDavNamespaceContext");

   /**
    * Namespace prefixes.
    */
   private HashMap<String, String> prefixes = new HashMap<String, String>();

   /**
    * NAmespaces.
    */
   private HashMap<String, String> namespaces = new HashMap<String, String>();

   /**
    * Namespace registry.
    */
   private final NamespaceRegistry namespaceRegistry;

   /**
    * WebDav namespace context.
    * 
    * @param session current session.
    * @throws RepositoryException {@link RepositoryException}
    */
   public WebDavNamespaceContext(Session session) throws RepositoryException
   {
      this.namespaceRegistry = session.getWorkspace().getNamespaceRegistry();

      prefixes.put("DAV:", "D");
      namespaces.put("D", "DAV:");
   }

   /**
    * Converts String into QName.
    * 
    * @param strName string name
    * @return new QName
    */
   public QName createQName(String strName)
   {
      String[] parts = strName.split(":");
      if (parts.length > 1)
         return new QName(getNamespaceURI(parts[0]), parts[1], parts[0]);
      else
         return new QName(parts[0]);
   }

   /**
    * Converts QName into the String.
    * 
    * @param qName QName
    * @return string name
    */
   public static String createName(QName qName)
   {
      return qName.getPrefix() + ":" + qName.getLocalPart();
   }

   /**
    * Returns namespace URI.
    * @see javax.xml.namespace.NamespaceContext#getNamespaceURI(java.lang.String).
    * @param prefix namespace prefix
    * @return namespace URI
    */
   public String getNamespaceURI(String prefix)
   {
      String uri = null;
      try
      {
         uri = namespaceRegistry.getURI(prefix);
      }
      catch (NamespaceException exc)
      {
         uri = namespaces.get(prefix);
      }
      catch (RepositoryException exc)
      {
         log.error(exc.getMessage(), exc);
      }
      return uri;
   }

   /**
    * Returns namespace prefix.
    * @see javax.xml.namespace.NamespaceContext#getPrefix(java.lang.String).
    * @param namespaceURI namespace URI 
    * @return namespace prefix
    */
   public String getPrefix(String namespaceURI)
   {
      String prefix = null;
      try
      {
         prefix = namespaceRegistry.getPrefix(namespaceURI);
      }
      catch (NamespaceException exc)
      {
         prefix = prefixes.get(namespaceURI);
      }
      catch (RepositoryException exc)
      {
         log.error(exc.getMessage(), exc);
      }
      return prefix;

   }

   /**
    * Returns the list of registered for this URI namespace prefixes.
    * @see javax.xml.namespace.NamespaceContext#getPrefixes(java.lang.String).
    * @param namespaceURI namespace URI
    * @return list of registered for prefixes
    */
   public Iterator<String> getPrefixes(String namespaceURI)
   {
      List<String> list = new ArrayList<String>();
      list.add(getPrefix(namespaceURI));
      return list.iterator();
   }

}
