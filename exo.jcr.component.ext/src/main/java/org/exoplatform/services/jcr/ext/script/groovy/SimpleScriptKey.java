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
package org.exoplatform.services.jcr.ext.script.groovy;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: SimpleScriptKey.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
// Need this for back compatibility, see @deprecated methods in
// GroovyScript2RestLoader.
public class SimpleScriptKey implements ScriptKey
{

   protected final String key;

   public SimpleScriptKey(String key)
   {
      this.key = key;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      return key.equals(((SimpleScriptKey)obj).key);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      return key.hashCode();
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      return key;
   }

}
