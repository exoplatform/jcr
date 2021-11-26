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

package org.exoplatform.frameworks.jcr.cli;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Vitaliy Obmanjuk
 * @version $Id: $
 */

public class AddNodeCommand extends AbstractCliCommand
{

   public boolean perform(CliAppContext ctx)
   {
      String output = "";
      try
      {
         int parametersCount = ctx.getParameters().size();
         String nodeName = ctx.getParameter(0);
         // String nodeType = ctx.getParameter(1);
         String nodeType = parametersCount == 2 ? ctx.getParameter(1) : null;
         Node curNode = (Node)ctx.getCurrentItem();
         Node newNode = null;
         if (nodeType == null)
         {
            newNode = curNode.addNode(nodeName);
         }
         else
         {
            newNode = curNode.addNode(nodeName, nodeType);
         }
         ctx.getSession().save();
         ctx.setCurrentItem(newNode);
         output = "Node: " + newNode.getPath() + " created succesfully \n";
      }
      catch (Exception e)
      {
         output = "Can't execute command - " + e.getMessage() + "\n";
      }
      ctx.setOutput(output);
      return false;
   }
}
