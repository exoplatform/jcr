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

import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS. <br>Date:
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
    * Realm to connection
    */
   private String realm = null;
   
   /**
    * Form authentication parameters.
    */
   private FormAuthentication formAuthentication;

   /**
    * Constructor.
    * 
    * @param login Login string.
    * @param password Password string.
    * @param host host string.
    */
   public ClientTransportImpl(String login, String password, String host, String protocol)
   {
      this.host = host;
      this.login = login;
      this.password = password;
      this.protocol = protocol;
   }

   /**
    * Constructor.

    * @param formAuthentication form authentication parameters.
    * @param host host string.
    * @param protocol host string.
    */
   public ClientTransportImpl(FormAuthentication formAuthentication, String host, String protocol)
   {
      this(null, null, host, protocol);
      this.formAuthentication = formAuthentication;
   }

   /**
    * Get realm by URL.
    * 
    * @param sUrl URL string.
    * @return realm name or null.
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
         if (authHeader == null)
         {
            return null;
         }

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
         connection.setAllowUserInteraction(false);

         //authentication
         if (formAuthentication != null)
         {
            //form authentication 
            HTTPResponse respLogin;

            URL urlLogin = new URL(protocol + "://" + host + formAuthentication.getFormPath());

            HTTPConnection connectionLogin = new HTTPConnection(urlLogin);
            connectionLogin.setAllowUserInteraction(false);

            NVPair[] formParams = new NVPair[formAuthentication.getFormParams().size()];
            int pairCount = 0;
            for (String key : formAuthentication.getFormParams().keySet())
            {
               formParams[pairCount++] = new NVPair(key, formAuthentication.getFormParams().get(key));
            }

            if ("POST".equalsIgnoreCase(formAuthentication.getMethod()))
            {
               respLogin = connectionLogin.Post(urlLogin.getFile(), formParams);
            }
            else
            {
               respLogin = connectionLogin.Get(urlLogin.getFile(), formParams);
            }

            if (Response.Status.OK.getStatusCode() != respLogin.getStatusCode())
            {
               System.out.println("Form authentication is fail, status code : " + respLogin.getStatusCode()); //NOSONAR
               System.exit(0);
            }
         }
         else
         {
            // basic authorization
            if (realm == null)
            {
               realm = getRealm(complURL);
               if (realm == null)
               {
                  throw new BackupExecuteException(
                     "Can not connect to server using basic authentication. Try to use form authentication.");
               }
            }

            connection.addBasicAuthorization(realm, login, password);
         }

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
         // execute the GET
         String complURL = protocol + "://" + host + sURL;

         URL url = new URL(complURL);
         HTTPConnection connection = new HTTPConnection(url);
         connection.setAllowUserInteraction(false);

         //authentication
         if (formAuthentication != null)
         {
            //form authentication 
            HTTPResponse respLogin;

            URL urlLogin = new URL(protocol + "://" + host + formAuthentication.getFormPath());

            HTTPConnection connectionLogin = new HTTPConnection(urlLogin);
            connectionLogin.setAllowUserInteraction(false);

            NVPair[] formParams = new NVPair[formAuthentication.getFormParams().size()];
            int pairCount = 0;
            for (String key : formAuthentication.getFormParams().keySet())
            {
               formParams[pairCount++] = new NVPair(key, formAuthentication.getFormParams().get(key));
            }

            if ("POST".equalsIgnoreCase(formAuthentication.getMethod()))
            {
               respLogin = connectionLogin.Post(urlLogin.getFile(), formParams);
            }
            else
            {
               respLogin = connectionLogin.Get(urlLogin.getFile(), formParams);
            }

            if (Response.Status.OK.getStatusCode() != respLogin.getStatusCode())
            {
               System.out.println("Form authentication is fail, status code : " + respLogin.getStatusCode()); //NOSONAR
               System.exit(0);
            }
         }
         else
         {
            // basic authorization
            if (realm == null)
            {
               realm = getRealm(complURL);
               if (realm == null)
               {
                  throw new BackupExecuteException(
                     "Can not connect to server using basic authentication. Try to use form authentication.");
               }
            }

            connection.addBasicAuthorization(realm, login, password);
         }

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
