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

package org.exoplatform.frameworks.jcr.command.ext;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.exoplatform.frameworks.jcr.command.DefaultKeys;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: FilterNodesByTypesCommand.java 5800 2006-05-28 18:03:31Z geaz $
 */

public class FilterNodesByTypesCommand implements Command
{

   private String pathKey = DefaultKeys.PATH;

   private String incomingNodesKey = DefaultKeys.RESULT;

   private String typesKey = "nodeTypes";

   private String resultKey = DefaultKeys.RESULT;

   public boolean execute(Context context) throws Exception
   {
      Object obj = context.get(incomingNodesKey);
      if (obj == null || !(obj instanceof NodeIterator))
      {
         throw new IllegalArgumentException("Invalid incoming nodes iterator " + obj);
      }
      NodeIterator nodes = (NodeIterator)obj;

      obj = context.get(typesKey);
      if (obj == null || !(obj instanceof String[]))
      {
         throw new IllegalArgumentException("Invalid node types object, expected String[] " + obj);
      }
      String[] nts = (String[])context.get(typesKey);

      List nodes1 = new ArrayList();
      while (nodes.hasNext())
      {
         Node n = nodes.nextNode();
         for (int i = 0; i < nts.length; i++)
         {
            if (n.isNodeType(nts[i]))
            {
               nodes1.add(n);
            }
         }
      }
      // context.put(resultKey, new EntityCollection(nodes1));
      context.put(resultKey, nodes1);

      return false;
   }

   public String getIncomingNodesKey()
   {
      return incomingNodesKey;
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
