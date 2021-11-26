/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.ext.script.groovy;

import org.exoplatform.services.rest.ext.groovy.BaseResourceId;
import org.exoplatform.services.rest.ext.groovy.ResourceId;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: NodeScriptKey.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class NodeScriptKey extends BaseResourceId implements ResourceId
{
   private final String repositoryName;

   private final String workspaceName;

   private final String path;

   public NodeScriptKey(String repositoryName, String workspaceName, Node node) throws RepositoryException
   {
      this(repositoryName, workspaceName, node.getPath());
   }

   public NodeScriptKey(String repositoryName, String workspaceName, String path)
   {
      super(repositoryName + '@' + workspaceName + ':' + path);
      this.repositoryName = repositoryName;
      this.workspaceName = workspaceName;
      this.path = path;
   }

   public String getRepositoryName()
   {
      return repositoryName;
   }

   public String getWorkspaceName()
   {
      return workspaceName;
   }

   public String getPath()
   {
      return path;
   }
}
