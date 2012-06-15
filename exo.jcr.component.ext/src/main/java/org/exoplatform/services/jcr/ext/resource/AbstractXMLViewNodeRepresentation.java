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

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */
public abstract class AbstractXMLViewNodeRepresentation implements NodeRepresentation
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.AbstractXMLViewNodeRepresentation");

   private Node node;

   protected boolean isSystem;

   /**
    * AbstractXMLViewNodeRepresentation.
    * 
    * @param node
    */
   protected AbstractXMLViewNodeRepresentation(Node node)
   {
      this.node = node;
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getContentEncoding()
    */
   public String getContentEncoding()
   {
      return Constants.DEFAULT_ENCODING;
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getContentLenght()
    */
   public long getContentLenght() throws RepositoryException
   {
      return -1;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getMediaType()
    */
   public String getMediaType() throws RepositoryException
   {
      return "text/xml";
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getLastModified()
    */
   public long getLastModified() throws RepositoryException
   {
      return 0;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getProperty(java.lang.String)
    */
   public HierarchicalProperty getProperty(String name) throws RepositoryException
   {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getProperty(java.lang.String)
    */
   public Collection<HierarchicalProperty> getProperties(String name) throws RepositoryException
   {
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getPropertyNames()
    */
   public Collection<String> getPropertyNames() throws RepositoryException
   {
      return new ArrayList<String>();
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getInputStream()
    */
   public InputStream getInputStream() throws IOException, RepositoryException
   {
      final PipedInputStream pin = new PipedInputStream();
      final PipedOutputStream pout = new PipedOutputStream(pin);

      try
      {

         new Thread()
         {

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Thread#run()
             */
            public void run()
            {
               try
               {
                  if (isSystem)
                  {
                     node.getSession().exportSystemView(node.getPath(), pout, false, false);
                  }
                  else
                  {
                     node.getSession().exportDocumentView(node.getPath(), pout, false, false);
                  }
               }
               catch (Exception e)
               {
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An exception occurred: " + e.getMessage());
                  }
               }
               finally
               {
                  try
                  {
                     pout.flush();
                     pout.close();
                  }
                  catch (Exception e)
                  {
                     if (LOG.isTraceEnabled())
                     {
                        LOG.trace("An exception occurred: " + e.getMessage());
                     }
                  }
               }
            }

         }.start();

         return pin;
      }
      catch (Exception e)
      {
         LOG.error(e.getLocalizedMessage(), e);
         throw new IOException("can't get input stream", e);
      }

   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getNode()
    */
   public Node getNode()
   {
      return node;
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#addProperties(java.lang.String, java.util.Collection)
    */
   public void addProperties(Collection<HierarchicalProperty> properties)
      throws UnsupportedRepositoryOperationException
   {
      throw new UnsupportedRepositoryOperationException();

   }

   /**
    * {@inheritDoc}
    */
   public void addProperty(HierarchicalProperty property) throws UnsupportedRepositoryOperationException
   {
      throw new UnsupportedRepositoryOperationException();

   }

   /**
    * {@inheritDoc}
    */
   public void removeProperty(String name) throws UnsupportedRepositoryOperationException
   {
      throw new UnsupportedRepositoryOperationException();

   }

}
