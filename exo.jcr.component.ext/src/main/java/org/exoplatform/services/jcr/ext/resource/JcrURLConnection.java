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

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class JcrURLConnection extends URLConnection
{

   private static final Log LOG = ExoLogger.getLogger("org.exoplatform.services.jcr.ext.resource.JcrURLConnection");

   private SessionProvider sessionProvider;

   private NodeRepresentationService nodeRepresentationService;

   private UnifiedNodeReference nodeReference;

   private NodeRepresentation nodeRepresentation;

   private Session session;

   private boolean closeSessionProvider;

   public JcrURLConnection(UnifiedNodeReference nodeReference, SessionProvider sessionProvider,
      NodeRepresentationService nodeRepresentationService, boolean closeSessionProvider) throws MalformedURLException
   {

      super(nodeReference.getURL());
      this.sessionProvider = sessionProvider;
      this.nodeReference = nodeReference;
      this.nodeRepresentationService = nodeRepresentationService;
      this.closeSessionProvider = closeSessionProvider;

      doOutput = false;
      allowUserInteraction = false;
      useCaches = false;
      ifModifiedSince = 0;
   }

   public JcrURLConnection(UnifiedNodeReference nodeReference, SessionProvider sessionProvider,
      NodeRepresentationService nodeRepresentationService) throws MalformedURLException
   {

      super(nodeReference.getURL());
      this.sessionProvider = sessionProvider;
      this.nodeReference = nodeReference;
      this.nodeRepresentationService = nodeRepresentationService;

      doOutput = false;
      allowUserInteraction = false;
      useCaches = false;
      ifModifiedSince = 0;
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#connect()
    */
   @Override
   public void connect() throws IOException
   {
      if (connected)
         return;

      try
      {
         session =
            sessionProvider.getSession(sessionProvider.getCurrentWorkspace(), sessionProvider.getCurrentRepository());

         Node node = null;
         if (nodeReference.isPath())
         {
            node = (Node)session.getItem(nodeReference.getPath());
         }
         else if (nodeReference.isIdentitifier())
         {
            node = session.getNodeByUUID(nodeReference.getIdentitifier().getString());
         }
         else
         {
            throw new IllegalArgumentException("Absolute path or Identifier was not found!");
         }

         nodeRepresentation = nodeRepresentationService.getNodeRepresentation(node, "text/xml");
         connected = true;
      }
      catch (Exception e)
      {
         //e.printStackTrace();
         throw new IOException("Connection refused!");
      }
   }

   /**
    * Close connection to JCR.
    */
   public void disconnect()
   {
      if (!connected)
         return;
      session.logout();
      connected = false;
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getInputStream()
    */
   @Override
   public InputStream getInputStream() throws IOException
   {
      if (!connected)
         connect();

      try
      {
         return nodeRepresentation.getInputStream();
      }
      catch (Exception e)
      {
         //e.printStackTrace();
         throw new IOException("can't get input stream");
      }
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getContent()
    */
   @Override
   public Object getContent() throws IOException
   {
      if (!connected)
         connect();

      return nodeRepresentation.getNode();
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getContent(java.lang.Class[])
    */
   @Override
   public Object getContent(Class[] classes) throws IOException
   {
      throw new UnsupportedOperationException("protocol support only "
         + "javax.jcr.Node as content, use method getContent() instead this.");
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getContentType()
    */
   @Override
   public String getContentType()
   {
      try
      {
         if (!connected)
         {
            connect();
         }

         return nodeRepresentation.getMediaType();
      }
      catch (Exception e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getContentLength()
    */
   @Override
   public int getContentLength()
   {
      try
      {
         if (!connected)
         {
            connect();
         }

         return (int)nodeRepresentation.getContentLenght();
      }
      catch (Exception e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return -1;
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#setDoOutput(boolean)
    */
   @Override
   public void setDoOutput(boolean dooutput)
   {
      if (dooutput)
         throw new UnsupportedOperationException("protocol doesn't support output!");
      super.setDoOutput(dooutput);
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getContentEncoding()
    */
   @Override
   public String getContentEncoding()
   {
      try
      {
         if (!connected)
         {
            connect();
         }

         return nodeRepresentation.getContentEncoding();
      }
      catch (Exception e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return null;
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#getLastModified()
    */
   @Override
   public long getLastModified()
   {
      try
      {
         if (!connected)
         {
            connect();
         }

         return nodeRepresentation.getLastModified();
      }
      catch (Exception e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return 0;
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#setAllowUserInteraction(boolean)
    */
   @Override
   public void setAllowUserInteraction(boolean allowuserinteraction)
   {
      if (allowuserinteraction)
         throw new UnsupportedOperationException("protocol doesn't support user interaction!");
      super.setAllowUserInteraction(allowuserinteraction);
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#setUseCaches(boolean)
    */
   @Override
   public void setUseCaches(boolean usecaches)
   {
      if (usecaches)
         throw new UnsupportedOperationException("protocol doesn't support caches!");
      super.setUseCaches(usecaches);
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#setIfModifiedSince(long)
    */
   @Override
   public void setIfModifiedSince(long ifmodifiedsince)
   {
      if (ifmodifiedsince > 0)
         throw new UnsupportedOperationException("protocol doesn't support this feature!");
      super.setIfModifiedSince(ifmodifiedsince);
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
    */
   @Override
   public void addRequestProperty(String key, String value)
   {
      throw new UnsupportedOperationException("protocol doesn't support request properties!");
   }

   /*
    * (non-Javadoc)
    * @see java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
    */
   @Override
   public void setRequestProperty(String key, String value)
   {
      throw new UnsupportedOperationException("protocol doesn't support request properties!");
   }

   @Override
   protected void finalize() throws Throwable
   {
      try
      {
         if (closeSessionProvider)
         {
            sessionProvider.close();
         }
      }
      catch (Exception t)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + t.getMessage());
         }
      }
      finally
      {
         super.finalize();
      }
   }

}
