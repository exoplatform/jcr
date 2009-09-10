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

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.ItemDataRemoveVisitor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataConvertor;
import org.exoplatform.services.jcr.impl.util.NodeDataReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.version.OnParentVersionAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: NodeTypeDataPersister.java 13962 2008-05-07 16:00:48Z
 *          pnedonosko $
 */

public class NodeTypeDataPersister
{

   public static final Log LOG = ExoLogger.getLogger("jcr.NodeTypeDataPersister");

   private DataManager dataManager;

   private NodeData ntRoot;

   public NodeTypeDataPersister(DataManager dataManager)
   {
      this.dataManager = dataManager;
      try
      {
         NodeData jcrSystem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);
         if (jcrSystem != null)
            this.ntRoot = (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.JCR_NODETYPES, 1));
      }
      catch (RepositoryException e)
      {
         LOG.warn("Nodetypes storage (/jcr:system/jcr:nodetypes node) is not initialized.");
      }
   }

   public PlainChangesLog addNodeType(NodeTypeData nodeType) throws PathNotFoundException, RepositoryException,
      ValueFormatException
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      if (!isInitialized())
      {
         LOG.warn("Nodetypes storage (/jcr:system/jcr:nodetypes node) is not initialized.");
         return null;
      }

      NodeData ntNode = TransientNodeData.createNodeData(ntRoot, nodeType.getName(), Constants.NT_NODETYPE);

      TransientPropertyData primaryType =
         TransientPropertyData.createPropertyData(ntNode, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false);
      primaryType.setValue(new TransientValueData(ntNode.getPrimaryTypeName()));

      // jcr:nodeTypeName
      TransientPropertyData name =
         TransientPropertyData.createPropertyData(ntNode, Constants.JCR_NODETYPENAME, PropertyType.NAME, false);
      name.setValue(new TransientValueData(nodeType.getName()));

      // jcr:isMixin
      TransientPropertyData isMixin =
         TransientPropertyData.createPropertyData(ntNode, Constants.JCR_ISMIXIN, PropertyType.BOOLEAN, false);
      isMixin.setValue(new TransientValueData(nodeType.isMixin()));

      // jcr:hasOrderableChildNodes
      TransientPropertyData hasOrderableChildNodes =
         TransientPropertyData.createPropertyData(ntNode, Constants.JCR_HASORDERABLECHILDNODES, PropertyType.BOOLEAN,
            false);
      hasOrderableChildNodes.setValue(new TransientValueData(nodeType.hasOrderableChildNodes()));

      changesLog.add(ItemState.createAddedState(ntNode)).add(ItemState.createAddedState(primaryType)).add(
         ItemState.createAddedState(name)).add(ItemState.createAddedState(isMixin)).add(
         ItemState.createAddedState(hasOrderableChildNodes));

      if (nodeType.getPrimaryItemName() != null)
      {
         // jcr:primaryItemName
         TransientPropertyData primaryItemName =
            TransientPropertyData.createPropertyData(ntNode, Constants.JCR_PRIMARYITEMNAME, PropertyType.NAME, false);
         primaryItemName.setValue(new TransientValueData(nodeType.getPrimaryItemName()));
         changesLog.add(ItemState.createAddedState(primaryItemName));
      }

      List<ValueData> parents = new ArrayList<ValueData>();
      for (InternalQName nt : nodeType.getDeclaredSupertypeNames())
         parents.add(new TransientValueData(nt));

      if (parents.size() != 0)
      {
         // jcr:supertypes
         TransientPropertyData supertypes =
            TransientPropertyData.createPropertyData(ntNode, Constants.JCR_SUPERTYPES, PropertyType.NAME, true);
         supertypes.setValues(parents);
         changesLog.add(ItemState.createAddedState(supertypes));
      }

      for (int i = 0; i < nodeType.getDeclaredPropertyDefinitions().length; i++)
      {
         NodeData childProps =
            TransientNodeData.createNodeData(ntNode, Constants.JCR_PROPERTYDEFINITION, Constants.NT_PROPERTYDEFINITION,
               i + 1);

         TransientPropertyData cpPrimaryType =
            TransientPropertyData.createPropertyData(childProps, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false);
         cpPrimaryType.setValue(new TransientValueData(childProps.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(childProps)).add(ItemState.createAddedState(cpPrimaryType));

         changesLog.addAll(initPropertyDefProps(childProps, nodeType.getDeclaredPropertyDefinitions()[i]));
      }

      for (int i = 0; i < nodeType.getDeclaredChildNodeDefinitions().length; i++)
      {
         NodeData childNodes =
            TransientNodeData.createNodeData(ntNode, Constants.JCR_CHILDNODEDEFINITION,
               Constants.NT_CHILDNODEDEFINITION, i + 1);

         TransientPropertyData cnPrimaryType =
            TransientPropertyData.createPropertyData(childNodes, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false);
         cnPrimaryType.setValue(new TransientValueData(childNodes.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(childNodes)).add(ItemState.createAddedState(cnPrimaryType));

         changesLog.addAll(initNodeDefProps(childNodes, nodeType.getDeclaredChildNodeDefinitions()[i]));
      }

      return changesLog;
   }

   public boolean hasNodeTypeData(InternalQName nodeTypeName) throws RepositoryException
   {
      try
      {
         return getNodeTypesData(nodeTypeName).size() > 0;
      }
      catch (PathNotFoundException e)
      {
         return false;
      }
   }

   public synchronized PlainChangesLog initNodetypesRoot(NodeData nsSystem, boolean addACL)
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      if (ntRoot == null)
      {

         long start = System.currentTimeMillis();

         TransientNodeData jcrNodetypes =
            TransientNodeData.createNodeData(nsSystem, Constants.JCR_NODETYPES, Constants.NT_UNSTRUCTURED,
               Constants.NODETYPESROOT_UUID);

         TransientPropertyData primaryType =
            TransientPropertyData.createPropertyData(jcrNodetypes, Constants.JCR_PRIMARYTYPE, PropertyType.NAME, false);
         primaryType.setValue(new TransientValueData(jcrNodetypes.getPrimaryTypeName()));

         changesLog.add(ItemState.createAddedState(jcrNodetypes)).add(ItemState.createAddedState(primaryType));

         if (addACL)
         {
            AccessControlList acl = new AccessControlList();
            InternalQName[] mixins = new InternalQName[]{Constants.EXO_OWNEABLE, Constants.EXO_PRIVILEGEABLE};
            jcrNodetypes.setMixinTypeNames(mixins);

            // jcr:mixinTypes
            List<ValueData> mixValues = new ArrayList<ValueData>();
            for (InternalQName mixin : mixins)
            {
               mixValues.add(new TransientValueData(mixin));
            }
            TransientPropertyData exoMixinTypes =
               TransientPropertyData.createPropertyData(jcrNodetypes, Constants.JCR_MIXINTYPES, PropertyType.NAME,
                  true, mixValues);

            TransientPropertyData exoOwner =
               TransientPropertyData.createPropertyData(jcrNodetypes, Constants.EXO_OWNER, PropertyType.STRING, false,
                  new TransientValueData(acl.getOwner()));

            List<ValueData> permsValues = new ArrayList<ValueData>();
            for (int i = 0; i < acl.getPermissionEntries().size(); i++)
            {
               AccessControlEntry entry = acl.getPermissionEntries().get(i);
               permsValues.add(new TransientValueData(entry));
            }
            TransientPropertyData exoPerms =
               TransientPropertyData.createPropertyData(jcrNodetypes, Constants.EXO_PERMISSIONS,
                  ExtendedPropertyType.PERMISSION, true, permsValues);

            changesLog.add(ItemState.createAddedState(exoMixinTypes)).add(ItemState.createAddedState(exoOwner)).add(
               ItemState.createAddedState(exoPerms));

            changesLog.add(new ItemState(jcrNodetypes, ItemState.MIXIN_CHANGED, false, null));
         }

         ntRoot = jcrNodetypes;
         if (LOG.isDebugEnabled())
            LOG.debug("/jcr:system/jcr:nodetypes is created, creation time: " + (System.currentTimeMillis() - start)
               + " ms");
      }
      else
      {
         LOG.warn("/jcr:system/jcr:nodetypes already exists");
      }
      return changesLog;
   }

   public synchronized PlainChangesLog initStorage(Collection<NodeTypeData> nodetypes) throws PathNotFoundException,
      RepositoryException
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      if (!isInitialized())
      {
         LOG
            .warn("Nodetypes storage (/jcr:system/jcr:nodetypes node) is not exists. Possible is not initialized (call initNodetypesRoot() before)");
         return changesLog;
      }
      long ntStart = System.currentTimeMillis();
      for (NodeTypeData nt : nodetypes)
      {
         try
         {
            changesLog.addAll(addNodeType(nt).getAllStates());
            if (LOG.isDebugEnabled())
               LOG.debug("Node type " + nt.getName() + " is initialized. ");
         }
         catch (ItemExistsException e)
         {
            LOG.warn("Node exists " + nt.getName() + ". Error: " + e.getMessage());
         }
      }
      // saveChanges();
      LOG.info("Node types initialized. Time: " + (System.currentTimeMillis() - ntStart) + " ms");
      return changesLog;
   }

   public List<NodeTypeData> loadFromStorage() throws PathNotFoundException, RepositoryException
   {

      if (!isInitialized())
      {
         NodeData jcrSystem = (NodeData)dataManager.getItemData(Constants.SYSTEM_UUID);
         if (jcrSystem != null)
            this.ntRoot = (NodeData)dataManager.getItemData(jcrSystem, new QPathEntry(Constants.JCR_NODETYPES, 1));
         else
            throw new RepositoryException("jcr:system is not found. Possible the workspace is not initialized properly");
      }

      if (isInitialized())
      {

         List<NodeTypeData> loadedList = new ArrayList<NodeTypeData>();

         long cycleStart = System.currentTimeMillis();
         if (LOG.isDebugEnabled())
            LOG.debug(">>> Node types registration cycle started");

         NodeDataReader ntReader = new NodeDataReader(ntRoot, dataManager);
         ntReader.forNodesByType(Constants.NT_NODETYPE); // for nt:nodeType
         ntReader.read();

         nextNodeType : for (NodeDataReader ntr : ntReader.getNodesByType(Constants.NT_NODETYPE))
         {

            long ntStart = System.currentTimeMillis();

            InternalQName ntName = null;
            try
            {

               ntr.forProperty(Constants.JCR_NODETYPENAME, PropertyType.NAME);
               ntr.read();

               try
               {
                  ntName = ValueDataConvertor.readQName(ntr.getPropertyValue(Constants.JCR_NODETYPENAME));
               }
               catch (IllegalNameException e)
               {
                  LOG.error("NodeType name is not valid. " + e + ". NodeType skipped.");
                  continue nextNodeType;
               }

               if (LOG.isDebugEnabled())
                  LOG.debug("Reading from storage " + ntName.getAsString() + " "
                     + (System.currentTimeMillis() - ntStart));

               ntr.forProperty(Constants.JCR_PRIMARYTYPE, PropertyType.NAME).forProperty(Constants.JCR_ISMIXIN,
                  PropertyType.BOOLEAN).forProperty(Constants.JCR_HASORDERABLECHILDNODES, PropertyType.BOOLEAN)
                  .forProperty(Constants.JCR_PRIMARYITEMNAME, PropertyType.NAME).forProperty(Constants.JCR_SUPERTYPES,
                     PropertyType.NAME);
               ntr.forNodesByType(Constants.NT_PROPERTYDEFINITION).forNodesByType(Constants.NT_CHILDNODEDEFINITION);
               ntr.read();

               boolean mixin = ValueDataConvertor.readBoolean(ntr.getPropertyValue(Constants.JCR_ISMIXIN));
               boolean hasOrderableChilds =
                  ValueDataConvertor.readBoolean(ntr.getPropertyValue(Constants.JCR_HASORDERABLECHILDNODES));
               InternalQName primaryItemName;
               try
               {
                  primaryItemName = ValueDataConvertor.readQName(ntr.getPropertyValue(Constants.JCR_PRIMARYITEMNAME));
               }
               catch (PathNotFoundException e)
               {
                  primaryItemName = null;
               }
               catch (IllegalNameException e)
               {
                  LOG.error("NodeType primary item name is not valid. " + e + ". NodeType " + ntName.getAsString()
                     + " skipped.");
                  continue nextNodeType;
               }

               // -------- Super types --------
               InternalQName[] declaredSupertypes;
               try
               {
                  List<ValueData> dst = ntr.getPropertyValues(Constants.JCR_SUPERTYPES);
                  InternalQName[] supertypes = new InternalQName[dst.size()];
                  for (int i = 0; i < dst.size(); i++)
                     supertypes[i] = ValueDataConvertor.readQName(dst.get(i));

                  declaredSupertypes = supertypes;
               }
               catch (PathNotFoundException e)
               {
                  declaredSupertypes = new InternalQName[0];
               }
               catch (IllegalNameException e)
               {
                  LOG.error("NodeType supertype name is not valid. " + e + ". NodeType " + ntName.getAsString()
                     + " skipped.");
                  continue nextNodeType;
               }

               // -------- Property definitions --------
               if (LOG.isDebugEnabled())
                  LOG.debug("Reading Property definitions for " + ntName.getAsString() + " "
                     + (System.currentTimeMillis() - ntStart));

               PropertyDefinitionData[] declaredProperties;
               try
               {
                  List<NodeDataReader> pdNodes = ntr.getNodesByType(Constants.NT_PROPERTYDEFINITION);
                  PropertyDefinitionData[] declaredPropertyDefs = new PropertyDefinitionData[pdNodes.size()];
                  for (int pdi = 0; pdi < pdNodes.size(); pdi++)
                  {
                     NodeDataReader pdr = pdNodes.get(pdi);

                     pdr.forProperty(Constants.JCR_NAME, PropertyType.NAME) // jcr:name
                        .forProperty(Constants.JCR_AUTOCREATED, PropertyType.BOOLEAN)
                        // jcr:autoCreated
                        .forProperty(Constants.JCR_MANDATORY, PropertyType.BOOLEAN)
                        // jcr:mandatory
                        .forProperty(Constants.JCR_PROTECTED, PropertyType.BOOLEAN)
                        // jcr:protected
                        .forProperty(Constants.JCR_MULTIPLE, PropertyType.BOOLEAN)
                        // jcr:multiple
                        .forProperty(Constants.JCR_ONPARENTVERSION, PropertyType.STRING)
                        // jcr:onParentVersion
                        .forProperty(Constants.JCR_REQUIREDTYPE, PropertyType.STRING)
                        // jcr:requiredType
                        .forProperty(Constants.JCR_VALUECONSTRAINTS, PropertyType.STRING)
                        // jcr:valueConstraints
                        .forProperty(Constants.JCR_DEFAULTVALUES, PropertyType.STRING);
                     // jcr:defaultValues
                     pdr.read();

                     InternalQName pname;
                     try
                     {
                        pname = ValueDataConvertor.readQName(pdr.getPropertyValue(Constants.JCR_NAME));
                     }
                     catch (PathNotFoundException e)
                     {
                        pname = null; // residual property definition
                     }
                     catch (IllegalNameException e)
                     {
                        LOG.error("Property definition name is not valid. " + e + ". NodeType " + ntName.getAsString()
                           + " skipped.");
                        continue nextNodeType;
                     }

                     String[] valueConstraints;
                     try
                     {
                        List<ValueData> valueConstraintValues = pdr.getPropertyValues(Constants.JCR_VALUECONSTRAINTS);
                        valueConstraints = new String[valueConstraintValues.size()];
                        for (int j = 0; j < valueConstraintValues.size(); j++)
                           valueConstraints[j] = ValueDataConvertor.readString(valueConstraintValues.get(j));
                     }
                     catch (PathNotFoundException e)
                     {
                        valueConstraints = new String[0];
                     }

                     String[] defaultValues;
                     try
                     {
                        List<ValueData> dvl = pdr.getPropertyValues(Constants.JCR_DEFAULTVALUES);
                        defaultValues = new String[dvl.size()];
                        for (int i = 0; i < dvl.size(); i++)
                           defaultValues[i] = ValueDataConvertor.readString(dvl.get(i));
                     }
                     catch (PathNotFoundException e)
                     {
                        defaultValues = new String[0];
                     }

                     PropertyDefinitionData pDef =
                        new PropertyDefinitionData(pname, ntName, ValueDataConvertor.readBoolean(pdr
                           .getPropertyValue(Constants.JCR_AUTOCREATED)), ValueDataConvertor.readBoolean(pdr
                           .getPropertyValue(Constants.JCR_MANDATORY)), OnParentVersionAction
                           .valueFromName(ValueDataConvertor.readString(pdr
                              .getPropertyValue(Constants.JCR_ONPARENTVERSION))), ValueDataConvertor.readBoolean(pdr
                           .getPropertyValue(Constants.JCR_PROTECTED)), ExtendedPropertyType
                           .valueFromName(ValueDataConvertor.readString(pdr
                              .getPropertyValue(Constants.JCR_REQUIREDTYPE))), valueConstraints, defaultValues,
                           ValueDataConvertor.readBoolean(pdr.getPropertyValue(Constants.JCR_MULTIPLE)));
                     if (LOG.isDebugEnabled())
                        LOG.debug("Property definitions readed "
                           + (pname != null ? pname.getAsString() : Constants.JCR_ANY_NAME.getAsString()) + " "
                           + (System.currentTimeMillis() - ntStart));

                     declaredPropertyDefs[pdi] = pDef;
                  }

                  declaredProperties = declaredPropertyDefs;
               }
               catch (PathNotFoundException e)
               {
                  if (LOG.isDebugEnabled())
                     LOG.debug("Property definitions is not found. " + e + ". NodeType " + ntName.getAsString());
                  declaredProperties = new PropertyDefinitionData[]{};
               }

               // --------- Child nodes definitions ----------
               if (LOG.isDebugEnabled())
                  LOG.debug("Reading Child nodes definitions for " + ntName.getAsString() + " "
                     + (System.currentTimeMillis() - ntStart));

               NodeDefinitionData[] declaredChildNodes;
               try
               {
                  List<NodeDataReader> cdNodes = ntr.getNodesByType(Constants.NT_CHILDNODEDEFINITION);
                  NodeDefinitionData[] declaredChildNodesDefs = new NodeDefinitionData[cdNodes.size()];
                  for (int cdi = 0; cdi < cdNodes.size(); cdi++)
                  {
                     NodeDataReader cdr = cdNodes.get(cdi);

                     cdr.forProperty(Constants.JCR_NAME, PropertyType.NAME) // jcr:name
                        .forProperty(Constants.JCR_REQUIREDPRIMARYTYPES, PropertyType.NAME)
                        // jcr:requiredPrimaryTypes
                        .forProperty(Constants.JCR_AUTOCREATED, PropertyType.BOOLEAN)
                        // jcr:autoCreated
                        .forProperty(Constants.JCR_MANDATORY, PropertyType.BOOLEAN)
                        // jcr:mandatory
                        .forProperty(Constants.JCR_PROTECTED, PropertyType.BOOLEAN)
                        // jcr:protected
                        .forProperty(Constants.JCR_ONPARENTVERSION, PropertyType.STRING)
                        // jcr:onParentVersion
                        .forProperty(Constants.JCR_SAMENAMESIBLINGS, PropertyType.STRING)
                        // jcr:sameNameSiblings
                        .forProperty(Constants.JCR_DEFAULTPRIMNARYTYPE, PropertyType.NAME); // jcr
                     // :
                     // defaultPrimaryType
                     cdr.read();

                     InternalQName nname;
                     try
                     {
                        nname = ValueDataConvertor.readQName(cdr.getPropertyValue(Constants.JCR_NAME));
                     }
                     catch (PathNotFoundException e)
                     {
                        nname = null; // residual
                     }
                     catch (IllegalNameException e)
                     {
                        LOG.error("Child node definition name is not valid. " + e + ". NodeType "
                           + ntName.getAsString() + " skipped.");
                        continue nextNodeType;
                     }

                     InternalQName defaultNodeTypeName;
                     try
                     {
                        try
                        {
                           defaultNodeTypeName =
                              ValueDataConvertor.readQName(cdr.getPropertyValue(Constants.JCR_DEFAULTPRIMNARYTYPE));
                        }
                        catch (IllegalNameException e)
                        {
                           LOG.error("Child node default nodetype name is not valid. " + e + ". NodeType "
                              + ntName.getAsString() + " skipped.");
                           continue nextNodeType;
                        }
                     }
                     catch (PathNotFoundException e)
                     {
                        defaultNodeTypeName = null;
                     }

                     List<ValueData> requiredNodeTypesValues =
                        cdr.getPropertyValues(Constants.JCR_REQUIREDPRIMARYTYPES);
                     InternalQName[] requiredNodeTypes = new InternalQName[requiredNodeTypesValues.size()];
                     try
                     {
                        for (int j = 0; j < requiredNodeTypesValues.size(); j++)
                           requiredNodeTypes[j] = ValueDataConvertor.readQName(requiredNodeTypesValues.get(j));
                     }
                     catch (IllegalNameException e)
                     {
                        LOG.error("Child node required nodetype name is not valid. " + e + ". NodeType "
                           + ntName.getAsString() + " skipped.");
                        continue nextNodeType;
                     }

                     NodeDefinitionData nDef =
                        new NodeDefinitionData(nname, ntName, ValueDataConvertor.readBoolean(cdr
                           .getPropertyValue(Constants.JCR_AUTOCREATED)), ValueDataConvertor.readBoolean(cdr
                           .getPropertyValue(Constants.JCR_MANDATORY)), OnParentVersionAction
                           .valueFromName(ValueDataConvertor.readString(cdr
                              .getPropertyValue(Constants.JCR_ONPARENTVERSION))), ValueDataConvertor.readBoolean(cdr
                           .getPropertyValue(Constants.JCR_PROTECTED)), requiredNodeTypes, defaultNodeTypeName,
                           ValueDataConvertor.readBoolean(cdr.getPropertyValue(Constants.JCR_SAMENAMESIBLINGS)));

                     declaredChildNodesDefs[cdi] = nDef;

                     if (LOG.isDebugEnabled())
                        LOG.debug("Child nodes definitions readed "
                           + (nname != null ? nname.getAsString() : Constants.JCR_ANY_NAME.getAsString()) + " "
                           + (System.currentTimeMillis() - ntStart));
                  }

                  declaredChildNodes = declaredChildNodesDefs;
               }
               catch (PathNotFoundException e)
               {
                  if (LOG.isDebugEnabled())
                     LOG.debug("Child nodes definitions not found. " + e + ". NodeType " + ntName.getAsString());

                  declaredChildNodes = new NodeDefinitionData[]{};
               }

               // -------- NodeType done --------
               NodeTypeData ntype =
                  new NodeTypeData(ntName, primaryItemName, mixin, hasOrderableChilds, declaredSupertypes,
                     declaredProperties, declaredChildNodes);
               loadedList.add(ntype);

               if (LOG.isDebugEnabled())
                  LOG.debug("NodeType " + ntype.getName().getAsString() + " readed. "
                     + (System.currentTimeMillis() - ntStart) + " ms");

            }
            catch (IOException e)
            {
               LOG.error("Error of NodeType " + (ntName != null ? ntName.getAsString() : "") + " load. " + e);
            }
         }

         if (LOG.isDebugEnabled())
            LOG.debug("<<< Node types registration cycle finished. " + (System.currentTimeMillis() - cycleStart)
               + " ms");

         return loadedList;
      }
      else
      {
         LOG.warn("Nodetypes storage (/jcr:system/jcr:nodetypes node) is not initialized. No nodetypes loaded.");
         return new ArrayList<NodeTypeData>();
      }
   }

   public List<ItemState> removeNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      if (!isInitialized())
      {
         LOG.warn("Nodetypes storage (/jcr:system/jcr:nodetypes node) is not initialized.");
         return new ArrayList<ItemState>();
      }
      NodeData nodeTypeData = (NodeData)dataManager.getItemData(ntRoot, new QPathEntry(nodeType.getName(), 0));
      ItemDataRemoveVisitor removeVisitor = new ItemDataRemoveVisitor(dataManager, ntRoot.getQPath());
      nodeTypeData.accept(removeVisitor);
      return removeVisitor.getRemovedStates();
   }

   public void saveChanges(PlainChangesLog changesLog) throws RepositoryException, InvalidItemStateException
   {
      dataManager.save(new TransactionChangesLog(changesLog));
   }

   DataManager getDataManager()
   {
      return dataManager;
   }

   boolean isInitialized()
   {
      return ntRoot != null;
   }

   private List<NodeDataReader> getNodeTypesData(InternalQName nodeTypeName) throws RepositoryException
   {

      NodeDataReader ntReader = new NodeDataReader(ntRoot, dataManager);
      ntReader.forNode(nodeTypeName);
      ntReader.read();

      ntReader.getNodes(nodeTypeName);

      return ntReader.getNodes(nodeTypeName);
   }

   private List<ItemState> initNodeDefProps(NodeData parent, NodeDefinitionData def) throws ValueFormatException,
      RepositoryException
   {
      List<ItemState> changes = new ArrayList<ItemState>();
      if (def.getName() != null)
      { // Mandatory false
         TransientPropertyData name =
            TransientPropertyData.createPropertyData(parent, Constants.JCR_NAME, PropertyType.NAME, false);
         name.setValue(new TransientValueData(def.getName()));
         changes.add(ItemState.createAddedState(name));
      }

      TransientPropertyData autoCreated =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_AUTOCREATED, PropertyType.BOOLEAN, false);
      autoCreated.setValue(new TransientValueData(def.isAutoCreated()));

      TransientPropertyData isMandatory =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_MANDATORY, PropertyType.BOOLEAN, false);
      isMandatory.setValue(new TransientValueData(def.isMandatory()));

      TransientPropertyData onParentVersion =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_ONPARENTVERSION, PropertyType.STRING, false);
      onParentVersion.setValue(new TransientValueData(OnParentVersionAction.nameFromValue(def.getOnParentVersion())));

      TransientPropertyData isProtected =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_PROTECTED, PropertyType.BOOLEAN, false);
      isProtected.setValue(new TransientValueData(def.isProtected()));

      TransientPropertyData sameNameSiblings =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_SAMENAMESIBLINGS, PropertyType.BOOLEAN, false);
      sameNameSiblings.setValue(new TransientValueData(def.isAllowsSameNameSiblings()));

      if (def.getDefaultPrimaryType() != null)
      { // Mandatory false
         TransientPropertyData defaultPrimaryType =
            TransientPropertyData.createPropertyData(parent, Constants.JCR_DEFAULTPRIMNARYTYPE, PropertyType.NAME,
               false);
         defaultPrimaryType.setValue(new TransientValueData(def.getDefaultPrimaryType()));
         changes.add(ItemState.createAddedState(defaultPrimaryType));
      }

      changes.add(ItemState.createAddedState(autoCreated));
      changes.add(ItemState.createAddedState(isMandatory));
      changes.add(ItemState.createAddedState(onParentVersion));
      changes.add(ItemState.createAddedState(isProtected));
      changes.add(ItemState.createAddedState(sameNameSiblings));

      if (def.getRequiredPrimaryTypes() != null && def.getRequiredPrimaryTypes().length != 0)
      {
         List<ValueData> requiredPrimaryTypesValues = new ArrayList<ValueData>();
         for (InternalQName rpt : def.getRequiredPrimaryTypes())
            requiredPrimaryTypesValues.add(new TransientValueData(rpt));

         TransientPropertyData requiredPrimaryTypes =
            TransientPropertyData.createPropertyData(parent, Constants.JCR_REQUIREDPRIMARYTYPES, PropertyType.NAME,
               true);
         requiredPrimaryTypes.setValues(requiredPrimaryTypesValues);
         changes.add(ItemState.createAddedState(requiredPrimaryTypes));
      }
      return changes;
   }

   private List<ItemState> initPropertyDefProps(NodeData parent, PropertyDefinitionData def)
      throws ValueFormatException, RepositoryException
   {
      List<ItemState> changes = new ArrayList<ItemState>();
      if (def.getName() != null)
      {
         TransientPropertyData name =
            TransientPropertyData.createPropertyData(parent, Constants.JCR_NAME, PropertyType.NAME, false);
         name.setValue(new TransientValueData(def.getName()));
         changes.add(ItemState.createAddedState(name));
      }

      TransientPropertyData autoCreated =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_AUTOCREATED, PropertyType.BOOLEAN, false);
      autoCreated.setValue(new TransientValueData(def.isAutoCreated()));

      TransientPropertyData isMandatory =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_MANDATORY, PropertyType.BOOLEAN, false);
      isMandatory.setValue(new TransientValueData(def.isMandatory()));

      TransientPropertyData onParentVersion =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_ONPARENTVERSION, PropertyType.STRING, false);
      onParentVersion.setValue(new TransientValueData(OnParentVersionAction.nameFromValue(def.getOnParentVersion())));

      TransientPropertyData isProtected =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_PROTECTED, PropertyType.BOOLEAN, false);
      isProtected.setValue(new TransientValueData(def.isProtected()));

      TransientPropertyData requiredType =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_REQUIREDTYPE, PropertyType.STRING, false);
      requiredType.setValue(new TransientValueData(ExtendedPropertyType.nameFromValue(def.getRequiredType())));

      TransientPropertyData isMultiple =
         TransientPropertyData.createPropertyData(parent, Constants.JCR_MULTIPLE, PropertyType.BOOLEAN, false);
      isMultiple.setValue(new TransientValueData(def.isMultiple()));

      changes.add(ItemState.createAddedState(autoCreated));
      changes.add(ItemState.createAddedState(isMandatory));
      changes.add(ItemState.createAddedState(onParentVersion));
      changes.add(ItemState.createAddedState(isProtected));
      changes.add(ItemState.createAddedState(requiredType));
      changes.add(ItemState.createAddedState(isMultiple));

      if (def.getValueConstraints() != null && def.getValueConstraints().length != 0)
      {
         List<ValueData> valueConstraintsValues = new ArrayList<ValueData>();
         for (String vc : def.getValueConstraints())
            valueConstraintsValues.add(new TransientValueData(vc));

         TransientPropertyData valueConstraints =
            TransientPropertyData.createPropertyData(parent, Constants.JCR_VALUECONSTRAINTS, PropertyType.STRING, true);
         valueConstraints.setValues(valueConstraintsValues);
         changes.add(ItemState.createAddedState(valueConstraints));
      }

      if (def.getDefaultValues() != null && def.getDefaultValues().length != 0)
      {
         List<ValueData> defaultValuesValues = new ArrayList<ValueData>();
         for (String dv : def.getDefaultValues())
         {
            if (dv != null) // TODO dv can be null?
               defaultValuesValues.add(new TransientValueData(dv));
         }
         TransientPropertyData defaultValues =
            TransientPropertyData.createPropertyData(parent, Constants.JCR_DEFAULTVALUES, PropertyType.STRING, true);
         defaultValues.setValues(defaultValuesValues);
         changes.add(ItemState.createAddedState(defaultValues));
      }

      return changes;
   }

}
