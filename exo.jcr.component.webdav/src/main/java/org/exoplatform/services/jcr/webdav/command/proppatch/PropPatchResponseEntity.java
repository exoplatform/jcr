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
package org.exoplatform.services.jcr.webdav.command.proppatch;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.WorkspaceImpl;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;
import org.exoplatform.services.jcr.webdav.xml.PropertyWriteUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS. Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>
 * 
 * @version $Id: $
 */

public class PropPatchResponseEntity implements StreamingOutput
{

   /**
    * Namespace context.
    */
   private final WebDavNamespaceContext nsContext;

   /**
    * Node.
    */
   private Node node;

   /**
    * Uri.
    */
   private final URI uri;

   /**
    * The list of properties to set.
    */
   private final List<HierarchicalProperty> setList;

   /**
    * The list of properties ro remove.
    */
   private final List<HierarchicalProperty> removeList;

   /**
    * The list of properties that can-not be removed.
    */
   protected final static Set<QName> NON_REMOVING_PROPS = new HashSet<QName>();
   static
   {
      NON_REMOVING_PROPS.add(PropertyConstants.CREATIONDATE);
      NON_REMOVING_PROPS.add(PropertyConstants.DISPLAYNAME);
      NON_REMOVING_PROPS.add(PropertyConstants.GETCONTENTLANGUAGE);
      NON_REMOVING_PROPS.add(PropertyConstants.GETCONTENTLENGTH);
      NON_REMOVING_PROPS.add(PropertyConstants.GETCONTENTTYPE);
      NON_REMOVING_PROPS.add(PropertyConstants.GETLASTMODIFIED);

      NON_REMOVING_PROPS.add(PropertyConstants.JCR_DATA);
   };

   /**
    * The list of properties that can-not be changed.
    */
   protected final static Set<QName> READ_ONLY_PROPS = new HashSet<QName>();
   static
   {
      READ_ONLY_PROPS.add(PropertyConstants.JCR_DATA);
   };

   /**
    * Constructor.
    * 
    * @param nsContext namespace context
    * @param node node
    * @param uri iru
    * @param setList list of properties to set
    * @param removeList list of properties to remove
    */
   public PropPatchResponseEntity(WebDavNamespaceContext nsContext, Node node, URI uri,
      List<HierarchicalProperty> setList, List<HierarchicalProperty> removeList)
   {
      this.nsContext = nsContext;
      this.node = node;
      this.uri = uri;
      this.setList = setList;
      this.removeList = removeList;
   }

   /**
    * {@inheritDoc}
    */
   public void write(OutputStream outStream) throws IOException
   {
      try
      {
         XMLStreamWriter xmlStreamWriter =
            XMLOutputFactory.newInstance().createXMLStreamWriter(outStream, Constants.DEFAULT_ENCODING);

         xmlStreamWriter.setNamespaceContext(nsContext);
         xmlStreamWriter.setDefaultNamespace("DAV:");

         xmlStreamWriter.writeStartDocument();
         xmlStreamWriter.writeStartElement("D", "multistatus", "DAV:");
         xmlStreamWriter.writeNamespace("D", "DAV:");

         xmlStreamWriter.writeAttribute("xmlns:b", "urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/");

         xmlStreamWriter.writeStartElement("DAV:", "response");
         xmlStreamWriter.writeStartElement("DAV:", "href");
         xmlStreamWriter.writeCharacters(URLDecoder.decode(uri.toASCIIString(), "UTF-8"));
         xmlStreamWriter.writeEndElement();

         Map<String, Set<HierarchicalProperty>> propStats = getPropStat();
         PropertyWriteUtil.writePropStats(xmlStreamWriter, propStats);

         xmlStreamWriter.writeEndElement();

         // D:multistatus
         xmlStreamWriter.writeEndElement();
         xmlStreamWriter.writeEndDocument();

      }
      catch (XMLStreamException exc)
      {
         throw new IOException(exc.getMessage());
      }
   }

   /**
    * Performs manipulations with properties and returns the list of
    * corresponding statuses.
    * 
    * @return map with the list of properties statuses
    */
   protected Map<String, Set<HierarchicalProperty>> getPropStat()
   {
      Map<String, Set<HierarchicalProperty>> propStats = new HashMap<String, Set<HierarchicalProperty>>();

      for (int i = 0; i < setList.size(); i++)
      {
         HierarchicalProperty setProperty = setList.get(i);

         String statname = WebDavConst.getStatusDescription(HTTPStatus.OK);

         try
         {

            if (setProperty.getStringName().equals(WebDavConst.NodeTypes.JCR_CONTENT))
            {
               for (HierarchicalProperty child : setProperty.getChildren())
               {

                  if (child.getChildren().isEmpty())
                  {

                     if (node.isNodeType(WebDavConst.NodeTypes.MIX_VERSIONABLE) && !node.isCheckedOut())
                     {
                        node.checkout();
                        node.save();
                     }

                     Node content = getContentNode();
                     statname = setProperty(content, child);

                     if (!propStats.containsKey(statname))
                     {
                        propStats.put(statname, new HashSet<HierarchicalProperty>());
                     }

                     Set<HierarchicalProperty> propSet = propStats.get(statname);

                     HierarchicalProperty jcrContentProp = new HierarchicalProperty(PropertyConstants.JCR_CONTENT);
                     jcrContentProp.addChild(new HierarchicalProperty(child.getName()));

                     propSet.add(jcrContentProp);

                  }
                  else
                  {
                  }

               }
            }
            else
            {
               statname = setProperty(node, setProperty);

               if (!propStats.containsKey(statname))
               {
                  propStats.put(statname, new HashSet<HierarchicalProperty>());
               }

               Set<HierarchicalProperty> propSet = propStats.get(statname);
               propSet.add(new HierarchicalProperty(setProperty.getName()));

            }

         }
         catch (RepositoryException e)
         {
            statname = WebDavConst.getStatusDescription(HTTPStatus.CONFLICT);
         }

      }

      for (int i = 0; i < removeList.size(); i++)
      {
         HierarchicalProperty removeProperty = removeList.get(i);

         String statname = WebDavConst.getStatusDescription(HTTPStatus.OK);

         if (NON_REMOVING_PROPS.contains(removeProperty.getName()))
         {
            statname = WebDavConst.getStatusDescription(HTTPStatus.CONFLICT);

            if (!propStats.containsKey(statname))
            {
               propStats.put(statname, new HashSet<HierarchicalProperty>());
            }

            Set<HierarchicalProperty> propSet = propStats.get(statname);
            propSet.add(new HierarchicalProperty(removeProperty.getName()));

            continue;

         }

         if (removeProperty.getStringName().equals(WebDavConst.NodeTypes.JCR_CONTENT))
         {
            for (HierarchicalProperty child : removeProperty.getChildren())
            {
               try
               {
                  Node content = getContentNode();
                  statname = removeProperty(content, child);
                  if (!propStats.containsKey(statname))
                  {
                     propStats.put(statname, new HashSet<HierarchicalProperty>());
                  }

                  Set<HierarchicalProperty> propSet = propStats.get(statname);

                  HierarchicalProperty jcrContentProp =
                     new HierarchicalProperty(new QName(WebDavConst.NodeTypes.JCR_CONTENT));
                  jcrContentProp.addChild(new HierarchicalProperty(child.getName()));
                  propSet.add(jcrContentProp);

               }
               catch (RepositoryException e)
               {
                  statname = WebDavConst.getStatusDescription(HTTPStatus.CONFLICT);
               }
            }
         }
         else
         {
            statname = removeProperty(node, removeProperty);
            if (!propStats.containsKey(statname))
            {
               propStats.put(statname, new HashSet<HierarchicalProperty>());
            }

            Set<HierarchicalProperty> propSet = propStats.get(statname);
            propSet.add(new HierarchicalProperty(removeProperty.getName()));
         }

      }

      return propStats;
   }

   /**
    * Gets the content node.
    * 
    * @return content node
    * @throws RepositoryException Repository exception.
    */
   public Node getContentNode() throws RepositoryException
   {
      return node.getNode("jcr:content");
   }

   /**
    * Sets changes the property value.
    * 
    * @param node node
    * @param property property to set.
    * @return status
    */
   private String setProperty(Node node, HierarchicalProperty property)
   {

      String propertyName = WebDavNamespaceContext.createName(property.getName());

      if (READ_ONLY_PROPS.contains(property.getName()))
      {
         return WebDavConst.getStatusDescription(HTTPStatus.CONFLICT);
      }

      try
      {

         Workspace ws = node.getSession().getWorkspace();
         NodeTypeDataManager nodeTypeHolder = ((WorkspaceImpl)ws).getNodeTypesHolder();
         NodeData data = (NodeData)((NodeImpl)node).getData();
         InternalQName propName =
            ((SessionImpl)node.getSession()).getLocationFactory().parseJCRName(propertyName).getInternalName();
         PropertyDefinitionDatas propdefs =
            nodeTypeHolder.getPropertyDefinitions(propName, data.getPrimaryTypeName(), data.getMixinTypeNames());
         if (propdefs == null)
         {
            throw new RepositoryException();
         }

         PropertyDefinitionData propertyDefinitionData = propdefs.getAnyDefinition();
         if (propertyDefinitionData == null)
         {
            throw new RepositoryException();
         }

         boolean isMultiValued = propertyDefinitionData.isMultiple();

         if (node.isNodeType(WebDavConst.NodeTypes.MIX_VERSIONABLE) && !node.isCheckedOut())
         {
            node.checkout();
            node.save();
         }

         if (!isMultiValued)
         {
            node.setProperty(propertyName, property.getValue());
         }
         else
         {
            String[] value = new String[1];
            value[0] = property.getValue();
            node.setProperty(propertyName, value);
         }

         node.save();
         return WebDavConst.getStatusDescription(HTTPStatus.OK);

      }
      catch (AccessDeniedException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.FORBIDDEN);
      }
      catch (ItemNotFoundException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.NOT_FOUND);
      }
      catch (PathNotFoundException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.NOT_FOUND);
      }
      catch (RepositoryException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.CONFLICT);

      }

   }

   /**
    * Removes the property.
    * 
    * @param node node
    * @param property property
    * @return status
    */
   private String removeProperty(Node node, HierarchicalProperty property)
   {

      try
      {
         node.getProperty(property.getStringName()).remove();
         node.save();
         return WebDavConst.getStatusDescription(HTTPStatus.OK);
      }
      catch (AccessDeniedException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.FORBIDDEN);
      }
      catch (ItemNotFoundException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.NOT_FOUND);
      }
      catch (PathNotFoundException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.NOT_FOUND);
      }
      catch (RepositoryException e)
      {
         return WebDavConst.getStatusDescription(HTTPStatus.CONFLICT);
      }

   }

}
