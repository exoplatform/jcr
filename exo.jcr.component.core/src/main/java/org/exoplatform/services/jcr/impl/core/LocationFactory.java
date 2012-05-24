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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.xml.XMLChar;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.lang.ref.WeakReference;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br>
 * Helper for creating namespace mapping dependent entities like JCR path, name,
 * uuid
 * 
 * @author Gennady Azarenkov
 * @version $Id: LocationFactory.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class LocationFactory
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.LocationFactory");

   private final NamespaceAccessor namespaces;

   private final WeakReference<? extends NamespaceAccessor> weakAccessor;

   public LocationFactory(NamespaceAccessor namespaces)
   {
      this.namespaces = namespaces;
      this.weakAccessor = null;
   }

   public LocationFactory(WeakReference<? extends NamespaceAccessor> weakAccessor)
   {
      this.namespaces = null;
      this.weakAccessor = weakAccessor;
   }

   private NamespaceAccessor getNamespaceAccessor()
   {
      if (namespaces != null)
      {
         return namespaces;
      }
      NamespaceAccessor na = weakAccessor.get();
      if (na == null)
      {
         throw new IllegalStateException("The namespace accessor cannot be found probably because" +
                  " the corresponding eXo container has been removed");
      }
      return na;
   }
   
   public JCRPath createRootLocation() throws RepositoryException
   {
      return parseNames(JCRPath.ROOT_PATH, true);
   }

   /**
    * Creates JCRPath from parent path and relPath
    * 
    * @param parentLoc parent path
    * @param relPath related path
    * @param setIndexIfNotDefined if necessary to set index = 1 if not defined
    *          (usable for node's path only)
    * @return
    * @throws RepositoryException
    */
   public JCRPath createJCRPath(JCRPath parentLoc, String relPath) throws RepositoryException
   {
      JCRPath addPath = parseNames(relPath, false);
      return parentLoc.add(addPath);
   }

   /**
    * Parses absolute JCR path from string (JCR format /ns:name[index]/etc)
    * 
    * @param absPath
    * @return
    * @throws RepositoryException
    */
   public JCRPath parseAbsPath(String absPath) throws RepositoryException
   {
      return parseNames(absPath, true);
   }

   public JCRPath parseRelPath(String relPath) throws RepositoryException
   {
      return parseNames(relPath, false);
   }

   /**
    * creates abs(if convertable to abs path) or rel(otherwice) JCRPath
    * 
    * @param path
    * @return JCRPath
    * @throws RepositoryException
    */
   public JCRPath parseJCRPath(String path) throws RepositoryException
   {
      if (isAbsPathParseable(path))
      {
         return parseAbsPath(path);
      }
      else
      {
         return parseRelPath(path);
      }
   }

   /**
    * Creates JCRPath by internalQPath
    * 
    * @param qPath
    * @return
    * @throws RepositoryException
    */
   public JCRPath createJCRPath(QPath qPath) throws RepositoryException
   {
      return JCRPath.createJCRPath(getNamespaceAccessor(), qPath);
   }

   public JCRName createJCRName(InternalQName qname) throws RepositoryException
   {
      String prefix = getNamespaceAccessor().getNamespacePrefixByURI(qname.getNamespace());
      return new JCRName(qname, prefix);
   }

   public String formatPathElement(QPathEntry qe) throws RepositoryException
   {
      String prefix = getNamespaceAccessor().getNamespacePrefixByURI(qe.getNamespace());
      JCRPath p = JCRPath.createJCRPath();
      p = p.addEntry(qe.getNamespace(), qe.getName(), prefix, qe.getIndex());
      return p.getEntry(0).getAsString(false);
   }

   /**
    * Parses absolute JCR name from string (JCR format ns:name[index])
    * 
    * @param name
    * @return
    * @throws RepositoryException
    */
   public JCRName parseJCRName(String name) throws RepositoryException
   {
      JCRPath path = parsePathEntry(JCRPath.createJCRPath(), name);
      JCRPath.PathElement entry = path.getName();
      return new JCRName(entry);
   }

   public JCRPath.PathElement[] createRelPath(QPathEntry[] relPath) throws RepositoryException
   {
      return JCRPath.createJCRPath(getNamespaceAccessor(), relPath).getEntries();
   }

   private JCRPath parsePathEntry(JCRPath path, String name) throws RepositoryException
   {

      // should be reset here (if there is explicit index) or
      // in JCRPath.Entry() (with index == 1)
      int index = -1;

      if (name == null)
      {
         throw new RepositoryException("Name can not be null");
      }

      int delim = name.indexOf(":");
      int endOfName = name.length();
      int indexStart = name.indexOf("[");
      if (indexStart > 0)
      {
         int indexEnd = name.indexOf("]");
         if ((indexEnd <= indexStart + 1) || (indexEnd != name.length() - 1))
         {
            throw new RepositoryException("Invalid path entry: \"" + name + "\"");
         }
         index = Integer.parseInt(name.substring(indexStart + 1, indexEnd));
         if (index <= 0)
         {
            throw new RepositoryException("Invalid path entry: \"" + name + "\"");
         }
         endOfName = indexStart;
      }

      try
      {
         String prefix;
         if (delim <= 0)
         {
            prefix = "";
         }
         else
         {
            // prefix validation
            prefix = name.substring(0, delim);
            if (!XMLChar.isValidName(prefix))
            {
               throw new RepositoryException("Illegal path entry: \"" + name + "\"");
            }
         }

         // name validation
         String someName = name.substring(delim + 1, endOfName);
         if (!isValidName(someName, !prefix.equals("")))
         {
            throw new RepositoryException("Illegal path entry: \"" + name + "\"");
         }

         path = path.addEntry(getNamespaceAccessor().getNamespaceURIByPrefix(prefix), someName, prefix, index);
         return path;

      }
      catch (Exception e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   private JCRPath parseNames(String path, boolean absolute) throws RepositoryException
   {

      if ((path == null) || (path.equals("")))
      {
         throw new RepositoryException("Illegal relPath: \"" + path + "\"");
      }

      JCRPath jcrPath = JCRPath.createJCRPath();
      int start = 0;
      if (!absolute)
      {
         start = -1;
      }
      if (isAbsPathParseable(path))
      {
         if (!absolute)
         {
            throw new RepositoryException("Illegal relPath: \"" + path + "\"");
         }
         jcrPath = jcrPath.addEntry(getNamespaceAccessor().getNamespaceURIByPrefix(""), "", "", -1);
      }
      else
      {
         if (absolute)
         {
            throw new RepositoryException("Illegal absPath: \"" + path + "\"");
         }
      }

      int end = 0;
      while (end >= 0)
      {
         end = path.indexOf('/', start + 1);
         String qname = path.substring(start + 1, end == -1 ? path.length() : end);

         if (start + 1 != path.length())
         {
            jcrPath = parsePathEntry(jcrPath, qname);
         }
         else
         {
            // jcrPath.addEntry(getNamespaceAccessor().getNamespaceURIByPrefix(""), "", "", -1);
            return jcrPath;
         }

         start = end;
      }

      return jcrPath;
   }

   private static boolean isAbsPathParseable(String str)
   {
      return str.startsWith("/");
   }

   // Some functions for JCRPath Validation
   private boolean isNonspace(String str, char ch)
   {
      if (ch == '|')
      {
         log.info("Path entry: \"" + str + "\" contains illegal char: \"" + ch + "\"");
      }

      return !((ch == '\t') || (ch == '\n') || (ch == '\f') || (ch == '\r') || (ch == ' ') || (ch == '/')
         || (ch == ':') || (ch == '[') || (ch == ']') || (ch == '\'') || (ch == '\"') || (ch == '*'));
   }

   private boolean isSimpleString(String str)
   {
      char ch;

      for (int i = 0; i < str.length(); i++)
      {
         ch = str.charAt(i);
         if (!isNonspace(str, ch) && (ch != ' '))
         {
            return false;
         }
      }

      return true;
   }

   private boolean isLocalName(String str)
   {
      int strLen = str.length();

      switch (strLen)
      {
         case 0 :
            return false;
         case 1 :
            char ch = str.charAt(0);
            return (isNonspace(str, ch) && (ch != '.'));
         case 2 :
            char ch0 = str.charAt(0);
            char ch1 = str.charAt(1);
            return (((ch0 == '.') && (isNonspace(str, ch1) && (ch1 != '.')))
                     || ((isNonspace(str, ch0) && (ch0 != '.')) && (ch1 == '.')) || ((isNonspace(str, ch0) && (ch0 != '.')) 
                              && (isNonspace(str, ch1) && (ch1 != '.'))));
         default :
            return isNonspace(str, str.charAt(0)) && isSimpleString(str.substring(1, strLen - 1))
               && isNonspace(str, str.charAt(strLen - 1));
      }
   }

   private boolean isSimpleName(String str)
   {
      int strLen = str.length();

      switch (strLen)
      {
         case 0 :
            return false;
         case 1 :
            return isNonspace(str, str.charAt(0));
         case 2 :
            return isNonspace(str, str.charAt(0)) && isNonspace(str, str.charAt(1));
         default :
            return isNonspace(str, str.charAt(0)) && isSimpleString(str.substring(1, strLen - 1))
               && isNonspace(str, str.charAt(strLen - 1));
      }
   }

   private boolean isValidName(String str, boolean prefixed)
   {
      return (prefixed ? isLocalName(str) : isSimpleName(str) || str.equals(JCRPath.THIS_RELPATH)
         || str.equals(JCRPath.PARENT_RELPATH) || str.equals("*"));
   }
}
