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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeDefinitionComparator;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.PropertyDefinitionComparator;
import org.exoplatform.services.jcr.impl.core.query.RepositoryIndexSearcherHolder;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.value.BaseValue;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.version.VersionHistoryDataHelper;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import javax.jcr.InvalidItemStateException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date: 26.11.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: NodeTypeDataManagerImpl.java 111 2008-11-11 11:11:11Z
 *          pnedonosko $
 */
public class NodeTypeDataManagerImpl implements NodeTypeDataManager
{

   protected static final Log LOG = ExoLogger.getLogger("jcr.NodeTypeDataManagerImpl");

   private static final String NODETYPES_FILE = "nodetypes.xml";

   protected final NamespaceRegistry namespaceRegistry;

   protected final NodeTypeDataPersister persister;

   protected final LocationFactory locationFactory;

   protected final String accessControlPolicy;

   protected final NodeTypeDataHierarchyHolder hierarchy;

   protected final ItemDefinitionDataHolder defsHolder;

   private final Set<InternalQName> buildInNodeTypesNames;

   /**
    * Listeners (soft references)
    */
   private final Map<NodeTypeManagerListener, NodeTypeManagerListener> listeners;

   // protected HashSet<QueryHandler> queryHandlers;

   private final ValueFactoryImpl valueFactory;

   protected final RepositoryIndexSearcherHolder indexSearcherHolder;

   public NodeTypeDataManagerImpl(RepositoryEntry config, LocationFactory locationFactory,
      NamespaceRegistry namespaceRegistry, NodeTypeDataPersister persister,
      RepositoryIndexSearcherHolder indexSearcherHolder) throws RepositoryException
   {

      this.namespaceRegistry = namespaceRegistry;

      this.persister = persister;

      this.locationFactory = locationFactory;
      this.indexSearcherHolder = indexSearcherHolder;
      this.valueFactory = new ValueFactoryImpl(locationFactory);
      this.accessControlPolicy = config.getAccessControl();

      this.hierarchy = new NodeTypeDataHierarchyHolder();

      this.defsHolder = new ItemDefinitionDataHolder();
      this.listeners = Collections.synchronizedMap(new WeakHashMap<NodeTypeManagerListener, NodeTypeManagerListener>());
      this.buildInNodeTypesNames = new HashSet<InternalQName>();
      initDefault();
      //this.queryHandlers = new HashSet<QueryHandler>();
   }

   /**
    * @param accessControlPolicy
    * @param locationFactory
    * @param namespaceRegistry
    * @param persister
    * @throws RepositoryException
    */
   public NodeTypeDataManagerImpl(String accessControlPolicy, LocationFactory locationFactory,
      NamespaceRegistry namespaceRegistry, NodeTypeDataPersister persister,
      RepositoryIndexSearcherHolder indexSearcherHolder) throws RepositoryException
   {

      this.namespaceRegistry = namespaceRegistry;

      this.persister = persister;

      this.locationFactory = locationFactory;
      this.indexSearcherHolder = indexSearcherHolder;
      this.valueFactory = new ValueFactoryImpl(locationFactory);
      this.accessControlPolicy = accessControlPolicy;

      this.hierarchy = new NodeTypeDataHierarchyHolder();

      this.defsHolder = new ItemDefinitionDataHolder();
      this.listeners = Collections.synchronizedMap(new WeakHashMap<NodeTypeManagerListener, NodeTypeManagerListener>());
      this.buildInNodeTypesNames = new HashSet<InternalQName>();
      //this.queryHandlers = new HashSet<QueryHandler>();
   }

   /**
    * Add a <code>NodeTypeRegistryListener</code>
    * 
    * @param listener the new listener to be informed on (un)registration of node
    *          types
    */
   public void addListener(NodeTypeManagerListener listener)
   {
      if (!listeners.containsKey(listener))
      {
         listeners.put(listener, listener);
      }
   }

   //
   //   public void addQueryHandler(QueryHandler queryHandler)
   //   {
   //      queryHandlers.add(queryHandler);
   //   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinitionData findChildNodeDefinition(InternalQName nodeName, InternalQName... nodeTypeNames)
   {

      NodeDefinitionData ndResidual = defsHolder.getDefaultChildNodeDefinition(nodeName, nodeTypeNames);

      if (ndResidual == null && !Constants.JCR_ANY_NAME.equals(nodeName))
         ndResidual = findChildNodeDefinition(Constants.JCR_ANY_NAME, nodeTypeNames);

      return ndResidual;

   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinitionData findChildNodeDefinition(InternalQName nodeName, InternalQName primaryNodeType,
      InternalQName[] mixinTypes)
   {

      if (mixinTypes != null)
      {
         InternalQName[] nts = new InternalQName[mixinTypes.length + 1];
         nts[0] = primaryNodeType;
         for (int i = 0; i < mixinTypes.length; i++)
         {
            nts[i + 1] = mixinTypes[i];
         }
         return findChildNodeDefinition(nodeName, nts);
      }

      return findChildNodeDefinition(nodeName, primaryNodeType);
   }

   /**
    * {@inheritDoc}
    */
   public NodeTypeData findNodeType(InternalQName typeName)
   {
      return hierarchy.getNodeType(typeName);
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinitionDatas findPropertyDefinitions(InternalQName propertyName, InternalQName primaryNodeType,
      InternalQName[] mixinTypes)
   {

      if (mixinTypes != null)
      {
         InternalQName[] nts = new InternalQName[mixinTypes.length + 1];
         nts[0] = primaryNodeType;
         for (int i = 0; i < mixinTypes.length; i++)
         {
            nts[i + 1] = mixinTypes[i];
         }
         return getPropertyDefinitions(propertyName, nts);
      }

      return getPropertyDefinitions(propertyName, primaryNodeType);
   }

   /**
    * @return the accessControlPolicy
    */
   public String getAccessControlPolicy()
   {
      return accessControlPolicy;
   }

   /**
    * {@inheritDoc}
    */
   public NodeDefinitionData[] getAllChildNodeDefinitions(InternalQName... nodeTypeNames)
   {
      Collection<NodeDefinitionData> defs = new HashSet<NodeDefinitionData>();

      for (InternalQName ntname : nodeTypeNames)
      {
         for (NodeDefinitionData cnd : hierarchy.getNodeType(ntname).getDeclaredChildNodeDefinitions())
            defs.add(cnd);

         for (InternalQName suname : hierarchy.getSupertypes(ntname))
         {
            for (NodeDefinitionData cnd : hierarchy.getNodeType(suname).getDeclaredChildNodeDefinitions())
               defs.add(cnd);
         }
      }

      return defs.toArray(new NodeDefinitionData[defs.size()]);
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> getAllNodeTypes()
   {
      return hierarchy.getAllNodeTypes();
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinitionData[] getAllPropertyDefinitions(InternalQName... nodeTypeNames)
   {
      Collection<PropertyDefinitionData> defs = new HashSet<PropertyDefinitionData>();

      for (InternalQName ntname : nodeTypeNames)
      {
         for (PropertyDefinitionData pd : hierarchy.getNodeType(ntname).getDeclaredPropertyDefinitions())
            defs.add(pd);

         for (InternalQName suname : hierarchy.getSupertypes(ntname))
         {
            for (PropertyDefinitionData pd : hierarchy.getNodeType(suname).getDeclaredPropertyDefinitions())
               defs.add(pd);
         }
      }

      return defs.toArray(new PropertyDefinitionData[defs.size()]);
   }

   // impl

   /**
    * {@inheritDoc}
    */
   public NodeDefinitionData getChildNodeDefinition(InternalQName nodeName, InternalQName nodeTypeName,
      InternalQName parentTypeName)
   {
      NodeDefinitionData def = defsHolder.getChildNodeDefinition(parentTypeName, nodeName, nodeTypeName);
      // residual
      if (def == null)
         def = defsHolder.getChildNodeDefinition(parentTypeName, Constants.JCR_ANY_NAME, nodeTypeName);
      return def;
   }

   /**
    * {@inheritDoc}
    */
   public Set<InternalQName> getDeclaredSubtypes(final InternalQName nodeTypeName)
   {
      return hierarchy.getDeclaredSubtypes(nodeTypeName);

   }

   public List<ItemDefinitionData> getManadatoryItemDefs(InternalQName primaryNodeType, InternalQName[] mixinTypes)
   {
      Collection<ItemDefinitionData> mandatoryDefs = new HashSet<ItemDefinitionData>();
      // primary type properties
      ItemDefinitionData[] itemDefs = getAllPropertyDefinitions(new InternalQName[]{primaryNodeType});
      for (int i = 0; i < itemDefs.length; i++)
      {
         if (itemDefs[i].isMandatory())
            mandatoryDefs.add(itemDefs[i]);
      }
      // primary type nodes
      itemDefs = getAllChildNodeDefinitions(new InternalQName[]{primaryNodeType});
      for (int i = 0; i < itemDefs.length; i++)
      {
         if (itemDefs[i].isMandatory())
            mandatoryDefs.add(itemDefs[i]);
      }
      // mixin properties
      itemDefs = getAllPropertyDefinitions(mixinTypes);
      for (int i = 0; i < itemDefs.length; i++)
      {
         if (itemDefs[i].isMandatory())
            mandatoryDefs.add(itemDefs[i]);
      }
      // mixin nodes
      itemDefs = getAllChildNodeDefinitions(mixinTypes);
      for (int i = 0; i < itemDefs.length; i++)
      {
         if (itemDefs[i].isMandatory())
            mandatoryDefs.add(itemDefs[i]);
      }
      return new ArrayList<ItemDefinitionData>(mandatoryDefs);
   }

   /**
    * Return
    * 
    * @param nodeType
    * @return
    * @throws RepositoryException
    * @throws IOException
    */
   public Set<String> getNodes(InternalQName nodeType) throws RepositoryException
   {
      return indexSearcherHolder.getNodesByNodeType(nodeType);
   }

   /**
    * Return
    * 
    * @param nodeType
    * @return
    * @throws RepositoryException
    * @throws IOException
    */
   public Set<String> getNodes(InternalQName nodeType, InternalQName[] includeProperties,
      InternalQName[] excludeProperties) throws RepositoryException
   {
      return new HashSet<String>();

      //      Query query = getQuery(nodeType);
      //      if (includeProperties.length > 0)
      //      {
      //         BooleanQuery tmp = new BooleanQuery();
      //         for (int i = 0; i < includeProperties.length; i++)
      //         {
      //
      //            String field = locationFactory.createJCRName(includeProperties[i]).getAsString();
      //            tmp.add(new TermQuery(new Term(FieldNames.PROPERTIES_SET, field)), Occur.MUST);
      //         }
      //         tmp.add(query, Occur.MUST);
      //         query = tmp;
      //      }
      //
      //      if (excludeProperties.length > 0)
      //      {
      //         BooleanQuery tmp = new BooleanQuery();
      //         for (int i = 0; i < excludeProperties.length; i++)
      //         {
      //
      //            String field = locationFactory.createJCRName(excludeProperties[i]).getAsString();
      //            tmp.add(new TermQuery(new Term(FieldNames.PROPERTIES_SET, field)), Occur.MUST_NOT);
      //         }
      //         tmp.add(query, Occur.MUST);
      //         query = tmp;
      //      }
      //
      //      Iterator<QueryHandler> it = queryHandlers.iterator();
      //      Set<String> result = new HashSet<String>();
      //
      //      try
      //      {
      //         indexSearcherHolder.getNodesByNodeType()
      //      }
      //      catch (IOException e)
      //      {
      //         throw new RepositoryException(e.getLocalizedMessage(), e);
      //      }
      //      return result;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyDefinitionDatas getPropertyDefinitions(InternalQName propertyName, InternalQName... nodeTypeNames)
   {

      PropertyDefinitionDatas propertyDefinitions = defsHolder.getPropertyDefinitions(propertyName, nodeTypeNames);
      // Try super
      if (propertyDefinitions == null)
      {
         for (int i = 0; i < nodeTypeNames.length && propertyDefinitions == null; i++)
         {
            InternalQName[] supers = hierarchy.getNodeType(nodeTypeNames[i]).getDeclaredSupertypeNames();
            propertyDefinitions = getPropertyDefinitions(propertyName, supers);

         }
      }

      // try residual def
      if (propertyDefinitions == null && !propertyName.equals(Constants.JCR_ANY_NAME))
      {
         propertyDefinitions = getPropertyDefinitions(Constants.JCR_ANY_NAME, nodeTypeNames);
      }

      return propertyDefinitions;
   }

   //   // TODO make me private
   //   public Set<QueryHandler> getQueryHandlers()
   //   {
   //      return queryHandlers;
   //   }

   /**
    * @param nodeTypeName
    * @return
    */
   public Set<InternalQName> getSubtypes(final InternalQName nodeTypeName)
   {
      return hierarchy.getSubtypes(nodeTypeName);
   }

   public Set<InternalQName> getSupertypes(final InternalQName nodeTypeName)
   {
      return hierarchy.getSupertypes(nodeTypeName);
   }

   public boolean isChildNodePrimaryTypeAllowed(InternalQName childNodeTypeName, InternalQName parentNodeType,
      InternalQName[] parentMixinNames)
   {
      // NodeTypeData childDef = findNodeType(childNodeTypeName);
      Set<InternalQName> testSuperTypesNames = hierarchy.getSupertypes(childNodeTypeName);
      NodeDefinitionData[] allChildNodeDefinitions = getAllChildNodeDefinitions(parentNodeType);
      for (NodeDefinitionData cnd : allChildNodeDefinitions)
      {
         for (InternalQName req : cnd.getRequiredPrimaryTypes())
         {
            if (childNodeTypeName.equals(req))
               return true;
            for (InternalQName superName : testSuperTypesNames)
            {
               if (superName.equals(req))
                  return true;
            }
         }
      }
      allChildNodeDefinitions = getAllChildNodeDefinitions(parentMixinNames);
      for (NodeDefinitionData cnd : allChildNodeDefinitions)
      {
         for (InternalQName req : cnd.getRequiredPrimaryTypes())
         {
            if (childNodeTypeName.equals(req))
               return true;
            for (InternalQName superName : testSuperTypesNames)
            {
               if (superName.equals(req))
                  return true;
            }
         }
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeType(final InternalQName testTypeName, final InternalQName... typesNames)
   {
      return hierarchy.isNodeType(testTypeName, typesNames);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isNodeType(final InternalQName testTypeName, final InternalQName primaryType,
      final InternalQName[] mixinTypes)
   {

      if (hierarchy.isNodeType(testTypeName, primaryType))
         return true;

      if (hierarchy.isNodeType(testTypeName, mixinTypes))
         return true;

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isOrderableChildNodesSupported(final InternalQName primaryType, final InternalQName[] mixinTypes)
   {

      final int nlen = mixinTypes != null ? mixinTypes.length : 0;
      for (int i = -1; i < nlen; i++)
      {
         InternalQName name;
         if (i < 0)
            name = primaryType;
         else
            name = mixinTypes[i];

         NodeTypeData nt = hierarchy.getNodeType(name);

         if (nt != null)
         {
            if (nt.hasOrderableChildNodes())
               return true;

            Set<InternalQName> supers = hierarchy.getSupertypes(nt.getName());
            for (InternalQName suName : supers)
            {
               NodeTypeData su = hierarchy.getNodeType(suName);
               if (su != null && su.hasOrderableChildNodes())
                  return true;
            }
         }
      }

      return false;
   }

   public PlainChangesLog makeAutoCreatedItems(NodeData parent, InternalQName nodeTypeName,
      ItemDataConsumer dataManager, String owner) throws RepositoryException
   {
      PlainChangesLogImpl changes = new PlainChangesLogImpl();
      NodeTypeData type = findNodeType(nodeTypeName);

      changes.addAll(makeAutoCreatedProperties(parent, nodeTypeName, getAllPropertyDefinitions(nodeTypeName),
         dataManager, owner).getAllStates());
      changes.addAll(makeAutoCreatedNodes(parent, nodeTypeName, getAllChildNodeDefinitions(nodeTypeName), dataManager,
         owner).getAllStates());

      // Add autocreated child nodes

      // versionable
      if (isNodeType(Constants.MIX_VERSIONABLE, new InternalQName[]{type.getName()}))
      {

         // using VH helper as for one new VH, all changes in changes log
         makeMixVesionableChanges(parent, dataManager, changes);
      }
      return changes;
   }

   public PlainChangesLog makeAutoCreatedNodes(NodeData parent, InternalQName typeName, NodeDefinitionData[] nodeDefs,
      ItemDataConsumer dataManager, String owner) throws RepositoryException
   {
      PlainChangesLogImpl changes = new PlainChangesLogImpl();
      Set<InternalQName> addedNodes = new HashSet<InternalQName>();
      for (NodeDefinitionData ndef : nodeDefs)
      {
         if (ndef.isAutoCreated())
         {
            ItemData pdata = dataManager.getItemData(parent, new QPathEntry(ndef.getName(), 0));
            if ((pdata == null && !addedNodes.contains(ndef.getName())) || (pdata != null && !pdata.isNode()))
            {

               TransientNodeData childNodeData =
                  TransientNodeData.createNodeData(parent, ndef.getName(), ndef.getDefaultPrimaryType(), IdGenerator
                     .generate());
               changes.add(ItemState.createAddedState(childNodeData, false));
               changes.addAll(makeAutoCreatedItems(childNodeData, childNodeData.getPrimaryTypeName(), dataManager,
                  owner).getAllStates());
               addedNodes.add(ndef.getName());
            }
            else
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Skipping existed node " + ndef.getName() + " in " + parent.getQPath().getAsString()
                     + "   during the automatic creation of items for " + typeName.getAsString()
                     + " nodetype or mixin type");
               }
            }
         }
      }
      return changes;

   }

   public PlainChangesLog makeAutoCreatedProperties(NodeData parent, InternalQName typeName,
      PropertyDefinitionData[] propDefs, ItemDataConsumer dataManager, String owner) throws RepositoryException
   {
      PlainChangesLogImpl changes = new PlainChangesLogImpl();

      Set<InternalQName> addedProperties = new HashSet<InternalQName>();

      // Add autocreated child properties

      for (PropertyDefinitionData pdef : propDefs)
      {
         // if (propDefs[i] == null) // TODO it is possible for not mandatory
         // propDef
         // continue;

         if (pdef.isAutoCreated())
         {

            ItemData pdata = dataManager.getItemData(parent, new QPathEntry(pdef.getName(), 0));
            if ((pdata == null && !addedProperties.contains(pdef.getName())) || (pdata != null && pdata.isNode()))
            {

               List<ValueData> listAutoCreateValue = autoCreatedValue(parent, typeName, pdef, owner);

               if (listAutoCreateValue != null)
               {
                  TransientPropertyData propertyData =
                     TransientPropertyData.createPropertyData(parent, pdef.getName(), pdef.getRequiredType(), pdef
                        .isMultiple(), listAutoCreateValue);
                  changes.add(ItemState.createAddedState(propertyData));
                  addedProperties.add(pdef.getName());
               }
            }
            else
            {
               // TODO if autocreated property exists it's has wrong data (e.g. ACL)
               // - throw an exception
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Skipping existed property " + pdef.getName() + " in " + parent.getQPath().getAsString()
                     + "   during the automatic creation of items for " + typeName.getAsString()
                     + " nodetype or mixin type");
               }
            }
         }
      }
      return changes;
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> registerNodeTypes(InputStream xml, int alreadyExistsBehaviour) throws RepositoryException
   {

      try
      {

         IBindingFactory factory = BindingDirectory.getFactory(NodeTypeValuesList.class);
         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList)uctx.unmarshalDocument(xml, null);
         List ntvList = nodeTypeValuesList.getNodeTypeValuesList();

         long start = System.currentTimeMillis();
         List<NodeTypeValue> nts = new ArrayList<NodeTypeValue>();
         for (int i = 0; i < ntvList.size(); i++)
         {
            if (ntvList.get(i) != null)
            {
               NodeTypeValue nodeTypeValue = (NodeTypeValue)ntvList.get(i);
               nts.add(nodeTypeValue);
            }
            else
            {
               // Hm! Smth is wrong in xml document
               LOG.error("Empty nodeTypeValue in xml document, index: " + i + ", skiping...");
            }
         }
         if (LOG.isDebugEnabled())
            LOG.debug("Nodetypes registered from xml definitions (count: " + ntvList.size() + "). "
               + (System.currentTimeMillis() - start) + " ms.");

         return registerNodeTypes(nts, alreadyExistsBehaviour);

      }
      catch (JiBXException e)
      {
         throw new RepositoryException("Error in config initialization " + e, e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> registerNodeTypes(List<NodeTypeValue> ntvalues, int alreadyExistsBehaviour)
      throws RepositoryException
   {

      PlainChangesLog changesLog = new PlainChangesLogImpl();

      Map<InternalQName, NodeTypeData> nodeTypeDataList = parseNodeTypes(ntvalues);

      for (NodeTypeData nodeTypeData : nodeTypeDataList.values())
      {
         changesLog.addAll(registerNodeType(nodeTypeData, alreadyExistsBehaviour, nodeTypeDataList).getAllStates());
      }

      persister.saveChanges(changesLog);
      return new ArrayList<NodeTypeData>(nodeTypeDataList.values());
   }

   /**
    * Remove a <code>NodeTypeRegistryListener</code>.
    * 
    * @param listener an existing listener
    */
   public void removeListener(NodeTypeManagerListener listener)
   {
      listeners.remove(listener);
   }

   /**
    * Unregisters the specified node type. In order for a node type to be
    * successfully unregistered it must meet the following conditions:
    * <ol>
    * <li>the node type must obviously be registered.</li>
    * <li>a built-in node type can not be unregistered.</li>
    * <li>the node type must not have dependents, i.e. other node types that are
    * referencing it.</li>
    * <li>the node type must not be currently used by any workspace.</li>
    * </ol>
    * 
    * @param ntName name of the node type to be unregistered
    * @throws NoSuchNodeTypeException if <code>ntName</code> does not denote a
    *           registered node type.
    * @throws RepositoryException
    * @throws RepositoryException if another error occurs.
    * @see #unregisterNodeTypes(Collection)
    */
   public void unregisterNodeType(InternalQName nodeTypeName) throws RepositoryException
   {

      NodeTypeData nodeType = hierarchy.getNodeType(nodeTypeName);
      if (nodeType == null)
         throw new NoSuchNodeTypeException(nodeTypeName.getAsString());
      // check build in
      if (buildInNodeTypesNames.contains(nodeTypeName))
         throw new RepositoryException(nodeTypeName.toString() + ": can't unregister built-in node type.");
      // check dependencies
      Set<InternalQName> descendantNt = hierarchy.getSubtypes(nodeTypeName);
      if (descendantNt.size() > 0)
      {
         String message =
            "Can not remove " + nodeTypeName.getAsString()
               + "nodetype, because the following node types depend on it: ";
         for (InternalQName internalQName : descendantNt)
         {
            message += internalQName.getAsString() + " ";
         }
         throw new RepositoryException(message);
      }
      Set<String> nodes = getNodes(nodeTypeName);
      if (nodes.size() > 0)
      {
         String message =
            "Can not remove " + nodeTypeName.getAsString()
               + " nodetype, because the following node types is used in nodes with uuid: ";
         for (String uuids : nodes)
         {
            message += uuids + " ";
         }
         throw new RepositoryException(message);

      }
      internalUnregister(nodeTypeName, nodeType);
   }

   protected List<ValueData> autoCreatedValue(NodeData parent, InternalQName typeName, PropertyDefinitionData def,
      String owner) throws RepositoryException
   {
      NodeTypeDataManager typeDataManager = this;
      List<ValueData> vals = new ArrayList<ValueData>();

      if (typeDataManager.isNodeType(Constants.NT_BASE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.JCR_PRIMARYTYPE))
      {
         vals.add(new TransientValueData(parent.getPrimaryTypeName()));

      }
      else if (typeDataManager.isNodeType(Constants.MIX_REFERENCEABLE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.JCR_UUID))
      {
         vals.add(new TransientValueData(parent.getIdentifier()));

      }
      else if (typeDataManager.isNodeType(Constants.NT_HIERARCHYNODE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.JCR_CREATED))
      {
         vals.add(new TransientValueData(Calendar.getInstance()));

      }
      else if (typeDataManager.isNodeType(Constants.EXO_OWNEABLE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.EXO_OWNER))
      {
         // String owner = session.getUserID();
         vals.add(new TransientValueData(owner));
         parent.setACL(new AccessControlList(owner, parent.getACL().getPermissionEntries()));

      }
      else if (typeDataManager.isNodeType(Constants.EXO_PRIVILEGEABLE, new InternalQName[]{typeName})
         && def.getName().equals(Constants.EXO_PERMISSIONS))
      {
         for (AccessControlEntry ace : parent.getACL().getPermissionEntries())
         {
            vals.add(new TransientValueData(ace));
         }

      }
      else
      {
         String[] propVal = def.getDefaultValues();
         // there can be null in definition but should not be null value
         if (propVal != null && propVal.length != 0)
         {
            for (String v : propVal)
            {
               if (v != null)
                  if (def.getRequiredType() == PropertyType.UNDEFINED)
                     vals.add(((BaseValue)valueFactory.createValue(v)).getInternalData());
                  else
                     vals.add(((BaseValue)valueFactory.createValue(v, def.getRequiredType())).getInternalData());
               else
               {
                  vals.add(null);
               }
            }
         }
         else
            return null;
      }

      return vals;
   }

   protected void initDefault() throws RepositoryException
   {
      long start = System.currentTimeMillis();
      try
      {
         InputStream xml = NodeTypeManagerImpl.class.getResourceAsStream(NODETYPES_FILE);
         if (xml != null)
         {
            List<NodeTypeData> registerNodeTypes = registerNodeTypes(xml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
            for (NodeTypeData nodeTypeData : registerNodeTypes)
            {
               buildInNodeTypesNames.add(nodeTypeData.getName());
            }
         }
         else
         {
            String msg =
               "Resource file '" + NODETYPES_FILE
                  + "' with NodeTypes configuration does not found. Can not create node type manager";
            LOG.error(msg);
            throw new RepositoryException(msg);
         }
      }
      catch (RepositoryException e)
      {
         String msg =
            "Error of initialization default types. Resource file with NodeTypes configuration '" + NODETYPES_FILE
               + "'. " + e;
         LOG.error(msg);
         throw new RepositoryException(msg, e);
      }
      finally
      {
         LOG.info("Initialization of default nodetypes done. " + (System.currentTimeMillis() - start) + " ms.");
      }
   }

   /**
    * @param nodeType
    * @throws RepositoryException
    * @throws ValueFormatException
    * @throws PathNotFoundException
    */
   protected void internalRegister(NodeTypeData nodeType, Map<InternalQName, NodeTypeData> volatileNodeTypes)
      throws PathNotFoundException, ValueFormatException, RepositoryException
   {
      hierarchy.addNodeType(nodeType, volatileNodeTypes);

      defsHolder.putDefinitions(nodeType.getName(), nodeType);
      // put supers
      Set<InternalQName> supers = hierarchy.getSupertypes(nodeType.getName(), volatileNodeTypes);

      for (InternalQName superName : supers)
      {
         defsHolder.putDefinitions(nodeType.getName(), hierarchy.getNodeType(superName, volatileNodeTypes));
      }
   }

   //
   // /**
   // * @param nodeType
   // * @return
   // * @throws RepositoryException
   // */
   // private NodeDefinitionData[] getAllChildNodeDefinitions(NodeTypeData
   // nodeType) throws RepositoryException {
   // Collection<NodeDefinitionData> defs = new HashSet<NodeDefinitionData>();
   //
   // for (NodeDefinitionData cnd : nodeType.getDeclaredChildNodeDefinitions()) {
   // defs.add(cnd);
   // }
   //
   // for (InternalQName suname : nodeType.getDeclaredSupertypeNames()) {
   // NodeDefinitionData[] superDefinitionData =
   // getAllChildNodeDefinitions(hierarchy.getNodeType(suname));
   // for (int i = 0; i < superDefinitionData.length; i++) {
   // defs.add(superDefinitionData[i]);
   // }
   // }
   // return defs.toArray(new NodeDefinitionData[defs.size()]);
   // }
   //
   // /**
   // * @param nodeType
   // * @return
   // */
   // private PropertyDefinitionData[] getAllPropertyDefinitions(NodeTypeData
   // nodeType) {
   // Collection<PropertyDefinitionData> defs = new
   // HashSet<PropertyDefinitionData>();
   //
   // for (PropertyDefinitionData pd : nodeType.getDeclaredPropertyDefinitions())
   // defs.add(pd);
   //
   // for (InternalQName suname : nodeType.getDeclaredSupertypeNames()) {
   // PropertyDefinitionData[] superDefinitionData =
   // getAllPropertyDefinitions(hierarchy.getNodeType(suname));
   // for (int i = 0; i < superDefinitionData.length; i++) {
   // defs.add(superDefinitionData[i]);
   // }
   //
   // }
   //
   // return defs.toArray(new PropertyDefinitionData[defs.size()]);
   // }

   /**
    * @param nodeTypeName
    * @param nodeType
    * @throws RepositoryException
    */
   protected void internalUnregister(InternalQName nodeTypeName, NodeTypeData nodeType) throws RepositoryException
   {
      // put supers
      Set<InternalQName> supers = hierarchy.getSupertypes(nodeTypeName);

      // remove from internal lists
      hierarchy.removeNodeType(nodeTypeName);

      // remove supers
      if (supers != null)
         for (InternalQName superName : supers)
         {
            defsHolder.removeDefinitions(nodeTypeName, hierarchy.getNodeType(superName));
         }
      // remove it self
      defsHolder.removeDefinitions(nodeTypeName, nodeType);

   }

   /**
    * parseNodeType.
    * 
    * @param ntvalue
    * @return
    * @throws RepositoryException
    */
   protected Map<InternalQName, NodeTypeData> parseNodeTypes(List<NodeTypeValue> ntvalues) throws RepositoryException
   {
      Map<InternalQName, NodeTypeData> nodeTypeDataList = new HashMap<InternalQName, NodeTypeData>();
      for (NodeTypeValue ntvalue : ntvalues)
      {

         if (accessControlPolicy.equals(AccessControlPolicy.DISABLE))
         {
            List<String> nsupertypes = ntvalue.getDeclaredSupertypeNames();
            if (nsupertypes != null && nsupertypes.contains("exo:privilegeable")
               || ntvalue.getName().equals("exo:privilegeable"))
            {
               // skip this node, so it's not necessary at this runtime
               // + "' -- it's not necessary at this runtime";
               LOG.warn("Node type " + ntvalue.getName() + " is not register due to DISABLE control policy");
               break;
            }
         }

         // We have to validate node value before registering it
         ntvalue.validateNodeType();
         // throw new RepositoryException("Invalid node type value");

         // declaring NT name
         InternalQName ntName = locationFactory.parseJCRName(ntvalue.getName()).getInternalName();

         List<String> stlist = ntvalue.getDeclaredSupertypeNames();
         InternalQName[] supertypes = new InternalQName[stlist.size()];
         for (int i = 0; i < stlist.size(); i++)
         {
            supertypes[i] = locationFactory.parseJCRName(stlist.get(i)).getInternalName();
         }

         List<PropertyDefinitionValue> pdlist = ntvalue.getDeclaredPropertyDefinitionValues();
         PropertyDefinitionData[] props = new PropertyDefinitionData[pdlist.size()];
         for (int i = 0; i < pdlist.size(); i++)
         {
            PropertyDefinitionValue v = pdlist.get(i);

            PropertyDefinitionData pd;
            pd =
               new PropertyDefinitionData(locationFactory.parseJCRName(v.getName()).getInternalName(), ntName, v
                  .isAutoCreate(), v.isMandatory(), v.getOnVersion(), v.isReadOnly(), v.getRequiredType(), v
                  .getValueConstraints() != null ? v.getValueConstraints().toArray(
                  new String[v.getValueConstraints().size()]) : new String[0], v.getDefaultValueStrings() == null
                  ? new String[0] : v.getDefaultValueStrings().toArray(new String[v.getDefaultValueStrings().size()]),
                  v.isMultiple());

            props[i] = pd;
         }

         List<NodeDefinitionValue> ndlist = ntvalue.getDeclaredChildNodeDefinitionValues();
         NodeDefinitionData[] nodes = new NodeDefinitionData[ndlist.size()];
         for (int i = 0; i < ndlist.size(); i++)
         {
            NodeDefinitionValue v = ndlist.get(i);

            List<String> rnts = v.getRequiredNodeTypeNames();
            InternalQName[] requiredNTs = new InternalQName[rnts.size()];
            for (int ri = 0; ri < rnts.size(); ri++)
            {
               requiredNTs[ri] = locationFactory.parseJCRName(rnts.get(ri)).getInternalName();
            }
            InternalQName defaultNodeName = null;
            if (v.getDefaultNodeTypeName() != null)
            {
               defaultNodeName = locationFactory.parseJCRName(v.getDefaultNodeTypeName()).getInternalName();
            }
            NodeDefinitionData nd =
               new NodeDefinitionData(locationFactory.parseJCRName(v.getName()).getInternalName(), ntName, v
                  .isAutoCreate(), v.isMandatory(), v.getOnVersion(), v.isReadOnly(), requiredNTs, defaultNodeName, v
                  .isSameNameSiblings());
            nodes[i] = nd;
         }

         InternalQName primaryItemName = null;
         if (ntvalue.getPrimaryItemName() != null)
            primaryItemName = locationFactory.parseJCRName(ntvalue.getPrimaryItemName()).getInternalName();

         NodeTypeData nodeTypeData =
            new NodeTypeData(ntName, primaryItemName, ntvalue.isMixin(), ntvalue.isOrderableChild(), supertypes, props,
               nodes);

         validateNodeType(nodeTypeData);
         nodeTypeDataList.put(nodeTypeData.getName(), nodeTypeData);
      }
      checkCyclicDependencies(nodeTypeDataList);
      return nodeTypeDataList;
   }

   /**
    * Check according the JSR-170
    */
   protected void validateNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      if (nodeType == null)
      {
         throw new RepositoryException("NodeType object " + nodeType + " is null");
      }

      for (int i = 0; i < nodeType.getDeclaredSupertypeNames().length; i++)
      {
         if (nodeType.getName().equals(nodeType.getDeclaredSupertypeNames()[i]))
         {
            throw new RepositoryException("Invalid super type name"
               + nodeType.getDeclaredSupertypeNames()[i].getAsString());
         }
      }
      for (int i = 0; i < nodeType.getDeclaredPropertyDefinitions().length; i++)
      {
         if (!nodeType.getDeclaredPropertyDefinitions()[i].getDeclaringNodeType().equals(nodeType.getName()))
         {
            throw new RepositoryException("Invalid declared  node type in property definitions with name "
               + nodeType.getDeclaredPropertyDefinitions()[i].getName().getAsString() + " not registred");
         }
      }
      for (int i = 0; i < nodeType.getDeclaredChildNodeDefinitions().length; i++)
      {
         if (!nodeType.getDeclaredChildNodeDefinitions()[i].getDeclaringNodeType().equals(nodeType.getName()))
         {
            throw new RepositoryException("Invalid declared  node type in child node definitions with name "
               + nodeType.getDeclaredChildNodeDefinitions()[i].getName().getAsString() + " not registred");
         }
      }

      if (nodeType.getName() == null)
      {
         throw new RepositoryException("NodeType implementation class " + nodeType.getClass().getName()
            + " is not supported in this method");
      }
   }

   private void checkCyclicDependencies(Map<InternalQName, NodeTypeData> nodeTypeDataList) throws RepositoryException
   {
      Set<InternalQName> unresolvedDependecies = new HashSet<InternalQName>();
      Set<InternalQName> resolvedDependecies = new HashSet<InternalQName>();
      for (Entry<InternalQName, NodeTypeData> entry : nodeTypeDataList.entrySet())
      {
         // / add itself
         NodeTypeData nodeTypeData = entry.getValue();
         resolvedDependecies.add(nodeTypeData.getName());
         // remove from unresolved
         unresolvedDependecies.remove(nodeTypeData.getName());
         // check suppers
         for (int i = 0; i < nodeTypeData.getDeclaredSupertypeNames().length; i++)
         {
            InternalQName superName = nodeTypeData.getDeclaredSupertypeNames()[i];
            if (hierarchy.getNodeType(superName) == null && !resolvedDependecies.contains(superName))
            {
               unresolvedDependecies.add(superName);
            }
         }
         // check node definition
         for (int i = 0; i < nodeTypeData.getDeclaredChildNodeDefinitions().length; i++)
         {
            NodeDefinitionData childnodeDefinitionData = nodeTypeData.getDeclaredChildNodeDefinitions()[i];
            for (int j = 0; j < childnodeDefinitionData.getRequiredPrimaryTypes().length; j++)
            {
               InternalQName requiredPrimaryTypeName = childnodeDefinitionData.getRequiredPrimaryTypes()[j];
               if (hierarchy.getNodeType(requiredPrimaryTypeName) == null
                  && !resolvedDependecies.contains(requiredPrimaryTypeName))
               {
                  unresolvedDependecies.add(requiredPrimaryTypeName);
               }
            }
            if (childnodeDefinitionData.getDefaultPrimaryType() != null)
            {
               if (hierarchy.getNodeType(childnodeDefinitionData.getDefaultPrimaryType()) == null
                  && !resolvedDependecies.contains(childnodeDefinitionData.getDefaultPrimaryType()))
               {
                  unresolvedDependecies.add(childnodeDefinitionData.getDefaultPrimaryType());

               }
            }
         }
      }
      if (unresolvedDependecies.size() > 0)
      {
         String msg = "Fail. Unresolved cyclic dependecy for :";
         for (InternalQName internalQName : resolvedDependecies)
         {
            msg += " " + internalQName.getAsString();
         }
         throw new RepositoryException(msg);
      }
   }

   private NodeDefinitionData[] getAllChildNodeDefinitions(NodeTypeData nodeType,
      Map<InternalQName, NodeTypeData> volatileNodeTypes)
   {
      Collection<NodeDefinitionData> defs = new HashSet<NodeDefinitionData>();

      for (NodeDefinitionData cnd : nodeType.getDeclaredChildNodeDefinitions())
      {
         defs.add(cnd);
      }

      for (InternalQName suname : nodeType.getDeclaredSupertypeNames())
      {
         NodeTypeData superNodeType = volatileNodeTypes.get(suname);
         if (superNodeType == null)
            superNodeType = hierarchy.getNodeType(suname);
         NodeDefinitionData[] superDefinitionData = getAllChildNodeDefinitions(superNodeType, volatileNodeTypes);
         for (int i = 0; i < superDefinitionData.length; i++)
         {
            defs.add(superDefinitionData[i]);
         }
      }
      return defs.toArray(new NodeDefinitionData[defs.size()]);
   }

   /**
    * @param recipientDefinition
    * @param volatileNodeTypes
    * @return
    */
   private PropertyDefinitionData[] getAllPropertyDefinitions(NodeTypeData recipientDefinition,
      Map<InternalQName, NodeTypeData> volatileNodeTypes)
   {
      Collection<PropertyDefinitionData> defs = new HashSet<PropertyDefinitionData>();

      for (PropertyDefinitionData pd : recipientDefinition.getDeclaredPropertyDefinitions())
         defs.add(pd);

      for (InternalQName suname : recipientDefinition.getDeclaredSupertypeNames())
      {
         NodeTypeData superNodeType = volatileNodeTypes.get(suname);
         if (superNodeType == null)
            superNodeType = hierarchy.getNodeType(suname);
         PropertyDefinitionData[] superDefinitionData = getAllPropertyDefinitions(superNodeType, volatileNodeTypes);
         for (int i = 0; i < superDefinitionData.length; i++)
         {
            defs.add(superDefinitionData[i]);
         }

      }

      return defs.toArray(new PropertyDefinitionData[defs.size()]);
   }

   private Query getQuery(InternalQName nodeType) throws RepositoryException
   {
      List<Term> terms = new ArrayList<Term>();
      // try {
      String mixinTypesField = locationFactory.createJCRName(Constants.JCR_MIXINTYPES).getAsString();
      String primaryTypeField = locationFactory.createJCRName(Constants.JCR_PRIMARYTYPE).getAsString();

      // ExtendedNodeTypeManager ntMgr =
      // session.getWorkspace().getNodeTypeManager();
      NodeTypeData base = findNodeType(nodeType);

      if (base.isMixin())
      {
         // search for nodes where jcr:mixinTypes is set to this mixin
         Term t =
            new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(mixinTypesField, locationFactory.createJCRName(
               nodeType).getAsString()));
         terms.add(t);
      }
      else
      {
         // search for nodes where jcr:primaryType is set to this type
         Term t =
            new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(primaryTypeField, locationFactory
               .createJCRName(nodeType).getAsString()));
         terms.add(t);
      }

      // now search for all node types that are derived from base
      Set<InternalQName> allTypes = getSubtypes(nodeType);
      for (InternalQName descendantNt : allTypes)
      {

         String ntName = locationFactory.createJCRName(descendantNt).getAsString();
         NodeTypeData nt = findNodeType(descendantNt);
         Term t;
         if (nt.isMixin())
         {
            // search on jcr:mixinTypes
            t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(mixinTypesField, ntName));
         }
         else
         {
            // search on jcr:primaryType
            t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(primaryTypeField, ntName));
         }
         terms.add(t);
      }
      // now search for all node types that are derived from base

      if (terms.size() == 0)
      {
         // exception occured
         return new BooleanQuery();
      }
      else if (terms.size() == 1)
      {
         return new TermQuery(terms.get(0));
      }
      else
      {
         BooleanQuery b = new BooleanQuery();
         for (Term term : terms)
         {
            b.add(new TermQuery(term), Occur.SHOULD);
         }
         return b;
      }
   }

   /**
    * @param parent
    * @param dataManager
    * @param changes
    * @throws RepositoryException
    */
   private void makeMixVesionableChanges(NodeData parent, ItemDataConsumer dataManager, PlainChangesLog changes)
      throws RepositoryException
   {
      new VersionHistoryDataHelper(parent, changes, dataManager, this);
   }

   /**
    * Notify the listeners that a node type <code>ntName</code> has been
    * registered.
    * 
    * @param ntName NT name.
    */
   private void notifyRegistered(InternalQName ntName)
   {
      // copy listeners to array to avoid ConcurrentModificationException
      NodeTypeManagerListener[] la = listeners.values().toArray(new NodeTypeManagerListener[listeners.size()]);
      for (int i = 0; i < la.length; i++)
      {
         if (la[i] != null)
         {
            la[i].nodeTypeRegistered(ntName);
         }
      }
   }

   /**
    * Notify the listeners that a node type <code>ntName</code> has been
    * re-registered.
    * 
    * @param ntName NT name.
    */
   private void notifyReRegistered(InternalQName ntName)
   {
      // copy listeners to array to avoid ConcurrentModificationException
      NodeTypeManagerListener[] la = listeners.values().toArray(new NodeTypeManagerListener[listeners.size()]);
      for (int i = 0; i < la.length; i++)
      {
         if (la[i] != null)
         {
            la[i].nodeTypeReRegistered(ntName);
         }
      }
   }

   /**
    * Notify the listeners that a node type <code>ntName</code> has been
    * unregistered.
    * 
    * @param ntName NT name.
    */
   private void notifyUnregistered(InternalQName ntName)
   {
      // copy listeners to array to avoid ConcurrentModificationException
      NodeTypeManagerListener[] la = listeners.values().toArray(new NodeTypeManagerListener[listeners.size()]);
      for (int i = 0; i < la.length; i++)
      {
         if (la[i] != null)
         {
            la[i].nodeTypeUnregistered(ntName);
         }
      }
   }

   /**
    * @param nodeType
    * @param checkExistence
    * @return
    * @throws RepositoryException
    * @throws PathNotFoundException
    * @throws ValueFormatException
    */
   private PlainChangesLog persistNodeTypeData(NodeTypeData nodeType, boolean checkExistence)
      throws RepositoryException, PathNotFoundException, ValueFormatException
   {
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      long start = System.currentTimeMillis();
      if (persister.isInitialized())
      {
         try
         {
            if (!(checkExistence && persister.hasNodeTypeData(nodeType.getName())))
            {
               changesLog.addAll(persister.addNodeType(nodeType).getAllStates());
            }
         }
         catch (InvalidItemStateException e)
         {
            LOG.warn("Error of storing node type " + nodeType.getName() + ". May be node type already registered .", e);
         }
         if (LOG.isDebugEnabled())
            LOG.debug("NodeType " + nodeType.getName() + " initialized. " + (System.currentTimeMillis() - start)
               + " ms");
      }
      else
      {
         if (LOG.isDebugEnabled())
            LOG.debug("NodeType " + nodeType.getName()
               + " registered but not initialized (storage is not initialized). "
               + (System.currentTimeMillis() - start) + " ms");
      }
      return changesLog;
   }

   /**
    * {@inheritDoc}
    */
   private PlainChangesLog registerNodeType(NodeTypeData nodeType, int alreadyExistsBehaviour,
      Map<InternalQName, NodeTypeData> volatileNodeTypes) throws RepositoryException
   {

      if (nodeType == null)
      {
         throw new RepositoryException("NodeTypeData object " + nodeType + " is null");
      }

      long start = System.currentTimeMillis();

      if (accessControlPolicy.equals(AccessControlPolicy.DISABLE) && nodeType.getName().equals("exo:privilegeable"))
      {
         throw new RepositoryException("NodeType exo:privilegeable is DISABLED");
      }

      InternalQName qname = nodeType.getName();
      if (qname == null)
      {
         throw new RepositoryException("NodeType implementation class " + nodeType.getClass().getName()
            + " is not supported in this method");
      }
      PlainChangesLog changesLog = new PlainChangesLogImpl();

      NodeTypeData registeredNodeType = findNodeType(qname);
      if (registeredNodeType != null)
      {
         switch (alreadyExistsBehaviour)
         {
            case ExtendedNodeTypeManager.FAIL_IF_EXISTS :
               throw new RepositoryException("NodeType " + nodeType.getName() + " is already registered");
            case ExtendedNodeTypeManager.IGNORE_IF_EXISTS :
               if (LOG.isDebugEnabled())
                  LOG.debug("Skipped " + nodeType.getName().getAsString() + " as already registered");
               break;
            case ExtendedNodeTypeManager.REPLACE_IF_EXISTS :
               changesLog.addAll(reregisterNodeType(registeredNodeType, nodeType, volatileNodeTypes).getAllStates());
               break;
         }
      }
      else
      {
         internalRegister(nodeType, volatileNodeTypes);
         changesLog.addAll(persistNodeTypeData(nodeType, true).getAllStates());

      }
      return changesLog;
   }

   /**
    * @param nodeType
    * @return
    * @throws RepositoryException
    */
   private List<ItemState> removePersistedNodeType(NodeTypeData nodeType) throws RepositoryException
   {
      return persister.removeNodeType(nodeType);
   }

   private PlainChangesLog reregisterNodeType(NodeTypeData ancestorDefinition, NodeTypeData recipientDefinition,
      Map<InternalQName, NodeTypeData> volatileNodeTypes) throws ConstraintViolationException, RepositoryException
   {
      if (!ancestorDefinition.getName().equals(recipientDefinition.getName()))
      {
         throw new RepositoryException("Unsupported Operation");
      }
      if (buildInNodeTypesNames.contains(recipientDefinition.getName()))
      {
         throw new RepositoryException(recipientDefinition.getName() + ": can't reregister built-in node type.");
      }
      PlainChangesLog changesLog = new PlainChangesLogImpl();
      VolatileNodeTypeDataManager volatileNodeTypeDataManager = new VolatileNodeTypeDataManager(this);

      volatileNodeTypeDataManager.registerVolatileNodeTypes(volatileNodeTypes);

      Set<String> nodes = getNodes(recipientDefinition.getName());
      // check add mix:versionable super
      if (isNodeType(Constants.MIX_VERSIONABLE, recipientDefinition.getDeclaredSupertypeNames())
         && !isNodeType(Constants.MIX_VERSIONABLE, ancestorDefinition.getDeclaredSupertypeNames()))
      {

         for (String uuid : nodes)
         {
            ItemData item = persister.getDataManager().getItemData(uuid);
            if (item != null && item.isNode())
            {
               makeMixVesionableChanges(((NodeData)item), persister.getDataManager(), changesLog);
            }
         }
      }
      else if (!isNodeType(Constants.MIX_VERSIONABLE, recipientDefinition.getDeclaredSupertypeNames())
         && isNodeType(Constants.MIX_VERSIONABLE, ancestorDefinition.getDeclaredSupertypeNames()))
      {
         if (nodes.size() > 0)
         {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Fail to change ");
            buffer.append(recipientDefinition.getName().getAsString());
            buffer.append(" node type from mix:versionable = true  to mix:versionable = false");
            buffer.append(" because the folowing node exists: ");
            for (String uuid : nodes)
            {
               ItemData item = persister.getDataManager().getItemData(uuid);
               if (item != null && item.isNode())
               {
                  buffer.append(item.getQPath().getAsString());
                  buffer.append(" ");
               }
            }
            throw new ConstraintViolationException(buffer.toString());
         }
      }

      // child nodes
      NodeDefinitionComparator nodeDefinitionComparator =
         new NodeDefinitionComparator(volatileNodeTypeDataManager, persister.getDataManager());
      changesLog.addAll(nodeDefinitionComparator.compare(recipientDefinition,
         getAllChildNodeDefinitions(ancestorDefinition, new HashMap<InternalQName, NodeTypeData>()),
         getAllChildNodeDefinitions(recipientDefinition, volatileNodeTypes)).getAllStates());

      // properties defs
      PropertyDefinitionComparator propertyDefinitionComparator =
         new PropertyDefinitionComparator(volatileNodeTypeDataManager, persister.getDataManager(), locationFactory);
      changesLog.addAll(propertyDefinitionComparator.compare(recipientDefinition,
         getAllPropertyDefinitions(ancestorDefinition, new HashMap<InternalQName, NodeTypeData>()),
         getAllPropertyDefinitions(recipientDefinition, volatileNodeTypes))

      .getAllStates());

      // mixin changed
      if (!Arrays.deepEquals(recipientDefinition.getDeclaredSupertypeNames(), ancestorDefinition
         .getDeclaredSupertypeNames()))
      {
         for (String uuid : nodes)
         {
            ItemData item = persister.getDataManager().getItemData(uuid);
            if (item != null && item.isNode())
            {
               if (!(item instanceof TransientNodeData))
                  item =
                     new TransientNodeData(item.getQPath(), item.getIdentifier(), item.getPersistedVersion(),
                        ((NodeData)item).getPrimaryTypeName(), ((NodeData)item).getMixinTypeNames(), ((NodeData)item)
                           .getOrderNumber(), ((NodeData)item).getParentIdentifier(), ((NodeData)item).getACL());
               changesLog.add(new ItemState(item, ItemState.MIXIN_CHANGED, false, null));
            }
         }
      }

      // mixin
      if (ancestorDefinition.isMixin() != recipientDefinition.isMixin())
      {

         if (nodes.size() > 0)
         {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Fail to change ");
            buffer.append(recipientDefinition.getName().getAsString());
            buffer.append(" node type from IsMixin=");
            buffer.append(ancestorDefinition.isMixin());
            buffer.append(" to IsMixin=");
            buffer.append(recipientDefinition.isMixin());
            buffer.append(" because the folowing node exists: ");
            for (String uuid : nodes)
            {
               ItemData item = persister.getDataManager().getItemData(uuid);
               if (item != null && item.isNode())
               {
                  buffer.append(item.getQPath().getAsString());
                  buffer.append(" ");
               }
            }
            throw new ConstraintViolationException(buffer.toString());
         }
      }

      internalUnregister(ancestorDefinition.getName(), ancestorDefinition);
      changesLog.addAll(removePersistedNodeType(ancestorDefinition));

      internalRegister(recipientDefinition, volatileNodeTypes);
      changesLog.addAll(persistNodeTypeData(recipientDefinition, false).getAllStates());

      return changesLog;
   }
}
