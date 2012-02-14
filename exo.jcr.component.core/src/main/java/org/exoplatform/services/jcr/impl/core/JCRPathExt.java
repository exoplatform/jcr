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
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id$
 */

public class JCRPathExt extends JCRPath
{

   private static PathElement[] EMPTY_PATH = new PathElement[0];

   private static final PathElement THIS_ELEMENT =
            new PathElement(Constants.NS_DEFAULT_URI, THIS_RELPATH, Constants.NS_EMPTY_PREFIX, -1);

   private static final PathElement MOVE_UP_ELEMENT =
            new PathElement(Constants.NS_DEFAULT_URI, PARENT_RELPATH, Constants.NS_EMPTY_PREFIX, -1);

   protected final PathElement[] names;

   protected final int size;

   protected String cachedToString;

   protected String cachedToStringShowIndex;

   protected QPath cachedInternalQPath;

   public final static JCRPathExt ROOT = new JCRPathExt();

   private JCRPathExt()
   {
      this(EMPTY_PATH);
   }

   JCRPathExt(NamespaceAccessor namespaces, QPathEntry[] path) throws RepositoryException
   {
      PathElement[] names = new PathElement[path.length];
      int size = 0;
      for (QPathEntry entry : path)
      {
         String prefix = namespaces.getNamespacePrefixByURI(entry.getNamespace());
         PathElement element = element(entry.getNamespace(), entry.getName(), prefix, entry.getIndex());
         size = addEntry(names, size, element);
      }

      //
      this.names = names;
      this.size = size;
   }

   JCRPathExt(JCRPathExt that, PathElement[] addedEntries, int addedSize)
   {
      PathElement[] names = new PathElement[that.size + addedSize];
      int size = 0;
      for (int i = 0;i < that.size;i++)
      {
         size = addEntry(names, size, that.names[i]);
      }
      for (int i = 0;i < addedSize;i++)
      {
         size = addEntry(names, size, addedEntries[i]);
      }

      //
      this.names = names;
      this.size = size;
   }

   JCRPathExt(PathElement[] names)
   {
      this(names, names.length);
   }

   JCRPathExt(PathElement[] names, int size)
   {
      if (size < 0)
      {
         throw new AssertionError("Size value is negative: " + size);
      }
      if (size > names.length)
      {
         throw new AssertionError("Size value is too large: " + size + " instead of max: " + names.length);
      }

      //
      this.names = names;
      this.size = size;
   }

   public boolean isAbsolute()
   {
      if (size > 0)
      {
         PathElement first = names[0];
         if (first.getName().equals(ROOT_NAME))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   JCRPath add(JCRPath path)
   {
      return new JCRPathExt(this, ((JCRPathExt)path).names, ((JCRPathExt)path).size);
   }

   @Override
   JCRPath addEntries(PathElement... entries)
   {
      return new JCRPathExt(this, entries, entries.length);
   }

   JCRPathExt addEntry(String namespace, String name, String prefix, int index)
   {
      return addEntry(element(namespace, name, prefix, index));
   }

   public JCRPathExt makeAncestorPath(int relativeDegree)
   {
      return new JCRPathExt(names, size - relativeDegree);
   }

   JCRPathExt addEntry(PathElement entry)
   {
      return new JCRPathExt(this, new PathElement[]{entry}, 1);
   }

   private static PathElement element(String namespace, String name, String prefix, int index)
   {
      if (name.equals(THIS_RELPATH))
      {
         return THIS_ELEMENT;
      }
      else if (name.equals(PARENT_RELPATH))
      {
         return MOVE_UP_ELEMENT;
      }
      else
      {
         return new PathElement(namespace, name, prefix, index);
      }
   }

   private static int addEntry(PathElement[] entries, int size, PathElement entry)
   {
      if (entry == THIS_ELEMENT) // NOSONAR
      {
         return size;
      }
      if (size > 0 && entry == MOVE_UP_ELEMENT && !(entries[size - 1] == MOVE_UP_ELEMENT)) // NOSONAR
      {
         if (size <= 0)
         {
            LOG.warn("Wrong relative path. Can't move up in path hierarhy.");
            return 0;
         }
         return size - 1;
      }

      //
      entries[size] = entry;

      //
      return size + 1;
   }

   public JCRPathExt makeParentPath()
   {
      return makeAncestorPath(1);
   }

   @Override
   public int getLength()
   {
      return size;
   }

   @Override
   public PathElement getEntry(int index)
   {
      if (index > size)
      {
         throw new ArrayIndexOutOfBoundsException();
      }
      return names[index];
   }

   public PathElement[] getRelPath(int relativeDegree)
   {
      PathElement[] relPath = new PathElement[relativeDegree];
      System.arraycopy(names, size - relativeDegree, relPath, 0, relativeDegree);
      return relPath;
   }

   public QPath getInternalPath()
   {
      if (cachedInternalQPath == null)
      {
         QPathEntry[] entries = new QPathEntry[size];

         for (int i = 0; i < size; i++)
            entries[i] = new QPathEntry(names[i].getNamespace(), names[i].getName(), names[i].getIndex());

         cachedInternalQPath = new QPath(entries);
      }

      return cachedInternalQPath;
   }

   public String getAsString(boolean showIndex)
   {
      if (showIndex)
      {
         if (cachedToStringShowIndex != null)
         {
            return cachedToStringShowIndex;
         }
      }
      else
      {
         if (cachedToString != null)
         {
            return cachedToString;
         }
      }

      // [PN] 27.06.07
      String path;
      if (isAbsolute())
      {
         if (size == 1)
         {
            path = "/";
         }
         else
         {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < size; i++)
            {
               builder.append("/").append(names[i].getAsString(showIndex));
            }
            path = builder.toString();
         }
      }
      else
      {
         // relative
         StringBuilder builder = new StringBuilder();
         for (int i = 0; i < size; i++)
         {
            if (i > 0)
            {
               builder.append("/");
            }
            else
            {

            }
            builder.append(names[i].getAsString(showIndex));
         }
         path = builder.toString();
      }


      //
      if (showIndex)
      {
         cachedToStringShowIndex = path;
      }
      else
      {
         cachedToString = path;
      }

      //
      return path;
   }

   public int getDepth()
   {
      return size - 1;
   }

   public boolean isDescendantOf(JCRPath ancestorLocation, boolean childOnly)
   {
      return isDescendantOf((JCRPathExt)ancestorLocation, childOnly);
   }

   public boolean isDescendantOf(JCRPathExt ancestorLocation, boolean childOnly)
   {
      int depthDiff = getDepth() - ancestorLocation.getDepth();
      if (depthDiff <= 0 || (childOnly && depthDiff != 1))
         return false;

      JCRPathExt.PathElement[] anotherNames = ancestorLocation.names;
      for (int i = 0; i < ancestorLocation.size; i++)
      {
         boolean result = anotherNames[i].equals(names[i]);
         if (!result)
            return false;
      }
      return true;
   }

   public boolean isAncestorOf(JCRPath descendantLocation, boolean childOnly)
   {
      return descendantLocation.isDescendantOf(this, childOnly);
   }

   public PathElement getName()
   {
      if (size > 0)
         return names[size - 1];

      return THIS_ELEMENT;
   }

   public int getIndex()
   {
      return names[size - 1].getIndex();
   }

   public boolean isIndexSetExplicitly()
   {
      return names[size - 1].isIndexSetExplicitly();
   }

   public boolean isSameNameSibling(JCRPath anotherPath)
   {
      return isSameNameSibling((JCRPathExt)anotherPath);
   }

   public boolean isSameNameSibling(JCRPathExt anotherPath)
   {
      JCRName[] anotherNames = anotherPath.names;
      for (int i = 0; i < anotherPath.size - 1; i++)
      {
         boolean result = anotherNames[i].equals(names[i]);
         if (!result)
            return false;
      }
      return getName().getName().equals(anotherPath.getName().getName())
         && this.getName().getPrefix().equals(anotherPath.getName().getPrefix());
   }

   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj instanceof JCRPathExt)
      {
         JCRPathExt other = (JCRPathExt)obj;
         return this.getInternalPath().equals(other.getInternalPath());
      }
      return false;
   }

   public PathElement[] getEntries()
   {
      return names.clone();
   }

}
