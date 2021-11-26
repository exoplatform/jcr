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

package org.exoplatform.frameworks.jcr.command.core;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.exoplatform.frameworks.jcr.command.DefaultKeys;
import org.exoplatform.frameworks.jcr.command.JCRAppContext;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: GetNodeCommand.java 5878 2006-05-31 09:44:14Z peterit $
 */

public class GetNodeCommand implements Command
{

   private String pathKey = DefaultKeys.PATH;

   private String workspaceKey = DefaultKeys.WORKSPACE;

   private String currentNodeKey = DefaultKeys.CURRENT_NODE;

   private String resultKey = DefaultKeys.RESULT;

   public boolean execute(Context context) throws Exception
   {

      String wsName = (String)context.get(workspaceKey);
      if (wsName != null)
         ((JCRAppContext)context).setCurrentWorkspace(wsName);
      Session session = ((JCRAppContext)context).getSession();
      String relPath = (String)context.get(pathKey);

      Node parentNode = (Node)session.getItem((String)context.get(currentNodeKey));

      context.put(resultKey, parentNode.getNode(relPath));
      return false;
   }

   public String getCurrentNodeKey()
   {
      return currentNodeKey;
   }

   public String getPathKey()
   {
      return pathKey;
   }

   public String getResultKey()
   {
      return resultKey;
   }

}
