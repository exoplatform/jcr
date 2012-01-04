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
package org.exoplatform.services.jcr.ext.resource;

import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.services.jcr.datamodel.Identifier;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.StringTokenizer;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class UnifiedNodeReference
{

   public static final String JCR_SCHEME = "jcr";

   private String userInfo;

   private String repository;

   private String workspace;

   private Identifier id;

   private String path;

   private static URLStreamHandler handler;

   public UnifiedNodeReference(final String spec) throws URISyntaxException, MalformedURLException
   {
      this(new URL(null, spec, getURLStreamHandler()));
   }

   public UnifiedNodeReference(final URL url) throws URISyntaxException
   {
      this(url.toURI());
   }

   public UnifiedNodeReference(final URI uri) throws URISyntaxException
   {

      String scheme = uri.getScheme();
      if (uri.getScheme() == null)
         scheme = JCR_SCHEME;
      if (!scheme.equals(JCR_SCHEME))
         throw new URISyntaxException(scheme, "Only 'jcr' scheme is acceptable!");

      userInfo = uri.getUserInfo();

      repository = uri.getHost();

      workspace = parseWorkpace(uri);

      String fragment = uri.getFragment();
      if (fragment != null)
      {
         if (fragment.startsWith("/"))
            this.path = fragment;
         else
            this.id = new Identifier(uri.getFragment());
      }
      else
         throw new URISyntaxException(fragment, "Neither Path nor Identifier defined!");

   }

   public UnifiedNodeReference(final URI uri, final String defaultRepository, final String defaultWorkspace)
      throws URISyntaxException
   {

      String scheme = uri.getScheme();
      if (uri.getScheme() == null)
         scheme = JCR_SCHEME;
      if (!scheme.equals(JCR_SCHEME))
         throw new URISyntaxException(scheme, "Only 'jcr' scheme is acceptable!");

      userInfo = uri.getUserInfo();

      repository = uri.getHost();
      if (repository == null)
         repository = defaultRepository;

      workspace = parseWorkpace(uri);
      if (workspace == null || workspace.length() == 0)
         workspace = defaultWorkspace;

      String fragment = uri.getFragment();
      if (fragment != null)
      {
         if (fragment.startsWith("/"))
            this.path = fragment;
         else
            this.id = new Identifier(uri.getFragment());
      }
      else
         throw new URISyntaxException(fragment, "Neither Path nor Identifier defined!");

   }

   public UnifiedNodeReference(final String repository, final String workspace, final Identifier identifier)
   {

      this.repository = repository;
      this.workspace = workspace;
      this.id = identifier;

   }

   public UnifiedNodeReference(final String repository, final String workspace, final String path)
   {

      this.repository = repository;
      this.workspace = workspace;
      this.path = path;

   }

   /**
    * @return the repository name.
    */
   public String getRepository()
   {
      return repository;
   }

   /**
    * @return the workspace name.
    */
   public String getWorkspace()
   {
      return workspace;
   }

   /**
    * @return the node identifier.
    */
   public Identifier getIdentitifier()
   {
      return id;
   }

   /**
    * @return true if UUID used as node identifier.
    */
   public boolean isIdentitifier()
   {
      return id != null;
   }

   /**
    * @return the node path.
    */
   public String getPath()
   {
      return path;
   }

   /**
    * @return true if full path used as node identifier.
    */
   public boolean isPath()
   {
      return path != null;
   }

   /**
    * @return the user info part of URL, it looks like <code>user:pass</code>.
    */
   public String getUserInfo()
   {
      return userInfo;
   }

   /**
    * @return the URI of node.
    * @throws URISyntaxException
    */
   public URI getURI() throws URISyntaxException
   {
      if (id != null)
         return new URI(JCR_SCHEME, userInfo, repository, -1, '/' + workspace, null, id.getString());
      else if (path != null)
         return new URI(JCR_SCHEME, userInfo, repository, -1, '/' + workspace, null, path);
      throw new URISyntaxException("", "Path or Idenfifier is not defined!");
   }

   /**
    * @return the URL of node.
    * @throws MalformedURLException
    */
   public URL getURL() throws MalformedURLException
   {
      URI uri;
      try
      {
         uri = getURI();
      }
      catch (URISyntaxException e)
      {
         throw new MalformedURLException();
      }

      try
      {
         return new URL(uri.toString());
      }
      catch (MalformedURLException e)
      {
         // If handler can't be found by java.net.URL#getStreamHandler()
         return new URL(null, uri.toString(), getURLStreamHandler());
      }
   }

   /**
    * @return the handler for protocol <code>jcr</code>.
    * 
    * @see java.net.URLStreamHandler.
    */
   public static URLStreamHandler getURLStreamHandler()
   {

      if (handler != null)
         return handler;

      // use Class#forName(), instead created by 'new' to be sure handler
      // was started and set required system property.
      // Usually this job must be done by java.net.URL, but it does
      // not work in web container. Under tomcat class of handler can't be found in
      // $CATALINA_HOME/lib/*.jar. Probably the same problem can be under AS.
      String packagePrefixList = PrivilegedSystemHelper.getProperty("java.protocol.handler.pkgs");

      if (packagePrefixList == null)
         return null;

      StringTokenizer packagePrefixIter = new StringTokenizer(packagePrefixList, "|");

      while (handler == null && packagePrefixIter.hasMoreTokens())
      {
         String packagePrefix = packagePrefixIter.nextToken().trim();
         try
         {
            String clsName = packagePrefix + "." + JCR_SCHEME + ".Handler";
            Class<?> cls = ClassLoading.forName(clsName, UnifiedNodeReference.class);
            if (cls != null)
            {
               handler = (URLStreamHandler)cls.newInstance();
            }
         }
         catch (Exception e)
         {
            // exceptions can get thrown here if class not be loaded y system ClassLoader
            // or if class can't be instantiated.
         }
      }
      return handler;
   }

   private static String parseWorkpace(URI uri)
   {
      String path = uri.getPath();
      int sl = path.indexOf('/', 1);
      if (sl <= 0)
         return path.substring(1);
      return path.substring(1, sl);
   }

}
