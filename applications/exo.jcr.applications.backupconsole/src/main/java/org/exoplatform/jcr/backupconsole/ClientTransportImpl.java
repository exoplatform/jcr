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

import org.exoplatform.common.http.client.AuthorizationHandler;
import org.exoplatform.common.http.client.AuthorizationInfo;
import org.exoplatform.common.http.client.CookieModule;
import org.exoplatform.common.http.client.HTTPConnection;
import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.common.http.client.ModuleException;
import org.exoplatform.common.http.client.NVPair;

import java.io.IOException;
import java.net.URL;

/**
 * Created by The eXo Platform SAS. <br/>Date:
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: ClientTransportImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class ClientTransportImpl implements ClientTransport
{

   /**
    * String form - hostIP:port.
    */
   private final String host;

   /**
    * Login.
    */
   private final String login;

   /**
    * Password.
    */
   private final String password;

   /**
    * Flag is SSL.
    */
   private final String protocol;
   
   /**
    * Is realm get.
    */
   private boolean isRealmGet = false;
   
   /**
    * Realm to connection
    */
   private String realm;

   /**
    * Constructor.
    * 
    * @param login Login string.
    * @param password Password string.
    * @param host host string.
    * @param isSSL isSSL flag.
    */
   public ClientTransportImpl(String login, String password, String host, String protocol)
   {
      this.host = host;
      this.login = login;
      this.password = password;
      this.protocol = protocol;
   }

   /**
    * Get realm by URL.
    * 
    * @param sUrl URL string.
    * @return realm name string.
    * @throws IOException transport exception.
    * @throws ModuleException ModuleException.
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

   /**
    * {@inheritDoc}
    */
   public BackupAgentResponse executePOST(String sURL, String postData) throws IOException, BackupExecuteException
   {
      try
      {
         // execute the POST
         String complURL = protocol + "://" + host + sURL;

         URL url = new URL(complURL);
         HTTPConnection connection = new HTTPConnection(url);
         connection.removeModule(CookieModule.class);

         if (!isRealmGet)
         {
            realm = getRealm(complURL);
            isRealmGet = true;
         }
         
         connection.addBasicAuthorization(realm, login, password);

         HTTPResponse resp;
         if (postData == null)
         {
            resp = connection.Post(url.getFile());
         }
         else
         {
            NVPair[] pairs = new NVPair[2];
            pairs[0] = new NVPair("Content-Type", "application/json; charset=UTF-8");
            pairs[1] = new NVPair("Content-Length", Integer.toString(postData.length()));

            resp = connection.Post(url.getFile(), postData.getBytes(), pairs);
         }

         BackupAgentResponse responce = new BackupAgentResponse(resp.getData(), resp.getStatusCode());
         return responce;
      }
      catch (ModuleException e)
      {
         throw new BackupExecuteException(e.getMessage(), e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public BackupAgentResponse executeGET(String sURL) throws IOException, BackupExecuteException
   {
      try
      {
         // execute the POST
         String complURL = protocol + "://" + host + sURL;

         URL url = new URL(complURL);
         HTTPConnection connection = new HTTPConnection(url);
         connection.removeModule(CookieModule.class);

         if (!isRealmGet)
         {
            realm = getRealm(complURL);
            isRealmGet = true;
         }
         
         connection.addBasicAuthorization(realm, login, password);

         HTTPResponse resp = connection.Get(url.getFile());

         BackupAgentResponse responce = new BackupAgentResponse(resp.getData(), resp.getStatusCode());
         return responce;
      }
      catch (ModuleException e)
      {
         throw new BackupExecuteException(e.getMessage(), e);
      }

   }

}
