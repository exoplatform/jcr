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

import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class CNDNodeTypeDataPersister implements NodeTypeDataPersister
{
   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger(CNDNodeTypeDataPersister.class);

   private final InputStream is;

   private final OutputStream os;

   private final NamespaceRegistryImpl namespaceRegistry;

   /**
    * {@inheritDoc}
    */
   public CNDNodeTypeDataPersister(InputStream is, OutputStream os, NamespaceRegistryImpl namespaceRegistry)
   {
      super();
      this.is = is;
      this.os = os;
      this.namespaceRegistry = namespaceRegistry;
   }

   public CNDNodeTypeDataPersister(OutputStream os, NamespaceRegistryImpl namespaceRegistry)
   {
      this(null, os, namespaceRegistry);
   }

   public CNDNodeTypeDataPersister(InputStream is, NamespaceRegistryImpl namespaceRegistry)
   {
      this(is, null, namespaceRegistry);
   }

   public CNDNodeTypeDataPersister(OutputStream os)
   {
      this(null, os, new NamespaceRegistryImpl());
   }

   public CNDNodeTypeDataPersister(InputStream is)
   {
      this(is, null, new NamespaceRegistryImpl());
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
      new CNDStreamWriter(namespaceRegistry).write(nodeTypes, os);
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
      return new CNDStreamReader(namespaceRegistry).read(is);
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
