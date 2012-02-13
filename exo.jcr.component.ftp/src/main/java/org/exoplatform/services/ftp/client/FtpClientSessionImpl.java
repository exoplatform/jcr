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
package org.exoplatform.services.ftp.client;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.ftp.FtpServer;
import org.exoplatform.services.ftp.data.FtpDataTransiver;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.login.LoginException;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly <gavrik-vetal@ukr.net/mail.ru>
 * 
 * @version $Id: $
 */

public class FtpClientSessionImpl implements FtpClientSession
{

   private static Log log = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpClientSessionImpl");

   private FtpServer ftpServer;

   FtpClientCommandThread commandThread;

   FtpClientTimeOutThread timeOutThread;

   private FtpDataTransiver transiver = null;

   private Socket clientSocket;

   // private PrintStream outPrintStream;

   private SessionProvider sessionFactory;

   private ArrayList<String> path = new ArrayList<String>();

   private String serverIp;

   private String userName = "";

   private String userPass = "";

   private boolean logged = false;

   private String prevCommand = "";

   private String prevParams = "";

   private String prevParamsEx = "";

   private String userId;

   public FtpClientSessionImpl(FtpServer ftpServer, Socket clientSocket) throws Exception
   {
      this.ftpServer = ftpServer;
      this.clientSocket = clientSocket;
      // outPrintStream = new PrintStream(clientSocket.getOutputStream());

      SocketAddress addr = clientSocket.getLocalSocketAddress();
      String serverAddr = addr.toString();
      if (serverAddr.startsWith("/"))
      {
         serverAddr = serverAddr.substring(1);
      }
      String[] serverLocations = serverAddr.split(":");
      serverIp = serverLocations[0];

      welcomeClient();

      commandThread = new FtpClientCommandThread(this);
      commandThread.start();

      if (getFtpServer().getConfiguration().isNeedTimeOut())
      {
         timeOutThread = new FtpClientTimeOutThread(this);
         timeOutThread.start();
      }
   }

   public Socket getClientSocket()
   {
      return clientSocket;
   }

   public void reply(String replyString) throws IOException
   {
      String encodingType = ftpServer.getConfiguration().getClientSideEncoding();
      try
      {
         byte[] data = replyString.getBytes(encodingType);
         // outPrintStream.println(new String(, encodingType));
         clientSocket.getOutputStream().write(data);
      }
      catch (UnsupportedEncodingException eexc)
      {
         log.info("Unsupported encoding exception. See for CLIENT-SIDE-ENCODING parameter. " + eexc.getMessage(), eexc);
         byte[] data = replyString.getBytes();
         clientSocket.getOutputStream().write(data);
         // outPrintStream.println(replyString);
      }
      clientSocket.getOutputStream().write("\r\n".getBytes());
   }

   public FtpServer getFtpServer()
   {
      return ftpServer;
   }

   protected void welcomeClient() throws IOException
   {
      for (int i = 0; i < FtpConst.EXO_LOGO.length; i++)
      {
         reply(FtpConst.EXO_LOGO[i]);
      }
   }

   private boolean isLoggedOut = false;

   public void logout()
   {
      if (isLoggedOut)
      {
         return;
      }
      isLoggedOut = true;

      commandThread.interrupt();
      if (timeOutThread != null)
      {
         timeOutThread.interrupt();
      }

      closeDataTransiver();

      try
      {
         clientSocket.close();
      }
      catch (IOException exc)
      {
         log.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      getFtpServer().unRegisterClient(this);

      if (sessionFactory != null)
      {
         sessionFactory.close();
      }

      this.unRegistrateIdentity();
   }

   public boolean isLogged()
   {
      return logged;
   }

   public void setUserName(String userName)
   {
      this.userName = userName;
      logged = false;
   }

   public void setPassword(String userPass) throws Exception
   {
      this.userPass = userPass;

      if (sessionFactory != null)
      {
         sessionFactory.close();
      }

      ConversationState state = getConversationState();
      ConversationState.setCurrent(state);
      this.sessionFactory = new SessionProvider(state);

      logged = true;
   }

   public String getUserName()
   {
      return userName;
   }

   public String getUserPassword()
   {
      return userPass;
   }

   public String getServerIp()
   {
      return serverIp;
   }

   public void setDataTransiver(FtpDataTransiver newTransiver)
   {
      if (transiver != null)
      {
         transiver.close();
      }
      transiver = newTransiver;
   }

   public void closeDataTransiver()
   {
      if (transiver != null)
      {
         transiver.close();
         transiver = null;
      }
   }

   public FtpDataTransiver getDataTransiver()
   {
      return transiver;
   }

   public void setPrevCommand(String prevCommand)
   {
      this.prevCommand = prevCommand;
   }

   public void setPrevParams(String prevParams)
   {
      this.prevParams = prevParams;
   }

   public void setPrevParamsEx(String prevParamsEx)
   {
      this.prevParamsEx = prevParamsEx;
   }

   public String getPrevCommand()
   {
      return prevCommand;
   }

   public String getPrevParams()
   {
      return prevParams;
   }

   public String getPrevParamsEx()
   {
      return prevParamsEx;
   }

   public ArrayList<String> getFullPath(String resPath)
   {
      ArrayList<String> curPath = getPath();
      if (resPath.startsWith("/"))
      {
         curPath.clear();
      }

      String[] pathes = resPath.split("/");
      for (int i = 0; i < pathes.length; i++)
      {
         if (!"".equals(pathes[i]))
         {
            if ("..".equals(pathes[i]))
            {
               if (curPath.size() != 0)
               {
                  curPath.remove(curPath.size() - 1);
               }
            }
            else
            {
               curPath.add(pathes[i]);
            }
         }
      }
      return curPath;
   }

   public String getRepoPath(ArrayList<String> repoPath)
   {
      StringBuilder curPath = new StringBuilder("/");
      for (int i = 1; i < repoPath.size(); i++)
      {
         curPath.append(repoPath.get(i));
         if (i < (repoPath.size() - 1))
         {
            curPath.append("/");
         }
      }
      return curPath.toString();
   }

   public Session getSession(String workspaceName) throws Exception
   {
      if (ftpServer.getRepository() == null)
      {
         throw new RepositoryException("Repository can not be retrieved.");
      }
      Session curSession = sessionFactory.getSession(workspaceName, ftpServer.getRepository());
      curSession.refresh(false);
      return curSession;
   }

   public String changePath(String resPath)
   {
      ArrayList<String> newPath = getFullPath(resPath);

      if (newPath.size() == 0)
      {
         path = new ArrayList<String>();
         return FtpConst.Replyes.REPLY_250;
      }

      String repoWorkspace = newPath.get(0);
      String repoPath = getRepoPath(newPath);

      try
      {
         Session curSession = getSession(repoWorkspace);

         Node curNode = (Node)curSession.getItem(repoPath);
         if (curNode.isNodeType(FtpConst.NodeTypes.NT_FILE))
         {
            return FtpConst.Replyes.REPLY_550;
         }

         path = (ArrayList<String>)newPath.clone();
         return FtpConst.Replyes.REPLY_250;
      }
      catch (RepositoryException exc)
      {
         return FtpConst.Replyes.REPLY_550;
      }
      catch (Exception exc)
      {
         log.info("Unhandled exception. " + exc.getMessage(), exc);
      }

      return FtpConst.Replyes.REPLY_550;
   }

   public ArrayList<String> getPath()
   {
      return (ArrayList<String>)path.clone();
   }

   public void refreshTimeOut()
   {
      if (timeOutThread != null)
      {
         timeOutThread.refreshTimeOut();
      }
   }

   private ConversationState getConversationState() throws Exception
   {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      Authenticator authenticator = (Authenticator)container.getComponentInstanceOfType(Authenticator.class);

      IdentityRegistry identityRegistry =
         (IdentityRegistry)container.getComponentInstanceOfType(IdentityRegistry.class);

      if (authenticator == null)
         throw new LoginException("No Authenticator component found, check your configuration");

      Credential[] credentials =
         new Credential[]{new UsernameCredential(this.userName), new PasswordCredential(this.userPass)};

      this.userId = authenticator.validateUser(credentials);
      Identity identity = authenticator.createIdentity(this.userId);
      identityRegistry.register(identity);

      ConversationState state = new ConversationState(identity);
      // keep subject as attribute in ConversationState
      state.setAttribute(ConversationState.SUBJECT, identity.getSubject());

      return state;
   }

   private void unRegistrateIdentity()
   {
      ConversationState.setCurrent(null);

      ExoContainer container = ExoContainerContext.getCurrentContainer();
      IdentityRegistry identityRegistry =
         (IdentityRegistry)container.getComponentInstanceOfType(IdentityRegistry.class);

      // The check need for case when login failed
      if (this.userId != null)
      {
         identityRegistry.unregister(this.userId);
      }
   }
}
