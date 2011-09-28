/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.distribution.impl;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public abstract class AbstractDataDistributionType implements DataDistributionType
{
   /**
    * The default node type to use when we create a new node
    */
   private static final String DEFAULT_NODE_TYPE = "nt:unstructured".intern();

   /**
    * The map defining all the locks available
    */
   private final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>(64, 0.75f, 64);

   /**
    * {@inheritDoc}
    */
   public Node getDataNode(Node rootNode, String dataId) throws PathNotFoundException, RepositoryException
   {
      return rootNode.getNode(getRelativePath(dataId));
   }

   /**
    * {@inheritDoc}
    */
   public Node getOrCreateDataNode(Node rootNode, String dataId) throws RepositoryException
   {
      return getOrCreateDataNode(rootNode, dataId, null);
   }

   /**
    * {@inheritDoc}
    */
   public Node getOrCreateDataNode(Node rootNode, String dataId, String nodeType) throws RepositoryException
   {
      return getOrCreateDataNode(rootNode, dataId, nodeType, null);
   }

   /**
    * {@inheritDoc}
    */
   public Node getOrCreateDataNode(Node rootNode, String dataId, String nodeType, List<String> mixinTypes)
      throws RepositoryException
   {
      return getOrCreateDataNode(rootNode, dataId, nodeType, mixinTypes, null);
   }

   /**
    * {@inheritDoc}
    */
   public Node getOrCreateDataNode(Node rootNode, String dataId, String nodeType, List<String> mixinTypes,
      Map<String, String[]> permissions) throws RepositoryException
   {
      try
      {
         return getDataNode(rootNode, dataId);
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }
      // The node could not be found so we need to create it
      Node node = rootNode;
      List<String> ancestors = getAncestors(dataId);
      for (int i = 0, length = ancestors.size(); i < length; i++)
      {
         String nodeName = ancestors.get(i);
         try
         {
            node = node.getNode(nodeName);
            continue;
         }
         catch (PathNotFoundException e)
         {
            // ignore me
         }
         // The node doesn't exist we need to create it
         node = createNode(node, nodeName, nodeType, mixinTypes, permissions, i == length - 1, true);
      }
      return node;
   }

   /**
    * {@inheritDoc}
    */
   public void removeDataNode(Node rootNode, String dataId) throws RepositoryException
   {      
      Lock lock = getLock(rootNode, getRelativePath(dataId));
      lock.lock();
      try
      {
         Node node = getDataNode(rootNode, dataId);
         Node parentNode = node.getParent();
         node.remove();
         parentNode.save();
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }
      finally 
      {
         lock.unlock();
      }     
   }

   /**
    * {@inheritDoc}
    */
   public void migrate(Node rootNode) throws RepositoryException
   {
      throw new UnsupportedOperationException("The method is not supported");
   }

   /**
    * {@inheritDoc}
    */
   public void migrate(Node rootNode, String nodeType, List<String> mixinTypes, Map<String, String[]> permissions)
      throws RepositoryException
   {
      throw new UnsupportedOperationException("The method is not supported");
   }

   /**
    * Creates the node of the given node type with the given node name directly under 
    * the given parent node, using the given mixin types and permissions
    * @param parentNode the parent node
    * @param nodeName the name of the node to create
    * @param nodeType the node type to use
    * @param mixinTypes the list of mixin types to use
    * @param permissions the map of permissions to use
    * @param isLeaf indicates whether or not the current node to create is the leaf node
    * @return the created node
    * @throws RepositoryException if any exception occurs while creating the node
    */
   protected Node createNode(final Node parentNode, final String nodeName, final String nodeType,
      final List<String> mixinTypes, final Map<String, String[]> permissions, final boolean isLeaf,
      final boolean callSave) throws RepositoryException
   {
      Lock lock = getLock(parentNode, nodeName);
      lock.lock();
      try
      {
         try
         {
            // We ensure that the node has not been created since the last time we checked
            return parentNode.getNode(nodeName);
         }
         catch (PathNotFoundException e)
         {
            // ignore me
         }
         
         boolean useParameters = !useParametersOnLeafOnly() || (useParametersOnLeafOnly() && isLeaf);
         Node node;
         if (nodeType == null || nodeType.isEmpty() || !useParameters)
         {
            node = parentNode.addNode(nodeName, DEFAULT_NODE_TYPE);
         }
         else
         {
            node = parentNode.addNode(nodeName, nodeType);
         }
         if (useParameters)
         {
            if (permissions != null && !permissions.isEmpty())
            {
               if (node.canAddMixin("exo:privilegeable"))
               {
                  node.addMixin("exo:privilegeable");
               }
               ((ExtendedNode)node).setPermissions(permissions);
            }
            if (mixinTypes != null)
            {
               for (int i = 0, length = mixinTypes.size(); i < length; i++)
               {
                  String mixin = mixinTypes.get(i);
                  if (node.canAddMixin(mixin))
                  {
                     node.addMixin(mixin);
                  }
               }
            }
         }

         if (callSave)
         {
            parentNode.save();
         }
         return node;         
      }
      finally 
      {
         lock.unlock();
      }
   }

   /**
    * Creates the full path of the given node name
    * @param parentNode the parent node from which we extract the absolute path
    * @param relativePath the relative path of the node for which we want the full path
    * @return the full path of the node for which we want the path
    * @throws RepositoryException if any exception occurs while creating the path
    */
   private String createFullPath(Node parentNode, String relativePath) throws RepositoryException
   {
      StringBuilder buffer = new StringBuilder(256);
      buffer.append(((RepositoryImpl)parentNode.getSession().getRepository()).getName());
      buffer.append('/');
      buffer.append(parentNode.getSession().getWorkspace().getName());
      String rootPath = parentNode.getPath();
      buffer.append(rootPath);
      if (!rootPath.endsWith("/"))
      {
         buffer.append('/');
      }
      buffer.append(relativePath);
      return buffer.toString();
   }

   /**
    * Get the lock corresponding to the given node name
    * @param parentNode the parent node of the node to lock
    * @param relativePath the relative path of the node to lock
    * @return the lock corresponding to the given node name
    * @throws RepositoryException if any exception occurs while getting the lock
    */
   private Lock getLock(Node parentNode, String relativePath) throws RepositoryException
   {
      String fullPath = createFullPath(parentNode, relativePath);
      Lock lock = locks.get(fullPath);
      if (lock != null)
      {
         return lock;
      }
      lock = new InternalLock(fullPath);
      Lock prevLock = locks.putIfAbsent(fullPath, lock);
      if (prevLock != null)
      {
         lock = prevLock;
      }
      return lock;

   }
   
   /**
    * Gives the relative path corresponding to the given id of the data to find/create
    * @param dataId the id of the data to find/create
    * @return the relative path of the data to find/create
    */
   protected String getRelativePath(String dataId)
   {
      StringBuilder buffer = new StringBuilder(256);
      List<String> ancestors = getAncestors(dataId);
      for (int i = 0, length = ancestors.size(); i < length; i++)
      {
         buffer.append(ancestors.get(i));
         if (i != length - 1)
         {
            buffer.append('/');
         }
      }
      return buffer.toString();
   }

   /**
    * Gives the list of all the name of the ancestors
    * @param dataId the id of the data to find/create
    * @return the list of the ancestor names
    */
   protected abstract List<String> getAncestors(String dataId);

   /**
    * Indicates whether or not the node type, the mixin types and the permissions have to
    * be used on leaf node only.
    * @return <code>true</code> if only the leaf node has to be created with the parameters
    * <code>false</code> otherwise.
    */
   protected abstract boolean useParametersOnLeafOnly();
   
   
   /**
    * This kind of locks can self unregister from the map of locks
    */
   private class InternalLock extends ReentrantLock {
      
      /**
       * Serial Version UID
       */
      private static final long serialVersionUID = -3362387346368015145L;
      
      /**
       * The id corresponding to the lock in the map
       */
      private final String fullPath;

      /**
       * The default constructor
       * @param fullId the id corresponding to the lock in the map
       */
      public InternalLock(String fullPath) {
         super();
         this.fullPath = fullPath;
      }

      @Override
      public void unlock() {
         if (!hasQueuedThreads()) {
            // No thread is currently waiting for this lock
            // The lock will then be removed
            locks.remove(fullPath, this);
         }
         super.unlock();
      }
   }   
}
