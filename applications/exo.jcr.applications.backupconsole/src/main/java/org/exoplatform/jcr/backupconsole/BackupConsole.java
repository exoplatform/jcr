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
package org.exoplatform.jcr.backupconsole;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: BackupConsole.java 111 2008-11-11 11:11:11Z serg $
 */
public class BackupConsole
{

   /**
    * Incorrect parameter string.
    */
   private static final String INCORRECT_PARAM = "Incorrect parameter: ";

   /**
    * Too many parameters string.
    */
   private static final String TOO_MANY_PARAMS = "Too many parameters.";

   /**
    * Login password string.
    */
   private static final String LOGIN_PASS_SPLITTER = ":";

   /**
    * Force close session parameter string.
    */
   private static final String FORCE_CLOSE = "force-close-session";

   /**
    * Help string.
    */
   private static final String HELP_INFO =
      "Help info:\n" + " <url> <cmd> \n" + " <url>  :   http(s)//login:password@host:port/<context> \n"
         + " <cmd>  :   start <repo[/ws]> <backup_dir> [<incr>] \n" 
         + "            stop <backup_id> \n"
         + "            status <backup_id> \n" 
         + "            restores <repo[/ws]> \n"
         + "            restore <repo[/ws]> <backup_id> <pathToConfigFile> \n" 
         + "            list [completed] \n"
         + "            info \n" 
         + "            drop [force-close-session] <repo[/ws]>  \n" 
         + "            help  \n\n"

         + " start          - start backup of repositpry or workspace \n" 
         + " stop           - stop backup \n"
         + " status         - information about the current or completed backup by 'backup_id' \n"
         + " restores       - information about the last restore on specific repository or workspace \n"
         + " restore        - restore the repository or workspace from specific backup \n"
         + " list           - information about the current backups (in progress) \n"
         + " list completed - information about the completed (ready to restore) backups \n"
         + " info           - information about the service backup \n" 
         + " drop           - delete the repository or workspace \n"
         + " help           - print help information about backup console \n\n"

         + " <repo[/ws]>           - /<reponsitory-name>[/<workspace-name>]  the repository or workspace \n"
         + " <backup_dir>        - path to folder for backup on remote server \n"
         + " <backup_id>         - the identifier for backup \n" 
         + " <incr>              - incemental job period \n"
         + " <pathToConfigFile>  - path (local) to  repository or workspace configuration \n"
         + " force-close-session - close opened sessions on repositpry or workspace. \n\n";

   /**
    * Main.
    * 
    * @param args -
    *          arguments used as parameters for execute backup server commands.
    */
   public static void main(String[] args)
   {

      int curArg = 0;
      if (curArg == args.length)
      {
         System.out.println(INCORRECT_PARAM + "There is no any parameters.");
         return;
      }

      // help
      if (args[curArg].equalsIgnoreCase("help"))
      {
         System.out.println(HELP_INFO);
         return;
      }

      // url
      String sUrl = args[curArg];
      curArg++;
      URL url;
      try
      {
         url = new URL(sUrl);
      }
      catch (MalformedURLException e)
      {
         System.out.println(INCORRECT_PARAM + "There is no url parameter.");
         return;
      }

      String urlPath = null;

      if (!("".equals(url.getPath())))
         urlPath = url.getPath();

      // login:password
      String login = url.getUserInfo();
      if (login == null)
      {
         System.out.println(INCORRECT_PARAM + "There is no specific Login:Password in url parameter - " + sUrl);
         return;
      }
      else if (!login.matches("[^:]+:[^:]+"))
      {
         System.out.println(INCORRECT_PARAM + "There is incorrect Login:Password parameter - " + login);
         return;
      }

      String host = url.getHost() + ":" + url.getPort();

      // initialize transport and backup client
      String[] lp = login.split(LOGIN_PASS_SPLITTER);
      ClientTransport transport = new ClientTransportImpl(lp[0], lp[1], host, url.getProtocol());
      BackupClient client = new BackupClientImpl(transport, lp[0], lp[1], urlPath);

      // commands
      if (curArg == args.length)
      {
         System.out.println(INCORRECT_PARAM + "There is no command parameter.");
         return;
      }
      String command = args[curArg++];

      try
      {
         if (command.equalsIgnoreCase("start"))
         {
            String pathToWS = getRepoWS(args, curArg++);
            if (pathToWS == null)
               return;

            String repositoryName = getRepositoryName(pathToWS);
            String workspaceName = (pathToWS.split("/").length == 3 ? getWorkspaceName(pathToWS) : null);

            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no path to backup dir parameter.");
               return;
            }
            String backupDir = args[curArg++];

            if (curArg == args.length)
            {
               System.out.println(client.startBackUp(repositoryName, workspaceName, backupDir));
            }
            else
            {
               // incremental job period
               String incr = args[curArg++];
               long inc = 0;
               try
               {
                  inc = Long.parseLong(incr);
               }
               catch (NumberFormatException e)
               {
                  System.out.println(INCORRECT_PARAM + "Incemental job period is not didgit - " + e.getMessage());
                  return;
               }

               if (curArg < args.length)
               {
                  System.out.println(TOO_MANY_PARAMS);
                  return;
               }
               System.out.println(client.startIncrementalBackUp(repositoryName, workspaceName, backupDir, inc));
            }
         }
         else if (command.equalsIgnoreCase("stop"))
         {
            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no backup identifier parameter.");
               return;
            }
            String backupId = args[curArg++];

            if (curArg < args.length)
            {
               System.out.println(TOO_MANY_PARAMS);
               return;
            }
            System.out.println(client.stop(backupId));
         }
         else if (command.equalsIgnoreCase("drop"))
         {

            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no path to workspace or force-session-close parameter.");
               return;
            }

            String param = args[curArg++];
            boolean isForce = true;

            if (!param.equalsIgnoreCase(FORCE_CLOSE))
            {
               curArg--;
               isForce = false;
            }

            String pathToWS = getRepoWS(args, curArg++);

            if (pathToWS == null)
               return;

            String repositoryName = getRepositoryName(pathToWS);
            String workspaceName = (pathToWS.split("/").length == 3 ? getWorkspaceName(pathToWS) : null);

            if (curArg < args.length)
            {
               System.out.println(TOO_MANY_PARAMS);
               return;
            }
            System.out.println(client.drop(isForce, repositoryName, workspaceName));
         }
         else if (command.equalsIgnoreCase("status"))
         {
            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no backup identifier parameter.");
               return;
            }

            String backupId = args[curArg++];

            if (curArg < args.length)
            {
               System.out.println(TOO_MANY_PARAMS);
               return;
            }
            System.out.println(client.status(backupId));
         }
         else if (command.equalsIgnoreCase("info"))
         {
            if (curArg < args.length)
            {
               System.out.println(TOO_MANY_PARAMS);
               return;
            }
            System.out.println(client.info());
         }
         else if (command.equalsIgnoreCase("restores"))
         {

            String pathToWS = getRepoWS(args, curArg++);
            if (pathToWS == null)
               return;

            String repositoryName = getRepositoryName(pathToWS);
            String workspaceName = (pathToWS.split("/").length == 3 ? getWorkspaceName(pathToWS) : null);;

            if (curArg < args.length)
            {
               System.out.println(TOO_MANY_PARAMS);
               return;
            }

            System.out.println(client.restores(repositoryName, workspaceName));
         }
         else if (command.equalsIgnoreCase("list"))
         {
            if (curArg == args.length)
            {
               System.out.println(client.list());
            }
            else
            {
               String complated = args[curArg++];

               if (complated.equalsIgnoreCase("completed"))
               {
                  if (curArg < args.length)
                  {
                     System.out.println(TOO_MANY_PARAMS);
                     return;
                  }
                  System.out.println(client.listCompleted());
               }
               else
               {
                  System.out.println(INCORRECT_PARAM + "There is no 'completed' parameter - " + complated);
                  return;
               }
            }

         }
         else if (command.equalsIgnoreCase("restore"))
         {

            String pathToWS = getRepoWS(args, curArg++);
            if (pathToWS == null)
               return;

            String repositoryName = getRepositoryName(pathToWS);
            String workspaceName = (pathToWS.split("/").length == 3 ? getWorkspaceName(pathToWS) : null);

            // backup id
            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no backup identifier parameter.");
               return;
            }
            String backupId = args[curArg++];

            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no path to config file parameter.");
               return;
            }
            String pathToConf = args[curArg++];

            File conf = new File(pathToConf);
            if (!conf.exists())
            {
               System.out.println(" File " + pathToConf + " do not exist. Check the path.");
               return;
            }

            if (curArg < args.length)
            {
               System.out.println(TOO_MANY_PARAMS);
               return;
            }
            System.out.println(client.restore(repositoryName, workspaceName, backupId, new FileInputStream(conf)));
         }
         else
         {
            System.out.println("Unknown command <" + command + ">");
         }

      }
      catch (IOException e)
      {
         System.out.println("ERROR: " + e.getMessage());
         e.printStackTrace();
      }
      catch (BackupExecuteException e)
      {
         System.out.println("ERROR: " + e.getMessage());
         e.printStackTrace();
      }
   }

   /**
    * getWorkspaceName.
    * 
    * @param pathToWS
    *          the /<repository-name>/<workspace name>
    * @return String return the workspace name
    */
   private static String getWorkspaceName(String pathToWS)
   {
      return pathToWS.split("/")[2];
   }

   /**
    * getRepositoryName.
    * 
    * @param pathToWS
    *          the /<repository-name>/<workspace name>
    * @return String return the repository name
    */
   private static String getRepositoryName(String pathToWS)
   {
      return pathToWS.split("/")[1];
   }

   /**
    * Get parameter from argument list, check it and return as valid path to repository and
    * workspace.
    * 
    * @param args
    *          list of arguments.
    * @param curArg
    *          argument index.
    * @return String valid path.
    */
   private static String getRepoWS(String[] args, int curArg)
   {
      if (curArg == args.length)
      {
         System.out.println(INCORRECT_PARAM + "There is no path to workspace parameter.");
         return null;
      }
      // make correct path
      String repWS = args[curArg];
      repWS = repWS.replaceAll("\\\\", "/");

      if ( !repWS.matches("[/][^/]+") && !repWS.matches("[/][^/]+[/][^/]+"))
      {
         System.out.println(INCORRECT_PARAM + "There is incorrect path to workspace parameter: " + repWS);
         return null;
      }
      else
      {
         return repWS;
      }
   }
}
