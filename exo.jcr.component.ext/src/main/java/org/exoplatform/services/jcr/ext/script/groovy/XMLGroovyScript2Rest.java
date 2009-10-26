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
 * @version $Id: XMLGroovyScript2Rest.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class XMLGroovyScript2Rest
{

   /**
    * Script name.
    */
   private String name;

   /**
    * From this URL script will be loaded.
    */
   private String path;

   /**
    * If this parameter true script will be loaded automatically.
    * 
    * @see {@link GroovyScript2RestLoader}
    */
   private boolean autoload;

   public XMLGroovyScript2Rest(String name, String path, boolean autoload)
   {
      this.name = name;
      this.path = path;
      this.autoload = autoload;
   }

   public XMLGroovyScript2Rest()
   {
   }

   /**
    * @return the name
    */
   public String getName()
   {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return the path
    */
   public String getPath()
   {
      return path;
   }

   /**
    * @param path the path to set
    */
   public void setPath(String path)
   {
      this.path = path;
   }

   /**
    * @return the autoload
    */
   public boolean isAutoload()
   {
      return autoload;
   }

   /**
    * @param autoload the autoload to set
    */
   public void setAutoload(boolean autoload)
   {
      this.autoload = autoload;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("{name: ").append(this.name).append("; path: ").append(this.path).append("; autoload: ").append(
         this.autoload).append("}");
      return sb.toString();
   }

}
