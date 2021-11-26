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
 * @version $Id: AddNodeCommand.java 5800 2006-05-28 18:03:31Z geaz $
 */

public class AddNodeCommand implements Command
{

   private String pathKey = DefaultKeys.PATH;

   private String currentNodeKey = DefaultKeys.CURRENT_NODE;

   private String resultKey = DefaultKeys.RESULT;

   private String nodeTypeKey = DefaultKeys.NODE_TYPE;

   public boolean execute(Context context) throws Exception
   {

      Session session = ((JCRAppContext)context).getSession();

      Node parentNode = (Node)session.getItem((String)context.get(currentNodeKey));
      String relPath = (String)context.get(pathKey);
      if (context.containsKey(nodeTypeKey))
         context.put(resultKey, parentNode.addNode(relPath, (String)context.get(nodeTypeKey)));
      else
         context.put(resultKey, parentNode.addNode(relPath));

      return true;
   }

   public String getResultKey()
   {
      return resultKey;
   }

   public String getCurrentNodeKey()
   {
      return currentNodeKey;
   }

   public String getNodeTypeKey()
   {
      return nodeTypeKey;
   }

   public String getPathKey()
   {
      return pathKey;
   }
}
