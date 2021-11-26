/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemDataVisitor;

import java.util.Collection;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 1 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: NodeDataIndexing.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class NodeDataIndexing implements NodeData
{
   private final NodeData nodeData;

   private final Map<String, PropertyData> properties;

   /**
    * Constructor NodeDataIndexing.
    */
   public NodeDataIndexing(NodeData nodeData)
   {
      this(nodeData, null);
   }

   /**
    * Constructor NodeDataIndexing.
    */
   public NodeDataIndexing(NodeData nodeData, Map<String, PropertyData> properties)
   {
      this.nodeData = nodeData;
      this.properties = properties;
   }

   /**
    * List of properties data.
    * 
    * @return Collection
    */
   public Collection<PropertyData> getChildPropertiesData()
   {
      return properties == null ? null : properties.values();
   }

   /**
    * Property data.
    * 
    * @return PropertyData
    */
   public PropertyData getProperty(String name)
   {
      return properties == null ? null : properties.get(name);
   }

   /**
    * {@inheritDoc}
    */
   public QPath getQPath()
   {
      return nodeData.getQPath();
   }

   /**
    * {@inheritDoc}
    */
   public String getIdentifier()
   {
      return nodeData.getIdentifier();
   }

   /**
    * {@inheritDoc}
    */
   public int getPersistedVersion()
   {
      return nodeData.getPersistedVersion();
   }

   /**
    * {@inheritDoc}
    */
   public String getParentIdentifier()
   {
      return nodeData.getParentIdentifier();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNode()
   {
      return nodeData.isNode();
   }

   /**
    * {@inheritDoc}
    */
   public void accept(ItemDataVisitor visitor) throws RepositoryException
   {
      nodeData.accept(visitor);
   }

   /**
    * {@inheritDoc}
    */
   public int getOrderNumber()
   {
      return nodeData.getOrderNumber();
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getPrimaryTypeName()
   {
      return nodeData.getPrimaryTypeName();
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName[] getMixinTypeNames()
   {
      return nodeData.getMixinTypeNames();
   }

   /**
    * {@inheritDoc}
    */
   public AccessControlList getACL()
   {
      return nodeData.getACL();
   }

   /**
    * Indicates if all properties was read from storage.
    */
   public boolean containAllProperties()
   {
      return properties != null;
   }
}
