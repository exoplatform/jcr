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
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: PermissionType.java 14515 2008-05-20 11:45:21Z ksm $
 */
public interface PermissionType
{
   public static final String READ = "read";

   public static final String ADD_NODE = "add_node";

   public static final String SET_PROPERTY = "set_property";

   public static final String REMOVE = "remove";

   public static final String[] ALL = new String[]{READ, ADD_NODE, SET_PROPERTY, REMOVE};

   public static final String[] DEFAULT_AC = new String[]{READ};

   public static final String CHANGE_PERMISSION = ADD_NODE + "," + SET_PROPERTY + "," + REMOVE;
}
