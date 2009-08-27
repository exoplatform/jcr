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
package org.exoplatform.services.jcr.impl.core.nodetype;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 02.12.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeTypeImpl.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class NodeTypeImpl
   implements NodeType
{

   private static final Log LOG = ExoLogger.getLogger("jcr.NodeTypeImpl");

   protected final NodeTypeData nodeTypeData;

   protected final NodeTypeDataManager nodeTypeDataManager;

   protected final ExtendedNodeTypeManager nodeTypeManager;

   protected final LocationFactory locationFactory;

   protected final ValueFactory valueFactory;

   /**
    * @param nodeTypeData
    * @param nodeTypeDataManager
    * @param nodeTypeManager
    * @param locationFactory
    * @param valueFactory
    */
   public NodeTypeImpl(NodeTypeData nodeTypeData, NodeTypeDataManager nodeTypeDataManager,
            ExtendedNodeTypeManager nodeTypeManager, LocationFactory locationFactory, ValueFactory valueFactory)
   {
      this.nodeTypeData = nodeTypeData;
      this.nodeTypeDataManager = nodeTypeDataManager;
      this.nodeTypeManager = nodeTypeManager;
      this.locationFactory = locationFactory;
      this.valueFactory = valueFactory;
   }

   /**
    * {@inheritDoc}
    */
   public boolean canAddChildNode(String childNodeName)
   {

      try
      {
         InternalQName cname = locationFactory.parseJCRName(childNodeName).getInternalName();

         NodeDefinitionData childNodeDef = nodeTypeDataManager.findChildNodeDefinition(cname, nodeTypeData.getName());
         return !(childNodeDef == null || childNodeDef.isProtected() || childNodeDef.getDefaultPrimaryType() == null);
      }
      catch (RepositoryException e)
      {
         LOG.error("canAddChildNode " + e, e);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean canAddChildNode(String childNodeName, String nodeTypeName)
   {
      try
      {
         InternalQName cname = locationFactory.parseJCRName(childNodeName).getInternalName();
         InternalQName ntname = locationFactory.parseJCRName(nodeTypeName).getInternalName();

         NodeDefinitionData childNodeDef =
                  nodeTypeDataManager.findChildNodeDefinition(cname, ntname, nodeTypeData.getName());
         return !(childNodeDef == null || childNodeDef.isProtected()) && isChildNodePrimaryTypeAllowed(nodeTypeName);
      }
      catch (RepositoryException e)
      {
         if (LOG.isDebugEnabled())
            LOG.debug("canAddChildNode " + e, e);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean canRemoveItem(String itemName)
   {
      try
      {
         InternalQName iname = locationFactory.parseJCRName(itemName).getInternalName();

         PropertyDefinitionDatas pdefs = nodeTypeDataManager.getPropertyDefinitions(iname, nodeTypeData.getName());
         if (pdefs != null)
         {
            PropertyDefinitionData pd = pdefs.getAnyDefinition();
            if (pd != null)
               return !(pd.isMandatory() || pd.isProtected());
         }
         NodeDefinitionData cndef = nodeTypeDataManager.findChildNodeDefinition(iname, nodeTypeData.getName());
         if (cndef != null)
            return !(cndef.isMandatory() || cndef.isProtected());

         return false;
      }
      catch (RepositoryException e)
      {
         if (LOG.isDebugEnabled())
            LOG.debug("canRemoveItem " + e, e);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean canSetProperty(String propertyName, Value value)
   {
      try
      {
         InternalQName pname = locationFactory.parseJCRName(propertyName).getInternalName();

         PropertyDefinitionDatas pdefs = nodeTypeDataManager.getPropertyDefinitions(pname, nodeTypeData.getName());
         if (pdefs != null)
         {
            PropertyDefinitionData pd = pdefs.getDefinition(false);
            if (pd != null)
            {
               if (pd.isProtected())
                  // can set (edit)
                  return false;
               else if (value != null)
                  // can set (add or edit)
                  return canSetPropertyForType(pd.getRequiredType(), value, pd.getValueConstraints());
               else
                  // can remove
                  return !pd.isMandatory();
            }
         }
         return false;
      }
      catch (RepositoryException e)
      {
         LOG.error("canSetProperty value " + e, e);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean canSetProperty(String propertyName, Value[] values)
   {
      try
      {
         InternalQName pname = locationFactory.parseJCRName(propertyName).getInternalName();

         PropertyDefinitionDatas pdefs = nodeTypeDataManager.getPropertyDefinitions(pname, nodeTypeData.getName());
         PropertyDefinitionData pd = pdefs.getDefinition(true);
         if (pd != null)
         {
            if (pd.isProtected())
               // can set (edit)
               return false;
            else if (values != null)
            {
               // can set (add or edit)
               int res = 0;
               for (Value value : values)
               {
                  if (canSetPropertyForType(pd.getRequiredType(), value, pd.getValueConstraints()))
                     res++;
               }
               return res == values.length;
            }
            else
               // can remove
               return !pd.isMandatory();
         }
         else
            return false;
      }
      catch (RepositoryException e)
      {
         LOG.error("canSetProperty value " + e, e);
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinition[] getChildNodeDefinitions()
   {
      NodeDefinitionData[] nodeDefs = nodeTypeDataManager.getAllChildNodeDefinitions(new InternalQName[]
      {nodeTypeData.getName()});
      NodeDefinition[] ndefs = new NodeDefinition[nodeDefs.length];
      for (int i = 0; i < nodeDefs.length; i++)
      {
         NodeDefinitionData cnd = nodeDefs[i];
         try
         {
            ndefs[i] = makeNodeDefinition(cnd);
         }
         catch (NoSuchNodeTypeException e)
         {
            LOG.error("Node type not found " + e, e);
         }
         catch (RepositoryException e)
         {
            LOG.error("Error of declared child node definition create " + e, e);
         }
      }

      return ndefs;
   }

   public NodeDefinition[] getDeclaredChildNodeDefinitions()
   {
      NodeDefinitionData[] cndefs = nodeTypeData.getDeclaredChildNodeDefinitions();
      NodeDefinition[] ndefs = new NodeDefinition[cndefs.length];
      for (int i = 0; i < cndefs.length; i++)
      {
         NodeDefinitionData cnd = cndefs[i];
         try
         {
            ndefs[i] = makeNodeDefinition(cnd);
         }
         catch (NoSuchNodeTypeException e)
         {
            LOG.error("Node type not found " + e, e);
         }
         catch (RepositoryException e)
         {
            LOG.error("Error of declared child node definition create " + e, e);
         }
      }

      return ndefs;
   }

   private NodeDefinition makeNodeDefinition(NodeDefinitionData data) throws NoSuchNodeTypeException,
            RepositoryException
   {
      InternalQName[] rnames = data.getRequiredPrimaryTypes();
      NodeType[] rnts = new NodeType[rnames.length];
      for (int j = 0; j < rnames.length; j++)
      {
         rnts[j] = nodeTypeManager.findNodeType(rnames[j]);
      }

      String name =
               locationFactory.createJCRName(data.getName() != null ? data.getName() : Constants.JCR_ANY_NAME)
                        .getAsString();
      NodeType defType =
               data.getDefaultPrimaryType() != null ? nodeTypeManager.findNodeType(data.getDefaultPrimaryType()) : null;
      return new NodeDefinitionImpl(name, this, rnts, defType, data.isAutoCreated(), data.isMandatory(), data
               .getOnParentVersion(), data.isProtected(), data.isAllowsSameNameSiblings());
   }

   public PropertyDefinition[] getDeclaredPropertyDefinitions()
   {
      PropertyDefinitionData[] pdefs = nodeTypeData.getDeclaredPropertyDefinitions();
      return getPropertyDefinition(pdefs);
   }

   private PropertyDefinition[] getPropertyDefinition(PropertyDefinitionData[] pdefs)
   {
      PropertyDefinition[] propertyDefinitions = new PropertyDefinition[pdefs.length];
      // TODO same in PropertyImpl
      for (int i = 0; i < pdefs.length; i++)
      {

         try
         {
            PropertyDefinitionData propertyDef = pdefs[i];
            String name =
                     locationFactory.createJCRName(
                              propertyDef.getName() != null ? propertyDef.getName() : Constants.JCR_ANY_NAME)
                              .getAsString();

            Value[] defaultValues = new Value[propertyDef.getDefaultValues().length];
            String[] propVal = propertyDef.getDefaultValues();
            // there can be null in definition but should not be null value
            if (propVal != null)
            {
               for (int j = 0; j < propVal.length; j++)
               {
                  if (propertyDef.getRequiredType() == PropertyType.UNDEFINED)
                     defaultValues[j] = valueFactory.createValue(propVal[j]);
                  else
                     defaultValues[j] = valueFactory.createValue(propVal[j], propertyDef.getRequiredType());
               }
            }

            propertyDefinitions[i] =
                     new PropertyDefinitionImpl(name, nodeTypeManager.findNodeType(propertyDef.getDeclaringNodeType()),
                              propertyDef.getRequiredType(), propertyDef.getValueConstraints(), defaultValues,
                              propertyDef.isAutoCreated(), propertyDef.isMandatory(), propertyDef.getOnParentVersion(),
                              propertyDef.isProtected(), propertyDef.isMultiple());
         }
         catch (ValueFormatException e)
         {
            e.printStackTrace();
         }
         catch (NoSuchNodeTypeException e)
         {
            e.printStackTrace();
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
         }
      }
      return propertyDefinitions;
   }

   public NodeType[] getDeclaredSupertypes()
   {
      InternalQName[] snames = nodeTypeData.getDeclaredSupertypeNames();

      NodeType[] supers = new NodeType[snames.length];

      for (int i = 0; i < snames.length; i++)
      {
         supers[i] =
                  new NodeTypeImpl(nodeTypeDataManager.findNodeType(snames[i]), nodeTypeDataManager, nodeTypeManager,
                           locationFactory, valueFactory);
      }

      return supers;
   }

   public String getName()
   {
      try
      {
         return locationFactory.createJCRName(nodeTypeData.getName()).getAsString();
      }
      catch (RepositoryException e)
      {
         // TODO
         throw new RuntimeException("Wrong name in nodeTypeData " + e, e);
      }
   }

   public String getPrimaryItemName()
   {
      try
      {
         if (nodeTypeData.getPrimaryItemName() != null)
            return locationFactory.createJCRName(nodeTypeData.getPrimaryItemName()).getAsString();
         else
            return null;
      }
      catch (RepositoryException e)
      {
         // TODO
         throw new RuntimeException("Wrong primary item name in nodeTypeData " + e, e);
      }
   }

   public PropertyDefinition[] getPropertyDefinitions()
   {
      PropertyDefinitionData[] propertyDefs = nodeTypeDataManager.getAllPropertyDefinitions(nodeTypeData.getName());
      return getPropertyDefinition(propertyDefs);
   }

   /**
    * {@inheritDoc}
    */
   public NodeType[] getSupertypes()
   {
      Set<InternalQName> supers = nodeTypeDataManager.getSupertypes(nodeTypeData.getName());
      NodeType[] superTypes = new NodeType[supers.size()];
      int i = 0;
      for (InternalQName nodeTypeName : supers)
      {
         try
         {
            superTypes[i++] = nodeTypeManager.findNodeType(nodeTypeName);
         }
         catch (NoSuchNodeTypeException e)
         {
            e.printStackTrace();
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
         }
      }
      return superTypes;
   }

   public boolean hasOrderableChildNodes()
   {
      return nodeTypeData.hasOrderableChildNodes();
   }

   public boolean isMixin()
   {
      return nodeTypeData.isMixin();
   }

   public boolean isNodeType(String nodeTypeName)
   {
      try
      {
         return nodeTypeDataManager.isNodeType(locationFactory.parseJCRName(nodeTypeName).getInternalName(),
                  nodeTypeData.getName());
      }
      catch (RepositoryException e)
      {
         throw new RuntimeException("Wrong nodetype name " + e, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getQName()
   {
      return nodeTypeData.getName();
   }

   public boolean isChildNodePrimaryTypeAllowed(String typeName)
   {
      try
      {
         InternalQName iname = locationFactory.parseJCRName(typeName).getInternalName();

         return nodeTypeDataManager.isChildNodePrimaryTypeAllowed(iname, nodeTypeData.getName(), new InternalQName[0]);
      }
      catch (RepositoryException e)
      {
         return false;
      }
   }

   public boolean isNodeType(InternalQName nodeTypeQName)
   {
      return nodeTypeDataManager.isNodeType(nodeTypeQName, nodeTypeData.getName(), new InternalQName[0]);
   }

   // internal stuff ============================

   /**
    * Ported from 1.10. Check on empty value (property remove) removed.
    */
   private boolean canSetPropertyForType(int requiredType, Value value, String[] constrains)
   {

      if (requiredType == value.getType())
      {
         return checkValueConstraints(constrains, value);
      }
      else if (requiredType == PropertyType.BINARY
               && (value.getType() == PropertyType.STRING || value.getType() == PropertyType.DATE
                        || value.getType() == PropertyType.LONG || value.getType() == PropertyType.DOUBLE
                        || value.getType() == PropertyType.NAME || value.getType() == PropertyType.PATH || value
                        .getType() == PropertyType.BOOLEAN))
      {
         return checkValueConstraints(constrains, value);
      }
      else if (requiredType == PropertyType.BOOLEAN)
      {
         if (value.getType() == PropertyType.STRING)
         {
            return checkValueConstraints(constrains, value);
         }
         else if (value.getType() == PropertyType.BINARY)
         {
            try
            {
               return isCharsetString(value.getString(), Constants.DEFAULT_ENCODING)
                        && checkValueConstraints(constrains, value);
            }
            catch (Exception e)
            {
               // Hm, this is not string and not UTF-8 too
               return false;
            }
         }
         else
         {
            return false;
         }
      }
      else if (requiredType == PropertyType.DATE)
      {
         String likeDataString = null;
         try
         {
            if (value.getType() == PropertyType.STRING)
            {
               likeDataString = value.getString();
            }
            else if (value.getType() == PropertyType.BINARY)
            {
               likeDataString = getCharsetString(value.getString(), Constants.DEFAULT_ENCODING);
            }
            else if (value.getType() == PropertyType.DOUBLE || value.getType() == PropertyType.LONG)
            {
               return checkValueConstraints(constrains, value);
            }
            else
            {
               return false;
            }
            // try parse...
            JCRDateFormat.parse(likeDataString);
            // validate
            return checkValueConstraints(constrains, value);
         }
         catch (Exception e)
         {
            // Hm, this is not date format string
            return false;
         }
      }
      else if (requiredType == PropertyType.DOUBLE)
      {
         String likeDoubleString = null;
         try
         {
            if (value.getType() == PropertyType.STRING)
            {
               likeDoubleString = value.getString();
            }
            else if (value.getType() == PropertyType.BINARY)
            {
               likeDoubleString = getCharsetString(value.getString(), Constants.DEFAULT_ENCODING);
            }
            else if (value.getType() == PropertyType.DATE)
            {
               return true;
            }
            else if (value.getType() == PropertyType.LONG)
            {
               return checkValueConstraints(constrains, value);
            }
            else
            {
               return false;
            }
            Double doubleValue = new Double(likeDoubleString);
            return doubleValue != null && checkValueConstraints(constrains, value);
         }
         catch (Exception e)
         {
            // Hm, this is not double formated string
            return false;
         }
      }
      else if (requiredType == PropertyType.LONG)
      {
         String likeLongString = null;
         try
         {
            if (value.getType() == PropertyType.STRING)
            {
               likeLongString = value.getString();
            }
            else if (value.getType() == PropertyType.BINARY)
            {
               likeLongString = getCharsetString(value.getString(), Constants.DEFAULT_ENCODING);
            }
            else if (value.getType() == PropertyType.DATE)
            {
               return true;
            }
            else if (value.getType() == PropertyType.DOUBLE)
            {
               return true;
            }
            else
            {
               return false;
            }
            Long longValue = new Long(likeLongString);
            return longValue != null && checkValueConstraints(constrains, value);
         }
         catch (Exception e)
         {
            // Hm, this is not long formated string
            return false;
         }
      }
      else if (requiredType == PropertyType.NAME)
      {
         String likeNameString = null;
         try
         {
            if (value.getType() == PropertyType.STRING)
            {
               likeNameString = value.getString();
            }
            else if (value.getType() == PropertyType.BINARY)
            {
               likeNameString = getCharsetString(value.getString(), Constants.DEFAULT_ENCODING);
            }
            else if (value.getType() == PropertyType.PATH)
            {
               String pathString = value.getString();
               String[] pathParts = pathString.split("\\/");
               if (pathString.startsWith("/") && (pathParts.length > 1 || pathString.indexOf("[") > 0))
               {
                  // Path is not relative - absolute
                  // FALSE if it is more than one element long
                  // or has an index
                  return false;
               }
               else if (!pathParts.equals("/") && pathParts.length == 1 && pathString.indexOf("[") < 0)
               {
                  // Path is relative
                  // TRUE if it is one element long
                  // and has no index
                  return checkValueConstraints(constrains, value);
               }
               else if (pathString.startsWith("/") && pathString.lastIndexOf("/") < 1 && pathString.indexOf("[") < 0)
               {
                  return checkValueConstraints(constrains, value);
               }
               else
               {
                  return false;
               }
            }
            else
            {
               return false;
            }
            try
            {
               Value nameValue = valueFactory.createValue(likeNameString, requiredType);
               return nameValue != null && checkValueConstraints(constrains, value);
            }
            catch (Exception e)
            {
               return false;
            }
         }
         catch (Exception e)
         {
            return false;
         }
      }
      else if (requiredType == PropertyType.PATH)
      {
         String likeNameString = null;
         try
         {
            if (value.getType() == PropertyType.STRING)
            {
               likeNameString = value.getString();
            }
            else if (value.getType() == PropertyType.BINARY)
            {
               likeNameString = getCharsetString(value.getString(), Constants.DEFAULT_ENCODING);
            }
            else if (value.getType() == PropertyType.NAME)
            {
               return checkValueConstraints(constrains, value);
            }
            else
            {
               return false;
            }
            try
            {
               Value nameValue = valueFactory.createValue(likeNameString, requiredType);
               return nameValue != null && checkValueConstraints(constrains, value);
            }
            catch (Exception e)
            {
               return false;
            }
         }
         catch (Exception e)
         {
            return false;
         }
      }
      else if (requiredType == PropertyType.STRING)
      {
         String likeStringString = null;
         try
         {
            if (value.getType() == PropertyType.BINARY)
            {
               likeStringString = getCharsetString(value.getString(), Constants.DEFAULT_ENCODING);
            }
            else if (value.getType() == PropertyType.DATE || value.getType() == PropertyType.LONG
                     || value.getType() == PropertyType.BOOLEAN || value.getType() == PropertyType.NAME
                     || value.getType() == PropertyType.PATH || value.getType() == PropertyType.DOUBLE)
            {
               likeStringString = value.getString();
            }
            else
            {
               return false;
            }
            return likeStringString != null && checkValueConstraints(constrains, value);
         }
         catch (Exception e)
         {
            return false;
         }
      }
      else if (requiredType == PropertyType.UNDEFINED)
      {
         return checkValueConstraints(constrains, value);
      }
      else
      {
         return false;
      }
   }

   private boolean checkValueConstraints(String[] constraints, Value value)
   {

      if (constraints != null && constraints.length > 0)
      {
         for (int i = 0; i < constraints.length; i++)
         {
            try
            {
               if (constraints[i].equals(value.getString()))
               {
                  return true;
               }
            }
            catch (RepositoryException e)
            {
               LOG.error("Can't get value's string value " + e, e);
            }
         }
      }
      else
         return true;

      return false;
   }

   private String getCharsetString(String source, String charSetName)
   {
      try
      {
         CharBuffer cb = CharBuffer.wrap(source.toCharArray());
         Charset cs = Charset.forName(charSetName);
         CharsetEncoder cse = cs.newEncoder();
         ByteBuffer encoded = cse.encode(cb);
         return new String(encoded.array()).trim(); // Trim is very important!!!
      }
      catch (IllegalStateException e)
      {
         return null;
      }
      catch (MalformedInputException e)
      {
         return null;
      }
      catch (UnmappableCharacterException e)
      {
         return null;
      }
      catch (CharacterCodingException e)
      {
         return null;
      }
   }

   private boolean isCharsetString(String source, String charSetName)
   {
      try
      {
         String s = getCharsetString(source, charSetName);
         return s != null;
      }
      catch (Exception e)
      {
         return false;
      }
   }

}
