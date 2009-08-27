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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.util.Text;

/**
 * <code>AggregateRule</code> defines a configuration for a node index aggregate. It defines rules
 * for items that should be included in the node scope index of an ancestor. Per default the values
 * of properties are only added to the node scope index of the parent node.
 */
class AggregateRuleImpl
   implements AggregateRule
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
    * The rules that define this indexing aggregate.
    */
   private final Rule[] rules;

   /**
    * The item state manager to retrieve additional item states.
    */
   private final ItemDataConsumer ism;

   /**
    * Creates a new indexing aggregate using the given <code>config</code>.
    * 
    * @param config
    *          the configuration for this indexing aggregate.
    * @param resolver
    *          the name resolver for parsing Names within the config.
    * @param ism
    *          the item state manager of the workspace.
    * @param hmgr
    *          a hierarchy manager for the item state manager.
    * @throws IllegalNameException
    * @throws MalformedPathException
    *           if a path in the configuration is malformed.
    * @throws IllegalNameException
    *           if a node type name contains illegal characters.
    * @throws NamespaceException
    *           if a node type contains an unknown prefix.
    * @throws RepositoryException
    * @throws PathNotFoundException
    */
   AggregateRuleImpl(Node config, LocationFactory resolver, ItemDataConsumer ism) throws IllegalNameException,
            PathNotFoundException, RepositoryException
   {
      this.resolver = resolver;
      this.nodeTypeName = getNodeTypeName(config);
      this.rules = getRules(config);
      this.ism = ism;
   }

   /**
    * Returns root node state for the indexing aggregate where <code>nodeState</code> belongs to.
    * 
    * @param nodeState
    * @return the root node state of the indexing aggregate or <code>null</code> if
    *         <code>nodeState</code> does not belong to an indexing aggregate.
    * @throws ItemStateException
    *           if an error occurs.
    * @throws RepositoryException
    *           if an error occurs.
    */
   public NodeData getAggregateRoot(NodeData nodeState) throws RepositoryException
   {
      for (int i = 0; i < rules.length; i++)
      {
         NodeData aggregateRoot = rules[i].matches(nodeState);
         if (aggregateRoot != null && aggregateRoot.getPrimaryTypeName().equals(nodeTypeName))
         {
            return aggregateRoot;
         }
      }
      return null;
   }

   /**
    * Returns the node states that are part of the indexing aggregate of the <code>nodeState</code>.
    * 
    * @param nodeState
    *          a node state
    * @return the node states that are part of the indexing aggregate of <code>nodeState</code>.
    *         Returns <code>null</code> if this aggregate does not apply to <code>nodeState</code>.
    * @throws RepositoryException
    * @throws ItemStateException
    *           if an error occurs.
    */
   public NodeData[] getAggregatedNodeStates(NodeData nodeState) throws RepositoryException
   {
      if (nodeState.getPrimaryTypeName().equals(nodeTypeName))
      {
         List<NodeData> nodeStates = new ArrayList<NodeData>();
         for (int i = 0; i < rules.length; i++)
         {
            nodeStates.addAll(rules[i].resolve(nodeState));
         }
         if (nodeStates.size() > 0)
         {
            return nodeStates.toArray(new NodeData[nodeStates.size()]);
         }
      }
      return null;
   }

   // ---------------------------< internal
   // >-----------------------------------

   /**
    * Reads the node type of the root node of the indexing aggregate.
    * 
    * @param config
    *          the configuration.
    * @return the name of the node type.
    * @throws IllegalNameException
    *           if the node type name contains illegal characters.
    * @throws RepositoryException
    * @throws PathNotFoundException
    */
   private InternalQName getNodeTypeName(Node config) throws IllegalNameException, PathNotFoundException,
            RepositoryException
   {
      String ntString = config.getAttributes().getNamedItem("primaryType").getNodeValue();
      return resolver.parseJCRName(ntString).getInternalName();
   }

   /**
    * Creates rules defined in the <code>config</code>.
    * 
    * @param config
    *          the indexing aggregate configuration.
    * @return the rules defined in the <code>config</code>.
    * @throws MalformedPathException
    *           if a path in the configuration is malformed.
    * @throws IllegalNameException
    *           if the node type name contains illegal characters.
    * @throws RepositoryException
    * @throws PathNotFoundException
    */
   private Rule[] getRules(Node config) throws IllegalNameException, PathNotFoundException, RepositoryException
   {
      List<Rule> newRules = new ArrayList<Rule>();
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
            // PathBuilder builder = new PathBuilder();
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
            newRules.add(new Rule(new QPath(path), ntName));
         }
      }
      return newRules.toArray(new Rule[newRules.size()]);
   }

   // ---------------------------< internal >-----------------------------------

   /**
    * @param node
    *          a node.
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
            content.append(((CharacterData) n).getData());
         }
      }
      return content.toString();
   }

   private final class Rule
   {

      /**
       * Optional node type name.
       */
      private final InternalQName nodeTypeName;

      /**
       * A relative path pattern.
       */
      private final QPath pattern;

      /**
       * Creates a new rule with a relative path pattern and an optional node type name.
       * 
       * @param nodeTypeName
       *          node type name or <code>null</code> if all node types are allowed.
       * @param pattern
       *          a relative path pattern.
       */
      private Rule(QPath pattern, InternalQName nodeTypeName)
      {
         this.nodeTypeName = nodeTypeName;
         this.pattern = pattern;
      }

      /**
       * If the given <code>nodeState</code> matches this rule the root node state of the indexing
       * aggregate is returned.
       * 
       * @param nodeState
       *          a node state.
       * @return the root node state of the indexing aggregate or <code>null</code> if
       *         <code>nodeState</code> does not belong to an indexing aggregate defined by this rule.
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
               NodeData parent = (NodeData) ism.getItemData(parentId);
               if (elements[e].getName().equals("*"))
               {
                  // match any parent
                  nodeState = parent;
               }
               else
               {
                  // check name
                  ItemData item = ism.getItemData(nodeState.getIdentifier());

                  if (item != null && item.isNode() && elements[e].equals(item.getQPath().getName()))
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

      /**
       * Resolves the <code>nodeState</code> using this rule.
       * 
       * @param nodeState
       *          the root node of the enclosing indexing aggregate.
       * @return the descendant node states as defined by this rule.
       * @throws RepositoryException
       * @throws ItemStateException
       *           if an error occurs while resolving the node states.
       */
      List<NodeData> resolve(NodeData nodeState) throws RepositoryException
      {
         List<NodeData> nodeStates = new ArrayList<NodeData>();
         resolve(nodeState, nodeStates, 0);
         return nodeStates;
      }

      // -----------------------------< internal >-----------------------------

      /**
       * Recursively resolves node states along the path {@link #pattern}.
       * 
       * @param nodeState
       *          the current node state.
       * @param collector
       *          resolved node states are collected using the list.
       * @param offset
       *          the current path element offset into the path pattern.
       * @throws RepositoryException
       * @throws ItemStateException
       *           if an error occurs while accessing node states.
       */
      private void resolve(NodeData nodeState, List<NodeData> collector, int offset) throws RepositoryException
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
            ItemData item = ism.getItemData(nodeState, currentName);
            if (item != null && item.isNode())
            {
               cne.add((NodeData) item);
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
}
