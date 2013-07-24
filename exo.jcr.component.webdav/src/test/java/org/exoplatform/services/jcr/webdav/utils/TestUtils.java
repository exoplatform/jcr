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
package org.exoplatform.services.jcr.webdav.utils;

import org.exoplatform.common.http.client.HTTPConnection;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDav;
import org.exoplatform.services.jcr.webdav.command.propfind.PropFindResponseEntity;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 14, 2008
 */
public class TestUtils
{

   public static final String HOST = "localhost";

   public static final String SERVLET_PATH = "/rest/jcr/repository";

   public static final String WORKSPACE = "/ws";

   public static final String INAVLID_WORKSPACE = "/invalid";

   public static final String REALM = "eXo REST services";

   public static final String ROOTID = "root";

   public static final String ROOTPASS = "exo";

   public static HTTPConnection GetAuthConnection()
   {
      HTTPConnection connection = new HTTPConnection(HOST, WebDav.PORT_INT);
      connection.addBasicAuthorization(REALM, ROOTID, ROOTPASS);

      return connection;
   }

   public static String getFullPath()
   {
      return "http://" + HOST + ":" + WebDav.PORT_INT + SERVLET_PATH + WORKSPACE;
   }

   public static String getFullWorkSpacePath()
   {
      return SERVLET_PATH + WORKSPACE;
   }

   public static String getFullUri()
   {
      return "http://" + HOST + ":" + WebDav.PORT_INT + getFullWorkSpacePath();
   }

   public static String getFolderName()
   {
      return "/test-folder-" + System.currentTimeMillis();
   }

   public static String getFileName()
   {
      return "/test-file-" + System.currentTimeMillis() + ".txt";
   }

   public static Document getXmlFromString(String string) throws Exception
   {

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      InputStream inputStream = new ByteArrayInputStream(string.getBytes());
      return builderFactory.newDocumentBuilder().parse(inputStream);

   }

   public static String getFileContent()
   {
      String content = new String();
      for (int i = 0; i < 10; i++)
      {
         content += UUID.randomUUID().toString();
      }
      return content;
   }

   public static Node addContent(Session session, String path, InputStream inputStream, String nodeType, String mimeType)
      throws RepositoryException
   {
      return addContent(session, path, inputStream, nodeType, "nt:resource", mimeType);
   }

   public static Node addContent(Session session, String path, InputStream inputStream, String nodeType,
      String contentType, String mimeType) throws RepositoryException
   {
      Node node = session.getRootNode().addNode(TextUtil.relativizePath(path), nodeType);
      node.addNode("jcr:content", contentType);
      Node content = node.getNode("jcr:content");
      content.setProperty("jcr:mimeType", mimeType);
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", inputStream);
      session.save();
      return node;
   }

   public static void addFolder(Session session, String path, String nodeType, String mimeType)
      throws RepositoryException
   {
      session.getRootNode().addNode(TextUtil.relativizePath(path), nodeType);
      session.save();
   }

   public static String stream2string(InputStream stream, String charset) throws IOException
   {
      Reader r;
      if (charset != null)
         r = new InputStreamReader(stream, charset);
      else
         r = new InputStreamReader(stream);
      StringWriter sw = new StringWriter();
      char[] buffer = new char[1024];
      for (int n; (n = r.read(buffer)) != -1;)
         sw.write(buffer, 0, n);
      String str = sw.toString();
      return str;
   }

   public static Property getNodeProperty(Session session, String path, String property) throws PathNotFoundException,
      RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      if (node.hasProperty(property))
         return node.getProperty(property);
      else
         return null;
   }

   public static void addNodeProperty(Session session, String path, String propName, String propValue)
      throws PathNotFoundException, RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      node.setProperty(propName, propValue);
      session.save();
   }

   public static String lockNode(Session session, String path, Boolean depth) throws PathNotFoundException,
      RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      if (!node.isNodeType("mix:lockable"))
      {
         if (node.canAddMixin("mix:lockable"))
         {
            node.addMixin("mix:lockable");
            session.save();
         }
      }
      Lock lock = node.lock(depth, true);
      session.save();
      String tok = lock.getLockToken();
      // System.out.println("TestUtils.lockNode()" + tok);
      return "<" + WebDavConst.Lock.OPAQUE_LOCK_TOKEN + ":" + tok + ">";
   }

   public static void find(Session session, String queryString) throws InvalidQueryException, RepositoryException
   {
      Query query = session.getWorkspace().getQueryManager().createQuery(queryString, "sql");
      QueryResult queryResult = query.execute();
   }

   public static String getFileNodeType(Session session, String path) throws PathNotFoundException, RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      return node.getPrimaryNodeType().getName();

   }

   public static String getContentNodeType(Session session, String path) throws PathNotFoundException,
      RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      Node content = node.getNode("jcr:content");
      return content.getPrimaryNodeType().getName();

   }

   public static Node getContentNode(Session session, String path) throws PathNotFoundException, RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      return node.getNode("jcr:content");
   }

   public static NodeType[] getContentMixins(Session session, String path) throws PathNotFoundException,
      RepositoryException
   {
      Node node = session.getRootNode().getNode(TextUtil.relativizePath(path));
      Node content = node.getNode("jcr:content");
      return content.getMixinNodeTypes();
   }

   public static InputStream getResponseAsStream(ContainerResponse response) throws IOException
   {
      if (response.getEntity() instanceof PropFindResponseEntity)
      {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         ((PropFindResponseEntity)response.getEntity()).write(outputStream);
         return new ByteArrayInputStream(outputStream.toByteArray());
      }

      return null;
   }

}
