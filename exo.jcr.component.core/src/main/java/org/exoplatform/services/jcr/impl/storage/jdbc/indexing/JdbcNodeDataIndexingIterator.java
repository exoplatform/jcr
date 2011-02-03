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
package org.exoplatform.services.jcr.impl.storage.jdbc.indexing;

import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NodeDataIndexing;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.core.query.NodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.GenericConnectionFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 1 02 2011
 * 
 * Iterator for fetching NodeData from database with all properties and its values.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JdbcIndexingDataIterator.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JdbcNodeDataIndexingIterator implements NodeDataIndexingIterator
{

   /**
    * Connection factory. Allows to open jdbc storage connection. 
    */
   private final GenericConnectionFactory connFactory;

   /**
    * The amount of the rows which could be retrieved from database for once.
    */
   private final int pageSize;

   /**
    * The current offset in database.
    */
   private int offset = 0;

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JdbcIndexingDataIterator");

   /**
    * The list of nodes to return in next() method.
    */
   private List<NodeDataIndexing> current;

   /**
    * Node data which may not contains all its properties. It is the last
    * data in the resulted list in readNext() method. Should be merged with
    * first one during next invoking readNext() method.
    */
   private NodeDataIndexing unCompletedNode;

   /**
    * Indicates if all rows has been read and no need more access to database.
    */
   private boolean isReadAll = false;

   /**
    * Constructor JdbcIndexingDataIterator.
    * 
    */
   public JdbcNodeDataIndexingIterator(GenericConnectionFactory connFactory, int pageSize) throws RepositoryException
   {
      this.connFactory = connFactory;
      this.pageSize = pageSize;
      this.current = readNext();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNext()
   {
      return this.current.size() != 0;
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeDataIndexing> next() throws RepositoryException
   {
      List<NodeDataIndexing> next = this.current;
      this.current = readNext();

      return next;
   }

   /**
    * Read nodes from database.  
    * 
    * @return List<NodeDataIndexing> 
    * @throws RepositoryException 
    */
   private List<NodeDataIndexing> readNext() throws RepositoryException
   {
      if (isReadAll)
      {
         return new ArrayList<NodeDataIndexing>();
      }

      List<NodeDataIndexing> result = getNodesAndProperties();

      if (result.isEmpty())
      {
         if (unCompletedNode != null)
         {
            result.add(unCompletedNode);
            unCompletedNode = null;
         }
      }
      else
      {
         if (unCompletedNode != null)
         {
            NodeDataIndexing node = result.get(0);
            if (unCompletedNode.getIdentifier().equals(node.getIdentifier()))
            {
               result.set(0, mergeWithUnCompletedNode(node));
               unCompletedNode = null;
            }
            else
            {
               result.add(0, unCompletedNode);
            }
         }

         if (!isReadAll)
         {
            unCompletedNode = result.remove(result.size() - 1);
         }

         if (result.isEmpty())
         {
            return readNext();
         }
      }

      return result;
   }

   /**
    * Read next nodes from database. 
    * 
    * @return List
    * @throws RepositoryException 
    */
   private List<NodeDataIndexing> getNodesAndProperties() throws RepositoryException
   {
      JDBCStorageConnection conn = (JDBCStorageConnection)connFactory.openConnection();
      try
      {
         List<NodeDataIndexing> result = new ArrayList<NodeDataIndexing>();

         int read = conn.getNodesAndProperties(offset, pageSize, result);
         offset += pageSize;
         
         isReadAll = read != pageSize;

         return result;
      }
      finally
      {
         conn.close();
      }
   }

   /**
    * Merge two nodes.
    */
   private NodeDataIndexing mergeWithUnCompletedNode(NodeDataIndexing node)
   {
      InternalQName primaryType = unCompletedNode.getPrimaryTypeName();
      if (primaryType == null)
      {
         primaryType = node.getPrimaryTypeName();
      }

      List<InternalQName> mixins = new ArrayList<InternalQName>();
      for (InternalQName mixin : unCompletedNode.getMixinTypeNames())
      {
         mixins.add(mixin);
      }

      for (InternalQName mixin : node.getMixinTypeNames())
      {
         mixins.add(mixin);
      }

      NodeData nodeData =
         new PersistedNodeData(unCompletedNode.getIdentifier(), unCompletedNode.getQPath(),
            unCompletedNode.getParentIdentifier(), unCompletedNode.getPersistedVersion(),
            unCompletedNode.getOrderNumber(), primaryType, mixins.toArray(new InternalQName[mixins.size()]), null);

      Map<String, PropertyData> props = new HashMap<String, PropertyData>();
      for (PropertyData prop : unCompletedNode.getChildPropertiesData())
      {
         props.put(prop.getIdentifier(), prop);
      }

      for (PropertyData prop : node.getChildPropertiesData())
      {
         if (props.containsKey(prop.getIdentifier()))
         {
            List<ValueData> values = new ArrayList<ValueData>(props.get(prop.getIdentifier()).getValues());
            values.addAll(prop.getValues());
            
            PropertyData propertyData =
               new PersistedPropertyData(prop.getIdentifier(), prop.getQPath(), prop.getParentIdentifier(),
                  prop.getPersistedVersion(), prop.getType(), prop.isMultiValued(), values);

            props.put(prop.getIdentifier(), propertyData);
         }
         else
         {
            props.put(prop.getIdentifier(), prop);
         }
      }

      return new NodeDataIndexing(nodeData, new ArrayList<PropertyData>(props.values()));
   }
}

