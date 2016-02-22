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
package org.exoplatform.services.jcr.impl.util;

import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.ValueData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS 15.05.2006 NodeData bulk reader.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeDataReader.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class NodeDataReader extends ItemDataReader
{

   private final HashMap<InternalQName, NodeInfo> nodes = new HashMap<InternalQName, NodeInfo>();

   private final HashMap<InternalQName, NodeInfo> nodesByType = new HashMap<InternalQName, NodeInfo>();

   private PropertyDataReader nodePropertyReader = null;

   private final List<NodeData> skiped = new ArrayList<NodeData>();

   private boolean rememberSkiped = false;

   private class NodeInfo
   {
      private final InternalQName nodeName;

      private final List<NodeDataReader> childNodesReaders;

      NodeInfo(InternalQName nodeName, List<NodeDataReader> childNodesReaders)
      {
         this.nodeName = nodeName;
         this.childNodesReaders = childNodesReaders;
      }

      public InternalQName getNodeName()
      {
         return nodeName;
      }

      public List<NodeDataReader> getChildNodesReaders()
      {
         return childNodesReaders;
      }
   }

   public NodeDataReader(NodeData node, DataManager dataManager)
   {
      super(node, dataManager);
   }

   public NodeDataReader forNodesByType(InternalQName name)
   {
      nodesByType.put(name, new NodeInfo(name, new ArrayList<NodeDataReader>()));
      return this;
   }

   public NodeDataReader forNode(InternalQName name)
   {
      nodes.put(name, new NodeInfo(name, new ArrayList<NodeDataReader>()));
      return this;
   }

   public List<NodeDataReader> getNodes(InternalQName name) throws PathNotFoundException
   {
      List<NodeDataReader> nr = nodes.get(name).getChildNodesReaders();
      if (nr.size() > 0)
         return nr;
      throw new PathNotFoundException("Node with name " + parent.getQPath().getAsString() + name.getAsString()
         + " not found");
   }

   public List<NodeDataReader> getNodesByType(InternalQName typeName) throws PathNotFoundException
   {
      List<NodeDataReader> nr = nodesByType.get(typeName).getChildNodesReaders();
      if (nr.size() > 0)
         return nr;
      throw new PathNotFoundException("Nodes with type " + typeName.getAsString() + " not found. Parent "
         + parent.getQPath().getAsString());
   }

   public ValueData getPropertyValue(InternalQName name) throws ValueFormatException, PathNotFoundException,
      RepositoryException
   {
      return nodePropertyReader.getPropertyValue(name);
   }

   public List<ValueData> getPropertyValues(InternalQName name) throws ValueFormatException, PathNotFoundException,
      RepositoryException
   {
      return nodePropertyReader.getPropertyValues(name);
   }

   /**
    * Read node properties
    * 
    * @param name
    * @param type
    * @return
    */
   public PropertyDataReader forProperty(InternalQName name, int type)
   {
      if (nodePropertyReader == null)
      {
         nodePropertyReader = new PropertyDataReader(parent, dataManager);
      }
      return nodePropertyReader.forProperty(name, type);
   }

   public void read() throws RepositoryException
   {

      cleanReaders();

      if (nodePropertyReader != null)
      {
         nodePropertyReader.read();
      }

      if (nodes.size() > 0 || nodesByType.size() > 0 || rememberSkiped)
      {

         List<NodeData> ndNodes = dataManager.getChildNodesData(parent);
         for (NodeData node : ndNodes)
         {

            boolean isSkiped = true;

            NodeDataReader cnReader = new NodeDataReader(node, dataManager);

            NodeInfo nodeInfo = nodes.get(node.getQPath().getName());
            if (nodeInfo != null)
            {
               nodeInfo.getChildNodesReaders().add(cnReader);
               isSkiped = false;
            }

            NodeInfo nodesByTypeInfo = nodesByType.get(node.getPrimaryTypeName());
            if (nodesByTypeInfo != null)
            {
               nodesByTypeInfo.getChildNodesReaders().add(cnReader);
               isSkiped = false;
            }

            if (isSkiped && rememberSkiped)
            {
               skiped.add(node);
            }
         }
      }
   }

   private void cleanReaders()
   {
      for (Map.Entry<InternalQName, NodeInfo> nodesEntry : nodes.entrySet())
      {
         nodesEntry.getValue().getChildNodesReaders().clear();
      }
      for (Map.Entry<InternalQName, NodeInfo> nodesEntry : nodesByType.entrySet())
      {
         nodesEntry.getValue().getChildNodesReaders().clear();
      }
      skiped.clear();
   }

   public void clean()
   {
      nodes.clear();
      nodesByType.clear();
      skiped.clear();
      nodePropertyReader = null;
   }

   public boolean isRememberSkiped()
   {
      return rememberSkiped;
   }

   public void setRememberSkiped(boolean rememberSkiped)
   {
      this.rememberSkiped = rememberSkiped;
   }

   public List<NodeData> getSkiped()
   {
      return skiped;
   }
}
