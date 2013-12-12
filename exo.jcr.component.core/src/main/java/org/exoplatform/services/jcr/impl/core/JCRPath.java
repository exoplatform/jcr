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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: JCRPath.java 11907 2008-03-13 15:36:21Z ksm $
 */

public abstract class JCRPath
{

   public final static String ROOT_PATH = "/";

   public final static String ROOT_NAME = "";

   public final static String THIS_RELPATH = ".";

   public final static String PARENT_RELPATH = "..";

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JCRPath");

   public static JCRPath createJCRPath()
   {
      return JCRPathExt.ROOT;
   }

   public static JCRPath createJCRPath(NamespaceAccessor namespaces, QPath qpath) throws RepositoryException
   {
      return new JCRPathExt(namespaces, qpath.getEntries());
   }

   public static JCRPath createJCRPath(NamespaceAccessor namespaces, QPathEntry[] relPath) throws RepositoryException
   {
      return new JCRPathExt(namespaces, relPath);
   }

   public abstract boolean isAbsolute();

   abstract JCRPath addEntry(String namespace, String name, String prefix, int index);

   abstract JCRPath addEntry(PathElement entry);

   abstract JCRPath add(JCRPath path);

   abstract JCRPath addEntries(PathElement... entries);

   public abstract JCRPath makeParentPath();

   public abstract JCRPath makeAncestorPath(int relativeDegree);

   public abstract PathElement[] getRelPath(int relativeDegree);

   public abstract QPath getInternalPath();

   public abstract String getAsString(boolean showIndex);

   public abstract int getDepth();

   public abstract int getLength();

   public abstract PathElement getEntry(int index);

   public abstract PathElement[] getEntries();

   public abstract boolean isDescendantOf(JCRPath ancestorLocation, boolean childOnly);

   public abstract boolean isAncestorOf(JCRPath descendantLocation, boolean childOnly);

   public abstract PathElement getName();

   public abstract int getIndex();

   public abstract boolean isIndexSetExplicitly();

   public abstract boolean isSameNameSibling(JCRPath anotherPath);

   public abstract boolean equals(Object obj);

   protected JCRPath()
   {
   }

   public static class PathElement extends JCRName
   {

      private final int index;

      private final boolean indexSetExplicitly;

      private String cachedToString;

      private String cachedToStringShowIndex;

      public PathElement(String namespace, String name, String prefix, int index)
      {
         super(namespace, name, prefix);

         //
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

      public PathElement(InternalQName qname, String prefix, int index)
      {
         super(qname, prefix);

         //
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

      public PathElement(PathElement that, int index)
      {
         super(that);

         //
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
         return new PathElement(this, newIndex);
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

         StringBuilder sb = new StringBuilder(super.getAsString());
         if (showIndex || getIndex() > 1)
            sb.append("[").append(index).append("]");
         String res = sb.toString();

         //
         if (showIndex)
         {
            cachedToStringShowIndex = res;
         }
         else
         {
            cachedToString = res;
         }

         //
         return res;
      }

      public boolean isIndexSetExplicitly()
      {
         return indexSetExplicitly;
      }
   }
}
