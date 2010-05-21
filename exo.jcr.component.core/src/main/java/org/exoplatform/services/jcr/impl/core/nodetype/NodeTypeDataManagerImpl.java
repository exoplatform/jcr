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

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.ItemDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionData;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionDatas;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeDefinitionComparator;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeConverter;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataPersister;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.NodeTypeDataValidator;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.PropertyDefinitionComparator;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.XmlNodeTypeDataPersister;
import org.exoplatform.services.jcr.impl.core.query.RepositoryIndexSearcherHolder;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
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
public class NodeTypeDataManagerImpl implements NodeTypeDataManager, Startable
{

   private static final String NODETYPES_FILE = "nodetypes.xml";

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.NodeTypeDataManagerImpl");

   protected final String accessControlPolicy;

   protected final ItemDataConsumer dataManager;

   protected final RepositoryIndexSearcherHolder indexSearcherHolder;

   protected final LocationFactory locationFactory;

   protected final NamespaceRegistry namespaceRegistry;

   private final Set<InternalQName> buildInNodeTypesNames;

   protected final NodeTypeRepository nodeTypeRepository;

   protected final NodeTypeConverter nodeTypeConverter;

   protected final NodeTypeDataValidator nodeTypeDataValidator;

   /**
    * Listeners (soft references)
    */
   private final Map<NodeTypeManagerListener, NodeTypeManagerListener> listeners;

   private final ValueFactoryImpl valueFactory;

   private boolean started = false;

   /**
    * NodeTypeDataManagerImpl constructor.
    * 
    * @param accessControlPolicy String
    * @param locationFactory LocationFactory
    * @param namespaceRegistry NamespaceRegistry
    * @param persister NodeTypeDataPersister
    * @param dataManager ItemDataConsumer
    * @param indexSearcherHolder RepositoryIndexSearcherHolder
    * @param nodeTypeRepository NodeTypeRepository
    */
   public NodeTypeDataManagerImpl(final String accessControlPolicy, final LocationFactory locationFactory,
      final NamespaceRegistry namespaceRegistry, final NodeTypeDataPersister persister,
      final ItemDataConsumer dataManager, final RepositoryIndexSearcherHolder indexSearcherHolder,
      final NodeTypeRepository nodeTypeRepository)
   {

      this.namespaceRegistry = namespaceRegistry;
      this.locationFactory = locationFactory;
      this.dataManager = dataManager;
      this.indexSearcherHolder = indexSearcherHolder;

      this.valueFactory = new ValueFactoryImpl(locationFactory);
      this.accessControlPolicy = accessControlPolicy;

      this.nodeTypeRepository = nodeTypeRepository;
      this.listeners = Collections.synchronizedMap(new WeakHashMap<NodeTypeManagerListener, NodeTypeManagerListener>());
      this.buildInNodeTypesNames = new HashSet<InternalQName>();

      this.nodeTypeConverter = new NodeTypeConverter(this.locationFactory, this.accessControlPolicy);
      this.nodeTypeDataValidator = new NodeTypeDataValidator(this.locationFactory, this.nodeTypeRepository);
   }

   /**
    * Constructor for in-container use.
    * 
    * @param config RepositoryEntry
    * @param locationFactory LocationFactory
    * @param namespaceRegistry NamespaceRegistry
    * @param persister NodeTypeDataPersister
    * @param dataManager ItemDataConsumer
    * @param indexSearcherHolder RepositoryIndexSearcherHolder
    */
   public NodeTypeDataManagerImpl(final RepositoryEntry config, final LocationFactory locationFactory,
      final NamespaceRegistry namespaceRegistry, final NodeTypeDataPersister persister,
      final ItemDataConsumer dataManager, final RepositoryIndexSearcherHolder indexSearcherHolder)
   {
      this(config.getAccessControl(), locationFactory, namespaceRegistry, persister, dataManager, indexSearcherHolder,
         new InmemoryNodeTypeRepository(persister));
   }

   /**
    * Add a <code>NodeTypeRegistryListener</code>
    * 
    * @param listener the new listener to be informed on (un)registration of node
    *          types
    */
   public void addListener(final NodeTypeManagerListener listener)
   {
      if (!this.listeners.containsKey(listener))
      {
         this.listeners.put(listener, listener);
      }
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public NodeDefinitionData[] getAllChildNodeDefinitions(final InternalQName... nodeTypeNames)

   {
      final Collection<NodeDefinitionData> defsAny = new ArrayList<NodeDefinitionData>();
      final HashMap<InternalQName, NodeDefinitionData> defs = new HashMap<InternalQName, NodeDefinitionData>();

      for (final InternalQName ntname : nodeTypeNames)
      {
         for (final InternalQName suname : this.nodeTypeRepository.getSupertypes(ntname))
         {
            for (final NodeDefinitionData cnd : this.nodeTypeRepository.getNodeType(suname)
               .getDeclaredChildNodeDefinitions())
            {
               if (cnd.getName().equals(Constants.JCR_ANY_NAME))
               {
                  defsAny.add(cnd);
               }
               else
               {
                  defs.put(cnd.getName(), cnd);
               }
            }
         }

         for (final NodeDefinitionData cnd : this.nodeTypeRepository.getNodeType(ntname)
            .getDeclaredChildNodeDefinitions())
         {
            if (cnd.getName().equals(Constants.JCR_ANY_NAME))
            {
               defsAny.add(cnd);
            }
            else
            {
               defs.put(cnd.getName(), cnd);
            }
         }
      }

      defsAny.addAll(defs.values());

      return defsAny.toArray(new NodeDefinitionData[defsAny.size()]);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public List<NodeTypeData> getAllNodeTypes()
   {
      try
      {
         return this.nodeTypeRepository.getAllNodeTypes();
      }
      catch (RepositoryException e)
      {
         log.error(e.getLocalizedMessage());
      }
      return new ArrayList<NodeTypeData>();

   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public PropertyDefinitionData[] getAllPropertyDefinitions(final InternalQName... nodeTypeNames)

   {
      final Collection<PropertyDefinitionData> defsAny = new ArrayList<PropertyDefinitionData>();
      final HashMap<InternalQName, PropertyDefinitionData> defs = new HashMap<InternalQName, PropertyDefinitionData>();

      for (final InternalQName ntname : nodeTypeNames)
      {

         for (final InternalQName suname : this.nodeTypeRepository.getSupertypes(ntname))
         {
            for (final PropertyDefinitionData pd : this.nodeTypeRepository.getNodeType(suname)
               .getDeclaredPropertyDefinitions())
            {
               if (pd.getName().equals(Constants.JCR_ANY_NAME))
               {
                  defsAny.add(pd);
               }
               else
               {
                  defs.put(pd.getName(), pd);
               }
            }
         }

         for (final PropertyDefinitionData pd : this.nodeTypeRepository.getNodeType(ntname)
            .getDeclaredPropertyDefinitions())
         {
            if (pd.getName().equals(Constants.JCR_ANY_NAME))
            {
               defsAny.add(pd);
            }
            else
            {
               defs.put(pd.getName(), pd);
            }
         }
      }

      defsAny.addAll(defs.values());

      return defsAny.toArray(new PropertyDefinitionData[defsAny.size()]);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public NodeDefinitionData getChildNodeDefinition(final InternalQName nodeName, final InternalQName... nodeTypeNames)
      throws RepositoryException
   {

      NodeDefinitionData ndResidual = this.nodeTypeRepository.getDefaultChildNodeDefinition(nodeName, nodeTypeNames);

      if (ndResidual == null && !Constants.JCR_ANY_NAME.equals(nodeName))
      {
         ndResidual = getChildNodeDefinition(Constants.JCR_ANY_NAME, nodeTypeNames);
      }

      return ndResidual;

   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public NodeDefinitionData getChildNodeDefinition(final InternalQName nodeName, final InternalQName primaryNodeType,
      final InternalQName[] mixinTypes) throws RepositoryException
   {

      if (mixinTypes != null)
      {
         final InternalQName[] nts = new InternalQName[mixinTypes.length + 1];
         nts[0] = primaryNodeType;
         for (int i = 0; i < mixinTypes.length; i++)
         {
            nts[i + 1] = mixinTypes[i];
         }
         return getChildNodeDefinition(nodeName, nts);
      }

      return getChildNodeDefinition(nodeName, primaryNodeType);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public Set<InternalQName> getDeclaredSubtypes(final InternalQName nodeTypeName)
   {
      return this.nodeTypeRepository.getDeclaredSubtypes(nodeTypeName);

   }

   public List<ItemDefinitionData> getManadatoryItemDefs(final InternalQName primaryNodeType,
      final InternalQName[] mixinTypes) throws RepositoryException
   {
      final Collection<ItemDefinitionData> mandatoryDefs = new HashSet<ItemDefinitionData>();
      // primary type properties
      ItemDefinitionData[] itemDefs = getAllPropertyDefinitions(new InternalQName[]{primaryNodeType});
      for (final ItemDefinitionData itemDef : itemDefs)
      {
         if (itemDef.isMandatory())
         {
            mandatoryDefs.add(itemDef);
         }
      }
      // primary type nodes
      itemDefs = getAllChildNodeDefinitions(new InternalQName[]{primaryNodeType});
      for (final ItemDefinitionData itemDef : itemDefs)
      {
         if (itemDef.isMandatory())
         {
            mandatoryDefs.add(itemDef);
         }
      }
      // mixin properties
      itemDefs = getAllPropertyDefinitions(mixinTypes);
      for (final ItemDefinitionData itemDef : itemDefs)
      {
         if (itemDef.isMandatory())
         {
            mandatoryDefs.add(itemDef);
         }
      }
      // mixin nodes
      itemDefs = getAllChildNodeDefinitions(mixinTypes);
      for (final ItemDefinitionData itemDef : itemDefs)
      {
         if (itemDef.isMandatory())
         {
            mandatoryDefs.add(itemDef);
         }
      }
      return new ArrayList<ItemDefinitionData>(mandatoryDefs);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public NodeTypeData getNodeType(final InternalQName typeName)
   {
      return this.nodeTypeRepository.getNodeType(typeName);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public PropertyDefinitionDatas getPropertyDefinitions(final InternalQName propertyName,
      final InternalQName... nodeTypeNames) throws RepositoryException
   {

      PropertyDefinitionDatas propertyDefinitions =
         this.nodeTypeRepository.getPropertyDefinitions(propertyName, nodeTypeNames);

      // Try super
      if (propertyDefinitions == null)
      {
         for (int i = 0; i < nodeTypeNames.length && propertyDefinitions == null; i++)
         {
            final InternalQName[] supers =
               this.nodeTypeRepository.getNodeType(nodeTypeNames[i]).getDeclaredSupertypeNames();
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

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public PropertyDefinitionDatas getPropertyDefinitions(final InternalQName propertyName,
      final InternalQName primaryNodeType, final InternalQName[] mixinTypes) throws RepositoryException
   {

      if (mixinTypes != null)
      {
         final InternalQName[] nts = new InternalQName[mixinTypes.length + 1];
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
    * @param nodeTypeName
    * @return
    * @throws RepositoryException 
    */
   public Set<InternalQName> getSubtypes(final InternalQName nodeTypeName)
   {
      return this.nodeTypeRepository.getSubtypes(nodeTypeName);
   }

   public Set<InternalQName> getSupertypes(final InternalQName nodeTypeName)
   {
      return this.nodeTypeRepository.getSupertypes(nodeTypeName);
   }

   public boolean isChildNodePrimaryTypeAllowed(final InternalQName childNodeTypeName,
      final InternalQName parentNodeType, final InternalQName[] parentMixinNames) throws RepositoryException
   {
      final Set<InternalQName> testSuperTypesNames = this.nodeTypeRepository.getSupertypes(childNodeTypeName);
      NodeDefinitionData[] allChildNodeDefinitions = getAllChildNodeDefinitions(parentNodeType);
      for (final NodeDefinitionData cnd : allChildNodeDefinitions)
      {
         for (final InternalQName req : cnd.getRequiredPrimaryTypes())
         {
            if (childNodeTypeName.equals(req))
            {
               return true;
            }
            for (final InternalQName superName : testSuperTypesNames)
            {
               if (superName.equals(req))
               {
                  return true;
               }
            }
         }
      }
      allChildNodeDefinitions = getAllChildNodeDefinitions(parentMixinNames);
      for (final NodeDefinitionData cnd : allChildNodeDefinitions)
      {
         for (final InternalQName req : cnd.getRequiredPrimaryTypes())
         {
            if (childNodeTypeName.equals(req))
            {
               return true;
            }
            for (final InternalQName superName : testSuperTypesNames)
            {
               if (superName.equals(req))
               {
                  return true;
               }
            }
         }
      }

      return false;
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public boolean isNodeType(final InternalQName testTypeName, final InternalQName... typesNames)

   {
      return this.nodeTypeRepository.isNodeType(testTypeName, typesNames);
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public boolean isNodeType(final InternalQName testTypeName, final InternalQName primaryType,
      final InternalQName[] mixinTypes)
   {

      if (this.nodeTypeRepository.isNodeType(testTypeName, primaryType))
      {
         return true;
      }

      if (this.nodeTypeRepository.isNodeType(testTypeName, mixinTypes))
      {
         return true;
      }

      return false;
   }

   /**
    * {@inheritDoc}
    * @throws RepositoryException 
    */
   public boolean isOrderableChildNodesSupported(final InternalQName primaryType, final InternalQName[] mixinTypes)
      throws RepositoryException
   {

      final int nlen = mixinTypes != null ? mixinTypes.length : 0;
      for (int i = -1; i < nlen; i++)
      {
         InternalQName name;
         if (i < 0)
         {
            name = primaryType;
         }
         else
         {
            name = mixinTypes[i];
         }

         final NodeTypeData nt = this.nodeTypeRepository.getNodeType(name);

         if (nt != null)
         {
            if (nt.hasOrderableChildNodes())
            {
               return true;
            }

            final Set<InternalQName> supers = this.nodeTypeRepository.getSupertypes(nt.getName());
            for (final InternalQName suName : supers)
            {
               final NodeTypeData su = this.nodeTypeRepository.getNodeType(suName);
               if (su != null && su.hasOrderableChildNodes())
               {
                  return true;
               }
            }
         }
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> registerNodeTypes(final InputStream is, final int alreadyExistsBehaviour,
      final String contentType) throws RepositoryException
   {
      NodeTypeDataPersister serializer = null;
      if (contentType.equalsIgnoreCase(TEXT_XML))
      {
         serializer = new XmlNodeTypeDataPersister(nodeTypeConverter, is);
      }
      else if (contentType.equalsIgnoreCase(TEXT_X_JCR_CND))
      {
         throw new RepositoryException("Unsupported content type:" + contentType);
      }
      else
      {
         throw new RepositoryException("Unsupported content type:" + contentType);
      }
      final List<NodeTypeData> nodeTypes = serializer.getAllNodeTypes();

      // validate
      nodeTypeDataValidator.validateNodeType(nodeTypes);

      nodeTypeRepository.registerNodeType(nodeTypes, this, accessControlPolicy, alreadyExistsBehaviour);
      return nodeTypes;
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeTypeData> registerNodeTypes(final List<NodeTypeValue> ntvalues, final int alreadyExistsBehaviour)
      throws RepositoryException
   {
      // convert to Node data.
      final List<NodeTypeData> nodeTypes = nodeTypeConverter.convertFromValueToData(ntvalues);

      // validate
      nodeTypeDataValidator.validateNodeType(nodeTypes);

      nodeTypeRepository.registerNodeType(nodeTypes, this, accessControlPolicy, alreadyExistsBehaviour);

      return nodeTypes;
   }

   /**
    * Remove a <code>NodeTypeRegistryListener</code>.
    * 
    * @param listener an existing listener
    */
   public void removeListener(final NodeTypeManagerListener listener)
   {
      this.listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public PlainChangesLog setPrimaryType(final NodeData nodeData, final InternalQName nodeTypeName)
      throws RepositoryException
   {
      final PlainChangesLog changesLog = new PlainChangesLogImpl();

      final NodeTypeData ancestorDefinition = getNodeType(nodeData.getPrimaryTypeName());
      final NodeTypeData recipientDefinition = getNodeType(nodeTypeName);

      InternalQName[] ancestorAllNodeTypeNames = null;
      if (nodeData.getMixinTypeNames() == null || nodeData.getMixinTypeNames().length == 0)
      {
         ancestorAllNodeTypeNames = new InternalQName[]{nodeData.getPrimaryTypeName()};
      }
      else
      {
         ancestorAllNodeTypeNames = new InternalQName[nodeData.getMixinTypeNames().length + 1];
         ancestorAllNodeTypeNames[0] = nodeData.getPrimaryTypeName();
         System.arraycopy(nodeData.getMixinTypeNames(), 0, ancestorAllNodeTypeNames, 1,
            nodeData.getMixinTypeNames().length);
      }
      InternalQName[] recipienAllNodeTypeNames = null;
      if (nodeData.getMixinTypeNames() == null || nodeData.getMixinTypeNames().length == 0)
      {
         recipienAllNodeTypeNames = new InternalQName[]{nodeTypeName};
      }
      else
      {
         recipienAllNodeTypeNames = new InternalQName[nodeData.getMixinTypeNames().length + 1];
         recipienAllNodeTypeNames[0] = nodeTypeName;
         System.arraycopy(nodeData.getMixinTypeNames(), 0, recipienAllNodeTypeNames, 1,
            nodeData.getMixinTypeNames().length);
      }

      final boolean recipientsMixVersionable = isNodeType(Constants.MIX_VERSIONABLE, recipienAllNodeTypeNames);
      final boolean ancestorIsMixVersionable = isNodeType(Constants.MIX_VERSIONABLE, ancestorAllNodeTypeNames);

      ItemAutocreator itemAutocreator = new ItemAutocreator(this, valueFactory, dataManager, false);
      if (recipientsMixVersionable && !ancestorIsMixVersionable)
      {

         changesLog.addAll(itemAutocreator.makeMixVesionableChanges(nodeData).getAllStates());
      }
      else if (!recipientsMixVersionable && ancestorIsMixVersionable)
      {

         final StringBuffer buffer = new StringBuffer();
         buffer.append("Fail to change  node type from ");
         buffer.append(ancestorDefinition.getName().getAsString());
         buffer.append(" to ");
         buffer.append(recipientDefinition.getName().getAsString());
         buffer.append(" because change from  mix:versionable = true ");
         buffer.append(" to mix:versionable = false is not alowed");

         throw new ConstraintViolationException(buffer.toString());
      }

      // update primary type

      final PropertyData item =
         (PropertyData)this.dataManager.getItemData(nodeData, new QPathEntry(Constants.JCR_PRIMARYTYPE, 1));

      final TransientPropertyData primaryTypeData =
         new TransientPropertyData(item.getQPath(), item.getIdentifier(), item.getPersistedVersion(), item.getType(),
            item.getParentIdentifier(), item.isMultiValued(), new TransientValueData(nodeTypeName));

      changesLog.add(ItemState.createUpdatedState(primaryTypeData, true));

      final List<NodeData> affectedNodes = new ArrayList<NodeData>();
      affectedNodes.add(nodeData);

      // child nodes
      final NodeDefinitionComparator nodeDefinitionComparator =
         new NodeDefinitionComparator(this, dataManager, itemAutocreator, affectedNodes);
      changesLog.addAll(nodeDefinitionComparator.compare(recipientDefinition,
         getAllChildNodeDefinitions(ancestorAllNodeTypeNames), getAllChildNodeDefinitions(recipienAllNodeTypeNames))
         .getAllStates());

      // properties defs
      final PropertyDefinitionComparator propertyDefinitionComparator =
         new PropertyDefinitionComparator(this, dataManager, itemAutocreator, affectedNodes, locationFactory);
      changesLog.addAll(propertyDefinitionComparator.compare(recipientDefinition,
         getAllPropertyDefinitions(ancestorAllNodeTypeNames), getAllPropertyDefinitions(recipienAllNodeTypeNames))

      .getAllStates());

      return changesLog;
   }

   /**
    * 
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
      if (!started)
      {
         try
         {

            // check if default node type saved
            if (!nodeTypeRepository.isStorageFilled())
            {
               final InputStream xml = NodeTypeManagerImpl.class.getResourceAsStream(NODETYPES_FILE);

               if (xml != null)
               {
                  List<NodeTypeData> registerNodeTypes =
                     registerNodeTypes(xml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS, TEXT_XML);
                  for (NodeTypeData nodeTypeData : registerNodeTypes)
                  {
                     buildInNodeTypesNames.add(nodeTypeData.getName());
                  }
               }
               else
               {
                  throw new RuntimeException("Resource file '" + NODETYPES_FILE
                     + "' with NodeTypes configuration does not found. Can not create node type manager");
               }
            }
            else
            {
               final List<NodeTypeData> allNodeTypes = nodeTypeRepository.getAllNodeTypes();
               // register nodetypes in runtime
               final Map<InternalQName, NodeTypeData> volatileNodeTypes = new HashMap<InternalQName, NodeTypeData>();
               //create map from list
               for (final NodeTypeData nodeTypeData : allNodeTypes)
               {
                  volatileNodeTypes.put(nodeTypeData.getName(), nodeTypeData);
               }

               for (final NodeTypeData nodeTypeData : allNodeTypes)
               {
                  this.nodeTypeRepository.addNodeType(nodeTypeData, volatileNodeTypes);
               }
            }

         }
         catch (final RepositoryException e)
         {
            throw new RuntimeException(e.getLocalizedMessage(), e);
         }
         started = true;
      }
   }

   /**
    * 
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
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
   public void unregisterNodeType(final InternalQName nodeTypeName) throws RepositoryException
   {

      final NodeTypeData nodeType = this.nodeTypeRepository.getNodeType(nodeTypeName);
      if (nodeType == null)
      {
         throw new NoSuchNodeTypeException(nodeTypeName.getAsString());
      }
      // check build in
      if (this.buildInNodeTypesNames.contains(nodeTypeName))
      {
         throw new RepositoryException(nodeTypeName.toString() + ": can't unregister built-in node type.");
      }
      // check dependencies
      final Set<InternalQName> descendantNt = this.nodeTypeRepository.getSubtypes(nodeTypeName);
      if (descendantNt.size() > 0)
      {
         String message =
            "Can not remove " + nodeTypeName.getAsString()
               + "nodetype, because the following node types depend on it: ";
         for (final InternalQName internalQName : descendantNt)
         {
            message += internalQName.getAsString() + " ";
         }
         throw new RepositoryException(message);
      }
      final Set<String> nodes = this.indexSearcherHolder.getNodesByNodeType(nodeTypeName);
      if (nodes.size() > 0)
      {
         String message =
            "Can not remove " + nodeTypeName.getAsString()
               + " nodetype, because the following node types is used in nodes with uuid: ";
         for (final String uuids : nodes)
         {
            message += uuids + " ";
         }
         throw new RepositoryException(message);

      }
      this.nodeTypeRepository.removeNodeType(nodeType);
   }

   /**
    * @throws RepositoryException 
    * @throws ConstraintViolationException 
    * @see org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager#updateNodeType(org.exoplatform.services.jcr.core.nodetype.NodeTypeData, org.exoplatform.services.jcr.core.nodetype.NodeTypeData, java.util.Map)
    */
   public PlainChangesLog updateNodeType(NodeTypeData ancestorDefinition, NodeTypeData recipientDefinition,
      Map<InternalQName, NodeTypeData> volatileNodeTypes) throws ConstraintViolationException, RepositoryException
   {

      if (!ancestorDefinition.getName().equals(recipientDefinition.getName()))
      {
         throw new RepositoryException("Unsupported Operation");
      }
      if (this.buildInNodeTypesNames.contains(recipientDefinition.getName()))
      {
         throw new RepositoryException(recipientDefinition.getName() + ": can't reregister built-in node type.");
      }
      final PlainChangesLog changesLog = new PlainChangesLogImpl();
      final VolatileNodeTypeDataManager volatileNodeTypeDataManager = new VolatileNodeTypeDataManager(this);

      volatileNodeTypeDataManager.registerVolatileNodeTypes(volatileNodeTypes);

      ItemAutocreator itemAutocreator =
         new ItemAutocreator(volatileNodeTypeDataManager, valueFactory, dataManager, false);

      final Set<String> nodes = this.indexSearcherHolder.getNodesByNodeType(recipientDefinition.getName());
      // check add mix:versionable super
      if (isNodeType(Constants.MIX_VERSIONABLE, recipientDefinition.getDeclaredSupertypeNames())
         && !isNodeType(Constants.MIX_VERSIONABLE, ancestorDefinition.getDeclaredSupertypeNames()))
      {

         for (final String uuid : nodes)
         {
            final ItemData item = this.dataManager.getItemData(uuid);
            if (item != null && item.isNode())
            {
               changesLog.addAll(itemAutocreator.makeMixVesionableChanges((NodeData)item).getAllStates());
            }
         }
      }
      else if (!isNodeType(Constants.MIX_VERSIONABLE, recipientDefinition.getDeclaredSupertypeNames())
         && isNodeType(Constants.MIX_VERSIONABLE, ancestorDefinition.getDeclaredSupertypeNames()))
      {
         if (nodes.size() > 0)
         {
            final StringBuffer buffer = new StringBuffer();
            buffer.append("Fail to change ");
            buffer.append(recipientDefinition.getName().getAsString());
            buffer.append(" node type from mix:versionable = true  to mix:versionable = false");
            buffer.append(" because the folowing node exists: ");
            if (nodes.size() < 100)
            {
               for (final String uuid : nodes)
               {
                  final ItemData item = this.dataManager.getItemData(uuid);
                  if (item != null && item.isNode())
                  {
                     buffer.append(item.getQPath().getAsString());
                     buffer.append(" ");
                  }
               }
            }
            throw new ConstraintViolationException(buffer.toString());
         }
      }

      final List<NodeData> affectedNodes = new ArrayList<NodeData>();
      for (final String uuid : nodes)
      {
         final ItemData nodeData = this.dataManager.getItemData(uuid);
         if (nodeData != null)
         {
            if (nodeData.isNode())
            {
               affectedNodes.add((NodeData)nodeData);
            }
         }
      }

      // child nodes
      final NodeDefinitionComparator nodeDefinitionComparator =
         new NodeDefinitionComparator(volatileNodeTypeDataManager, this.dataManager, itemAutocreator, affectedNodes);
      changesLog.addAll(nodeDefinitionComparator.compare(recipientDefinition,
         getAllChildNodeDefinitions(ancestorDefinition.getName()),
         volatileNodeTypeDataManager.getAllChildNodeDefinitions(recipientDefinition.getName())).getAllStates());

      // properties defs
      final PropertyDefinitionComparator propertyDefinitionComparator =
         new PropertyDefinitionComparator(volatileNodeTypeDataManager, this.dataManager, itemAutocreator,
            affectedNodes, this.locationFactory);
      changesLog.addAll(propertyDefinitionComparator.compare(recipientDefinition,
         getAllPropertyDefinitions(ancestorDefinition.getName()),
         volatileNodeTypeDataManager.getAllPropertyDefinitions(recipientDefinition.getName()))

      .getAllStates());

      // notify listeners about changes
      if (!Arrays.deepEquals(recipientDefinition.getDeclaredSupertypeNames(), ancestorDefinition
         .getDeclaredSupertypeNames()))
      {
         for (final String uuid : nodes)
         {
            ItemData item = this.dataManager.getItemData(uuid);
            if (item != null && item.isNode())
            {
               if (!(item instanceof TransientNodeData))
               {
                  item =
                     new TransientNodeData(item.getQPath(), item.getIdentifier(), item.getPersistedVersion(),
                        ((NodeData)item).getPrimaryTypeName(), ((NodeData)item).getMixinTypeNames(), ((NodeData)item)
                           .getOrderNumber(), ((NodeData)item).getParentIdentifier(), ((NodeData)item).getACL());
               }
               changesLog.add(new ItemState(item, ItemState.MIXIN_CHANGED, false, null));
            }
         }
      }

      // mixin changed
      if (ancestorDefinition.isMixin() != recipientDefinition.isMixin())
      {

         if (nodes.size() > 0)
         {
            final StringBuffer buffer = new StringBuffer();
            buffer.append("Fail to change ");
            buffer.append(recipientDefinition.getName().getAsString());
            buffer.append(" node type from IsMixin=");
            buffer.append(ancestorDefinition.isMixin());
            buffer.append(" to IsMixin=");
            buffer.append(recipientDefinition.isMixin());
            buffer.append(" because the folowing node exists: ");
            if (nodes.size() < 100)
            {
               for (final String uuid : nodes)
               {
                  final ItemData item = this.dataManager.getItemData(uuid);
                  if (item != null && item.isNode())
                  {
                     buffer.append(item.getQPath().getAsString());
                     buffer.append(" ");
                  }
               }
            }
            throw new ConstraintViolationException(buffer.toString());
         }
      }

      this.nodeTypeRepository.removeNodeType(ancestorDefinition);

      this.nodeTypeRepository.addNodeType(recipientDefinition, volatileNodeTypes);

      return changesLog;
   }
}
