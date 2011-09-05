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
package org.exoplatform.services.jcr.ext.backup.server.bean.response;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 14.04.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: Pair.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class Pair
{

   /**
    * The name of parameter.
    */
   private String name;

   /**
    * The value.
    */
   private String value;

   /**
    * Pair constructor.
    * 
    */
   private Pair()
   {
   }

   /**
    * Pair constructor.
    * 
    * @param name
    *          String, name of parameter
    * @param value
    *          String, value of parameter
    */
   private Pair(String name, String value)
   {
      this.name = name;
      this.value = value;
   }

   /**
    * getName.
    * 
    * @return String return the name of parameter
    */
   public String getName()
   {
      return name;
   }

   /**
    * setName.
    * 
    * @param name
    *          String, the name of parameter
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * getValue.
    * 
    * @return String return the value of parameter
    */
   public String getValue()
   {
      return value;
   }

   /**
    * setValue.
    * 
    * @param value
    *          String, the value of parameter
    */
   public void setValue(String value)
   {
      this.value = value;
   }

}
