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

import org.exoplatform.services.jcr.datamodel.InternalQName;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JCRName.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class JCRName
{

   protected final String prefix;

   protected final String name;

   protected final String namespace;

   protected final String stringName;

   protected final int hashCode;

   protected JCRName(InternalQName qname, String prefix)
   {
      this(qname.getNamespace(), qname.getName(), prefix);
   }

   JCRName(JCRPath.PathElement that)
   {
      this.prefix = that.prefix;
      this.name = that.name;
      this.namespace = that.namespace;
      this.stringName = that.stringName;
      this.hashCode = that.hashCode;
   }

   JCRName(String namespace, String name, String prefix)
   {
      int hk = 31 + namespace.hashCode();
      hk = hk * 31 + name.hashCode();
      int hashCode = hk * 31 + prefix.hashCode();

      //
      String stringName;
      if (prefix.length() == 0)
      {
         stringName = name;
      }
      else
      {
         stringName = prefix + ":" + name;
      }

      //
      this.name = name;
      this.namespace = namespace;
      this.prefix = prefix;
      this.stringName = stringName;
      this.hashCode = hashCode;
   }

   /**
    * @return Returns the internalName.
    */
   public String getNamespace()
   {
      return namespace;
   }

   /**
    * @return Returns the name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * @return Returns the namespace.
    */
   public String getPrefix()
   {
      return prefix;
   }

   /**
    * @return Returns the internalName.
    */
   public InternalQName getInternalName()
   {
      return new InternalQName(namespace, name);
   }

   /**
    * Return this name as string.
    * 
    * @param showIndex
    *          if index should be included to the string.
    * @return name as string.
    */
   public String getAsString()
   {
      return stringName;
   }

   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;

      if (obj == null)
         return false;

      if (obj instanceof JCRName)
      {
         return hashCode == obj.hashCode();
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return hashCode;
   }

   @Override
   public String toString()
   {
      return super.toString() + " (" + getAsString() + ")";
   }
}
