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
import java.util.HashMap;

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
            "Help info:\n"
                     + " <url_basic_authentication>|<url form authentication>  <cmd> \n"
                     + " <url_basic_authentication>  :   http(s)//login:password@host:port/<context> \n\n"
                     + " <url form authentication>   :   http(s)//host:port/<context> \"<form auth parm>\" \n"
                     + "     <form auth parm>        :   form <method> <form path>\n"
                     + "     <method>                :   POST or GET\n"
                     + "     <form path>             :   /path/path?<paramName1>=<paramValue1>&<paramName2>=<paramValue2>...\n"
         + "     Example to <url form authentication> : http://127.0.0.1:8080/portal/rest form POST \"/portal/login?username=root&password=gtn\"\n\n"

         + " <cmd>  :   start <repo[/ws]> <backup_dir> [<incr>] \n" 
         + "            stop <backup_id> \n"
         + "            status <backup_id> \n" 
         + "            restores <repo[/ws]> \n"
         + "            restore [remove-exists] [<repo[/ws]>] {<backup_id>|<backup_set_path>} [<pathToConfigFile>] \n"
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

         + " <repo[/ws]>         - /<reponsitory-name>[/<workspace-name>]  the repository or workspace \n"
         + " <backup_dir>        - path to folder for backup on remote server \n"
         + " <backup_id>         - the identifier for backup \n"
         + " <backup_set_dir>    - path to folder with backup set on remote server\n"
         + " <incr>              - incemental job period \n"
         + " <pathToConfigFile>  - path (local) to  repository or workspace configuration \n"
                     + " remove-exists       - remove fully (db, value storage, index) exists repository/workspace \n"
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

      // try form
      String form = null;
      if (curArg < args.length)
      {
         if (args[curArg].equals("form"))
         {
            form = args[curArg++];

            if (url.getUserInfo() != null)
            {  
               System.out.println(INCORRECT_PARAM + "Parameters Login:Password should not be specified in url parameter to form authentication - " + sUrl);
               return;
            }
         }
      }

      // login:password
      String login = url.getUserInfo();
      
      FormAuthentication formAuthentication = null;
      if (form != null && form.equals("form"))
      {
         //check POST or GET
         if (curArg == args.length)
         {
            System.out.println(INCORRECT_PARAM + "No specified  POST or GET parameter to form parameter.");
            return;
         }
         String method = args[curArg++];

         if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("POST"))
         {
            System.out.println(INCORRECT_PARAM + "Method to form authentication shulde be GET or POST to form parameter - " + method);
            return;
         }
         
         //url to form authentication
         if (curArg == args.length)
         {
            System.out.println(INCORRECT_PARAM + "No specified  url and form properties to form parameter.");
            return;
         }
         String[] params = args[curArg++].split("[?]");
         
         if (params.length != 2)
         {
            System.out.println(INCORRECT_PARAM + "From parameters is not spacified to form parameter - " + args[curArg]);
            return;
         }
         String formUrl = params[0];

         // parameters to form
         String[] formParams = params[1].split("&");

         if (formParams.length < 2)
         {
            System.out.println(INCORRECT_PARAM
                     + "From parameters shoulde be conatains at least two (for login and for pasword) parameters - "
                     + params[1]);
            return;
         }
         
         HashMap<String, String> mapFormParams = new HashMap<String, String>();

         for (String fParam : formParams)
         {
            String[] para = fParam.split("=");
            
            if (para.length != 2)
            {
               System.out.println(INCORRECT_PARAM + "From parameters is incorect, shoulde be as \"name=value\"  - " + fParam);
               return;
            }
            
            mapFormParams.put(para[0], para[1]);
         }
         
         formAuthentication = new FormAuthentication(method, formUrl, mapFormParams);
      }
      else
      {
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
      }

      String host = url.getHost() + ":" + url.getPort();

      // initialize transport and backup client
      ClientTransport transport;
      BackupClient client;
      
      if (formAuthentication != null)
      {
         transport = new ClientTransportImpl(formAuthentication, host, url.getProtocol());
         client = new BackupClientImpl(transport, formAuthentication, urlPath);
      }
      else
      {
         String[] lp = login.split(LOGIN_PASS_SPLITTER);
         transport = new ClientTransportImpl(lp[0], lp[1], host, url.getProtocol());
         client = new BackupClientImpl(transport, urlPath);
      }

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

            /*
            All valid combination of paramter.  
            1. restore remove-exists <repo/ws> <backup_id>       <pathToConfigFile>
            2. restore remove-exists <repo>    <backup_id>       <pathToConfigFile>
            3. restore remove-exists <repo/ws> <backup_set_path> <pathToConfigFile>
            4. restore remove-exists <repo>    <backup_set_path> <pathToConfigFile>
            5. restore remove-exists <backup_id>       
            6. restore remove-exists <backup_set_path> 
            7. restore <repo/ws> <backup_id>       <pathToConfigFile>
            8. restore <repo>    <backup_id>       <pathToConfigFile>
            9. restore <repo/ws> <backup_set_path> <pathToConfigFile>
            10. restore <repo>    <backup_set_path> <pathToConfigFile>
            11. restore <backup_id>       
            12. restore <backup_set_path> 
            */
            
            boolean removeExists = false;
            String backupSetPath = null;
            String backupId = null;
            String pathToWS = null;
            String repositoryName = null;
            String workspaceName = null;

            String parameter = args[curArg++];

            //check remove-exists
            if (parameter.equals("remove-exists"))
            {
               removeExists = true;

               if (curArg == args.length)
               {
                  System.out.println(INCORRECT_PARAM + "Should be more parameters.");
                  return;
               }
            }

            if (removeExists)
            {
               parameter = args[curArg++];
            }

            //check backup_id
            if (isBackupId(parameter))
            {
               backupId = parameter;
               curArg++;

               if (curArg < args.length)
               {
                  System.out.println(TOO_MANY_PARAMS);
                  return;
               }

               //5. restore remove-exists <backup_id>
               //11. restore <backup_id>
               System.out.println(client.restore(repositoryName, workspaceName, backupId, null,
                        backupSetPath, removeExists));
               return;
            }
            //check /repo/ws or /repo
            else if (isRepoWS(parameter))
            {
               pathToWS = getRepoWS(args, curArg - 1);
               if (pathToWS == null)
                  return;

               repositoryName = getRepositoryName(pathToWS);
               workspaceName = (pathToWS.split("/").length == 3 ? getWorkspaceName(pathToWS) : null);
            }
            // this is backup_set_path
            else
            {
               backupSetPath = parameter;

               if (curArg < args.length)
               {
                  System.out.println(INCORRECT_PARAM + "Should be less parameters : " + parameter);
                  return;
               }

               //6. restore remove-exists <backup_set_path>
               //12. restore <backup_set_path>
               System.out.println(client.restore(repositoryName, workspaceName, backupId, null, backupSetPath,
                        removeExists));
               return;
            }

            // check backup_id or backup_set_path
            if (curArg == args.length)
            {
               System.out.println(INCORRECT_PARAM + "There is no backup identifier or backup set path parameter.");
               return;
            }
            parameter = args[curArg++];

            if (isBackupId(parameter))
            {
               backupId = parameter;
            }
            else
            {
               backupSetPath = parameter;
            }

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

            /*
            1. restore remove-exists <repo/ws> <backup_id>       <pathToConfigFile>
            2. restore remove-exists <repo>    <backup_id>       <pathToConfigFile>
            3. restore remove-exists <repo/ws> <backup_set_path> <pathToConfigFile>
            4. restore remove-exists <repo>    <backup_set_path> <pathToConfigFile>
            7. restore <repo/ws> <backup_id>       <pathToConfigFile>
            8. restore <repo>    <backup_id>       <pathToConfigFile>
            9. restore <repo/ws> <backup_set_path> <pathToConfigFile>
            10. restore <repo>    <backup_set_path> <pathToConfigFile>
            */
            System.out.println(client.restore(repositoryName, workspaceName, backupId, new FileInputStream(conf),
                     backupSetPath, removeExists));
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

      System.exit(0);
   }

   /**
    * Check is "/repo" or "/repo/ws" parameter.
    * 
    * @param parameter
    *          String, parameter.
    * @return Boolean
    *           return "true" if it "/repo" or "/repo/ws" parameter
    */
   private static boolean isRepoWS(String parameter)
   {
      String repWS = parameter;
      repWS = repWS.replaceAll("\\\\", "/");

      if ( !repWS.matches("[/][^/]+") && !repWS.matches("[/][^/]+[/][^/]+"))
      {
         return false;
      }
      else
      {
         return true;
      }
   }

   /**
    * Check is backip_id parameter.
    * 
    * @param firstParameter
    *          String, parameter.
    * @return Boolean
    *           return "true" if it backup identifier parameter
    */
   private static boolean isBackupId(String parameter)
   {
      return parameter.matches("[0-9abcdef]+") && parameter.length() == 32;
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
