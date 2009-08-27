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
package org.exoplatform.services.jcr.ext.resource.representation;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.resource.NodeRepresentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class NtResourceNodeRepresentation implements NodeRepresentation
{

   protected Node node;

   protected ArrayList<HierarchicalProperty> properties;

   /**
    * @param node
    * @throws RepositoryException
    */
   public NtResourceNodeRepresentation(Node node) throws RepositoryException
   {

      this.node = node;
      this.properties = new ArrayList<HierarchicalProperty>();

      PropertyIterator iter = node.getProperties();

      while (iter.hasNext())
      {
         Property prop = iter.nextProperty();
         String name = prop.getName();
         if (!"jcr:primaryType".equals(name) && !"jcr:mixinTypes".equals(name) && !"jcr:data".equals(name)
            && !"jcr:uuid".equals(name))
         {
            String ns = ((ExtendedSession)node.getSession()).getLocationFactory().parseJCRName(name).getNamespace();
            String value = prop.getString();
            properties.add(new HierarchicalProperty(name, value, ns));
         }
      }

   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getContentEncoding()
    */
   public String getContentEncoding()
   {
      return null;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getContentLenght()
    */
   public long getContentLenght() throws RepositoryException
   {
      return node.getProperty("jcr:data").getLength();
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getMediaType()
    */
   public String getMediaType() throws RepositoryException
   {
      return node.getProperty("jcr:mimeType").getString();
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getLastModified()
    */
   public long getLastModified() throws RepositoryException
   {
      return node.getProperty("jcr:lastModified").getLong();
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getNode()
    */
   public Node getNode()
   {
      return node;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getProperty(java.lang.String)
    */
   public HierarchicalProperty getProperty(String name) throws RepositoryException
   {

      for (HierarchicalProperty p : properties)
      {
         if (p.getStringName().equals(name))
            return p;
      }
      return null;

      // if ("jcr:primaryType".equals(name) || "jcr:mixinTypes".equals(name) ||
      // "jcr:data".equals(name)
      // || "jcr:uuid".equals(name))
      // return null;
      //
      // String value;
      // try {
      // value = node.getProperty(name).getString();
      // } catch (PathNotFoundException e) {
      // return null;
      // }
      // String ns = ((ExtendedSession) node.getSession()).getLocationFactory()
      // .parseJCRName(name)
      // .getNamespace();
      // return new HierarchicalProperty(name, value, ns);

   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getProperties(java.lang.String)
    */
   public Collection<HierarchicalProperty> getProperties(String name) throws RepositoryException
   {
      ArrayList<HierarchicalProperty> props = new ArrayList<HierarchicalProperty>();
      for (HierarchicalProperty p : properties)
      {
         if (p.getStringName().equals(name))
            props.add(p);
      }
      return props;
      // HierarchicalProperty prop = getProperty(name);
      // if(prop != null)
      // props.add( prop );
      // return props;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getPropertyNames()
    */
   public Collection<String> getPropertyNames() throws RepositoryException
   {

      HashSet<String> props = new HashSet<String>();
      for (HierarchicalProperty p : properties)
      {
         props.add(p.getStringName());
      }
      return props;

      // PropertyIterator iter = node.getProperties();
      // ArrayList<String> props = new ArrayList<String>();
      // while (iter.hasNext()) {
      // String name = iter.nextProperty().getName();
      // if (!"jcr:primaryType".equals(name) && !"jcr:mixinTypes".equals(name)
      // && !"jcr:data".equals(name) && !"jcr:uuid".equals(name))
      // props.add(name);
      // }
      // return props;
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#getInputStream()
    */
   public InputStream getInputStream() throws IOException, RepositoryException
   {
      return node.getProperty("jcr:data").getStream();
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#addProperties(java.lang.String, java.util.Collection)
    */
   public void addProperties(Collection<HierarchicalProperty> properties)
      throws UnsupportedRepositoryOperationException
   {

      this.properties.addAll(properties);
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#addProperty(java.lang.String, org.exoplatform.common.util.HierarchicalProperty)
    */
   public void addProperty(HierarchicalProperty property) throws UnsupportedRepositoryOperationException
   {

      this.properties.add(property);
   }

   /* (non-Javadoc)
    * @see org.exoplatform.services.jcr.ext.resource.NodeRepresentation#removeProperty(java.lang.String)
    */
   public void removeProperty(String name) throws UnsupportedRepositoryOperationException
   {

      for (int i = 0; i < properties.size(); i++)
      {
         if (properties.get(i).getStringName().equals(name))
            properties.remove(i);
      }

   }

}
