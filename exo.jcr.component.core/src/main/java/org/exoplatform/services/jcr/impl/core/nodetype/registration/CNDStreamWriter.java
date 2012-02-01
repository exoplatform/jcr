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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS.<br>
 * Class provides CND grammar manipulation tool for writing list of node types
 * and concomitant namespaces to stream in CDN text format.
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: $
 */
public class CNDStreamWriter
{

   /** 
    * RegistryImpl instance
    */
   private final NamespaceRegistryImpl namespaceRegistry;

   /**
    * Location factory instance 
    */
   private final LocationFactory locationFactory;

   /**
    * Constructs instance of CNDStreamWriter. Instance of NamespaceRegistryImpl
    * is used to convert InternalQnames to strings.
    * 
    * @param namespaceRegistry
    */
   public CNDStreamWriter(NamespaceRegistryImpl namespaceRegistry)
   {
      this.namespaceRegistry = namespaceRegistry;
      this.locationFactory = new LocationFactory(namespaceRegistry);
   }

   /**
    * Write given list of node types to output stream.
    * 
    * @param nodeTypes
    *           List of NodeTypes to write.
    * @param os
    *           OutputStream to write to.
    * @throws RepositoryException
    */
   public void write(List<NodeTypeData> nodeTypes, OutputStream os) throws RepositoryException
   {
      OutputStreamWriter out = new OutputStreamWriter(os);
      try
      {
         for (NodeTypeData nodeType : nodeTypes)
         {
            printNamespaces(nodeType, out);
            printNodeTypeDeclaration(nodeType, out);
         }
         out.close();
      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }

   }

   /**
    * Print namespaces to stream
    * 
    * @param nodeTypeData
    * @param out
    * @throws RepositoryException
    * @throws IOException
    */
   private void printNamespaces(NodeTypeData nodeTypeData, OutputStreamWriter out) throws RepositoryException,
      IOException
   {

      /**
       * Using set to store all prefixes found in node types to avoid
       * duplication
       */
      Set<String> namespaces = new HashSet<String>();
      /** Scanning nodeType definition for used namespaces */
      printNameNamespace(nodeTypeData.getName(), namespaces);
      printNameNamespace(nodeTypeData.getPrimaryItemName(), namespaces);
      if (nodeTypeData.getDeclaredSupertypeNames() != null)
      {
         for (InternalQName reqType : nodeTypeData.getDeclaredSupertypeNames())
         {
            printNameNamespace(reqType, namespaces);
         }
      }
      /** Scanning property definitions for used namespaces */
      if (nodeTypeData.getDeclaredPropertyDefinitions() != null)
      {
         for (PropertyDefinitionData property : nodeTypeData.getDeclaredPropertyDefinitions())
         {
            printNameNamespace(property.getName(), namespaces);
         }
      }
      /** Scanning child definitions for used namespaces */
      if (nodeTypeData.getDeclaredChildNodeDefinitions() != null)
      {
         for (NodeDefinitionData child : nodeTypeData.getDeclaredChildNodeDefinitions())
         {
            printNameNamespace(child.getName(), namespaces);
            printNameNamespace(child.getDefaultPrimaryType(), namespaces);
            if (child.getRequiredPrimaryTypes() != null)
            {
               for (InternalQName reqType : child.getRequiredPrimaryTypes())
               {
                  printNameNamespace(reqType, namespaces);
               }
            }
         }
      }
      for (String prefix : namespaces)
      {
         String uri = namespaceRegistry.getURI(prefix);
         out.write("<" + prefix + "='" + uri + "'>\r\n");
      }
   }

   private void printNameNamespace(InternalQName name, Set<String> namespaces) throws RepositoryException
   {
      /** Adding current name's prefix to set if it is not default */
      if (name != null)
      {
         String prefix = locationFactory.createJCRName(name).getPrefix();
         if (!namespaceRegistry.isDefaultPrefix(prefix))
         {
            namespaces.add(prefix);
         }
      }
   }

   /**
    * Method recursively print to output stream node type definition in cnd
    * format.
    * 
    * @param nodeTypeData
    *           is nodeType to print
    * @throws RepositoryException
    *            this exception may be while converting QNames to string
    * @throws IOException
    *            this exception my be thrown during some operations with streams
    */
   private void printNodeTypeDeclaration(NodeTypeData nodeTypeData, OutputStreamWriter out) throws RepositoryException,
      IOException
   {

      /** Print name */
      out.write("[" + qNameToString(nodeTypeData.getName()) + "] ");
      /** Print supertypes */
      InternalQName[] superTypes = nodeTypeData.getDeclaredSupertypeNames();
      if (superTypes != null && superTypes.length > 0)
      {
         /** if there is only 1 element and it is NT_BASE then avoid printing it */
         if (superTypes.length > 1 || !superTypes[0].equals(Constants.NT_BASE))
         {
            out.write("> " + qNameToString(superTypes[0]));
            for (int i = 1; i < superTypes.length; i++)
            {
               out.write(", " + qNameToString(superTypes[i]));
            }
         }
      }

      /** Print attributes */
      StringBuffer attributes = new StringBuffer();
      //      if (nodeTypeData.isAbstract())
      //      {
      //         attributes += "abstract ";
      //      }
      if (nodeTypeData.hasOrderableChildNodes())
      {
         attributes.append("orderable ");
      }
      if (nodeTypeData.isMixin())
      {
         attributes.append("mixin ");
      }
      //      if (!nodeTypeData.isQueryable())
      //      {
      //         attributes += "noquery ";
      //      }
      if (nodeTypeData.getPrimaryItemName() != null)
      {
         attributes.append("primaryitem " + qNameToString(nodeTypeData.getPrimaryItemName()));
      }
      if (attributes.length() > 0)
      {
         out.write("\r\n  ");
         out.write(attributes.toString());
      }
      /** Print all property definitions */
      PropertyDefinitionData[] propertyDefinitions = nodeTypeData.getDeclaredPropertyDefinitions();
      if (propertyDefinitions != null)
      {
         for (PropertyDefinitionData propertyDefinition : propertyDefinitions)
         {
            printPropertyDeclaration(propertyDefinition, out);
         }
      }
      /** Print all child definitions */
      NodeDefinitionData[] nodeDefinitions = nodeTypeData.getDeclaredChildNodeDefinitions();
      if (nodeDefinitions != null)
      {
         for (NodeDefinitionData nodeDefinition : nodeDefinitions)
         {
            printChildDeclaration(nodeDefinition, out);
         }
      }

      out.write("\r\n");
   }

   /**
    * Prints to output stream property definition in CND format
    * 
    * @param propertyDefinition
    * @throws IOException
    * @throws RepositoryException
    */
   private void printPropertyDeclaration(PropertyDefinitionData propertyDefinition, OutputStreamWriter out)
      throws IOException, RepositoryException
   {
      /** Print name */
      out.write("\r\n  ");
      out.write("- " + qNameToString(propertyDefinition.getName()));
      out.write(" (" + ExtendedPropertyType.nameFromValue(propertyDefinition.getRequiredType()).toUpperCase() + ")");
      /** Print default values */
      out.write(listToString(propertyDefinition.getDefaultValues(), "'", "\r\n    = ", " "));

      /** Print attributes */
      StringBuffer attributes = new StringBuffer();

      if (propertyDefinition.isAutoCreated())
      {
         attributes.append("autocreated ");
      }
      if (propertyDefinition.isMandatory())
      {
         attributes.append("mandatory ");
      }
      if (propertyDefinition.isProtected())
      {
         attributes.append("protected ");
      }
      if (propertyDefinition.isMultiple())
      {
         attributes.append("multiple ");
      }
      //      if (!propertyDefinition.isFullTextSearchable())
      //      {
      //         attributes += "nofulltext ";
      //      }
      //      if (!propertyDefinition.isQueryOrderable())
      //      {
      //         attributes += "noqueryorder ";
      //      }

      //      /** Print operators avoiding printing all */
      //      String[] opArray = propertyDefinition.getAvailableQueryOperators();
      //      /** Using set to avoid duplication */
      //      Set<String> opSet = new HashSet<String>();
      //      for (String op : opArray)
      //      {
      //         opSet.add(op.toUpperCase());
      //      }
      //      /** if not all elements are mentioned in list then print */
      //      if (opSet.size() < 7 && opSet.size() > 0)
      //      {
      //         String opString = opSet.toString();
      //         // opString="queryops '"+opString.substring(1, opString.length()-1)+"' ";
      //         opString = listToString(opSet.toArray(new String[opSet.size()]), "", "\r\n    queryops '", "' ");
      //         attributes += opString;
      //      }

      if (propertyDefinition.getOnParentVersion() != OnParentVersionAction.COPY)
      {
         attributes.append("\r\n    " + OnParentVersionAction.nameFromValue(propertyDefinition.getOnParentVersion()) + " ");
      }
      /** Don't print if all attributes are default */
      if (attributes.length() > 0)
      {
         out.write("\r\n    ");
         out.write(attributes.toString());
      }

      out.write(listToString(propertyDefinition.getValueConstraints(), "'", "\r\n    < ", " "));
   }

   /**
    * Print to output stream child node definition in CND format
    * 
    * @param nodeDefinition
    * @throws IOException
    * @throws RepositoryException
    */
   private void printChildDeclaration(NodeDefinitionData nodeDefinition, OutputStreamWriter out) throws IOException,
      RepositoryException
   {
      out.write("\r\n  ");
      out.write("+ " + qNameToString(nodeDefinition.getName()) + " ");

      InternalQName[] requiredTypes = nodeDefinition.getRequiredPrimaryTypes();
      if (requiredTypes != null && requiredTypes.length > 0)
      {
         /** if there is only 1 element and it is NT_BASE then avoid printing it */
         if (requiredTypes.length > 1 || !requiredTypes[0].equals(Constants.NT_BASE))
         {
            out.write("(" + qNameToString(requiredTypes[0]));
            for (int i = 1; i < requiredTypes.length; i++)
            {
               out.write(", " + qNameToString(requiredTypes[i]));
            }
            out.write(")");
         }
      }

      if (nodeDefinition.getDefaultPrimaryType() != null)
      {
         out.write("\r\n    = " + qNameToString(nodeDefinition.getDefaultPrimaryType()));
      }

      StringBuffer attributes = new StringBuffer();

      if (nodeDefinition.isAutoCreated())
      {
         attributes.append("autocreated ");
      }
      if (nodeDefinition.isMandatory())
      {
         attributes.append("mandatory ");
      }
      if (nodeDefinition.isProtected())
      {
         attributes.append("protected ");
      }
      if (nodeDefinition.isAllowsSameNameSiblings())
      {
         attributes.append("sns ");
      }

      if (nodeDefinition.getOnParentVersion() != OnParentVersionAction.COPY)
      {
         attributes.append("\r\n    " + OnParentVersionAction.nameFromValue(nodeDefinition.getOnParentVersion()) + " ");
      }

      if (attributes.length() > 0)
      {
         out.write("\r\n    ");
         out.write(attributes.toString());
      }

   }

   /**
    * Converts String[] to String using given notation:
    * "beforeString+quote+element+quote+', '+quote+element...+quote+afterString"
    * 
    * @param list
    *           Array to print
    * @param quote
    *           Quote string for each element of array
    * @param beforeString
    *           starting string
    * @param afterString
    *           ending string
    * @return
    */
   private String listToString(String[] list, String quote, String beforeString, String afterString)
   {
      StringBuffer result = new StringBuffer();

      if (list != null && list.length > 0)
      {
         result.append(beforeString);
         result.append(quote + list[0] + quote);
         for (int i = 1; i < list.length; i++)
         {
            result.append(", " + quote + list[i] + quote);
         }
         result.append(afterString);
      }

      return result.toString();
   }

   /**
    * Converting InternalQName to String, using defined LocationFactory.
    * 
    * @param name
    * @return
    * @throws RepositoryException
    */
   private String qNameToString(InternalQName qName) throws RepositoryException
   {
      return locationFactory.createJCRName(qName).getAsString();
   }

}
