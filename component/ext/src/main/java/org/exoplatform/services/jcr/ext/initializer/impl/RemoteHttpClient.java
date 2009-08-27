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
package org.exoplatform.services.jcr.ext.initializer.impl;

import org.exoplatform.common.http.client.AuthorizationHandler;
import org.exoplatform.common.http.client.AuthorizationInfo;
import org.exoplatform.common.http.client.CookieModule;
import org.exoplatform.common.http.client.HTTPConnection;
import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.common.http.client.ModuleException;
import org.exoplatform.common.http.client.ParseException;
import org.exoplatform.services.jcr.ext.initializer.RemoteWorkspaceInitializationException;
import org.exoplatform.services.jcr.ext.initializer.RemoteWorkspaceInitializationService;

import java.io.IOException;
import java.net.URL;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 20.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RemoteHTTPClient.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class RemoteHttpClient
{

   /**
    * HTTP_OK. The http response 200.
    */
   private static final int HTTP_OK = 200;

   /**
    * Url to remote data source.
    */
   private final String dataSourceUrl;

   /**
    * RemoteHttpClient constructor.
    * 
    * @param dataSourceUrl
    *          the data source url
    */
   public RemoteHttpClient(String dataSourceUrl)
   {
      this.dataSourceUrl = dataSourceUrl;
   }

   /**
    * execute.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param id
    *          the channel id
    * @return String the response
    * @throws RemoteWorkspaceInitializationException
    *           will be generated the RemoteWorkspaceInitializerException
    */
   public String execute(String repositoryName, String workspaceName, String id)
      throws RemoteWorkspaceInitializationException
   {
      String result = "FAIL";

      try
      {
         // execute the GET

         String complURL =
            dataSourceUrl + RemoteWorkspaceInitializationService.Constants.BASE_URL + "/" + repositoryName + "/"
               + workspaceName + "/" + id + "/"
               + RemoteWorkspaceInitializationService.Constants.OperationType.GET_WORKSPACE;

         URL url = new URL(complURL);

         String userInfo = url.getUserInfo();
         if (userInfo == null || userInfo.split(":").length != 2)
            throw new RemoteWorkspaceInitializationException(
               "Fail remote initializetion : the user name or password not not specified : " + dataSourceUrl);

         String userName = userInfo.split(":")[0];
         String password = userInfo.split(":")[1];

         HTTPConnection connection = new HTTPConnection(url);
         connection.removeModule(CookieModule.class);

         String realmName = getRealm(complURL);
         connection.addBasicAuthorization(realmName, userName, password);

         HTTPResponse resp = connection.Get(url.getFile());

         result = resp.getText();

         AuthorizationInfo.removeAuthorization(url.getHost(), url.getPort(), "Basic", realmName);

         if (resp.getStatusCode() != HTTP_OK)
            throw new RemoteWorkspaceInitializationException("Fail remote initializetion : " + result);

      }
      catch (ModuleException e)
      {
         throw new RemoteWorkspaceInitializationException(e.getMessage(), e);
      }
      catch (ParseException e)
      {
         throw new RemoteWorkspaceInitializationException(e.getMessage(), e);
      }
      catch (IOException e)
      {
         throw new RemoteWorkspaceInitializationException(e.getMessage(), e);
      }

      return result;
   }

   /**
    * Get realm by URL.
    * 
    * @param sUrl
    *          URL string.
    * @return realm name string.
    * @throws IOException
    *           transport exception.
    * @throws ModuleException
    *           ModuleException.
    */
   private String getRealm(String sUrl) throws IOException, ModuleException
   {

      AuthorizationHandler ah = AuthorizationInfo.getAuthHandler();

      try
      {
         URL url = new URL(sUrl);
         HTTPConnection connection = new HTTPConnection(url);
         connection.removeModule(CookieModule.class);
         AuthorizationInfo.setAuthHandler(null);

         HTTPResponse resp = connection.Get(url.getFile());

         String authHeader = resp.getHeader("WWW-Authenticate");

         String realm = authHeader.split("=")[1];
         realm = realm.substring(1, realm.length() - 1);

         return realm;

      }
      finally
      {
         AuthorizationInfo.setAuthHandler(ah);
      }
   }
}
