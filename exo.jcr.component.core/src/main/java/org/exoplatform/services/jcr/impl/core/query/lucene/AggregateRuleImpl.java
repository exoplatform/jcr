/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.ItemImpl.ItemType;
import org.exoplatform.services.jcr.util.Text;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>AggregateRule</code> defines a configuration for a node index
 * aggregate. It defines rules for items that should be included in the node
 * scope index of an ancestor. Per default the values of properties are only
 * added to the node scope index of the parent node.
 */
class AggregateRuleImpl implements AggregateRule
{

   /**
    * A name resolver for parsing QNames in the configuration.
    */
   private final LocationFactory resolver;

   /**
    * The node type of the root node of the indexing aggregate.
    */
   private final InternalQName nodeTypeName;

   /**
    * The node includes of this indexing aggregate.
    */
   private final NodeInclude[] nodeIncludes;

   /**
    * The property includes of this indexing aggregate.
    */
   private final PropertyInclude[] propertyIncludes;

   /**
    * The item state manager to retrieve additional item states.
    */
   private final ItemDataConsumer ism;

   /**
    * Creates a new indexing aggregate using the given <code>config</code>.
    *
    * @param config     the configuration for this indexing aggregate.
    * @param resolver   the name resolver for parsing Names within the config.
    * @param ism        the item state manager of the workspace.
    * @param hmgr       a hierarchy manager for the item state manager.
    * @throws MalformedPathException if a path in the configuration is
    *                                malformed.
    * @throws IllegalNameException   if a node type name contains illegal
    *                                characters.
    * @throws RepositoryException 
    */
   AggregateRuleImpl(Node config, LocationFactory resolver, ItemDataConsumer ism) throws IllegalNameException,
      RepositoryException
   {
      this.resolver = resolver;
      this.nodeTypeName = getNodeTypeName(config);
      this.nodeIncludes = getNodeIncludes(config);
      this.propertyIncludes = getPropertyIncludes(config);
      this.ism = ism;
   }

   /**
    * Returns root node state for the indexing aggregate where
    * <code>nodeState</code> belongs to.
    *
    * @param nodeState the node state.
    * @return the root node state of the indexing aggregate or
    *         <code>null</code> if <code>nodeState</code> does not belong to an
    *         indexing aggregate.
    * @throws ItemStateException  if an error occurs.
    * @throws RepositoryException if an error occurs.
    */
   public NodeData getAggregateRoot(NodeData nodeState) throws RepositoryException
   {
      for (int i = 0; i < nodeIncludes.length; i++)
      {
         NodeData aggregateRoot = nodeIncludes[i].matches(nodeState);
         if (aggregateRoot != null && aggregateRoot.getPrimaryTypeName().equals(nodeTypeName))
         {
            return aggregateRoot;
         }
      }
      // check property includes
      for (int i = 0; i < propertyIncludes.length; i++)
      {
         NodeData aggregateRoot = propertyIncludes[i].matches(nodeState);
         if (aggregateRoot != null && aggregateRoot.getPrimaryTypeName().equals(nodeTypeName))
         {
            return aggregateRoot;
         }
      }
      return null;
   }

   /**
    * Returns the node states that are part of the indexing aggregate of the
    * <code>nodeState</code>.
    *
    * @param nodeState a node state
    * @return the node states that are part of the indexing aggregate of
    *         <code>nodeState</code>. Returns <code>null</code> if this
    *         aggregate does not apply to <code>nodeState</code>.
    * @throws RepositoryException 
    * @throws ItemStateException  if an error occurs.
    */
   public NodeData[] getAggregatedNodeStates(NodeData nodeState) throws RepositoryException
   {
      if (nodeState.getPrimaryTypeName().equals(nodeTypeName))
      {
         List nodeStates = new ArrayList();
         for (int i = 0; i < nodeIncludes.length; i++)
         {
            nodeStates.addAll(Arrays.asList(nodeIncludes[i].resolve(nodeState)));
         }
         if (nodeStates.size() > 0)
         {
            return (NodeData[])nodeStates.toArray(new NodeData[nodeStates.size()]);
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public PropertyData[] getAggregatedPropertyStates(NodeData nodeState) throws RepositoryException
   {
      if (nodeState.getPrimaryTypeName().equals(nodeTypeName))
      {
         List propStates = new ArrayList();
         for (int i = 0; i < propertyIncludes.length; i++)
         {
            propStates.addAll(Arrays.asList(propertyIncludes[i].resolvePropertyStates(nodeState)));
         }
         if (propStates.size() > 0)
         {
            return (PropertyData[])propStates.toArray(new PropertyData[propStates.size()]);
         }
      }
      return null;
   }

   //---------------------------< internal >-----------------------------------

   /**
    * Reads the node type of the root node of the indexing aggregate.
    *
    * @param config the configuration.
    * @return the name of the node type.
    * @throws IllegalNameException   if the node type name contains illegal
    *                                characters.
    * @throws RepositoryException 
    */
   private InternalQName getNodeTypeName(Node config) throws IllegalNameException, RepositoryException
   {
      String ntString = config.getAttributes().getNamedItem("primaryType").getNodeValue();
      return resolver.parseJCRName(ntString).getInternalName();
   }

   /**
    * Creates node includes defined in the <code>config</code>.
    *
    * @param config the indexing aggregate configuration.
    * @return the node includes defined in the <code>config</code>.
    * @throws MalformedPathException if a path in the configuration is
    *                                malformed.
    * @throws IllegalNameException   if the node type name contains illegal
    *                                characters.
    * @throws RepositoryException 
    */
   private NodeInclude[] getNodeIncludes(Node config) throws IllegalNameException, RepositoryException
   {
      List includes = new ArrayList();
      NodeList childNodes = config.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++)
      {
         Node n = childNodes.item(i);
         if (n.getNodeName().equals("include"))
         {
            InternalQName ntName = null;
            Node ntAttr = n.getAttributes().getNamedItem("primaryType");
            if (ntAttr != null)
            {
               ntName = resolver.parseJCRName(ntAttr.getNodeValue()).getInternalName();
            }
            String[] elements = Text.explode(getTextContent(n), '/');

            QPathEntry[] path = new QPathEntry[elements.length];
            for (int j = 0; j < elements.length; j++)
            {
               if (elements[j].equals("*"))
               {
                  path[j] = new QPathEntry(Constants.JCR_ANY_NAME, 0);
               }
               else
               {
                  path[j] = new QPathEntry(resolver.parseJCRName(elements[j]).getInternalName(), 0);
               }
            }

            includes.add(new NodeInclude(new QPath(path), ntName));
         }
      }
      return (NodeInclude[])includes.toArray(new NodeInclude[includes.size()]);
   }

   /**
    * Creates property includes defined in the <code>config</code>.
    *
    * @param config the indexing aggregate configuration.
    * @return the property includes defined in the <code>config</code>.
    * @throws MalformedPathException if a path in the configuration is
    *                                malformed.
    * @throws IllegalNameException   if the node type name contains illegal
    *                                characters.
    * @throws RepositoryException 
    */
   private PropertyInclude[] getPropertyIncludes(Node config) throws IllegalNameException, RepositoryException
   {
      List includes = new ArrayList();
      NodeList childNodes = config.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++)
      {
         Node n = childNodes.item(i);
         if (n.getNodeName().equals("include-property"))
         {
            String[] elements = Text.explode(getTextContent(n), '/');

            QPathEntry[] path = new QPathEntry[elements.length];
            for (int j = 0; j < elements.length; j++)
            {
               if (elements[j].equals("*"))
               {
                  throw new IllegalNameException("* not supported in include-property");
               }

               path[j] = new QPathEntry(resolver.parseJCRName(elements[j]).getInternalName(), 1);
            }
            includes.add(new PropertyInclude(new QPath(path)));
         }
      }
      return (PropertyInclude[])includes.toArray(new PropertyInclude[includes.size()]);
   }

   //---------------------------< internal >-----------------------------------

   /**
    * @param node a node.
    * @return the text content of the <code>node</code>.
    */
   private static String getTextContent(Node node)
   {
      StringBuffer content = new StringBuffer();
      NodeList nodes = node.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++)
      {
         Node n = nodes.item(i);
         if (n.getNodeType() == Node.TEXT_NODE)
         {
            content.append(((CharacterData)n).getData());
         }
      }
      return content.toString();
   }

   private abstract class AbstractInclude
   {

      /**
       * Optional node type name.
       */
      protected final InternalQName nodeTypeName;

      /**
       * A relative path pattern.
       */
      protected final QPath pattern;

      /**
       * Creates a new rule with a relative path pattern and an optional node
       * type name.
       *
       * @param nodeTypeName node type name or <code>null</code> if all node
       *                     types are allowed.
       * @param pattern      a relative path pattern.
       */
      AbstractInclude(QPath pattern, InternalQName nodeTypeName)
      {
         this.nodeTypeName = nodeTypeName;
         this.pattern = pattern;
      }

      /**
       * If the given <code>nodeState</code> matches this rule the root node
       * state of the indexing aggregate is returned.
       *
       * @param nodeState a node state.
       * @return the root node state of the indexing aggregate or
       *         <code>null</code> if <code>nodeState</code> does not belong
       *         to an indexing aggregate defined by this rule.
       * @throws ItemStateException if an error occurs while accessing node
       *                            states.
       * @throws RepositoryException if another error occurs.
       */
      NodeData matches(NodeData nodeState) throws RepositoryException
      {
         // first check node type
         if (nodeTypeName == null || nodeState.getPrimaryTypeName().equals(nodeTypeName))
         {
            // check pattern
            QPathEntry[] elements = pattern.getEntries();
            for (int e = elements.length - 1; e >= 0; e--)
            {
               String parentId = nodeState.getParentIdentifier();
               if (parentId == null)
               {
                  // nodeState is root node
                  return null;
               }
               NodeData parent = (NodeData)ism.getItemData(parentId);
               if (elements[e].getName().equals("*"))
               {
                  // match any parent
                  nodeState = parent;
               }
               else
               {
                  // check name
                  InternalQName name = nodeState.getQPath().getName();
                  if (elements[e].equals(name))
                  {
                     nodeState = parent;
                  }
                  else
                  {
                     return null;
                  }
               }
            }
            // if we get here nodeState became the root
            // of the indexing aggregate and is valid
            return nodeState;
         }
         return null;
      }

      //-----------------------------< internal >-----------------------------

      /**
       * Recursively resolves node states along the path {@link #pattern}.
       *
       * @param nodeState the current node state.
       * @param collector resolved node states are collected using the list.
       * @param offset    the current path element offset into the path
       *                  pattern.
       * @throws RepositoryException 
       * @throws ItemStateException if an error occurs while accessing node
       *                            states.
       */
      protected void resolve(NodeData nodeState, List collector, int offset) throws RepositoryException
      {
         QPathEntry currentName = pattern.getEntries()[offset];// [offset].getName();
         List<NodeData> cne;
         if (currentName.getAsString().equals("*"))
         {
            // matches all
            cne = ism.getChildNodesData(nodeState);// nodeState.getChildNodeEntries();
         }
         else
         {
            cne = new ArrayList<NodeData>();
            ItemData item = ism.getItemData(nodeState, currentName, ItemType.NODE);
            if (item != null && item.isNode())
            {
               cne.add((NodeData)item);
            }
         }
         if (pattern.getEntries().length - 1 == offset)
         {
            // last segment -> add to collector if node type matches
            for (Iterator<NodeData> it = cne.iterator(); it.hasNext();)
            {
               NodeData ns = it.next();
               if (nodeTypeName == null || (ns != null && ns.getPrimaryTypeName().equals(nodeTypeName)))
               {
                  collector.add(ns);
               }
            }
         }
         else
         {
            // traverse
            offset++;
            for (Iterator<NodeData> it = cne.iterator(); it.hasNext();)
            {
               NodeData nodeData = it.next();
               if (nodeData != null)
                  resolve(nodeData, collector, offset);
            }
         }
      }
   }

   private final class NodeInclude extends AbstractInclude
   {

      /**
       * Creates a new node include with a relative path pattern and an
       * optional node type name.
       *
       * @param nodeTypeName node type name or <code>null</code> if all node
       *                     types are allowed.
       * @param pattern      a relative path pattern.
       */
      NodeInclude(QPath pattern, InternalQName nodeTypeName)
      {
         super(pattern, nodeTypeName);
      }

      /**
       * Resolves the <code>nodeState</code> using this rule.
       *
       * @param nodeState the root node of the enclosing indexing aggregate.
       * @return the descendant node states as defined by this rule.
       * @throws RepositoryException 
       * @throws ItemStateException if an error occurs while resolving the
       *                            node states.
       */
      NodeData[] resolve(NodeData nodeState) throws RepositoryException
      {
         List nodeStates = new ArrayList();
         resolve(nodeState, nodeStates, 0);
         return (NodeData[])nodeStates.toArray(new NodeData[nodeStates.size()]);
      }
   }

   private final class PropertyInclude extends AbstractInclude
   {

      private final InternalQName propertyName;

      PropertyInclude(QPath pattern) throws PathNotFoundException, IllegalPathException
      {
         super(new QPath(pattern.getRelPath(1)), null);
         this.propertyName = pattern.getName();
      }

      /**
       * Resolves the <code>nodeState</code> using this rule.
       *
       * @param nodeState the root node of the enclosing indexing aggregate.
       * @return the descendant property states as defined by this rule.
       * @throws RepositoryException 
       * @throws ItemStateException if an error occurs while resolving the
       *                            property states.
       */
      PropertyData[] resolvePropertyStates(NodeData nodeState) throws RepositoryException
      {
         List nodeStates = new ArrayList();
         resolve(nodeState, nodeStates, 0);
         List propStates = new ArrayList();
         for (Iterator it = nodeStates.iterator(); it.hasNext();)
         {
            NodeData state = (NodeData)it.next();
            ItemData prop = ism.getItemData(state, new QPathEntry(propertyName, 1), ItemType.PROPERTY);
            if (prop != null && !prop.isNode())
            {
               propStates.add(prop);
            }

            //                if (state.hasPropertyName(propertyName)) {
            //                    PropertyId propId = new PropertyId(state.getNodeId(), propertyName);
            //                    propStates.add(ism.getItemState(propId));
            //                }
         }
         return (PropertyData[])propStates.toArray(new PropertyData[propStates.size()]);
      }

   }
}
