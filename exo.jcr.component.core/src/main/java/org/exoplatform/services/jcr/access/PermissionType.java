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
package org.exoplatform.services.jcr.access;

/**
 * Created by The eXo Platform SAS<br>
 *
 * Interface defining all the constants related to a permission type.
 *
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: PermissionType.java 14515 2008-05-20 11:45:21Z ksm $
 * @LevelAPI Platform
 */
public interface PermissionType
{
   /**
    * Name of the permission needed to read an item
    */
   public static final String READ = "read";

   /**
    * Name of the permission needed to add node to a node
    */
   public static final String ADD_NODE = "add_node";

   /**
    * The name of the permission needed to set a property to a node
    */
   public static final String SET_PROPERTY = "set_property";

   /**
    * Name of the permission needed to remove an item
    */
   public static final String REMOVE = "remove";

   /**
    * An array containing all the existing permission types.
    */
   public static final String[] ALL = new String[]{READ, ADD_NODE, SET_PROPERTY, REMOVE};

   /**
    * An array containing only the <i>read</i> permission
    */
   public static final String[] DEFAULT_AC = new String[]{READ};

   /**
    * A comma separated value composed of the add node, set property and remove permissions
    */
   public static final String CHANGE_PERMISSION = ADD_NODE + "," + SET_PROPERTY + "," + REMOVE;
}
