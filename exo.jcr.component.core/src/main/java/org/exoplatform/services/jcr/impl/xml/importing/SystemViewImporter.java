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
package org.exoplatform.services.jcr.impl.xml.importing;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.JCRName;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.xml.DecodedValue;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.ImportNodeData;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.ImportPropertyData;
import org.exoplatform.services.jcr.impl.xml.importing.dataflow.PropertyInfo;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SystemViewImporter.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class SystemViewImporter extends BaseXmlImporter
{
   /**
    * 
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.SystemViewImporter");

   protected PropertyInfo propertyInfo = new PropertyInfo();
   
   protected Map<String, NodePropertiesInfo> mapNodePropertiesInfo = new HashMap<String, NodePropertiesInfo>();
   
   /**
    * Root node name.
    */
   protected String ROOT_NODE_NAME = "jcr:root";

   /**
    * @param parent
    * @param uuidBehavior
    * @param saveType
    * @param respectPropertyDefinitionsConstraints
    */
   public SystemViewImporter(NodeData parent, QPath ancestorToSave, int uuidBehavior, ItemDataConsumer dataConsumer,
      NodeTypeDataManager ntManager, LocationFactory locationFactory, ValueFactoryImpl valueFactory,
      NamespaceRegistry namespaceRegistry, AccessManager accessManager, ConversationState userState,
      Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      super(parent, ancestorToSave, uuidBehavior, dataConsumer, ntManager, locationFactory, valueFactory,
         namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public void characters(char[] ch, int start, int length) throws RepositoryException
   {
      // property values
      if (propertyInfo.getValues().size() > 0)
      {
         DecodedValue curPropValue = propertyInfo.getValues().get(propertyInfo.getValues().size() - 1);
         if (curPropValue.isComplete())
         {
            return;
         }

         if (propertyInfo.getType() == PropertyType.BINARY)
         {
            try
            {
               curPropValue.getBinaryDecoder().write(ch, start, length);
            }
            catch (IOException e)
            {
               throw new RepositoryException(e);
            }
         }
         else
         {
            curPropValue.getStringBuffer().append(ch, start, length);
         }
      }
      else
      {
         log.debug("Wrong XML content. Element 'sv:value' expected,"
            + " but SAX event 'characters' occured. characters:[" + new String(ch, start, length) + "]");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void endElement(String uri, String localName, String name) throws RepositoryException
   {
      InternalQName elementName = locationFactory.parseJCRName(name).getInternalName();

      if (Constants.SV_NODE_NAME.equals(elementName))
      {
         // sv:node element
         endNode();
      }
      else if (Constants.SV_PROPERTY_NAME.equals(elementName))
      {
         // sv:property element

         ImportPropertyData propertyData = endProperty();
         if (propertyData != null)
         {
            changesLog.add(new ItemState(propertyData, ItemState.ADDED, true, getAncestorToSave()));
            
            ImportNodeData currentNodeInfo = (ImportNodeData)getParent();
            
            NodePropertiesInfo currentNodePropertiesInfo = mapNodePropertiesInfo.get(currentNodeInfo.getIdentifier());
            
            currentNodePropertiesInfo.addProperty(propertyData);
         }
      }
      else if (Constants.SV_VALUE_NAME.equals(elementName))
      {
         // sv:value element
         //mark current value as completed
         DecodedValue curPropValue = propertyInfo.getValues().get(propertyInfo.getValues().size() - 1);
         curPropValue.setComplete(true);
      }
      else if (Constants.SV_VERSION_HISTORY_NAME.equals(elementName))
      {
         // remove version storage node from tree
         tree.pop();
      }
      else
      {
         throw new RepositoryException("invalid element in system view xml document: " + localName);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void startElement(String namespaceURI, String localName, String name, Map<String, String> atts)
      throws RepositoryException
   {
      InternalQName elementName = locationFactory.parseJCRName(name).getInternalName();

      if (Constants.SV_NODE_NAME.equals(elementName))
      {
         // sv:node element

         // node name (value of sv:name attribute)
         String svName = getAttribute(atts, Constants.SV_NAME_NAME);
         if (svName == null)
         {
            throw new RepositoryException("Missing mandatory sv:name attribute of element sv:node");
         }

         NodeData parentData = null;

         parentData = getParent();

         InternalQName currentNodeName = null;
         if (ROOT_NODE_NAME.equals(svName))
         {
            currentNodeName = Constants.ROOT_PATH.getName();
         }
         else
         {
            currentNodeName = locationFactory.parseJCRName(svName).getInternalName();
         }

         int nodeIndex = getNodeIndex(parentData, currentNodeName, null);
         ImportNodeData newNodeData = new ImportNodeData(parentData, currentNodeName, nodeIndex);
         // preset of ACL
         newNodeData.setACL(parentData.getACL());
         newNodeData.setOrderNumber(getNextChildOrderNum(parentData));
         newNodeData.setIdentifier(IdGenerator.generate());
         
         changesLog.add(new ItemState(newNodeData, ItemState.ADDED, true, getAncestorToSave()));
         
         mapNodePropertiesInfo.put(newNodeData.getIdentifier(), new NodePropertiesInfo(newNodeData));

         tree.push(newNodeData);

      }
      else if (Constants.SV_PROPERTY_NAME.equals(elementName))
      {
         // sv:property element

         propertyInfo.setValues(new ArrayList<DecodedValue>());

         // property name (value of sv:name attribute)
         String svName = getAttribute(atts, Constants.SV_NAME_NAME);
         if (svName == null)
         {
            throw new RepositoryException("missing mandatory sv:name attribute of element sv:property");
         }
         propertyInfo.setName(locationFactory.parseJCRName(svName).getInternalName());
         propertyInfo.setIndentifer(IdGenerator.generate());
         // property type (sv:type attribute)
         String type = getAttribute(atts, Constants.SV_TYPE_NAME);
         if (type == null)
         {
            throw new RepositoryException("missing mandatory sv:type attribute of element sv:property");
         }
         try
         {
            propertyInfo.setType(ExtendedPropertyType.valueFromName(type));
         }
         catch (IllegalArgumentException e)
         {
            throw new RepositoryException("Unknown property type: " + type, e);
         }
      }
      else if (Constants.SV_VALUE_NAME.equals(elementName))
      {
         // sv:value element

         propertyInfo.getValues().add(new DecodedValue());

      }
      else if (Constants.SV_VERSION_HISTORY_NAME.equals(elementName))
      {
         String svName = getAttribute(atts, Constants.SV_NAME_NAME);
         if (svName == null)
         {
            throw new RepositoryException("Missing mandatory sv:name attribute of element sv:versionhistory");
         }

         NodeData versionStorage = (NodeData)this.dataConsumer.getItemData(Constants.VERSIONSTORAGE_UUID);

         NodeData versionHistory =
            (NodeData)dataConsumer.getItemData(versionStorage, new QPathEntry("", svName, 1), ItemType.NODE);

         if (versionHistory != null)
         {
            RemoveVisitor rv = new RemoveVisitor();
            rv.visit(versionHistory);
            changesLog.addAll(rv.getRemovedStates());
         }
         tree.push(versionStorage);

         List<String> list = (List<String>)context.get(ContentImporter.LIST_OF_IMPORTED_VERSION_HISTORIES);
         if (list == null)
         {
            list = new ArrayList<String>();
         }
         list.add(svName);
         context.put(ContentImporter.LIST_OF_IMPORTED_VERSION_HISTORIES, list);

      }
      else
      {
         throw new RepositoryException("Unknown element " + elementName.getAsString());
      }
   }

   /**
    * @return
    * @throws PathNotFoundException
    * @throws RepositoryException
    * @throws NoSuchNodeTypeException
    */
   private ImportPropertyData endMixinTypes() throws PathNotFoundException, RepositoryException,
      NoSuchNodeTypeException
   {
      ImportPropertyData propertyData;
      InternalQName[] mixinNames = new InternalQName[propertyInfo.getValuesSize()];
      List<ValueData> values = new ArrayList<ValueData>(propertyInfo.getValuesSize());
      ImportNodeData currentNodeInfo = (ImportNodeData)getParent();
      for (int i = 0; i < propertyInfo.getValuesSize(); i++)
      {

         String value = propertyInfo.getValues().get(i).toString();

         mixinNames[i] = locationFactory.parseJCRName(value).getInternalName();
         currentNodeInfo.addNodeType((nodeTypeDataManager.getNodeType(mixinNames[i])));
         values.add(new TransientValueData(value.toString()));
      }

      currentNodeInfo.setMixinTypeNames(mixinNames);

      propertyData =
         new ImportPropertyData(QPath.makeChildPath(currentNodeInfo.getQPath(), propertyInfo.getName()), propertyInfo
            .getIndentifer(), 0, propertyInfo.getType(), currentNodeInfo.getIdentifier(), true);
      propertyData.setValues(parseValues());
      return propertyData;
   }

   /**
    * endNode.
    * 
    * @throws RepositoryException
    */
   private void endNode() throws RepositoryException
   {
      ImportNodeData currentNodeInfo = (ImportNodeData)tree.pop();
      
      NodePropertiesInfo currentNodePropertiesInfo = mapNodePropertiesInfo.get(currentNodeInfo.getIdentifier());
      
      if (currentNodePropertiesInfo != null)
      {
         checkProperties(currentNodePropertiesInfo);
      }
      
      mapNodePropertiesInfo.remove(currentNodeInfo.getIdentifier());

      currentNodeInfo.setMixinTypeNames(currentNodeInfo.getMixinTypeNames());

      if (currentNodeInfo.isMixVersionable())
      {
         createVersionHistory(currentNodeInfo);
      }

      currentNodeInfo.setACL(initAcl(currentNodeInfo.getACL(), currentNodeInfo.isExoOwneable(), currentNodeInfo
         .isExoPrivilegeable(), currentNodeInfo.getExoOwner(), currentNodeInfo.getExoPrivileges()));
   }

   
   /**
    * Checking priopertis if nodetype is nt:frozennode
    * 
    * @param currentNodePropertiesInfo
    * @throws RepositoryException 
    * @throws IOException 
    * @throws IllegalNameException 
    * @throws IllegalStateException 
    */
   private void checkProperties(NodePropertiesInfo currentNodePropertiesInfo) throws RepositoryException
   {
      if (currentNodePropertiesInfo.getNode().getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH)
               && currentNodePropertiesInfo.getNode().getPrimaryTypeName().equals(Constants.NT_FROZENNODE))
      {
         InternalQName fptName = null;
         List<InternalQName> fmtNames = new ArrayList<InternalQName>();

         // get frozenPrimaryType and frozenMixinTypes
         try
         {
            for (ImportPropertyData propertyData : currentNodePropertiesInfo.getProperties())
            {
               if (propertyData.getQName().equals(Constants.JCR_FROZENPRIMARYTYPE))
               {
                  fptName = InternalQName.parse(new String(propertyData.getValues().get(0).getAsByteArray(), Constants.DEFAULT_ENCODING));
               }
               else if (propertyData.getQName().equals(Constants.JCR_FROZENMIXINTYPES))
               {
                  for (ValueData valueData : propertyData.getValues())
                  {
                     fmtNames.add(InternalQName.parse(new String(valueData.getAsByteArray(), Constants.DEFAULT_ENCODING)));
                  }
               }
            }
         }
         catch (IllegalStateException e)
         {
            throw new RepositoryException(e.getMessage(), e);
         }
         catch (IllegalNameException e)
         {
            throw new RepositoryException(e.getMessage(), e);
         }
         catch (IOException e)
         {
            throw new RepositoryException(e.getMessage(), e);
         }
         
         InternalQName nodePrimaryTypeName = currentNodePropertiesInfo.getNode().getPrimaryTypeName();
         InternalQName[] nodeMixinTypeName = currentNodePropertiesInfo.getNode().getMixinTypeNames();

         for (ImportPropertyData propertyData : currentNodePropertiesInfo.getProperties())
         {
            PropertyDefinitionDatas defs = nodeTypeDataManager.getPropertyDefinitions(propertyData.getQName(), nodePrimaryTypeName, nodeMixinTypeName);
            
            if (defs == null  || (defs != null && defs.getAnyDefinition().isResidualSet()))
            {
               PropertyDefinitionDatas vhdefs =
                        nodeTypeDataManager.getPropertyDefinitions(propertyData.getQName(), fptName, fmtNames
                                 .toArray(new InternalQName[fmtNames.size()]));
   
               if (vhdefs != null)
               {
                  boolean isMultivalue = (vhdefs.getDefinition(true) != null ? true : false);
                  propertyData.setMultivalue(isMultivalue);
               }
            }
         }
      }
   }

   /**
    * endPrimaryType.
    * 
    * @return
    * @throws PathNotFoundException
    * @throws RepositoryException
    * @throws NoSuchNodeTypeException
    */
   private ImportPropertyData endPrimaryType() throws PathNotFoundException, RepositoryException,
      NoSuchNodeTypeException
   {
      ImportPropertyData propertyData;
      String sName = propertyInfo.getValues().get(0).toString();
      InternalQName primaryTypeName = locationFactory.parseJCRName(sName).getInternalName();

      ImportNodeData nodeData = (ImportNodeData)tree.pop();
      if (!Constants.ROOT_UUID.equals(nodeData.getIdentifier()))
      {
         NodeData parentNodeData = getParent();
         // nodeTypeDataManager.findChildNodeDefinition(primaryTypeName,)

         // check is nt:versionedChild subnode of frozenNode
         if (nodeData.getQPath().getDepth() > 6 && primaryTypeName.equals(Constants.NT_VERSIONEDCHILD)
            && nodeData.getQPath().getEntries()[5].equals(Constants.JCR_FROZENNODE))
         {
            //do nothing
         }
         else if (!nodeTypeDataManager.isChildNodePrimaryTypeAllowed(primaryTypeName, parentNodeData
            .getPrimaryTypeName(), parentNodeData.getMixinTypeNames()))
         {
            throw new ConstraintViolationException("Can't add node " + nodeData.getQName().getAsString() + " to "
               + parentNodeData.getQPath().getAsString() + " node type " + sName
               + " is not allowed as child's node type for parent node type "
               + parentNodeData.getPrimaryTypeName().getAsString());
         }
      }
      //
      nodeData.addNodeType((nodeTypeDataManager.getNodeType(primaryTypeName)));
      nodeData.setPrimaryTypeName(primaryTypeName);

      propertyData =
         new ImportPropertyData(QPath.makeChildPath(nodeData.getQPath(), propertyInfo.getName()), propertyInfo
            .getIndentifer(), 0, propertyInfo.getType(), nodeData.getIdentifier(), false);
      propertyData.setValues(parseValues());
      
      tree.push(nodeData);
      
      return propertyData;
   }

   /**
    * @return
    * @throws PathNotFoundException
    * @throws RepositoryException
    * @throws NoSuchNodeTypeException
    * @throws IllegalPathException
    * @throws ValueFormatException
    */
   private ImportPropertyData endProperty() throws PathNotFoundException, RepositoryException, NoSuchNodeTypeException,
      IllegalPathException, ValueFormatException
   {
      ImportPropertyData propertyData = null;
      if (Constants.JCR_PRIMARYTYPE.equals(propertyInfo.getName()))
      {

         propertyData = endPrimaryType();

      }
      else if (Constants.JCR_MIXINTYPES.equals(propertyInfo.getName()))
      {
         propertyData = endMixinTypes();

      }
      else if (Constants.JCR_UUID.equals(propertyInfo.getName()))
      {
         propertyData = endUuid();

         // skip verionable properties
      }
      else if (!getParent().getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH)
         && (Constants.JCR_VERSIONHISTORY.equals(propertyInfo.getName())
            || Constants.JCR_BASEVERSION.equals(propertyInfo.getName()) || Constants.JCR_PREDECESSORS
            .equals(propertyInfo.getName())))
      {

         propertyData = null;

         endVersionable((ImportNodeData)getParent(), parseValues());
      }
      else
      {

         ImportNodeData currentNodeInfo = (ImportNodeData)getParent();
         List<ValueData> values = parseValues();

         // determinating is property multivalue;
         boolean isMultivalue = true;

         PropertyDefinitionDatas defs =
            nodeTypeDataManager.getPropertyDefinitions(propertyInfo.getName(), currentNodeInfo.getPrimaryTypeName(),
               currentNodeInfo.getMixinTypeNames());

         if (defs == null)
         {
            if (!((Boolean)context.get(ContentImporter.RESPECT_PROPERTY_DEFINITIONS_CONSTRAINTS)))
            {
               log.warn("Property definition not found for " + propertyInfo.getName());
               return null;
            }
            else
               throw new RepositoryException("Property definition not found for " + propertyInfo.getName());

         }

         if (values.size() == 1)
         {
            // there is single-value defeniton
            if (defs.getDefinition(false) != null)
            {
               isMultivalue = false;
            }
         }
         else
         {
            if ((defs.getDefinition(true) == null) && (defs.getDefinition(false) != null))
            {
               throw new ValueFormatException("Can not assign multiple-values " + "Value to a single-valued property "
                  + propertyInfo.getName().getName());
            }
         }
         log.debug("Import " + propertyInfo.getName().getName() + " size=" + propertyInfo.getValuesSize()
            + " isMultivalue=" + isMultivalue);

         propertyData =
            new ImportPropertyData(QPath.makeChildPath(currentNodeInfo.getQPath(), propertyInfo.getName()),
               propertyInfo.getIndentifer(), 0, propertyInfo.getType(), currentNodeInfo.getIdentifier(), isMultivalue);
         propertyData.setValues(values);

      }
      
      return propertyData;
   }

   /**
    * @return
    * @throws RepositoryException
    * @throws PathNotFoundException
    * @throws IllegalPathException
    */
   private ImportPropertyData endUuid() throws RepositoryException, PathNotFoundException, IllegalPathException
   {
      ImportPropertyData propertyData;
      ImportNodeData currentNodeInfo = (ImportNodeData)tree.pop();

      currentNodeInfo.setMixReferenceable(nodeTypeDataManager.isNodeType(Constants.MIX_REFERENCEABLE, currentNodeInfo
         .getPrimaryTypeName(), currentNodeInfo.getMixinTypeNames()));

      if (currentNodeInfo.isMixReferenceable())
      {
         currentNodeInfo.setMixVersionable(nodeTypeDataManager.isNodeType(Constants.MIX_VERSIONABLE, currentNodeInfo
            .getPrimaryTypeName(), currentNodeInfo.getMixinTypeNames()));
         checkReferenceable(currentNodeInfo, propertyInfo.getValues().get(0).toString());
      }

      propertyData =
         new ImportPropertyData(QPath.makeChildPath(currentNodeInfo.getQPath(), propertyInfo.getName()), propertyInfo
            .getIndentifer(), 0, propertyInfo.getType(), currentNodeInfo.getIdentifier(), false);
      
      if (currentNodeInfo.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
      {
         propertyData.setValue(new TransientValueData(propertyInfo.getValues().get(0).toString()));
      }
      else
      {
         propertyData.setValue(new TransientValueData(currentNodeInfo.getIdentifier()));
      }

      tree.push(currentNodeInfo);
      
      mapNodePropertiesInfo.put(currentNodeInfo.getIdentifier(), new NodePropertiesInfo(currentNodeInfo));
      
      return propertyData;
   }

   /**
    * @param currentNodeInfo
    * @param values
    * @throws RepositoryException
    */
   private void endVersionable(ImportNodeData currentNodeInfo, List<ValueData> values) throws RepositoryException
   {
      try
      {

         if (propertyInfo.getName().equals(Constants.JCR_VERSIONHISTORY))
         {
            String versionHistoryIdentifier = null;
            versionHistoryIdentifier = ValueDataConvertor.readString(values.get(0));

            currentNodeInfo.setVersionHistoryIdentifier(versionHistoryIdentifier);
            currentNodeInfo.setContainsVersionhistory(dataConsumer.getItemData(versionHistoryIdentifier) != null);

         }
         else if (propertyInfo.getName().equals(Constants.JCR_BASEVERSION))
         {
            currentNodeInfo.setBaseVersionIdentifier(ValueDataConvertor.readString(values.get(0)));
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }

   }

   /**
    * Returns the list of ValueData for current property
    * 
    * @return
    * @throws RepositoryException
    */
   private List<ValueData> parseValues() throws RepositoryException
   {
      List<ValueData> values = new ArrayList<ValueData>(propertyInfo.getValuesSize());
      List<String> stringValues = new ArrayList<String>();
      for (int k = 0; k < propertyInfo.getValuesSize(); k++)
      {

         if (propertyInfo.getType() == PropertyType.BINARY)
         {
            try
            {
               InputStream vStream = propertyInfo.getValues().get(k).getInputStream();

               // TODO cleanup
               // TransientValueData binaryValue = new TransientValueData(vStream);
               TransientValueData binaryValue =
                  new TransientValueData(k, null, vStream, null, valueFactory.getFileCleaner(), valueFactory
                     .getMaxBufferSize(), null, true);
               // Call to spool file into tmp
               binaryValue.getAsStream().close();
               vStream.close();
               propertyInfo.getValues().get(k).remove();
               values.add(binaryValue);
            }
            catch (IOException e)
            {
               throw new RepositoryException(e);
            }

         }
         else
         {
            String val = new String(propertyInfo.getValues().get(k).toString());
            stringValues.add(val);
            values.add(((BaseValue)valueFactory.createValue(val, propertyInfo.getType())).getInternalData());
         }
      }

      if (propertyInfo.getType() == ExtendedPropertyType.PERMISSION)
      {
         ImportNodeData currentNodeInfo = (ImportNodeData)getParent();
         currentNodeInfo.setExoPrivileges(stringValues);
      }
      else if (Constants.EXO_OWNER.equals(propertyInfo.getName()))
      {
         ImportNodeData currentNodeInfo = (ImportNodeData)getParent();
         currentNodeInfo.setExoOwner(stringValues.get(0));
      }
      return values;

   }

   /**
    * Returns the value of the named XML attribute.
    * 
    * @param attributes set of XML attributes
    * @param name attribute name
    * @return attribute value, or <code>null</code> if the named attribute is not
    *         found
    * @throws RepositoryException
    */

   protected String getAttribute(Map<String, String> attributes, InternalQName name) throws RepositoryException
   {
      JCRName jname = locationFactory.createJCRName(name);
      return attributes.get(jname.getAsString());
   }

   protected class RemoveVisitor extends ItemDataRemoveVisitor
   {
      /**
       * Default constructor.
       * 
       * @throws RepositoryException - exception.
       */
      RemoveVisitor() throws RepositoryException
      {
         super(dataConsumer, null, nodeTypeDataManager, accessManager, userState);
      }

      /**
       * {@inheritDoc}
       */
      protected void validateReferential(NodeData node) throws RepositoryException
      {
         // no REFERENCE validation here
      }
   };
}
