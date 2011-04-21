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

import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;
import org.exoplatform.services.jcr.impl.core.value.ValueConstraintsMatcher;
import org.exoplatform.services.jcr.impl.util.JCRDateFormat;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 02.12.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeTypeImpl.java 111 2008-11-11 11:11:11Z pnedonosko $
 */
public class NodeTypeImpl extends NodeTypeDefinitionImpl implements NodeType
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeImpl");

   /**
    * NodeTypeImpl contructor.
    * 
    * @param nodeTypeData NodeTypeData
    * @param nodeTypeDataManager NodeTypeDataManager
    * @param nodeTypeManager ExtendedNodeTypeManager
    * @param locationFactory LocationFactory
    * @param valueFactory ValueFactory
    * @param dataManager ItemDataConsumer 
    */
   public NodeTypeImpl(NodeTypeData nodeTypeData, NodeTypeDataManager nodeTypeDataManager,
      ExtendedNodeTypeManager nodeTypeManager, LocationFactory locationFactory, ValueFactory valueFactory,
      ItemDataConsumer dataManager)
   {
      super(nodeTypeData, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory, dataManager);
   }

   /**
    * {@inheritDoc}
    */
   public boolean canAddChildNode(String childNodeName)
   {
      try
      {
         InternalQName cname = locationFactory.parseJCRName(childNodeName).getInternalName();

         NodeDefinitionData childNodeDef = nodeTypeDataManager.getChildNodeDefinition(cname, nodeTypeData.getName());
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
            nodeTypeDataManager.getChildNodeDefinition(cname, ntname, nodeTypeData.getName());
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
         NodeDefinitionData cndef = nodeTypeDataManager.getChildNodeDefinition(iname, nodeTypeData.getName());
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
               {
                  // can set (edit)
                  return false;
               }
               else if (value != null)
               {
                  // can set (add or edit)
                  return canSetPropertyForType(pd.getRequiredType(), value, pd.getValueConstraints());
               }
               else
               {
                  // can remove
                  return !pd.isMandatory();
               }
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
            {
               // can set (edit)
               return false;
            }
            else if (values != null)
            {
               // can set (add or edit)
               int res = 0;
               for (Value value : values)
               {
                  if (canSetPropertyForType(pd.getRequiredType(), value, pd.getValueConstraints()))
                  {
                     res++;
                  }
               }
               return res == values.length;
            }
            else
            {
               // can remove
               return !pd.isMandatory();
            }
         }
         else
         {
            return false;
         }
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
      NodeDefinitionData[] nodeDefs =
         nodeTypeDataManager.getAllChildNodeDefinitions(new InternalQName[]{nodeTypeData.getName()});
      NodeDefinition[] ndefs = new NodeDefinition[nodeDefs.length];
      for (int i = 0; i < nodeDefs.length; i++)
      {
         NodeDefinitionData cnd = nodeDefs[i];
         ndefs[i] =
            new NodeDefinitionImpl(cnd, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory,
               dataManager);
      }

      return ndefs;
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinition[] getDeclaredChildNodeDefinitions()
   {
      NodeDefinitionData[] cndefs = nodeTypeData.getDeclaredChildNodeDefinitions();
      NodeDefinition[] ndefs = new NodeDefinition[cndefs.length];
      for (int i = 0; i < cndefs.length; i++)
      {
         NodeDefinitionData cnd = cndefs[i];
         ndefs[i] =
            new NodeDefinitionImpl(cnd, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory,
               dataManager);
      }

      return ndefs;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinition[] getDeclaredPropertyDefinitions()
   {
      PropertyDefinitionData[] pdefs = nodeTypeData.getDeclaredPropertyDefinitions();
      return getPropertyDefinition(pdefs);
   }

   public NodeType[] getDeclaredSupertypes()
   {
      InternalQName[] snames = nodeTypeData.getDeclaredSupertypeNames();

      NodeType[] supers = new NodeType[snames.length];

      for (int i = 0; i < snames.length; i++)
      {
         NodeTypeData superNodeTypeData = nodeTypeDataManager.getNodeType(snames[i]);
         supers[i] =
            new NodeTypeImpl(superNodeTypeData, nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory,
               dataManager);
      }

      return supers;
   }

   /**
    * {@inheritDoc}
    */
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

   /**
    * {@inheritDoc}
    */
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

   /**
    * {@inheritDoc}
    */
   public PropertyDefinition[] getPropertyDefinitions()
   {
      PropertyDefinitionData[] propertyDefs = nodeTypeDataManager.getAllPropertyDefinitions(nodeTypeData.getName());
      return getPropertyDefinition(propertyDefs);
   }

   /**
    * {@inheritDoc}
    */
   public InternalQName getQName()
   {
      return nodeTypeData.getName();
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
            LOG.error(e.getLocalizedMessage(), e);
         }
         catch (RepositoryException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }
      return superTypes;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasOrderableChildNodes()
   {
      return nodeTypeData.hasOrderableChildNodes();
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

   /**
    * {@inheritDoc}
    */
   public boolean isMixin()
   {
      return nodeTypeData.isMixin();
   }

   public boolean isNodeType(InternalQName nodeTypeQName)
   {
      return nodeTypeDataManager.isNodeType(nodeTypeQName, nodeTypeData.getName(), new InternalQName[0]);
   }

   /**
    * {@inheritDoc}
    */
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
    * Returns true if satisfy type and constrains. Otherwise returns false.
    * 
    * @param requiredType - type.
    * @param value - value.
    * @param constraints - constraints.
    * @return a boolean.
    */
   private boolean canSetPropertyForType(int requiredType, Value value, String[] constraints)
   {

      if (requiredType == value.getType())
      {
         return checkValueConstraints(requiredType, constraints, value);
      }
      else if (requiredType == PropertyType.BINARY
         && (value.getType() == PropertyType.STRING || value.getType() == PropertyType.DATE
            || value.getType() == PropertyType.LONG || value.getType() == PropertyType.DOUBLE
            || value.getType() == PropertyType.NAME || value.getType() == PropertyType.PATH || value.getType() == PropertyType.BOOLEAN))
      {
         return checkValueConstraints(requiredType, constraints, value);
      }
      else if (requiredType == PropertyType.BOOLEAN)
      {
         if (value.getType() == PropertyType.STRING)
         {
            return checkValueConstraints(requiredType, constraints, value);
         }
         else if (value.getType() == PropertyType.BINARY)
         {
            try
            {
               return isCharsetString(value.getString(), Constants.DEFAULT_ENCODING)
                  && checkValueConstraints(requiredType, constraints, value);
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
               return checkValueConstraints(requiredType, constraints, value);
            }
            else
            {
               return false;
            }
            // try parse...
            JCRDateFormat.parse(likeDataString);
            // validate
            return checkValueConstraints(requiredType, constraints, value);
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
               return checkValueConstraints(requiredType, constraints, value);
            }
            else
            {
               return false;
            }
            Double doubleValue = new Double(likeDoubleString);
            return doubleValue != null && checkValueConstraints(requiredType, constraints, value);
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
            return longValue != null && checkValueConstraints(requiredType, constraints, value);
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
                  return checkValueConstraints(requiredType, constraints, value);
               }
               else if (pathString.startsWith("/") && pathString.lastIndexOf("/") < 1 && pathString.indexOf("[") < 0)
               {
                  return checkValueConstraints(requiredType, constraints, value);
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
               return nameValue != null && checkValueConstraints(requiredType, constraints, value);
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
               return checkValueConstraints(requiredType, constraints, value);
            }
            else
            {
               return false;
            }
            try
            {
               Value nameValue = valueFactory.createValue(likeNameString, requiredType);
               return nameValue != null && checkValueConstraints(requiredType, constraints, value);
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
            return likeStringString != null && checkValueConstraints(requiredType, constraints, value);
         }
         catch (Exception e)
         {
            return false;
         }
      }
      else if (requiredType == PropertyType.UNDEFINED)
      {
         return checkValueConstraints(requiredType, constraints, value);
      }
      else
      {
         return false;
      }
   }

   /**
   * Check value constrains.
   * 
   * @param requiredType int
   * @param constraints - string constrains.
   * @param value - value to check.
   * @return result of check.
   */
   private boolean checkValueConstraints(int requiredType, String[] constraints, Value value)
   {
      ValueConstraintsMatcher constrMatcher =
         new ValueConstraintsMatcher(constraints, locationFactory, dataManager, nodeTypeDataManager);

      try
      {
         return constrMatcher.match(((BaseValue)value).getInternalData(), requiredType);
      }
      catch (RepositoryException e1)
      {
         return false;
      }

      // TODO old code
      //      if (constraints != null && constraints.length > 0)
      //      {
      //         for (int i = 0; i < constraints.length; i++)
      //         {
      //            try
      //            {
      //               if (constraints[i].equals(value.getString()))
      //               {
      //                  return true;
      //               }
      //            }
      //            catch (RepositoryException e)
      //            {
      //               LOG.error("Can't get value's string value " + e, e);
      //            }
      //         }
      //      }
      //      else
      //         return true;
      //
      //      return false;
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

   /**
    * @param pdefs
    * @return
    */
   private PropertyDefinition[] getPropertyDefinition(PropertyDefinitionData[] pdefs)
   {
      PropertyDefinition[] propertyDefinitions = new PropertyDefinition[pdefs.length];
      for (int i = 0; i < pdefs.length; i++)
      {
         propertyDefinitions[i] =
            new PropertyDefinitionImpl(pdefs[i], nodeTypeDataManager, nodeTypeManager, locationFactory, valueFactory,
               dataManager);
      }
      return propertyDefinitions;
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
