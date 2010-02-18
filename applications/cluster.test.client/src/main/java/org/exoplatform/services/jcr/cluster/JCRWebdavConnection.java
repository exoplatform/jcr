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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.cluster;

import org.exoplatform.common.http.client.CookieModule;
import org.exoplatform.common.http.client.HTTPConnection;
import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.common.http.client.HttpOutputStream;
import org.exoplatform.common.http.client.ModuleException;
import org.exoplatform.common.http.client.NVPair;
import org.exoplatform.services.rest.ExtHttpHeaders;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2009
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class JCRWebdavConnection extends HTTPConnection
{
   private String realm;

   private String user;

   private String pass;

   private String workspacePath;

   public JCRWebdavConnection(String host, int port, String user, String password, String realm, String workspacePath)
   {
      super(host, port);

      CookieModule.setCookiePolicyHandler(null);

      this.user = user;
      this.pass = password;
      this.realm = realm;
      this.workspacePath = workspacePath;

      addBasicAuthorization(this.realm, this.user, this.pass);
   }

   public HTTPResponse addNode(String name, byte[] data) throws IOException, ModuleException
   {
      HTTPResponse response = Put(workspacePath + name, data);
      response.getStatusCode();
      return response;
   }

   public void addNode(String name, String nodeType, byte[] data) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair("File-NodeType", nodeType);
      Put(workspacePath + name, data, headers).getStatusCode();
   }

   /**
    * Adds node with given mixin types.
    * 
    * @param name
    * @param nodeType
    * @param mixinTypes
    * @param data
    * @return
    * @throws IOException
    * @throws ModuleException
    */
   public HTTPResponse addNode(String name, String[] mixinTypes, byte[] data) throws IOException, ModuleException
   {
      // construct string containing mixins in comma separated format
      String mixins = mixinTypes.length > 0 ? mixinTypes[0] : "";
      for (int i = 1; i < mixinTypes.length; i++)
      {
         mixins = mixins + ", " + mixinTypes[i];
      }
      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(ExtHttpHeaders.CONTENT_MIXINTYPES, mixins);
      headers[1] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/plain");
      HTTPResponse response = Put(workspacePath + name, data, headers);
      response.getStatusCode();
      return response;
   }

   /**
    * Adds node (nt:file) with given mimetype.
    * 
    * @param name
    * @param data
    * @param mimeType
    * @return
    * @throws IOException
    * @throws ModuleException
    */
   public HTTPResponse addNode(String name, byte[] data, String mimeType) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, mimeType);
      HTTPResponse response = Put(workspacePath + name, data, headers);
      response.getStatusCode();
      return response;
   }

   public HTTPResponse addNode(String name, HttpOutputStream stream) throws IOException, ModuleException
   {
      return Put(workspacePath + name, stream);
   }

   public HTTPResponse removeNode(String name) throws IOException, ModuleException
   {
      HTTPResponse response = Delete(workspacePath + name);
      response.getStatusCode();

      return response;
   }

   /*public void getNode(String name) throws IOException, ModuleException
   {
      Get(workspacePath + name).getStatusCode();
   }*/

   public HTTPResponse getNode(String name) throws IOException, ModuleException
   {
      HTTPResponse response = Get(workspacePath + name);
      response.getStatusCode();
      return response;
   }

   public HTTPResponse addProperty(String nodeName, String property) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?>" + "<D:propertyupdate xmlns:D='DAV:'>" + "<D:set>" + "<D:prop>" + "<"
            + property + ">value</" + property + ">" + "</D:prop>" + "</D:set>" + "</D:propertyupdate>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("PROPPATCH", workspacePath + nodeName, xmlBody.getBytes(), headers);
      response.getStatusCode();

      return response;
   }

   public HTTPResponse setProperty(String nodeName, String property, String value) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?>" + "<D:propertyupdate xmlns:D='DAV:'>" + "<D:set>" + "<D:prop>" + "<"
            + property + ">" + value + "</" + property + ">" + "</D:prop>" + "</D:set>" + "</D:propertyupdate>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("PROPPATCH", workspacePath + nodeName, xmlBody.getBytes(), headers);
      response.getStatusCode();

      return response;
   }

   public HTTPResponse getProperty(String nodeName, String property) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?>" + "<D:propfind xmlns:D='DAV:' >" + "<D:prop><" + property
            + "/></D:prop>" + "</D:propfind>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("PROPFIND", workspacePath + nodeName, xmlBody.getBytes(), headers);
      response.getStatusCode();

      return response;
   }

   public HTTPResponse removeProperty(String nodeName, String property) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?>"
            + "<D:propertyupdate xmlns:D='DAV:' xmlns:Z='http://www.w3.com/standards/z39.50/'>" + "<D:remove>"
            + "<D:prop><" + property + "/></D:prop>" + "</D:remove>" + "</D:propertyupdate>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("PROPPATCH", workspacePath + nodeName, xmlBody.getBytes(), headers);
      response.getStatusCode();

      return response;
   }

   public String lock(String nodeName) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?>" + "<D:lockinfo xmlns:D='DAV:'>" + "<D:lockscope>" + "<D:exclusive/>"
            + "</D:lockscope>" + "<D:locktype>" + "<D:write/>" + "</D:locktype>" + "<D:owner>owner</D:owner>"
            + "</D:lockinfo>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("LOCK", workspacePath + nodeName, xmlBody.getBytes(), headers);

      response.getStatusCode();
      StringBuffer resp = new StringBuffer(new String(response.getData(), "UTF-8"));

      final String lockPrffix = "opaquelocktoken:";

      int pos = resp.lastIndexOf(lockPrffix);

      String lockToken = resp.substring(pos + lockPrffix.length(), pos + lockPrffix.length() + 32);

      return lockToken;
   }

   public void unlock(String nodeName, String lockToken) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[3];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString("".length()));
      headers[2] = new NVPair(ExtHttpHeaders.LOCKTOKEN, "<" + lockToken + ">");

      HTTPResponse response = ExtensionMethod("UNLOCK", workspacePath + nodeName, "".getBytes(), headers);
      response.getStatusCode();
   }

   public void addVersionControl(String nodeName) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[1];
      //      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[0] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString("".length()));

      HTTPResponse response = ExtensionMethod("VERSION-CONTROL", workspacePath + nodeName, "".getBytes(), headers);
      response.getStatusCode();
   }

   public void checkIn(String nodeName) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString("".length()));

      HTTPResponse response = ExtensionMethod("CHECKIN", workspacePath + nodeName, "".getBytes(), headers);
      response.getStatusCode();
   }

   public void checkOut(String nodeName) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[1];
      headers[0] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString("".length()));

      HTTPResponse response = ExtensionMethod("CHECKOUT", workspacePath + nodeName, "".getBytes(), headers);
      response.getStatusCode();
   }

   /**
    * Return true if create successfully
    * @param path
    * @throws IOException
    * @throws ModuleException
    */
   public HTTPResponse addDir(String path) throws IOException, ModuleException
   {
      HTTPResponse mkCol = MkCol(workspacePath + path);
      mkCol.getStatusCode();
      return mkCol;
   }

   public HTTPResponse restore(String node, String version) throws IOException, ModuleException
   {
      NVPair[] query = new NVPair[1];
      query[0] = new NVPair("version", version);

      HTTPResponse response = Get(workspacePath + node, query);
      response.getStatusCode();

      return response;
   }

   public HTTPResponse moveNode(String path, String destination) throws IOException, ModuleException
   {
      NVPair[] headers = new NVPair[2];
      headers[0] =
         new NVPair(ExtHttpHeaders.DESTINATION, this.getProtocol() + "://" + this.getHost() + ":" + this.getPort()
            + workspacePath + destination);
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString("".length()));

      HTTPResponse response = ExtensionMethod("MOVE", workspacePath + path, "".getBytes(), headers);
      response.getStatusCode();

      return response;
   }

   /**
    * Performs XPath query on workspace and returns plain HTTPResponse. It should be returned with status 207 
    * and must contain the XML with node collection. 
    * 
    * @param query
    * @return
    * @throws IOException
    * @throws ModuleException
    */
   public HTTPResponse xpathQuery(String query) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?><D:searchrequest xmlns:D='DAV:'>" + "<D:xpath>" + query + "</D:xpath>"
            + "</D:searchrequest>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("SEARCH", workspacePath, xmlBody.getBytes(), headers);
      response.getStatusCode();

      return response;
   }

   /**
    * Performs SQL query on workspace and returns plain HTTPResponse. It should be returned with status 207 
    * and must contain the XML with node collection. 
    * 
    * @param query
    * @return
    * @throws IOException
    * @throws ModuleException
    */
   public HTTPResponse sqlQuery(String query) throws IOException, ModuleException
   {
      String xmlBody =
         "<?xml version='1.0' encoding='utf-8' ?><D:searchrequest xmlns:D='DAV:'>" + "<D:sql>" + query + "</D:sql>"
            + "</D:searchrequest>";

      NVPair[] headers = new NVPair[2];
      headers[0] = new NVPair(HttpHeaders.CONTENT_TYPE, "text/xml; charset='utf-8'");
      headers[1] = new NVPair(HttpHeaders.CONTENT_LENGTH, Integer.toString(xmlBody.length()));

      HTTPResponse response = ExtensionMethod("SEARCH", workspacePath, xmlBody.getBytes(), headers);
      response.getStatusCode();

      return response;
   }

}