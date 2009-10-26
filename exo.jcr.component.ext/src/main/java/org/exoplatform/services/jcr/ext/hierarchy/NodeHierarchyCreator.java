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
package org.exoplatform.services.jcr.ext.hierarchy;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 10:10:10 AM
 */
public interface NodeHierarchyCreator
{

   public String getJcrPath(String alias);

   public void init(String repository) throws Exception;

   public Node getUserNode(SessionProvider sessionProvider, String userName) throws Exception;

   public Node getUserApplicationNode(SessionProvider sessionProvider, String userName) throws Exception;

   public Node getPublicApplicationNode(SessionProvider sessionProvider) throws Exception;

   public void addPlugin(ComponentPlugin plugin);
}
