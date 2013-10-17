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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
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

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.AbstractDataDistributionType");

   /**
    * The default node type to use when we create a new node
    */
   private static final String DEFAULT_NODE_TYPE = "nt:unstructured".intern();

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
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }

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
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }

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
      Node parentNode = null;
      try
      {
         Node node = getDataNode(rootNode, dataId);
         parentNode = node.getParent();
         node.remove();
         parentNode.save();
      }
      catch (InvalidItemStateException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
         if (parentNode != null)
            parentNode.refresh(false);
      }
      catch (PathNotFoundException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
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
      if (node.getIndex() > 1) 
      {
         // The node has already been created by a concurrent session
         parentNode.refresh(false);
         return parentNode.getNode(nodeName);
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
         try
         {
            parentNode.save();
         }
         catch (ItemExistsException e)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + e.getMessage());
            }
            parentNode.refresh(false);
            // Need to check until the concurrent tx that caused this ItemExistsException is fully committed
            while (!parentNode.hasNode(nodeName));
            return parentNode.getNode(nodeName);
         }
      }
      return node;
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
}
