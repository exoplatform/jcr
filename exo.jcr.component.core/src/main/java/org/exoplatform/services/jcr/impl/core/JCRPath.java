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

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;

import javax.jcr.PathNotFoundException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: JCRPath.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class JCRPath
{

   public final static String ROOT_PATH = "/";

   public final static String ROOT_NAME = "";

   public final static String THIS_RELPATH = ".";

   public final static String PARENT_RELPATH = "..";

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.JCRPath");

   protected PathElement[] names;

   JCRPath()
   {
      this.names = new PathElement[0];
   }

   public boolean isAbsolute()
   {
      if (names.length > 0)
      {
         PathElement first = names[0];
         if (first.getName().equals(ROOT_NAME))
         {
            return true;
         }
      }
      return false;
   }

   JCRPath addEntry(String namespace, String name, String prefix, int index)
   {
      if (name.equals(THIS_RELPATH))
         return this;

      if (name.equals(PARENT_RELPATH))
      {
         return addEntry(new MoveUpElement());
      }

      return addEntry(new PathElement(namespace, name, prefix, index));
   }

   JCRPath addEntry(PathElement entry)
   {
      if (names.length > 0 && entry instanceof MoveUpElement && !(names[names.length - 1] instanceof MoveUpElement))
      {
         return removeLastEntry();
      }

      PathElement[] newNames = new PathElement[names.length + 1];
      for (int i = 0; i < names.length; i++)
         newNames[i] = names[i];
      newNames[names.length] = entry;
      names = newNames;
      return this;
   }

   JCRPath removeLastEntry()
   {

      if (names.length <= 0)
      {
         log.warn("Wrong relative path. Can't move up in path hierarhy. " + getAsString(true));
         return this;
      }

      PathElement[] newNames = new PathElement[names.length - 1];
      for (int i = 0; i < newNames.length; i++)
         newNames[i] = names[i];
      names = newNames;
      return this;
   }

   public JCRPath makeParentPath()
   {
      return makeAncestorPath(1);
   }

   /**
    * Makes ancestor path by relative degree (For ex relativeDegree == 1 means parent path etc)
    * 
    * @param relativeDegree
    * @return
    * @throws PathNotFoundException
    */
   public JCRPath makeAncestorPath(int relativeDegree)
   {

      JCRPath path = new JCRPath();
      for (int i = 0; i < names.length - relativeDegree; i++)
         path.addEntry(names[i]);
      return path;
   }

   public PathElement[] getRelPath(int relativeDegree)
   {
      ArrayList<PathElement> entries = new ArrayList<PathElement>();
      for (int i = names.length - relativeDegree; i < names.length; i++)
         entries.add(names[i]);
      PathElement[] relPath = new PathElement[entries.size()];
      for (int i = 0; i < relPath.length; i++)
         relPath[i] = entries.get(i);
      return relPath;
   }

   public QPath getInternalPath()
   {

      QPathEntry[] entries = new QPathEntry[names.length];

      for (int i = 0; i < names.length; i++)
         entries[i] = new QPathEntry(names[i].getNamespace(), names[i].getName(), names[i].getIndex());

      QPath qpath = new QPath(entries);
      return qpath;
   }

   public String getAsString(boolean showIndex)
   {

      // [PN] 27.06.07
      String path = "";
      if (isAbsolute())
      {
         if (size() == 1)
            return "/";

         for (int i = 1; i < names.length; i++)
         {
            path += "/" + names[i].getAsString(showIndex);
         }
      }
      else
      {
         // relative
         for (int i = 0; i < names.length; i++)
         {
            path += i > 0 ? "/" + names[i].getAsString(showIndex) : names[i].getAsString(showIndex);
         }
      }

      return path;
   }

   public int getDepth()
   {
      return size() - 1;
   }

   public boolean isDescendantOf(JCRPath ancestorLocation, boolean childOnly)
   {
      int depthDiff = getDepth() - ancestorLocation.getDepth();
      if (depthDiff <= 0 || (childOnly && depthDiff != 1))
         return false;

      JCRPath.PathElement[] anotherNames = ancestorLocation.getEntries();
      for (int i = 0; i < anotherNames.length; i++)
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

   private int size()
   {
      return names.length;
   }

   public JCRName getName()
   {
      if (size() > 0)
         return names[size() - 1];

      return new ThisElement();
   }

   public int getIndex()
   {
      return names[size() - 1].getIndex();
   }

   public boolean isIndexSetExplicitly()
   {
      return names[size() - 1].isIndexSetExplicitly();
   }

   public boolean isSameNameSibling(JCRPath anotherPath)
   {
      JCRName[] anotherNames = anotherPath.getEntries();
      for (int i = 0; i < anotherNames.length - 1; i++)
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
      if (obj instanceof JCRPath)
      {
         JCRPath other = (JCRPath)obj;
         return this.getInternalPath().equals(other.getInternalPath());
      }
      return false;
   }

   PathElement[] getEntries()
   {
      return names;
   }

   public class ThisElement extends PathElement
   {

      ThisElement()
      {
         super(Constants.NS_DEFAULT_URI, THIS_RELPATH, Constants.NS_EMPTY_PREFIX, -1);
      }
   }

   public class MoveUpElement extends PathElement
   {

      MoveUpElement()
      {
         super(Constants.NS_DEFAULT_URI, PARENT_RELPATH, Constants.NS_EMPTY_PREFIX, -1);
      }
   }

   public class PathElement extends JCRName
   {

      private final int index;

      private final boolean indexSetExplicitly;

      public PathElement(String namespace, String name, String prefix, int index)
      {
         super(namespace, name, prefix);
         if (index == -1)
         {
            this.index = 1;
            this.indexSetExplicitly = false;
         }
         else
         {
            this.index = index;
            this.indexSetExplicitly = true;
         }
      }

      public PathElement clone(int newIndex)
      {
         return new PathElement(this.namespace, this.name, this.prefix, newIndex);
      }

      public int getIndex()
      {
         return index;
      }

      public boolean equals(Object obj)
      {
         if (!(obj instanceof PathElement))
            return false;

         return super.equals(obj) && index == ((PathElement)obj).getIndex();
      }

      public String getAsString(boolean showIndex)
      {
         String indexStr;
         if (showIndex || getIndex() > 1)
            indexStr = "[" + getIndex() + "]";
         else
            indexStr = "";
         return (super.getAsString() + indexStr);
      }

      public boolean isIndexSetExplicitly()
      {
         return indexSetExplicitly;
      }
   }

}
