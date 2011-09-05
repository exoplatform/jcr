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
package org.exoplatform.services.jcr.ext.replication.external;

import org.exoplatform.common.http.client.CookieModule;
import org.exoplatform.common.http.client.HTTPConnection;
import org.exoplatform.common.http.client.HTTPResponse;

import java.net.URL;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BasicAuthenticationHttpClient.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class BasicAuthenticationHttpClient
{
   private HTTPConnection connection;

   private long waitTime = 0;

   private final String ipAdress;

   private final int port;

   private final String login;

   private final String password;

   public BasicAuthenticationHttpClient(String ipAdress, int port, String login, String password)
   {
      this.ipAdress = ipAdress;
      this.port = port;
      this.login = login;
      this.password = password;
   }

   public BasicAuthenticationHttpClient(MemberInfo info)
   {
      this(info.getIpAddress(), info.getPort(), info.getLogin(), info.getPassword());
   }

   public BasicAuthenticationHttpClient(MemberInfo info, long waitTime)
   {
      this(info.getIpAddress(), info.getPort(), info.getLogin(), info.getPassword());
      this.waitTime = waitTime;
   }

   public String execute(String sURL)
   {
      String result = "fail";

      try
      {
         Thread.sleep(waitTime);

         // execute the GET
         URL url = new URL(sURL);
         connection = new HTTPConnection(url);
         connection.removeModule(CookieModule.class);

         connection.addBasicAuthorization(BaseTestCaseChecker.TEST_REALM, login, password);

         HTTPResponse resp = connection.Get(url.getFile());

         // print the status and response
         if (resp.getStatusCode() != 200)
            System.out.println(resp.getStatusCode() + "\n" + resp.getText());

         result = resp.getText();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      return result;
   }
}
