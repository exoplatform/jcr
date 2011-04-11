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
package org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NullItemData;
import org.exoplatform.services.jcr.datamodel.NullNodeData;
import org.exoplatform.services.jcr.datamodel.NullPropertyData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestor;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory;
import org.exoplatform.services.jcr.jbosscache.ExoJBossCacheFactory.CacheType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionService;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.config.Configuration.CacheMode;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.eviction.ExpirationAlgorithmConfig;
import org.picocontainer.Startable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.<p/>
 * 
 * Cache based on JBossCache.<p/>
 *
 * <ul>
 * <li>cache transparent: or item cached or not, we should not generate "not found" Exceptions </li>
 * <li>cache consists of next resident nodes:
 *      <ul>
 *      <li>/$ITEMS - stores items by Id (i.e. /$ITEMS/itemId)</li>
 *      <li>/$CHILD_NODES, /$CHILD_PROPS - stores items by parentId and name (i.e. /$CHILD_NODES/parentId/childName.$ITEM_ID)</li>
 *      <li>/$CHILD_NODES_LIST, /$CHILD_PROPS_LIST - stores child list by parentId and child Id
 *      (i.e. /$CHILD_NODES_LIST/parentId.lists = serialized Set<Object>)</li>
 *      </ul>
 * </li>
 * <li>all child properties/nodes lists should be evicted from parent at same time
 *      i.e. for /$CHILD_NODES_LIST, /$CHILD_PROPS_LIST we need customized eviction policy (EvictionActionPolicy) to evict
 *      whole list on one of childs eviction
 * </li>
 * </ul>
 * 
 * <p/>
 * Current state notes (subject of change):
 * <ul>
 * <li>cache implements WorkspaceStorageCache, without any stuff about references and locks</li>
 * <li>transaction style implemented via JBC barches, do with JTA (i.e. via exo's TransactionService + JBoss TM)</li>
 * <li>we need customized eviction policy (EvictionActionPolicy) for /$CHILD_NODES_LIST, /$CHILD_PROPS_LIST</li>
 * </ul>
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JBossCacheWorkspaceStorageCache.java 13869 2008-05-05 08:40:10Z pnedonosko $
 */
public class JBossCacheWorkspaceStorageCache implements WorkspaceStorageCache, Startable, Backupable
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JBossCacheWorkspaceStorageCache");

   private final boolean enabled;

   public static final String JBOSSCACHE_CONFIG = "jbosscache-configuration";

   public static final String JBOSSCACHE_EXPIRATION = "jbosscache-expiration-time";

   /**
    * Indicate whether the JBoss Cache instance used can be shared with other caches
    */
   public static final String JBOSSCACHE_SHAREABLE = "jbosscache-shareable";

   public static final Boolean JBOSSCACHE_SHAREABLE_DEFAULT = Boolean.FALSE;

   public static final long JBOSSCACHE_EXPIRATION_DEFAULT = 900000; // 15 minutes

   public static final String ITEMS = "$ITEMS".intern();

   public static final String NULL_ITEMS = "$NULL_ITEMS".intern();

   public static final String CHILD_NODES = "$CHILD_NODES".intern();

   public static final String CHILD_PROPS = "$CHILD_PROPS".intern();

   public static final String CHILD_NODES_LIST = "$CHILD_NODES_LIST".intern();

   public static final String CHILD_PROPS_LIST = "$CHILD_PROPS_LIST".intern();

   public static final String LOCKS = "$LOCKS".intern();

   public static final String REFERENCE = "$REFERENCE".intern();

   public static final String ITEM_DATA = "$data".intern();

   public static final String ITEM_ID = "$id".intern();

   public static final String ITEM_LIST = "$lists".intern();

   protected final BufferedJBossCache cache;

   protected final Fqn<String> itemsRoot;

   protected final Fqn<String> refRoot;

   protected final Fqn<String> childNodes;

   protected final Fqn<String> childProps;

   protected final Fqn<String> childNodesList;

   protected final Fqn<String> childPropsList;

   protected final Fqn<String> rootFqn;

   /**
    * Indicates whether the cache has already been initialized or not
    */
   protected volatile boolean initialized;

   /**
    * Node order comparator for getChildNodes().
    */
   class NodesOrderComparator<N extends NodeData> implements Comparator<NodeData>
   {

      /**
       * {@inheritDoc}
       */
      public int compare(NodeData n1, NodeData n2)
      {
         return n1.getOrderNumber() - n2.getOrderNumber();
      }
   }

   class ChildItemsIterator<T extends ItemData> implements Iterator<T>
   {

      final Iterator<Object> childs;

      final String parentId;

      final Fqn<String> root;

      T next;

      ChildItemsIterator(Fqn<String> root, String parentId)
      {
         this.parentId = parentId;
         this.root = root;

         Fqn<String> parentFqn = makeChildListFqn(root, parentId);
         Node<Serializable, Object> parent = cache.getNode(parentFqn);
         if (parent != null)
         {
            this.childs = cache.getChildrenNames(parentFqn).iterator();
            fetchNext();
         }
         else
         {
            this.childs = null;
            this.next = null;
         }
      }

      protected void fetchNext()
      {
         if (childs.hasNext())
         {
            // traverse to the first existing or the end of childs
            T n = null;
            do
            {
               String itemId = (String)cache.get(makeChildFqn(root, parentId, (String)childs.next()), ITEM_ID);
               if (itemId != null)
               {
                  n = (T)cache.get(makeItemFqn(itemId), ITEM_DATA);
               }
            }
            while (n == null && childs.hasNext());
            next = n;
         }
         else
         {
            next = null;
         }
      }

      public boolean hasNext()
      {
         return next != null;
      }

      public T next()
      {
         if (next == null)
         {
            throw new NoSuchElementException();
         }

         final T current = next;
         fetchNext();
         return current;
      }

      public void remove()
      {
         throw new IllegalArgumentException("Not implemented");
      }
   }

   class ChildNodesIterator<N extends NodeData> extends ChildItemsIterator<N>
   {
      ChildNodesIterator(String parentId)
      {
         super(childNodes, parentId);
      }

      @Override
      public N next()
      {
         return super.next();
      }
   }

   class ChildPropertiesIterator<P extends PropertyData> extends ChildItemsIterator<P>
   {

      ChildPropertiesIterator(String parentId)
      {
         super(childProps, parentId);
      }

      @Override
      public P next()
      {
         return super.next();
      }
   }

   /**
    * Cache constructor with eXo TransactionService support.
    * 
    * @param wsConfig WorkspaceEntry workspace config
    * @param transactionService TransactionService external transaction service
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public JBossCacheWorkspaceStorageCache(WorkspaceEntry wsConfig, TransactionService transactionService,
      ConfigurationManager cfm) throws RepositoryException, RepositoryConfigurationException
   {
      if (wsConfig.getCache() == null)
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }

      enabled = wsConfig.getCache().isEnabled();

      // create cache using custom factory
      ExoJBossCacheFactory<Serializable, Object> factory;

      if (transactionService != null)
      {
         factory = new ExoJBossCacheFactory<Serializable, Object>(cfm, transactionService.getTransactionManager());
      }
      else
      {
         factory = new ExoJBossCacheFactory<Serializable, Object>(cfm);
      }

      // create parent JBossCache instance
      Cache<Serializable, Object> parentCache = factory.createCache(wsConfig.getCache());
      // get all eviction configurations
      List<EvictionRegionConfig> evictionConfigurations =
         parentCache.getConfiguration().getEvictionConfig().getEvictionRegionConfigs();
      // append and default eviction configuration, since it is not present in region configurations
      evictionConfigurations.add(parentCache.getConfiguration().getEvictionConfig().getDefaultEvictionRegionConfig());

      boolean useExpiration = false;
      // looking over all eviction configurations till the end or till some expiration algorithm subclass not found.
      for (EvictionRegionConfig evictionRegionConfig : evictionConfigurations)
      {
         if (evictionRegionConfig.getEvictionAlgorithmConfig() instanceof ExpirationAlgorithmConfig)
         {
            // force set expiration key to default value in all Expiration configurations (if any)
            ((ExpirationAlgorithmConfig)evictionRegionConfig.getEvictionAlgorithmConfig())
               .setExpirationKeyName(ExpirationAlgorithmConfig.EXPIRATION_KEY);
            useExpiration = true;
         }
      }

      if (useExpiration)
      {
         LOG.info("Using BufferedJBossCache compatible with Expiration algorithm.");
      }

      this.rootFqn = Fqn.fromElements(wsConfig.getUniqueName());
      parentCache =
         ExoJBossCacheFactory.getUniqueInstance(CacheType.JCR_CACHE, rootFqn, parentCache, wsConfig.getCache()
            .getParameterBoolean(JBOSSCACHE_SHAREABLE, JBOSSCACHE_SHAREABLE_DEFAULT).booleanValue());

      // if expiration is used, set appropriate factory with with timeout set via configuration (or default one 15minutes)
      this.cache =
         new BufferedJBossCache(parentCache, useExpiration, wsConfig.getCache().getParameterTime(JBOSSCACHE_EXPIRATION,
            JBOSSCACHE_EXPIRATION_DEFAULT));

      this.itemsRoot = Fqn.fromRelativeElements(rootFqn, ITEMS);
      this.refRoot = Fqn.fromRelativeElements(rootFqn, REFERENCE);
      this.childNodes = Fqn.fromRelativeElements(rootFqn, CHILD_NODES);
      this.childProps = Fqn.fromRelativeElements(rootFqn, CHILD_PROPS);
      this.childNodesList = Fqn.fromRelativeElements(rootFqn, CHILD_NODES_LIST);
      this.childPropsList = Fqn.fromRelativeElements(rootFqn, CHILD_PROPS_LIST);

      if (cache.getConfiguration().getCacheMode() == CacheMode.LOCAL)
      {
         // The cache is local so we have no risk to get replication exception
         // during the initialization phase
         init();
      }
   }

   /**
    * Cache constructor with JBossCache JTA transaction support.
    * 
    * @param wsConfig WorkspaceEntry workspace config
    * @throws RepositoryException if error of initialization
    * @throws RepositoryConfigurationException if error of configuration
    */
   public JBossCacheWorkspaceStorageCache(WorkspaceEntry wsConfig, ConfigurationManager cfm)
      throws RepositoryException, RepositoryConfigurationException
   {
      this(wsConfig, null, cfm);
   }

   /**
    * Creates, starts and initialize the cache
    */
   private void init()
   {
      if (initialized)
      {
         // The cache has already been initialized
         return;
      }
      this.cache.create();
      this.cache.start();

      createResidentNode(childNodes);
      createResidentNode(refRoot);
      createResidentNode(childNodesList);
      createResidentNode(childProps);
      createResidentNode(childPropsList);
      createResidentNode(itemsRoot);

      this.initialized = true;
   }

   /**
    * {@inheritDoc} 
    */
   public void start()
   {
      // To prevent any timeout replication during the initialization of a JCR in a
      // cluster environment we create and start the cache only during the starting phase
      init();
   }

   /**
    * {@inheritDoc} 
    */
   public void stop()
   {
   }

   /**
    * Checks if node with give FQN not exists and creates resident node.
    * @param fqn
    */
   protected void createResidentNode(Fqn fqn)
   {
      Node<Serializable, Object> cacheRoot = cache.getRoot();
      if (!cacheRoot.hasChild(fqn))
      {
         cache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
         cacheRoot.addChild(fqn).setResident(true);
      }
      else
      {
         cache.getNode(fqn).setResident(true);
      }

   }

   protected static String readJBCConfig(final WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      if (wsConfig.getCache() != null)
      {
         return wsConfig.getCache().getParameterValue(JBOSSCACHE_CONFIG);
      }
      else
      {
         throw new RepositoryConfigurationException("Cache configuration not found");
      }
   }

   /**
    * Return TransactionManager used by JBossCache backing the JCR cache.
    * 
    * @return TransactionManager
    */
   public TransactionManager getTransactionManager()
   {
      return cache.getTransactionManager();
   }

   /**
    * {@inheritDoc}
    */
   public void put(ItemData item)
   {
      // There is different commit processing for NullNodeData and ordinary ItemData
      if (item instanceof NullItemData)
      {
         putNullItem((NullItemData)item);
         return;
      }

      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);

         if (item.isNode())
         {
            putNode((NodeData)item, ModifyChildOption.NOT_MODIFY);
         }
         else
         {
            putProperty((PropertyData)item, ModifyChildOption.NOT_MODIFY);
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void remove(ItemData item)
   {
      removeItem(item);
   }

   /**
    * {@inheritDoc}
    */
   public void onSaveItems(final ItemStateChangesLog itemStates)
   {
      //  if something happen we will rollback changes
      boolean rollback = true;
      try
      {
         cache.beginTransaction();
         for (ItemState state : itemStates.getAllStates())
         {
            if (state.isAdded())
            {
               if (state.isPersisted())
               {
                  putItem(state.getData());
               }
            }
            else if (state.isUpdated())
            {
               if (state.isPersisted())
               {
                  // There was a problem with removing a list of samename siblings in on transaction,
                  // so putItemInBufferedCache(..) and updateInBufferedCache(..) used instead put(..) and update (..) methods.
                  ItemData prevItem = putItemInBufferedCache(state.getData());

                  if (prevItem != null && state.isNode())
                  {
                     // nodes reordered, if previous is null it's InvalidItemState case
                     updateInBuffer((NodeData)state.getData(), (NodeData)prevItem);
                  }
               }
            }
            else if (state.isDeleted())
            {
               removeItem(state.getData());
            }
            else if (state.isRenamed())
            {
               putItem(state.getData());
            }
            else if (state.isPathChanged())
            {
               updateTreePath(state.getOldPath(), state.getData().getQPath(), null);
            }
            else if (state.isMixinChanged())
            {
               if (state.isPersisted())
               {
                  // update subtree ACLs
                  updateMixin((NodeData)state.getData());
               }
            }
         }
         cache.commitTransaction();
         rollback = false;
      }
      finally
      {
         if (rollback)
         {
            cache.rollbackTransaction();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildNodes(NodeData parent, List<NodeData> childs)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {

         if (!inTransaction)
         {
            cache.beginTransaction();
         }

         cache.setLocal(true);
         // remove previous all (to be sure about consistency)
         //TODO do we need remove node, while we can replace node?
         cache.removeNode(makeChildListFqn(childNodesList, parent.getIdentifier()));

         if (childs.size() > 0)
         {
            Set<Object> set = new HashSet<Object>();
            for (NodeData child : childs)
            {
               putNode(child, ModifyChildOption.NOT_MODIFY);
               set.add(child.getIdentifier());
            }
            cache.put(makeChildListFqn(childNodesList, parent.getIdentifier()), ITEM_LIST, set);
         }
         else
         {
            // cache fact of empty childs list
            cache.put(makeChildListFqn(childNodesList, parent.getIdentifier()), ITEM_LIST, new HashSet<Object>());
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildProperties(NodeData parent, List<PropertyData> childs)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);
         // remove previous all (to be sure about consistency)
         //TODO do we need remove node, while we can replace node?
         cache.removeNode(makeChildListFqn(childPropsList, parent.getIdentifier()));
         if (childs.size() > 0)
         {
            // add all new
            Set<Object> set = new HashSet<Object>();
            for (PropertyData child : childs)
            {
               putProperty(child, ModifyChildOption.NOT_MODIFY);
               set.add(child.getIdentifier());
            }
            cache.put(makeChildListFqn(childPropsList, parent.getIdentifier()), ITEM_LIST, set);

         }
         else
         {
            LOG.warn("Empty properties list cached " + (parent != null ? parent.getQPath().getAsString() : parent));
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addChildPropertiesList(NodeData parent, List<PropertyData> childProperties)
   {
      // TODO not implemented, will force read from DB
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(String parentId, QPathEntry name)
   {
      return get(parentId, name, ItemType.UNKNOWN);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(String parentId, QPathEntry name, ItemType itemType)
   {
      String itemId = null;

      if (itemType == ItemType.UNKNOWN)
      {
         // Try as node first.
         itemId = (String)cache.get(makeChildFqn(childNodes, parentId, name), ITEM_ID);

         if (itemId == null || itemId.equals(NullItemData.NULL_ID))
         {
            // node with such a name is not found or marked as not-exist, so check the properties
            String propId = (String)cache.get(makeChildFqn(childProps, parentId, name), ITEM_ID);
            if (propId != null)
            {
               itemId = propId;
            }
         }
      }
      else if (itemType == ItemType.NODE)
      {
         itemId = (String)cache.get(makeChildFqn(childNodes, parentId, name), ITEM_ID);
      }
      else
      {
         itemId = (String)cache.get(makeChildFqn(childProps, parentId, name), ITEM_ID);
      }

      if (itemId != null)
      {
         if (itemId.equals(NullItemData.NULL_ID))
         {
            if (itemType == ItemType.UNKNOWN || itemType == ItemType.NODE)
            {
               return new NullNodeData();
            }
            else
            {
               return new NullPropertyData();
            }
         }
         else
         {
            return get(itemId);
         }
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData get(String id)
   {
      return getFromCacheById(id);
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodes(final NodeData parent)
   {
      // empty Set<Object> marks that there is no child nodes
      // get list of children uuids
      final Set<Object> set =
         (Set<Object>)cache.get(makeChildListFqn(childNodesList, parent.getIdentifier()), ITEM_LIST);
      if (set != null)
      {
         final List<NodeData> childs = new ArrayList<NodeData>();

         for (Object child : set)
         {
            NodeData node = (NodeData)cache.get(makeItemFqn((String)child), ITEM_DATA);

            if (node == null || node instanceof NullItemData)
            {
               return null;
            }

            childs.add(node);
         }

         // order children by orderNumber, as HashSet returns children in other order
         Collections.sort(childs, new NodesOrderComparator<NodeData>());

         return childs;
      }
      else
      {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(NodeData parent)
   {
      // get list of children uuids
      final Set<Object> set =
         (Set<Object>)cache.get(makeChildListFqn(childNodesList, parent.getIdentifier()), ITEM_LIST);

      return set != null ? set.size() : -1;
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildProperties(NodeData parent)
   {
      return getChildProps(parent.getIdentifier(), true);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildProperties(NodeData parent)
   {
      return getChildProps(parent.getIdentifier(), false);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencedProperties(String identifier)
   {
      // get set of property uuids
      final Set<String> set = (Set<String>)cache.get(makeRefFqn(identifier), ITEM_LIST);
      if (set != null)
      {
         final List<PropertyData> props = new ArrayList<PropertyData>();

         for (String propId : set)
         {
            PropertyData prop = (PropertyData)cache.get(makeItemFqn(propId), ITEM_DATA);
            if (prop == null || prop instanceof NullItemData)
            {
               return null;
            }

            // add property as many times as has referenced values
            List<ValueData> lData = prop.getValues();
            for (int i = 0, length = lData.size(); i < length; i++)
            {
               ValueData vdata = lData.get(i);
               try
               {
                  if (new String(vdata.getAsByteArray(), Constants.DEFAULT_ENCODING).equals(identifier))
                  {
                     props.add(prop);
                  }
               }
               catch (IllegalStateException e)
               {
                  // Do not nothing.
               }
               catch (IOException e)
               {
                  // Do not nothing.
               }
            }
         }
         return props;
      }
      else
      {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addReferencedProperties(String identifier, List<PropertyData> refProperties)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }

         cache.setLocal(true);

         // remove previous all
         cache.removeNode(makeRefFqn(identifier));

         Set<Object> set = new HashSet<Object>();
         for (PropertyData prop : refProperties)
         {
            putProperty(prop, ModifyChildOption.NOT_MODIFY);
            set.add(prop.getIdentifier());
         }
         cache.put(makeRefFqn(identifier), ITEM_LIST, set);
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }
   }

   /**
    * Internal get child properties.
    *
    * @param parentId String
    * @param withValue boolean, if true only "full" Propeties can be returned
    * @return List of PropertyData
    */
   protected List<PropertyData> getChildProps(String parentId, boolean withValue)
   {
      // get set of property uuids
      final Set<Object> set = (Set<Object>)cache.get(makeChildListFqn(childPropsList, parentId), ITEM_LIST);
      if (set != null)
      {
         final List<PropertyData> childs = new ArrayList<PropertyData>();

         for (Object child : set)
         {
            PropertyData prop = (PropertyData)cache.get(makeItemFqn((String)child), ITEM_DATA);
            if (prop == null || prop instanceof NullItemData)
            {
               return null;
            }
            if (withValue && prop.getValues().size() <= 0)
            {
               // don't return list of empty-valued props (but listChildProperties() can)
               return null;
            }
            childs.add(prop);
         }
         return childs;
      }
      else
      {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getSize()
   {
      // Total number of JBC nodes in the cache - the total amount of resident nodes
      return numNodes(cache.getNode(rootFqn)) - 7;
   }

   /**
    * Evaluates the total amount of sub-nodes that the given node contains 
    */
   private static long numNodes(Node<Serializable, Object> n)
   {
      long count = 1;// for n
      if (n != null)
      {
         for (Node<Serializable, Object> child : n.getChildren())
         {
            count += numNodes(child);
         }
      }
      return count;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isEnabled()
   {
      return enabled && initialized;
   }

   // non-public members

   /**
    * Make Item absolute Fqn, i.e. /$ITEMS/itemID.
    *
    * @param itemId String
    * @return Fqn
    */
   protected Fqn<String> makeItemFqn(String itemId)
   {
      return Fqn.fromRelativeElements(itemsRoot, itemId);
   }

   /**
    * Make Item absolute Fqn, i.e. /$REF/itemID.
    *
    * @param itemId String
    * @return Fqn
    */
   protected Fqn<String> makeRefFqn(String itemId)
   {
      return Fqn.fromRelativeElements(refRoot, itemId);
   }

   /**
    * Make child Item absolute Fqn, i.e. /root/parentId/childName.
    *
    * @param root Fqn
    * @param parentId String
    * @param childName QPathEntry
    * @return Fqn
    */
   protected Fqn<String> makeChildFqn(Fqn<String> root, String parentId, QPathEntry childName)
   {
      return Fqn.fromRelativeElements(root, parentId, childName.getAsString(true));
   }

   /**
    * Make child Item absolute Fqn, i.e. /root/parentId/childName.
    *
    * @param root Fqn
    * @param parentId String
    * @param childName String
    * @return Fqn
    */
   protected Fqn<String> makeChildFqn(Fqn<String> root, String parentId, String childName)
   {
      return Fqn.fromRelativeElements(root, parentId, childName);
   }

   /**
    * Make child node parent absolute Fqn, i.e. /root/itemId.
    *
    * @param root Fqn
    * @param parentId String
    * @return Fqn
    */
   protected Fqn<String> makeChildListFqn(Fqn<String> root, String parentId)
   {
      return Fqn.fromRelativeElements(root, parentId);
   }

   /**
    * Gets item data from cache by item identifier.
    */
   protected ItemData getFromCacheById(String id)
   {
      // NullNodeData with id may be stored as ordinary NodeData or PropertyData
      return (ItemData)cache.get(makeItemFqn(id), ITEM_DATA);
   }

   /**
    * Internal put Item.
    *
    * @param item ItemData, new data to put in the cache
    * @return ItemData, previous data or null
    */
   protected ItemData putItem(ItemData item)
   {
      if (item.isNode())
      {
         return putNode((NodeData)item, ModifyChildOption.MODIFY);
      }
      else
      {
         return putProperty((PropertyData)item, ModifyChildOption.MODIFY);
      }
   }

   protected ItemData putItemInBufferedCache(ItemData item)
   {
      if (item.isNode())
      {
         return putNodeInBufferedCache((NodeData)item, ModifyChildOption.MODIFY);
      }
      else
      {
         return putProperty((PropertyData)item, ModifyChildOption.MODIFY);
      }

   }

   /**
    * Internal put Node.
    *
    * @param node, NodeData, new data to put in the cache
    * @return NodeData, previous data or null
    */
   protected ItemData putNode(NodeData node, ModifyChildOption modifyListsOfChild)
   {
      // if not a root node
      if (node.getParentIdentifier() != null)
      {
         // add in CHILD_NODES
         cache.put(makeChildFqn(childNodes, node.getParentIdentifier(), node.getQPath().getEntries()[node.getQPath()
            .getEntries().length - 1]), ITEM_ID, node.getIdentifier());

         if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
         {
            cache.addToList(makeChildListFqn(childNodesList, node.getParentIdentifier()), ITEM_LIST, node
               .getIdentifier(), modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }

      // add in ITEMS
      return (ItemData)cache.put(makeItemFqn(node.getIdentifier()), ITEM_DATA, node);
   }

   /**
    * Internal put NullNode.
    *
    * @param item, NullItemData, new data to put in the cache
    */
   protected void putNullItem(NullItemData item)
   {
      boolean inTransaction = cache.isTransactionActive();
      try
      {
         if (!inTransaction)
         {
            cache.beginTransaction();
         }
         cache.setLocal(true);

         if (!item.getIdentifier().equals(NullItemData.NULL_ID))
         {
            //put in $ITEMS
            cache.put(makeItemFqn(item.getIdentifier()), ITEM_DATA, item);
         }
         else if (item.getName() != null && item.getParentIdentifier() != null)
         {
            if (item.isNode())
            {
               // put in $CHILD_NODES
               cache.put(makeChildFqn(childNodes, item.getParentIdentifier(), item.getName()), ITEM_ID,
                  NullItemData.NULL_ID);
            }
            else
            {
               // put in $CHILD_PROPERTIES
               cache.put(makeChildFqn(childProps, item.getParentIdentifier(), item.getName()), ITEM_ID,
                  NullItemData.NULL_ID);
            }
         }
      }
      finally
      {
         cache.setLocal(false);
         if (!inTransaction)
         {
            dedicatedTxCommit();
         }
      }

   }

   protected ItemData putNodeInBufferedCache(NodeData node, ModifyChildOption modifyListsOfChild)
   {
      // if not a root node
      if (node.getParentIdentifier() != null)
      {
         // add in CHILD_NODES
         cache.put(makeChildFqn(childNodes, node.getParentIdentifier(), node.getQPath().getEntries()[node.getQPath()
            .getEntries().length - 1]), ITEM_ID, node.getIdentifier());

         if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
         {
            cache.addToList(makeChildListFqn(childNodesList, node.getParentIdentifier()), ITEM_LIST, node
               .getIdentifier(), modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }
      // add in ITEMS
      // NullNodeData must never be returned inside internal cache operations. 
      ItemData returnedData = (ItemData)cache.putInBuffer(makeItemFqn(node.getIdentifier()), ITEM_DATA, node);
      return (returnedData instanceof NullItemData) ? null : returnedData;
   }

   /**
    * Internal put Property.
    *
    * @param node, PropertyData, new data to put in the cache
    * @return PropertyData, previous data or null
    */
   protected PropertyData putProperty(PropertyData prop, ModifyChildOption modifyListsOfChild)
   {
      // add in CHILD_PROPS
      cache.put(makeChildFqn(childProps, prop.getParentIdentifier(), prop.getQPath().getEntries()[prop.getQPath()
         .getEntries().length - 1]), ITEM_ID, prop.getIdentifier());

      if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY)
      {
         cache.addToList(makeChildListFqn(childPropsList, prop.getParentIdentifier()), ITEM_LIST, prop.getIdentifier(),
            modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
      }

      // add referenced property
      if (modifyListsOfChild != ModifyChildOption.NOT_MODIFY && prop.getType() == PropertyType.REFERENCE)
      {
         List<ValueData> lData = prop.getValues();
         for (int i = 0, length = lData.size(); i < length; i++)
         {
            ValueData vdata = lData.get(i);
            String nodeIdentifier = null;
            try
            {
               nodeIdentifier = new String(vdata.getAsByteArray(), Constants.DEFAULT_ENCODING);
            }
            catch (IllegalStateException e)
            {
               // Do nothing. Never happens.
            }
            catch (IOException e)
            {
               // Do nothing. Never happens.
            }

            cache.addToList(makeRefFqn(nodeIdentifier), ITEM_LIST, prop.getIdentifier(),
               modifyListsOfChild == ModifyChildOption.FORCE_MODIFY);
         }
      }

      // add in ITEMS
      // NullItemData must never be returned inside internal cache operations. 
      ItemData returnedData = (ItemData)cache.put(makeItemFqn(prop.getIdentifier()), ITEM_DATA, prop);
      return (returnedData instanceof NullItemData) ? null : (PropertyData)returnedData;
   }

   protected void removeItem(ItemData item)
   {
      if (item.isNode())
      {
         if (item.getParentIdentifier() != null)
         {
            // if not a root node

            // remove from CHILD_NODES of parent
            cache.removeNode(makeChildFqn(childNodes, item.getParentIdentifier(), item.getQPath().getEntries()[item
               .getQPath().getEntries().length - 1]));

            // remove from CHILD_NODES_LIST of parent
            cache.removeFromList(makeChildListFqn(childNodesList, item.getParentIdentifier()), ITEM_LIST, item
               .getIdentifier());

            // remove from CHILD_NODES as parent
            cache.removeNode(makeChildListFqn(childNodes, item.getIdentifier()));

            // remove from CHILD_NODES_LIST as parent
            cache.removeNode(makeChildListFqn(childNodesList, item.getIdentifier()));

            // remove from CHILD_PROPS as parent
            cache.removeNode(makeChildListFqn(childProps, item.getIdentifier()));

            // remove from CHILD_PROPS_LIST as parent
            cache.removeNode(makeChildListFqn(childPropsList, item.getIdentifier()));
         }

         cache.removeNode(makeRefFqn(item.getIdentifier()));
      }
      else
      {
         // remove from CHILD_PROPS
         cache.removeNode(makeChildFqn(childProps, item.getParentIdentifier(), item.getQPath().getEntries()[item
            .getQPath().getEntries().length - 1]));

         // remove from CHILD_PROPS_LIST
         cache.removeFromList(makeChildListFqn(childPropsList, item.getParentIdentifier()), ITEM_LIST, item
            .getIdentifier());
      }
      // remove from ITEMS
      cache.removeNode(makeItemFqn(item.getIdentifier()));
   }

   /**
    * Update Node's mixin and ACL.
    *
    * @param node NodeData
    */
   protected void updateMixin(NodeData node)
   {
      NodeData prevData = (NodeData)cache.put(makeItemFqn(node.getIdentifier()), ITEM_DATA, node);
      // prevent update NullNodeData
      if (prevData != null && !(prevData instanceof NullItemData))
      {
         // do update ACL if needed
         if (prevData.getACL() == null || !prevData.getACL().equals(node.getACL()))
         {
            updateChildsACL(node.getIdentifier(), node.getACL());
         }
      }
      else if (LOG.isDebugEnabled())
      {
         LOG.debug("Previous NodeData not found for mixin update " + node.getQPath().getAsString());
      }
   }

   /**
    * Update Node hierachy in case of same-name siblings reorder.
    * Assumes the new (updated) nodes already putted in the cache. Previous name of updated nodes will be calculated
    * and that node will be deleted (if has same id as the new node). Childs paths will be updated to a new node path.
    *
    * @param node NodeData
    * @param prevNode NodeData
    */
   protected void update(final NodeData node, final NodeData prevNode)
   {
      // get previously cached NodeData and using its name remove child on the parent
      Fqn<String> prevFqn =
         makeChildFqn(childNodes, node.getParentIdentifier(), prevNode.getQPath().getEntries()[prevNode.getQPath()
            .getEntries().length - 1]);
      if (node.getIdentifier().equals(cache.get(prevFqn, ITEM_ID)))
      {
         // it's same-name siblings re-ordering, delete previous child
         if (!cache.removeNode(prevFqn) && LOG.isDebugEnabled())
         {
            LOG.debug("Node not extists as a child but update asked " + node.getQPath().getAsString());
         }
      }

      // update childs paths if index changed
      int nodeIndex = node.getQPath().getEntries()[node.getQPath().getEntries().length - 1].getIndex();
      int prevNodeIndex = prevNode.getQPath().getEntries()[prevNode.getQPath().getEntries().length - 1].getIndex();
      if (nodeIndex != prevNodeIndex)
      {
         updateTreePath(node.getIdentifier(), node.getQPath(), null); // don't change ACL, it's same parent
      }
   }

   /**
    * This method duplicate update method, except using getFromBuffer inside.
    * 
    * @param node NodeData
    * @param prevNode NodeData
    */
   protected void updateInBuffer(final NodeData node, final NodeData prevNode)
   {
      // I expect that NullNodeData will never update existing NodeData.
      // get previously cached NodeData and using its name remove child on the parent
      Fqn<String> prevFqn =
         makeChildFqn(childNodes, node.getParentIdentifier(), prevNode.getQPath().getEntries()[prevNode.getQPath()
            .getEntries().length - 1]);

      if (node.getIdentifier().equals(cache.getFromBuffer(prevFqn, ITEM_ID)))
      {
         // it's same-name siblings re-ordering, delete previous child
         if (!cache.removeNode(prevFqn) && LOG.isDebugEnabled())
         {
            LOG.debug("Node not extists as a child but update asked " + node.getQPath().getAsString());
         }
      }

      // node and prevNode are not NullNodeDatas
      // update childs paths if index changed
      int nodeIndex = node.getQPath().getEntries()[node.getQPath().getEntries().length - 1].getIndex();
      int prevNodeIndex = prevNode.getQPath().getEntries()[prevNode.getQPath().getEntries().length - 1].getIndex();
      if (nodeIndex != prevNodeIndex)
      {
         // its a samename reordering
         updateTreePath(prevNode.getQPath(), node.getQPath(), null); // don't change ACL, it's same parent
      }
   }

   /**
    * Check all items in cache - is it descendant of prevRootPath, and update path according newRootPath.
    * 
    * @param prevRootPath
    * @param newRootPath
    * @param acl
    */
   protected void updateTreePath(final QPath prevRootPath, final QPath newRootPath, final AccessControlList acl)
   {
      boolean inheritACL = acl != null;

      // check all ITEMS in cache 
      Node<Serializable, Object> items = cache.getNode(itemsRoot);
      Set<Object> childrenNames = items.getChildrenNames();
      Iterator<Object> namesIt = childrenNames.iterator();

      while (namesIt.hasNext())
      {
         String id = (String)namesIt.next();
         ItemData data = (ItemData)cache.get(makeItemFqn(id), ITEM_DATA);

         // check is this descendant of prevRootPath
         QPath nodeQPath = data.getQPath();
         if (nodeQPath != null && nodeQPath.isDescendantOf(prevRootPath))
         {
            //make relative path
            QPathEntry[] relativePath = null;
            try
            {
               relativePath = nodeQPath.getRelPath(nodeQPath.getDepth() - prevRootPath.getDepth());
            }
            catch (IllegalPathException e)
            {
               // Do nothing. Never happens.
            }

            // make new path - no matter  node or property
            QPath newPath = QPath.makeChildPath(newRootPath, relativePath);

            if (data.isNode())
            {
               // update node

               NodeData prevNode = (NodeData)data;

               TransientNodeData newNode =
                  new TransientNodeData(newPath, prevNode.getIdentifier(), prevNode.getPersistedVersion(), prevNode
                     .getPrimaryTypeName(), prevNode.getMixinTypeNames(), prevNode.getOrderNumber(), prevNode
                     .getParentIdentifier(), inheritACL ? acl : prevNode.getACL()); // TODO check ACL
               // update this node
               cache.put(makeItemFqn(newNode.getIdentifier()), ITEM_DATA, newNode);
            }
            else
            {
               //update property

               PropertyData prevProp = (PropertyData)data;

               if (inheritACL
                  && (prevProp.getQPath().getName().equals(Constants.EXO_PERMISSIONS) || prevProp.getQPath().getName()
                     .equals(Constants.EXO_OWNER)))
               {
                  inheritACL = false;
               }

               TransientPropertyData newProp =
                  new TransientPropertyData(newPath, prevProp.getIdentifier(), prevProp.getPersistedVersion(), prevProp
                     .getType(), prevProp.getParentIdentifier(), prevProp.isMultiValued(), prevProp.getValues());
               cache.put(makeItemFqn(newProp.getIdentifier()), ITEM_DATA, newProp);
            }
         }
      }
   }

   /**
    * Update Nodes tree with new path.
    *
    * @param parentId String - root node id of JCR subtree.
    * @param rootPath QPath
    * @param acl AccessControlList
    */
   protected void updateTreePath(final String parentId, final QPath rootPath, final AccessControlList acl)
   {
      boolean inheritACL = acl != null;

      // update properties
      for (Iterator<PropertyData> iter = new ChildPropertiesIterator<PropertyData>(parentId); iter.hasNext();)
      {
         PropertyData prevProp = iter.next();

         if (inheritACL
            && (prevProp.getQPath().getName().equals(Constants.EXO_PERMISSIONS) || prevProp.getQPath().getName()
               .equals(Constants.EXO_OWNER)))
         {
            inheritACL = false;
         }
         // recreate with new path for child Props only
         QPath newPath =
            QPath
               .makeChildPath(rootPath, prevProp.getQPath().getEntries()[prevProp.getQPath().getEntries().length - 1]);
         TransientPropertyData newProp =
            new TransientPropertyData(newPath, prevProp.getIdentifier(), prevProp.getPersistedVersion(), prevProp
               .getType(), prevProp.getParentIdentifier(), prevProp.isMultiValued(), prevProp.getValues());
         cache.put(makeItemFqn(newProp.getIdentifier()), ITEM_DATA, newProp);
      }

      // update child nodes
      for (Iterator<NodeData> iter = new ChildNodesIterator<NodeData>(parentId); iter.hasNext();)
      {
         NodeData prevNode = iter.next();
         // recreate with new path for child Nodes only
         QPath newPath =
            QPath
               .makeChildPath(rootPath, prevNode.getQPath().getEntries()[prevNode.getQPath().getEntries().length - 1]);
         TransientNodeData newNode =
            new TransientNodeData(newPath, prevNode.getIdentifier(), prevNode.getPersistedVersion(), prevNode
               .getPrimaryTypeName(), prevNode.getMixinTypeNames(), prevNode.getOrderNumber(), prevNode
               .getParentIdentifier(), inheritACL ? acl : prevNode.getACL()); // TODO check ACL
         // update this node
         cache.put(makeItemFqn(newNode.getIdentifier()), ITEM_DATA, newNode);
         // update childs recursive
         updateTreePath(newNode.getIdentifier(), newNode.getQPath(), inheritACL ? acl : null);
      }
   }

   /**
    * Update child Nodes ACLs.
    *
    * @param parentId String - root node id of JCR subtree.
    * @param acl AccessControlList
    */
   protected void updateChildsACL(final String parentId, final AccessControlList acl)
   {
      for (Iterator<NodeData> iter = new ChildNodesIterator<NodeData>(parentId); iter.hasNext();)
      {
         NodeData prevNode = iter.next();
         // is ACL changes on this node (i.e. ACL inheritance brokes)
         for (InternalQName mixin : prevNode.getMixinTypeNames())
         {
            if (mixin.equals(Constants.EXO_PRIVILEGEABLE) || mixin.equals(Constants.EXO_OWNEABLE))
            {
               continue;
            }
         }
         // recreate with new path for child Nodes only
         TransientNodeData newNode =
            new TransientNodeData(prevNode.getQPath(), prevNode.getIdentifier(), prevNode.getPersistedVersion(),
               prevNode.getPrimaryTypeName(), prevNode.getMixinTypeNames(), prevNode.getOrderNumber(), prevNode
                  .getParentIdentifier(), acl);
         // update this node
         cache.put(makeItemFqn(newNode.getIdentifier()), ITEM_DATA, newNode);
         // update childs recursive
         updateChildsACL(newNode.getIdentifier(), acl);
      }
   }

   public void beginTransaction()
   {
      cache.beginTransaction();
   }

   public void commitTransaction()
   {
      cache.commitTransaction();
   }

   public void rollbackTransaction()
   {
      cache.rollbackTransaction();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /**
    * <li>NOT_MODIFY - node(property) is not added to the parent's list (no persistent changes performed, cache used as cache)</li>
    * <li>MODIFY - node(property) is added to the parent's list if parent in the cache (new item is added to persistent, add to list if it is present)</li>
    * <li>FORCE_MODIFY - node(property) is added to the parent's list anyway (when list is read from DB, forcing write)</li>
    */
   private enum ModifyChildOption {
      NOT_MODIFY, MODIFY, FORCE_MODIFY
   }

   /**
    * Allows to commit the cache changes in a dedicated XA Tx in order to avoid potential
    * deadlocks
    */
   private void dedicatedTxCommit()
   {
      // Ensure that the commit is done in a dedicated tx to avoid deadlock due
      // to global XA Tx
      TransactionManager tm = getTransactionManager();
      Transaction tx = null;
      try
      {
         if (tm != null)
         {
            try
            {
               tx = tm.suspend();
            }
            catch (Exception e)
            {
               LOG.warn("Cannot suspend the current transaction", e);
            }
         }
         cache.commitTransaction();
      }
      finally
      {
         if (tx != null)
         {
            try
            {
               tm.resume(tx);
            }
            catch (Exception e)
            {
               LOG.warn("Cannot resume the current transaction", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void backup(File storageDir) throws BackupException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      cleanCache();
   }

   /**
    * {@inheritDoc}
    */
   public DataRestor getDataRestorer(File storageDir) throws BackupException
   {
      return new DataRestor()
      {
         /**
          * {@inheritDoc}
          */
         public void clean() throws BackupException
         {
            cleanCache();
         }

         /**
          * {@inheritDoc}
          */
         public void restore() throws BackupException
         {

         }

         /**
          * {@inheritDoc}
          */
         public void commit() throws BackupException
         {
         }

         /**
          * {@inheritDoc}
          */
         public void rollback() throws BackupException
         {
         }

         /**
          * {@inheritDoc}
          */
         public void close() throws BackupException
         {
         }
      };
   }

   /**
    * Clean all cache data.
    */
   private void cleanCache()
   {
      if (cache.getCacheStatus() == CacheStatus.STARTED)
      {
         cache.beginTransaction();

         cache.removeNode(itemsRoot);
         cache.removeNode(refRoot);
         cache.removeNode(childNodes);
         cache.removeNode(childProps);
         cache.removeNode(childNodesList);
         cache.removeNode(childPropsList);

         cache.commitTransaction();

         createResidentNode(childNodes);
         createResidentNode(refRoot);
         createResidentNode(childNodesList);
         createResidentNode(childProps);
         createResidentNode(childPropsList);
         createResidentNode(itemsRoot);
      }
   }
}
