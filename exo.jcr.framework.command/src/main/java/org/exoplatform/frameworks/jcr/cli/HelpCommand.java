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
package org.exoplatform.frameworks.jcr.cli;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Vitaliy Obmanjuk
 * @version $Id: $
 */

public class HelpCommand extends AbstractCliCommand
{

   private static final Log LOG = ExoLogger.getLogger("org.exoplatform.frameworks.jcr.cli.HelpCommand");

   private TreeMap<String, String> map = new TreeMap<String, String>();

   private final int WORD_LENGTH = 15;

   public HelpCommand()
   {
      map.put("addnode", "<name>, <type> add node as child node of current node");
      map.put("mkdir", "<name>, <type> add node as child node of current node");
      map.put("login", "[<workspace name>] login to workspace");
      map.put("getitem", "<absPath> or <relPath> or <..> change the current item");
      map.put("cd", "<absPath> or <relPath> or < ..> change the current item, node names should not contain spaces");
      map.put("getnode", "<relPath> change the current node");
      map.put("cdn", "<relPath> change the current node");
      map.put("getproperty", "<relPath> change the current property");
      map.put("cdp", "<relPath> change the current property");
      map.put("getnodes", "<> get the list of nodes");
      map.put("lsn", "<> get the list of nodes");
      map.put("getproperties", "<> get the list of properties");
      map.put("lsp", "<> get the list of properties");
      map.put("ls", "<> get the list of the nodes and properties");
      map.put("setproperty", "<name>, <value>, <type> set the property");
      map.put("setp", "<name>, <value>, <type> set the property");
      map.put("getcontextinfo", "<> show the info of the current context");
      map.put("info", "<> show the info of the current context");
      map.put("remove", "<> remove the current item and go to parent item");
      map.put("rem", "<> remove the current item and go to parent item");
      map.put("copynode", "<srcAbsPath>, <destAbsPath> copy the node at srcAbsPath to the new location at destAbsPath");
      map.put("copy", "<srcAbsPath>, <destAbsPath> copy the node at srcAbsPath to the new location at destAbsPath");
      map.put("movenode", "<srcAbsPath>, <destAbsPath> move the node at srcAbsPath to the new location at destAbsPath");
      map.put("move", "<srcAbsPath>, <destAbsPath> move the node at srcAbsPath to the new location at destAbsPath");
      map.put("|", "<console size> limit the count of lines to output, e.g. |20 will displayed only 20 lines, "
         + "works in standalone mode only");
   }

   @Override
   public boolean perform(CliAppContext ctx)
   {
      StringBuilder output = new StringBuilder();
      try
      {
         String findHelpCommand = null;
         try
         {
            findHelpCommand = ctx.getParameter(0);
         }
         catch (Exception e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
         }
         if (findHelpCommand != null)
         {
            Set keys = map.keySet();
            Iterator iterator = keys.iterator();
            boolean found = false;
            while (iterator.hasNext())
            {
               String currentHelpCommand = (String)(iterator.next());
               if (findHelpCommand.equals(currentHelpCommand))
               {
                  // begin format output string (adding spaces)
                  StringBuilder findHelpCommandFormatted = new StringBuilder(currentHelpCommand);
                  
                  int commandLength = currentHelpCommand.length();
                  if (commandLength < WORD_LENGTH)
                  {
                     for (int i = currentHelpCommand.length(); i < WORD_LENGTH; i++)
                     {
                        findHelpCommandFormatted.append(" ");
                     }
                  }
                  // end format
                  output.append(findHelpCommandFormatted).append(" - ").append(map.get(findHelpCommand)).append("\n");
                  found = true;
                  break;
               }
            }
            if (found == false)
            {
               output.append("Can't find help for the: ").append(findHelpCommand).append(" command\n");
            }
         }
         else
         {
            Set keys = map.keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext())
            {
               String currentHelpCommand = (String)(iterator.next());
               // begin format output string (adding spaces)
               StringBuilder currentHelpCommandFormatted = new StringBuilder(currentHelpCommand);
               int commandLength = currentHelpCommand.length();
               if (commandLength < WORD_LENGTH)
               {
                  for (int i = currentHelpCommand.length(); i < WORD_LENGTH; i++)
                  {
                     currentHelpCommandFormatted.append(" ");
                  }
               }
               // end format
               output.append(currentHelpCommandFormatted).append(" - ").append(map.get(currentHelpCommand))
                  .append("\n");
            }
         }
      }
      catch (Exception e)
      {
         output = new StringBuilder("Can't execute command - ").append(e.getMessage()).append("\n");
      }
      ctx.setOutput(output.toString());
      return false;
   }
}
