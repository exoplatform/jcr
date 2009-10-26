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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 * 
 * @since 1.9
 */

public class NodeRepresentationService implements Startable
{

   private Map<String, NodeRepresentationFactory> factoriesByNodeType;

   private Map<String, NodeRepresentationFactory> factoriesByKey;

   ExoContainerContext containerContext;

   private static final Log log = ExoLogger.getLogger("jcr.ext.resource.NodeRepresentationService");

   public NodeRepresentationService(ExoContainerContext containerContext)
   {
      this.factoriesByNodeType = new HashMap<String, NodeRepresentationFactory>();
      this.factoriesByKey = new HashMap<String, NodeRepresentationFactory>();
      this.containerContext = containerContext;
   }

   /**
    * Add new NodeRepresentationFactory for node type.
    * 
    * @param nodeType
    *          the node type.
    * @param representationFactory
    *          the NodeRepresentationFactory.
    */
   public void addNodeRepresentationFactory(String nodeType, NodeRepresentationFactory representationFactory)
   {
      factoriesByNodeType.put(nodeType, representationFactory);
   }

   /**
    * Get NodeRepresentation for given node. String mediaTypeHint can be used as external information
    * for representation. By default node will be represented as doc-view.
    * 
    * @param node
    *          the jcr node.
    * @param mediaTypeHint
    *          the mimetype hint or null if not known.
    * @return the NodeRepresentation.
    * @throws RepositoryException
    */
   public NodeRepresentation getNodeRepresentation(Node node, String mediaTypeHint) throws RepositoryException
   {

      NodeRepresentationFactory factory = factory(node);
      if (factory != null)
         return factory.createNodeRepresentation(node, mediaTypeHint);
      else
         return new DocumentViewNodeRepresentation(node);
   }

   /**
    * @return served node types.
    */
   public Collection<String> getNodeTypes()
   {
      return factoriesByNodeType.keySet();
   }

   /**
    * @return served keys.
    */
   public Collection<String> getKeys()
   {
      return factoriesByKey.keySet();
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      ExoContainer container = containerContext.getContainer();
      List<NodeRepresentationFactory> list = container.getComponentInstancesOfType(NodeRepresentationFactory.class);
      for (NodeRepresentationFactory f : list)
      {

         addNodeRepresentationFactory(f.getNodeType(), f);
         log.info("NodeRepresentationFactory added " + f.getNodeType() + " " + f.getClass().getName());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {

   }

   private NodeRepresentationFactory factory(Node node) throws RepositoryException
   {

      NodeRepresentationFactory f = factoriesByNodeType.get(node.getPrimaryNodeType().getName());

      if (f == null)
      {
         for (String nt : factoriesByNodeType.keySet())
         {
            if (node.isNodeType(nt))
            {
               f = factoriesByNodeType.get(nt);
               break;
            }
         }
      }

      if (f == null)
      {
         for (NodeType mixin : node.getMixinNodeTypes())
         {
            f = factoriesByNodeType.get(mixin.getName());
            if (f != null)
               return f;
         }
      }

      return f;

   }

}
