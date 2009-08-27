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
package org.exoplatform.services.jcr.datamodel;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.QName;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: QPath.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class QPath
   implements Comparable<QPath>
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("jcr.QPath");

   /**
    * QPath prefix delimiter.
    */
   public static final String PREFIX_DELIMITER = ":";

   /**
    * Names storage.
    */
   private final QPathEntry[] names;

   /**
    * Path hash code.
    */
   private final int hashCode;

   /**
    * String representation of the path.
    */
   private String stringName;

   /**
    * QPath  constructor.
    *
    * @param names
    */
   public QPath(QPathEntry[] names)
   {
      this.names = names;

      final int prime = 31;
      int hash = names.length > 0 ? 1 : super.hashCode();
      for (QPathEntry entry : names)
      {
         hash = prime * hash + entry.hashCode();
         hash = prime * hash + entry.getIndex();
      }
      this.hashCode = hash;
   }

   /**
    * Tell if the path is absolute.
    *
    * @return boolean 
    */
   public boolean isAbsolute()
   {
      if (names[0].getIndex() == 1 && names[0].getName().length() == 0 && names[0].getNamespace().length() == 0)
         return true;
      else
         return false;
   }

   /**
    * @return parent path
    * @throws PathNotFoundException
    *           if path could not have parent - i.e. root path
    */
   public QPath makeParentPath()
   {
      return makeAncestorPath(1);
   }

   /**
    * Makes ancestor path by relative degree (For ex relativeDegree == 1 means parent path etc)
    * 
    * @param relativeDegree
    * @return
    */
   public QPath makeAncestorPath(int relativeDegree)
   {
      int entryCount = getLength() - relativeDegree;
      QPathEntry[] ancestorEntries = new QPathEntry[entryCount];
      for (int i = 0; i < entryCount; i++)
      {
         QPathEntry entry = names[i];
         ancestorEntries[i] = new QPathEntry(entry.getNamespace(), entry.getName(), entry.getIndex());
      }

      return new QPath(ancestorEntries);
   }

   /**
    * Get relative path with degree.
    * 
    * @param relativeDegree
    *          - degree value
    * @return arrayf of QPathEntry
    * @throws IllegalPathException
    *           - if the degree is invalid
    */
   public QPathEntry[] getRelPath(int relativeDegree) throws IllegalPathException
   {

      int len = getLength() - relativeDegree;
      if (len < 0)
         throw new IllegalPathException("Relative degree " + relativeDegree + " is more than depth for "
                  + getAsString());

      QPathEntry[] relPath = new QPathEntry[relativeDegree];
      System.arraycopy(names, len, relPath, 0, relPath.length);

      return relPath;
   }

   /**
    * @return array of its path's names
    */
   public QPathEntry[] getEntries()
   {
      return names;
   }

   /**
    * @return depth of this path calculates as size of names array - 1. For ex root's depth=0 etc.
    */
   public int getDepth()
   {
      return names.length - 1;
   }

   /**
    * @param ancestorPath
    * @return if this path is descendant of given ancestor
    */
   public boolean isDescendantOf(final QPath ancestorPath)
   {
      final InternalQName[] ancestorNames = ancestorPath.names;

      if (names.length - ancestorNames.length <= 0)
         return false;

      for (int i = 0; i < ancestorNames.length; i++)
      {
         if (!ancestorNames[i].equals(names[i]))
            return false;
      }
      return true;
   }

   /**
    * @param anotherPath
    * @param childOnly
    *          if == true only direct children of the path will be taking in account
    * @return if this path is descendant of another one
    */
   public boolean isDescendantOf(final QPath anotherPath, final boolean childOnly)
   {
      final InternalQName[] anotherNames = anotherPath.names;

      // int depthDiff = getDepth() - anotherPath.getDepth();
      int depthDiff = names.length - anotherNames.length;
      if (depthDiff <= 0 || (childOnly && depthDiff != 1))
         return false;

      // InternalQName[] anotherNames = anotherPath.getEntries();
      for (int i = 0; i < anotherNames.length; i++)
      {
         if (!anotherNames[i].equals(names[i]))
            return false;
      }
      return true;
   }

   /**
    * Get common ancestor path.
    * 
    * @param firstPath
    * @param secondPath
    * @return The common ancestor of two paths.
    * @throws PathNotFoundException
    */
   public static QPath getCommonAncestorPath(QPath firstPath, QPath secondPath) throws PathNotFoundException
   {

      if (!firstPath.getEntries()[0].equals(secondPath.getEntries()[0]))
      {
         throw new PathNotFoundException("For the given ways there is no common ancestor.");
      }

      List<QPathEntry> caEntries = new ArrayList<QPathEntry>();
      for (int i = 0; i < firstPath.getEntries().length; i++)
      {
         if (firstPath.getEntries()[i].equals(secondPath.getEntries()[i]))
         {
            caEntries.add(firstPath.getEntries()[i]);
         }
         else
         {
            break;
         }
      }

      return new QPath(caEntries.toArray(new QPathEntry[caEntries.size()]));
   }

   /**
    * @return last name of this path
    */
   public InternalQName getName()
   {
      return names[getLength() - 1];
   }

   /**
    * @return index
    */
   public int getIndex()
   {
      return names[getLength() - 1].getIndex();
   }

   /**
    * @return length of names array
    */
   protected int getLength()
   {
      return names.length;
   }

   /**
    * Get String representation.
    * 
    * @return String
    */
   public String getAsString()
   {

      if (stringName == null)
      {

         String str = "";
         for (int i = 0; i < getLength(); i++)
         {
            str += names[i].getAsString(true);
         }
         stringName = str;
      }

      return stringName;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      return super.toString() + " (" + getAsString() + ")";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object o)
   {
      if (o == this)
         return true;

      if (!(o instanceof QPath))
         return false;

      return hashCode == o.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(QPath compare)
   {
      if (compare.equals(this))
         return 0;

      QPathEntry[] e1 = names;
      QPathEntry[] e2 = compare.getEntries();

      int len1 = e1.length;
      int len2 = e2.length;

      int k = 0;
      int lim = Math.min(len1, len2);
      while (k < lim)
      {

         QPathEntry c1 = e1[k];
         QPathEntry c2 = e2[k];

         if (!c1.isSame(c2))
         {
            return c1.compareTo(c2);
         }
         k++;
      }
      return len1 - len2;
   }

   @Override
   public int hashCode()
   {
      return hashCode;
   }

   // Factory methods ---------------------------

   /**
    * Parses string and make internal path from it.
    * 
    * @param qPath
    *          - String to be parsed
    * @return QPath
    * @throws RepositoryException
    *           - if string is invalid
    */
   public static QPath parse(String qPath) throws IllegalPathException
   {
      if (qPath == null)
         throw new IllegalPathException("Bad internal path '" + qPath + "'");

      if (qPath.length() < 2 || !qPath.startsWith("[]"))
         throw new IllegalPathException("Bad internal path '" + qPath + "'");

      int uriStart = 0;
      List<QPathEntry> entries = new ArrayList<QPathEntry>();
      while (uriStart >= 0)
      {

         uriStart = qPath.indexOf("[", uriStart);

         int uriFinish = qPath.indexOf("]", uriStart);
         String uri = qPath.substring(uriStart + 1, uriFinish);

         int tmp = qPath.indexOf("[", uriFinish); // next token
         if (tmp == -1)
         {
            tmp = qPath.length();
            uriStart = -1;
         }
         else
            uriStart = tmp;

         String localName = qPath.substring(uriFinish + 1, tmp);
         int index = 0;
         int ind = localName.indexOf(PREFIX_DELIMITER);
         if (ind != -1)
         { // has index
            index = Integer.parseInt(localName.substring(ind + 1));
            localName = localName.substring(0, ind);
         }
         else
         {
            if (uriStart > -1)
               throw new IllegalPathException("Bad internal path '" + qPath
                        + "' each intermediate name should have index");
         }

         entries.add(new QPathEntry(uri, localName, index));
      }
      return new QPath(entries.toArray(new QPathEntry[entries.size()]));
   }

   /**
    * Makes child path from existed path and child name. Assumed that parent path belongs to node so
    * it should have some index. If not sets index=1 automatically.
    * 
    * @param parent
    *          path
    * @param name
    *          child name
    * @return new InternalQPath
    */
   @Deprecated
   public static QPath makeChildPath(QPath parent, String name) throws IllegalPathException
   {

      QPathEntry[] parentEntries = parent.getEntries();
      QPathEntry[] names = new QPathEntry[parentEntries.length + 1];
      int index = 0;
      for (QPathEntry pname : parentEntries)
      {
         names[index++] = pname;
      }

      names[index] = parseEntry(name);
      QPath path = new QPath(names);
      return path;
   }

   /**
    * Make child path using JCR internal QName and index 1. <br/>
    * 
    * @param parent
    *          - parent QPath
    * @param name
    *          - Item InternalQName
    * @return new QPath
    */
   public static QPath makeChildPath(final QPath parent, final InternalQName name)
   {
      return makeChildPath(parent, name, 1);
   }

   /**
    * Make child path using QName and Item index. <br/>
    * 
    * @param parent
    *          - parent QPath
    * @param name
    *          - Item QName
    * @param itemIndex
    *          - Item index
    * @return new QPath
    */
   public static QPath makeChildPath(final QPath parent, final QName name, final int itemIndex)
   {

      QPathEntry[] parentEntries = parent.getEntries();
      QPathEntry[] names = new QPathEntry[parentEntries.length + 1];
      int index = 0;
      for (QPathEntry pname : parentEntries)
      {
         names[index++] = pname;
      }
      names[index] = new QPathEntry(name.getNamespace(), name.getName(), itemIndex);

      QPath path = new QPath(names);
      return path;
   }

   /**
    * Make child path using array of QPath entries (relative path). <br/>
    * 
    * @param parent
    *          - parent QPath
    * @param relEntries
    *          - QPathEntry array
    * @return new QPath
    */
   public static QPath makeChildPath(final QPath parent, final QPathEntry[] relEntries)
   {

      final QPathEntry[] parentEntries = parent.getEntries();
      final QPathEntry[] names = new QPathEntry[parentEntries.length + relEntries.length];
      int index = 0;
      for (QPathEntry name : parentEntries)
         names[index++] = name;

      for (QPathEntry name : relEntries)
         names[index++] = name;

      QPath path = new QPath(names);
      return path;
   }

   /**
    * Make child path using QPath entry. <br/>
    * 
    * Will replace <code>makeChildPath(final QPath parent, final InternalQName name)</code> for cases
    * when path entry already exists.
    * 
    * <br/> NOTE: it's important for same-name-siblings Items too.
    * 
    * @param parent
    *          - parent QPath
    * @param relEntry
    *          - QPathEntry instance
    * @return new QPath
    * 
    * @since 1.10
    */
   public static QPath makeChildPath(final QPath parent, final QPathEntry relEntry)
   {

      final QPathEntry[] parentEntries = parent.getEntries();
      final QPathEntry[] names = new QPathEntry[parentEntries.length + 1];
      int index = 0;
      for (QPathEntry name : parentEntries)
         names[index++] = name;

      names[index] = relEntry;

      QPath path = new QPath(names);
      return path;
   }

   /**
    * Parse textual path entry.
    *
    * @param entry - String text
    * @return QPathEntry
    * @throws IllegalPathException if text is not a valid entry
    */
   private static QPathEntry parseEntry(final String entry) throws IllegalPathException
   {

      if (!entry.startsWith("["))
         throw new IllegalPathException("Invalid QPath Entry '" + entry + "' Should start of '['");
      final int uriStart = 0;
      final int uriFinish = entry.indexOf("]", uriStart);
      if (uriFinish == -1)
         throw new IllegalPathException("Invalid QPath Entry '" + entry + "' No closed ']'");
      final String uri = entry.substring(uriStart + 1, uriFinish);

      final String localName = entry.substring(uriFinish + 1, entry.length());

      final int ind = localName.indexOf(PREFIX_DELIMITER);
      if (ind > 1)
      {
         return new QPathEntry(uri, localName.substring(0, ind), Integer.parseInt(localName.substring(ind + 1)));
      }

      return new QPathEntry(uri, localName, 1);
   }

}
