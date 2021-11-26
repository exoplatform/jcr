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

import javax.jcr.Item;
import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Vitaliy Obmanjuk
 * @version $Id: $
 */

public class GetItemCommand extends AbstractCliCommand
{

   @Override
   public boolean perform(CliAppContext ctx)
   {
      String output = "";
      try
      {
         String path = ctx.getParameter(0);
         Item resultItem = null;
         if (path.equals(".."))
         {
            Item currentItem = ctx.getCurrentItem();
            resultItem = currentItem.getParent();
         }
         else
         {
            // here we need to analize what kind of the path we have: relPath or
            // absPath
            if (path.startsWith("/"))
            {// absPath
               resultItem = ctx.getSession().getItem(path);
            }
            else
            {// relPath
               if (ctx.getCurrentItem().isNode())
               {
                  Node node = (Node)ctx.getCurrentItem();
                  resultItem = node.getNode(path);
               }
               else
               {// do nothing
                  resultItem = ctx.getCurrentItem();
               }
            }
         }
         ctx.setCurrentItem(resultItem);
         output = "Current item: " + ctx.getCurrentItem().getPath() + "\n";
      }
      catch (Exception e)
      {
         output = "Can't execute command - " + e.getMessage() + "\n";
      }
      ctx.setOutput(output);
      return false;
   }
}
