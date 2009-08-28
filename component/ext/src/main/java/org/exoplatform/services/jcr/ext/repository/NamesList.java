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
package org.exoplatform.services.jcr.ext.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 27.08.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: NamesList.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class NamesList
{

   /**
    *  List with names.
    */
   protected List<String> names = new ArrayList<String>();

   /**
    *  Empty constructor.
    */
   public NamesList()
   {
   }

   /**
    * Constructor.
    * 
    * @param names
    *          List, the list with names
    */
   public NamesList(List<String> names)
   {
      this.names = names;
   }

   /**
    * @return List
    *           return the list of name
    */
   public List<String> getNames()
   {
      return names;
   }

   /**
    * @param names
    *          List, the list with names
    */
   public void setNames(List<String> names)
   {
      this.names = names;
   }

}
