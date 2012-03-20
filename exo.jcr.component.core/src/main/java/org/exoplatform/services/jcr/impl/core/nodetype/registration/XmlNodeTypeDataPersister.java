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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class XmlNodeTypeDataPersister implements NodeTypeDataPersister
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.XmlNodeTypeDataPersister");

   private final NodeTypeConverter converter;

   private final InputStream is;

   private final OutputStream os;

   public XmlNodeTypeDataPersister(NodeTypeConverter converter, InputStream is)
   {
      super();
      this.converter = converter;
      this.is = is;
      this.os = null;
   }

   /**
    * @param converter
    * @param is
    * @param os
    */
   public XmlNodeTypeDataPersister(NodeTypeConverter converter, InputStream is, OutputStream os)
   {
      super();
      this.converter = converter;
      this.is = is;
      this.os = os;
   }

   public XmlNodeTypeDataPersister(NodeTypeConverter converter, OutputStream os)
   {
      super();
      this.converter = converter;
      this.os = os;
      this.is = null;
   }

   /**
    * {@inheritDoc}
    */
   public void addNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      throw new UnsupportedRepositoryOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNodeType(InternalQName nodeTypeName) throws RepositoryException
   {
      throw new UnsupportedRepositoryOperationException();
   }

   public boolean isStorageFilled()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void addNodeTypes(List<NodeTypeData> nodeTypes) throws RepositoryException
   {
      throw new UnsupportedRepositoryOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public void removeNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      throw new UnsupportedRepositoryOperationException();
   }

   public void start()
   {
   }

   public void stop()
   {
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> getAllNodeTypes() throws RepositoryException
   {
      try
      {
         IBindingFactory factory = null;
         PrivilegedExceptionAction<IBindingFactory> action = new PrivilegedExceptionAction<IBindingFactory>()
         {
            public IBindingFactory run() throws Exception
            {
               return BindingDirectory.getFactory(NodeTypeValuesList.class);
            }
         };
         try
         {
            factory = SecurityHelper.doPrivilegedExceptionAction(action);
         }
         catch (PrivilegedActionException pae)
         {
            Throwable cause = pae.getCause();
            if (cause instanceof JiBXException)
            {
               throw (JiBXException)cause;
            }
            else if (cause instanceof RuntimeException)
            {
               throw (RuntimeException)cause;
            }
            else
            {
               throw new RuntimeException(cause);
            }
         }

         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList)uctx.unmarshalDocument(is, null);
         List ntvList = nodeTypeValuesList.getNodeTypeValuesList();
         return converter.convertFromValueToData(ntvList);
      }
      catch (JiBXException e)
      {
         throw new RepositoryException(e);
      }
   }

   public void update(List<NodeTypeData> nodeTypes, UpdateNodeTypeObserver observer) throws RepositoryException
   {
      throw new UnsupportedRepositoryOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeData getNodeType(InternalQName nodeTypeName) throws RepositoryException
   {
      return null;
   }

}
