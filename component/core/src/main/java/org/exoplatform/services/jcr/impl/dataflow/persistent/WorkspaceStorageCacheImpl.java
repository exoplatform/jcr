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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.cache.CacheListener;
import org.exoplatform.services.cache.CacheListenerContext;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * Info: This cache implementation store item data and childs lists of item data. And it implements
 * OBJECTS cache - i.e. returns same java object that was cached before. Same item data or list of
 * childs will be returned from getXXX() calls. [PN] 13.04.06
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov </a>
 * @version $Id: WorkspaceStorageCacheImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */
@Deprecated
// TODO remove it in JCR 1.12
public class WorkspaceStorageCacheImpl
   implements WorkspaceStorageCache
{

   static public int MAX_CACHE_SIZE = 200;

   static public long MAX_CACHE_LIVETIME = 600; // in

   // sec

   protected static Log log = ExoLogger.getLogger("jcr.WorkspaceStorageCacheImpl");

   protected Log info = ExoLogger.getLogger("jcr.WorkspaceStorageCacheImplINFO");

   private final ExoCache cache;

   private final WeakHashMap<String, List<NodeData>> nodesCache;

   private final WeakHashMap<String, List<PropertyData>> propertiesCache;

   private final String name;

   private boolean enabled;

   private Timer debugInformer;

   public WorkspaceStorageCacheImpl(CacheService cacheService, WorkspaceEntry wsConfig) throws Exception
   {

      this.name = "jcr." + wsConfig.getUniqueName();

      log
               .warn("This cache implementaion (WorkspaceStorageCacheImpl) is deprecated and will be removed in future versions of eXo JCR.");

      this.cache = cacheService.getCacheInstance(name);

      CacheEntry cacheConfig = wsConfig.getCache();
      if (cacheConfig != null)
      {
         enabled = cacheConfig.isEnabled();

         int maxSize;
         try
         {
            maxSize = cacheConfig.getParameterInteger("max-size");
         }
         catch (RepositoryConfigurationException e)
         {
            maxSize = cacheConfig.getParameterInteger("maxSize");
         }
         cache.setMaxSize(maxSize);

         nodesCache = new WeakHashMap<String, List<NodeData>>(maxSize);
         propertiesCache = new WeakHashMap<String, List<PropertyData>>(maxSize);

         long liveTime;
         try
         {
            liveTime = cacheConfig.getParameterTime("live-time"); // apply in milliseconds
         }
         catch (RepositoryConfigurationException e)
         {
            liveTime = cacheConfig.getParameterTime("liveTime");
         }
         cache.setLiveTime(liveTime);
      }
      else
      {
         cache.setMaxSize(MAX_CACHE_SIZE);
         cache.setLiveTime(MAX_CACHE_LIVETIME);
         nodesCache = new WeakHashMap<String, List<NodeData>>();
         propertiesCache = new WeakHashMap<String, List<PropertyData>>();
         enabled = true;
      }

      cache.addCacheListener(new ExpiredListener());

      if (info.isDebugEnabled())
      {
         debugInformer = new Timer(this.name);
         TimerTask informerTask = new TimerTask()
         {
            public void run()
            {
               try
               {
                  int childNodes = 0;
                  try
                  {
                     for (Map.Entry<String, List<NodeData>> ne : nodesCache.entrySet())
                     {
                        childNodes += ne.getValue().size();
                     }
                  }
                  catch (ConcurrentModificationException e)
                  {
                     childNodes = -1;
                  }
                  int childProperties = 0;
                  try
                  {
                     for (Map.Entry<String, List<PropertyData>> pe : propertiesCache.entrySet())
                     {
                        childProperties += pe.getValue().size();
                     }
                  }
                  catch (ConcurrentModificationException e)
                  {
                     childProperties = -1;
                  }
                  info.info("C " + cache.getCacheSize() + ", CN " + nodesCache.size() + "/"
                           + (childNodes < 0 ? "?" : childNodes) + ", CP " + propertiesCache.size() + "/"
                           + (childProperties < 0 ? "?" : childProperties));
               }
               catch (Throwable e)
               {
                  info.error("Debug informer task error " + e);
               }
            }
         };

         Calendar firstTime = Calendar.getInstance();
         firstTime.add(Calendar.SECOND, 5); // begin task after 30 second
         debugInformer.schedule(informerTask, firstTime.getTime(), 10 * 1000); // report each minute
      }
   }

   public long getSize()
   {
      return cache.getCacheSize();
   }

   /**
    * @param identifier
    *          a Identifier of item cached
    */
   public ItemData get(final String identifier)
   {
      if (!enabled)
         return null;

      try
      {
         return getItem(identifier);
      }
      catch (Exception e)
      {
         log.error("GET operation fails. Item ID=" + identifier + ". Error " + e + ". NULL returned.", e);
         return null;
      }
   }

   /**
    * @return
    * @throws Exception
    */
   public ItemData get(String parentId, QPathEntry name)
   {
      if (!enabled)
         return null;

      try
      {
         return getItem(parentId, name);
      }
      catch (Exception e)
      {
         log.error("GET operation fails. Parent ID=" + parentId + " name " + (name != null ? name.getAsString() : name)
                  + ". Error " + e + ". NULL returned.", e);
         return null;
      }
   }

   /**
    * Called by read operations
    * 
    * @param data
    */
   public void put(final ItemData data)
   {

      try
      {
         if (enabled && data != null)
         {

            putItem(data);

            // add child item data to list of childs of the parent
            if (data.isNode())
            {
               // add child node
               List<NodeData> cachedParentChilds = nodesCache.get(data.getParentIdentifier());
               if (cachedParentChilds != null)
               {
                  // Playing for orderable work
                  NodeData nodeData = (NodeData) data;
                  int orderNumber = nodeData.getOrderNumber();

                  synchronized (cachedParentChilds)
                  {
                     int index = cachedParentChilds.indexOf(nodeData);
                     if (index >= 0)
                     {

                        if (orderNumber != cachedParentChilds.get(index).getOrderNumber())
                        {
                           // replace and reorder
                           List<NodeData> newChilds = new ArrayList<NodeData>(cachedParentChilds.size());
                           for (int ci = 0; ci < cachedParentChilds.size(); ci++)
                           {
                              if (index == ci)
                                 newChilds.add(nodeData); // place in new position
                              else
                                 newChilds.add(cachedParentChilds.get(ci)); // copy
                           }

                           nodesCache.put(data.getParentIdentifier(), newChilds); // cache new list
                           if (log.isDebugEnabled())
                              log.debug(name + ", put()    update child node  " + nodeData.getIdentifier()
                                       + "  order #" + orderNumber);
                        }
                        else
                        {

                           cachedParentChilds.set(index, nodeData); // replace at current position
                           if (log.isDebugEnabled())
                              log.debug(name + ", put()    update child node  " + nodeData.getIdentifier()
                                       + "  at index #" + index);
                        }

                     }
                     else
                     {

                        // add to the end
                        List<NodeData> newChilds = new ArrayList<NodeData>(cachedParentChilds.size() + 1);
                        for (int ci = 0; ci < cachedParentChilds.size(); ci++)
                           newChilds.add(cachedParentChilds.get(ci));

                        newChilds.add(nodeData); // add

                        nodesCache.put(data.getParentIdentifier(), newChilds); // cache new list
                        if (log.isDebugEnabled())
                           log.debug(name + ", put()    add child node  " + nodeData.getIdentifier());
                     }
                  }
               }
            }
            else
            {
               // add child property
               final List<PropertyData> cachedParentChilds = propertiesCache.get(data.getParentIdentifier());
               if (cachedParentChilds != null)
               {
                  synchronized (cachedParentChilds)
                  {
                     int index = cachedParentChilds.indexOf(data);
                     if (index >= 0)
                     {

                        cachedParentChilds.set(index, (PropertyData) data); // replace at current position
                        if (log.isDebugEnabled())
                           log.debug(name + ", put()    update child property  " + data.getIdentifier()
                                    + "  at index #" + index);

                     }
                     else
                     {

                        List<PropertyData> newChilds = new ArrayList<PropertyData>(cachedParentChilds.size() + 1);
                        for (int ci = 0; ci < cachedParentChilds.size(); ci++)
                           newChilds.add(cachedParentChilds.get(ci));

                        newChilds.add((PropertyData) data);

                        propertiesCache.put(data.getParentIdentifier(), newChilds); // cache new list
                        if (log.isDebugEnabled())
                           log.debug(name + ", put()    add child property  " + data.getIdentifier());
                     }
                  }
               }
            }
         }
      }
      catch (Exception e)
      {
         log.error(name + ", Error put item data in cache: "
                  + (data != null ? data.getQPath().getAsString() : "[null]"), e);
      }
   }

   public void addChildProperties(final NodeData parentData, final List<PropertyData> childItems)
   {
      if (enabled && parentData != null && childItems != null)
      {

         String logInfo = null;
         if (log.isDebugEnabled())
         {
            logInfo =
                     "parent:   " + parentData.getQPath().getAsString() + "    " + parentData.getIdentifier() + " "
                              + childItems.size();
            log.debug(name + ", addChildProperties() >>> " + logInfo);
         }

         final String parentIdentifier = parentData.getIdentifier();
         String operName = ""; // for debug/trace only
         try
         {
            // remove parent (no childs)
            operName = "removing parent";
            removeDeep(parentData, false);

            operName = "caching parent";
            putItem(parentData); // put parent in cache

            // [PN] 17.01.07 need to sync as the list can be accessed concurrently till the end of
            // addChildProperties()
            List<PropertyData> cp = childItems;
            synchronized (cp)
            {

               synchronized (propertiesCache)
               {
                  // removing prev list of child properties from cache C
                  operName = "removing child properties";
                  removeChildProperties(parentIdentifier);

                  operName = "caching child properties list";
                  propertiesCache.put(parentIdentifier, cp); // put childs in cache CP
               }

               operName = "caching child properties";
               putItems(cp); // put childs in cache C
            }
         }
         catch (Exception e)
         {
            log.error(name + ", Error in addChildProperties() " + operName + ": parent "
                     + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         if (log.isDebugEnabled())
            log.debug(name + ", addChildProperties() <<< " + logInfo);
      }
   }

   /**
    * Empty method for this cache impl.
    */
   public void addChildPropertiesList(NodeData parentData, List<PropertyData> childItems)
   {
   }

   public void addChildNodes(final NodeData parentData, final List<NodeData> childItems)
   {
      if (enabled && parentData != null && childItems != null)
      {

         String logInfo = null;
         if (log.isDebugEnabled())
         {
            logInfo =
                     "parent:   " + parentData.getQPath().getAsString() + "    " + parentData.getIdentifier() + " "
                              + childItems.size();
            log.debug(name + ", addChildNodes() >>> " + logInfo);
         }

         final String parentIdentifier = parentData.getIdentifier();
         String operName = ""; // for debug/trace only
         try
         {
            // remove parent (no childs)
            operName = "removing parent";
            removeDeep(parentData, false);

            operName = "caching parent";
            putItem(parentData); // put parent in cache

            // [PN] 17.01.07 need to sync as the list can be accessed concurrently till the end of
            // addChildNodes()
            List<NodeData> cn = childItems;
            synchronized (cn)
            {

               synchronized (nodesCache)
               {
                  // removing prev list of child nodes from cache C
                  operName = "removing child nodes";
                  final List<NodeData> removedChildNodes = removeChildNodes(parentIdentifier, false);

                  if (removedChildNodes != null && removedChildNodes.size() > 0)
                  {
                     operName = "search for stale child nodes not contains in the new list of childs";
                     final List<NodeData> forRemove = new ArrayList<NodeData>();
                     for (NodeData removedChildNode : removedChildNodes)
                     {
                        // used Object.equals(Object o), e.g. by UUID of nodes
                        if (!cn.contains(removedChildNode))
                        {
                           // this child node has been removed from the list
                           // we should remve it recursive in C, CN, CP
                           forRemove.add(removedChildNode);
                        }
                     }

                     if (forRemove.size() > 0)
                     {
                        operName = "removing stale child nodes not contains in the new list of childs";
                        // do remove of removed child nodes recursive in C, CN, CP
                        // we need here locks on cache, nodesCache, propertiesCache
                        synchronized (propertiesCache)
                        {
                           for (NodeData removedChildNode : forRemove)
                           {
                              removeDeep(removedChildNode, true);
                           }
                        }
                     }
                  }

                  operName = "caching child nodes list";
                  nodesCache.put(parentIdentifier, cn); // put childs in cache CN
               }

               operName = "caching child nodes";
               putItems(cn); // put childs in cache C
            }
         }
         catch (Exception e)
         {
            log.error(name + ", Error in addChildNodes() " + operName + ": parent "
                     + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
         if (log.isDebugEnabled())
            log.debug(name + ", addChildNodes() <<< " + logInfo);
      }
   }

   protected void putItem(final ItemData data) throws Exception
   {

      if (log.isDebugEnabled())
         log.debug(name + ", putItem()    " + data.getQPath().getAsString() + "    " + data.getIdentifier() + "  --  "
                  + data);

      cache.put(data.getIdentifier(), data);
      cache.put(new CacheQPath(data.getParentIdentifier(), data.getQPath()), data);
   }

   protected ItemData getItem(final String identifier) throws Exception
   {

      final ItemData c = (ItemData) cache.get(identifier);
      if (log.isDebugEnabled())
         log.debug(name + ", getItem() " + identifier + " --> "
                  + (c != null ? c.getQPath().getAsString() + " parent:" + c.getParentIdentifier() : "[null]"));
      return c;
   }

   /**
    * @param key
    *          a InternalQPath path of item cached
    */
   protected ItemData getItem(final String parentUuid, final QPathEntry qname) throws Exception
   {

      // ask direct cache (C)
      final ItemData c = (ItemData) cache.get(new CacheQPath(parentUuid, qname));
      if (log.isDebugEnabled())
         log.debug(name + ", getItem() " + (c != null ? c.getQPath().getAsString() : "[null]") + " --> "
                  + (c != null ? c.getIdentifier() + " parent:" + c.getParentIdentifier() : "[null]"));
      return c;
   }

   protected void putItems(final List<? extends ItemData> itemsList) throws Exception
   {
      final Map<Serializable, Object> subMap = new LinkedHashMap<Serializable, Object>();
      for (ItemData item : itemsList)
      {
         if (log.isDebugEnabled())
            log.debug(name + ", putItems()    " + item.getQPath().getAsString() + "    " + item.getIdentifier()
                     + "  --  " + item);

         subMap.put(item.getIdentifier(), item);
         subMap.put(new CacheQPath(item.getParentIdentifier(), item.getQPath()), item);
      }
      cache.putMap(subMap);
   }

   public List<NodeData> getChildNodes(final NodeData parentData)
   {

      if (!enabled)
         return null;

      try
      {
         final List<NodeData> cn = nodesCache.get(parentData.getIdentifier());
         if (log.isDebugEnabled())
         {
            log.debug(name + ", getChildNodes() " + parentData.getQPath().getAsString() + " "
                     + parentData.getIdentifier());
            final StringBuffer blog = new StringBuffer();
            if (cn != null)
            {
               blog.append("\n");
               for (NodeData nd : cn)
               {
                  blog.append("\t\t" + nd.getQPath().getAsString() + " " + nd.getIdentifier() + "\n");
               }
               log.debug("\t-->" + blog.toString());
            }
            else
            {
               log.debug("\t--> null");
            }
         }
         return cn;
      }
      catch (Exception e)
      {
         log.error(name + ", Error in getChildNodes() parentData: "
                  + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
      }
      return null; // nothing cached
   }

   public List<PropertyData> getChildProperties(final NodeData parentData)
   {
      if (enabled && parentData != null)
      {
         try
         {
            // we assume that parent cached too
            final List<PropertyData> cp = propertiesCache.get(parentData.getIdentifier());

            if (log.isDebugEnabled())
            {
               log.debug(name + ", getChildProperties() " + parentData.getQPath().getAsString() + " "
                        + parentData.getIdentifier());
               final StringBuffer blog = new StringBuffer();
               if (cp != null)
               {
                  blog.append("\n");
                  for (PropertyData pd : cp)
                  {
                     blog.append("\t\t" + pd.getQPath().getAsString() + " " + pd.getIdentifier() + "\n");
                  }
                  log.debug("\t--> " + blog.toString());
               }
               else
               {
                  log.debug("\t--> null");
               }
            }

            return cp;
         }
         catch (Exception e)
         {
            log.error(name + ", Error in getChildProperties() parentData: "
                     + (parentData != null ? parentData.getQPath().getAsString() : "[null]"), e);
         }
      }

      return null; // nothing cached
   }

   /**
    * Wrapps {@link getChildProperties} in this cache impl.
    */
   public List<PropertyData> listChildProperties(final NodeData parentData)
   {
      return getChildProperties(parentData);
   }

   public boolean isEnabled()
   {
      return enabled;
   }

   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }

   public void setMaxSize(int maxSize)
   {
      this.cache.setMaxSize(maxSize);
   }

   public void setLiveTime(long liveTime)
   {
      this.cache.setLiveTime(liveTime);
   }

   /**
    * Unload given property (outdated) from cache to be cached again. Add/update/remove mixins
    * usecase.
    */
   private void unloadProperty(PropertyData property) throws Exception
   {
      try
      {
         final ItemData parent = (ItemData) cache.get(property.getParentIdentifier());
         if (parent != null)
            // remove parent only (property like mixins lives inside the node data)
            removeDeep(parent, false);
      }
      catch (Exception e)
      {
         log.error("unloadProperty operation (remove of parent) fails. Parent ID=" + property.getParentIdentifier()
                  + ". Error " + e, e);
      }
      finally
      {
         remove(property); // remove property
      }
   }

   // --------------------- ItemsPersistenceListener --------------

   private boolean needReload(ItemData data)
   {
      // [PN] Add ORed property NAMEs here to unload a parent on the save action
      return data.getQPath().getName().equals(Constants.JCR_MIXINTYPES)
               || data.getQPath().getName().equals(Constants.EXO_PERMISSIONS)
               || data.getQPath().getName().equals(Constants.EXO_OWNER);
   }

   public synchronized void onSaveItems(final ItemStateChangesLog changesLog)
   {

      if (!enabled)
         return;

      for (Iterator<ItemState> iter = changesLog.getAllStates().iterator(); iter.hasNext();)
      {
         ItemState state = iter.next();
         ItemData data = state.getData();
         if (log.isDebugEnabled())
            log.debug(name + ", onSaveItems() " + ItemState.nameFromValue(state.getState()) + " "
                     + data.getQPath().getAsString() + " " + data.getIdentifier() + " parent:"
                     + data.getParentIdentifier());

         try
         {
            if (state.isAdded())
            {
               if (!data.isNode() && needReload(data))
                  unloadProperty((PropertyData) data);
               put(data);
            }
            else if (state.isUpdated())
            {
               if (data.isNode())
                  // orderable nodes will be removed, to be loaded back from the persistence
                  unloadNode((NodeData) data);
               else if (needReload(data))
                  unloadProperty((PropertyData) data); // remove mixins
               put(data);
            }
            else if (state.isDeleted())
            {
               if (!data.isNode() && needReload(data))
                  unloadProperty((PropertyData) data);
               else
                  remove(data);
            }
            else if (state.isRenamed())
            {
               if (data.isNode())
                  unloadNode((NodeData) data);
               else if (needReload(data))
                  unloadProperty((PropertyData) data); // remove mixins
               put(data);
            }
         }
         catch (Exception e)
         {
            log.error(name + ", Error process onSaveItems action for item data: "
                     + (data != null ? data.getQPath().getAsString() : "[null]"), e);
         }
      }
   }

   /**
    * Mark the item to be reloaded from the persistence.
    * 
    * The case made be removing all descendats of the item parent. Same as remove(item) but not
    * delete
    */
   private void unloadNode(final NodeData item) throws Exception
   {
      final ItemData parent = (ItemData) cache.get(item.getParentIdentifier());
      // NOTE. it's possible that we have to not use the fact of caching and remove anyway by
      // data.getParentIdentifier()
      if (parent != null)
      {
         // remove the item parent (no whole tree)
         // removeDeep(parentData, false);

         if (item.isNode())
         {
            // remove child nodes of the item parent recursive
            synchronized (nodesCache)
            {
               synchronized (propertiesCache)
               {
                  if (removeChildNodes(parent.getIdentifier(), true) == null)
                  {
                     // if no childs of the item (node) parent were cached - remove renamed node directly
                     removeDeep(item, true);
                  }
               }
            }

            // Traverse whole cache (C), select each descendant of the item and remove it from C. The
            // costly operation.
            removeSuccessors((NodeData) item);
         }
         else
         {
            // remove child properties of the item parent
            synchronized (propertiesCache)
            {
               removeChildProperties(parent.getIdentifier());
               // propertiesCache.remove(parentData.getIdentifier());
            }
         }
      }
   }

   // ---------------------------------------------------

   /**
    * Called by delete
    * 
    * @param data
    */
   public void remove(final ItemData data)
   {
      if (!enabled)
         return;

      try
      {
         if (log.isDebugEnabled())
            log.debug(name + ", remove() " + data.getQPath().getAsString() + " " + data.getIdentifier());

         // do actual deep remove
         if (data.isNode())
         {
            synchronized (propertiesCache)
            {
               synchronized (nodesCache)
               {
                  removeDeep(data, true);
               }
            }
            removeSuccessors((NodeData) data);
         }
         else
         {
            // [PN] 03.12.06 Fixed to forceDeep=true and synchronized block
            synchronized (propertiesCache)
            {
               removeDeep(data, true);
            }
         }
      }
      catch (Exception e)
      {
         log.error(name + ", Error remove item data from cache: "
                  + (data != null ? data.getQPath().getAsString() : "[null]"), e);
      }
   }

   /**
    * Deep remove of an item in all caches (C, CN, CP). Outside must be sinchronyzed by cache(C). If
    * forceDeep=true then it must be sinchronyzed by cache(CN,CP) too.
    * 
    * @param item
    *          - ItemData of item removing
    * @param forceDeep
    *          - if true then childs will be removed too, item's parent childs (nodes or properties)
    *          will be removed also. if false - no actual deep remove will be done, the item only and
    *          theirs 'phantom by identifier' if exists.
    */
   protected ItemData removeDeep(final ItemData item, final boolean forceDeep) throws Exception
   {
      if (log.isDebugEnabled())
         log.debug(name + ", removeDeep(" + forceDeep + ") >>> item " + item.getQPath().getAsString() + " "
                  + item.getIdentifier());

      if (forceDeep)
      {
         removeRelations(item);
      }

      cache.remove(item.getIdentifier());
      final ItemData itemData = (ItemData) cache.remove(new CacheQPath(item.getParentIdentifier(), item.getQPath()));
      if (itemData != null && !itemData.getIdentifier().equals(item.getIdentifier()))
      {
         // same path but diff identifier node... phantom
         removeDeep(itemData, forceDeep);
      }
      if (log.isDebugEnabled())
         log.debug(name + ", removeDeep(" + forceDeep + ") <<< item " + item.getQPath().getAsString() + " "
                  + item.getIdentifier());
      return itemData;
   }

   /**
    * Remove item relations in the cache(C,CN,CP) by Identifier in case of item remove from persisten
    * storage. Relations for a node it's a child nodes, properties and item in node's parent childs
    * list. Relations for a property it's a item in node's parent childs list.
    */
   protected void removeRelations(final ItemData item)
   {
      // removing child item data from list of childs of the parent
      try
      {
         if (item.isNode())
         {
            // removing childs of the node
            if (removeChildNodes(item.getIdentifier(), true) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", removeRelations() removeChildNodes() " + item.getIdentifier());
            }
            if (removeChildProperties(item.getIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", removeRelations() removeChildProperties() " + item.getIdentifier());
            }

            // removing child from the node's parent child nodes list
            if (removeChildNode(item.getParentIdentifier(), item.getIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", removeRelations() removeChildNode(parentIdentifier, childIdentifier) "
                           + item.getParentIdentifier() + " " + item.getIdentifier());
            }
         }
         else
         {
            // removing child from the node's parent properties list
            if (removeChildProperty(item.getParentIdentifier(), item.getIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", removeRelations() removeChildProperty(parentIdentifier, childIdentifier) "
                           + item.getParentIdentifier() + " " + item.getIdentifier());
            }
         }
      }
      catch (Exception e)
      {
         log.error(name + ", Error in removeRelations() item: "
                  + (item != null ? item.getQPath().getAsString() : "[null]"), e);
      }
   }

   protected List<NodeData> removeChildNodes(final String parentIdentifier, final boolean forceDeep) throws Exception
   {
      final List<NodeData> childNodes = nodesCache.remove(parentIdentifier);
      if (childNodes != null)
      {
         // we have child nodes
         synchronized (childNodes)
         { // [PN] 17.01.07
            for (NodeData cn : childNodes)
            {
               removeDeep(cn, forceDeep);
            }
         }
      }
      return childNodes;
   }

   protected List<PropertyData> removeChildProperties(final String parentIdentifier) throws Exception
   {
      final List<PropertyData> childProperties = propertiesCache.remove(parentIdentifier);
      if (childProperties != null)
      {
         // we have child properties
         synchronized (childProperties)
         { // [PN] 17.01.07
            for (PropertyData cp : childProperties)
            {
               removeDeep(cp, false);
            }
         }
      }
      return childProperties;
   }

   protected PropertyData removeChildProperty(final String parentIdentifier, final String childIdentifier)
            throws Exception
   {
      final List<PropertyData> childProperties = propertiesCache.get(parentIdentifier);
      if (childProperties != null)
      {
         synchronized (childProperties)
         { // [PN] 17.01.07
            for (Iterator<PropertyData> i = childProperties.iterator(); i.hasNext();)
            {
               PropertyData cn = i.next();
               if (cn.getIdentifier().equals(childIdentifier))
               {
                  // removedIndex = i;
                  i.remove();
                  break;
               }
            }
         }
      }
      return null;
   }

   protected NodeData removeChildNode(final String parentIdentifier, final String childIdentifier) throws Exception
   {
      final List<NodeData> childNodes = nodesCache.get(parentIdentifier);
      if (childNodes != null)
      {
         synchronized (childNodes)
         { // [PN] 17.01.07
            for (Iterator<NodeData> i = childNodes.iterator(); i.hasNext();)
            {
               NodeData cn = i.next();
               if (cn.getIdentifier().equals(childIdentifier))
               {
                  i.remove();
                  break;
               }
            }
         }
      }
      return null;
   }

   /**
    * Remove item by path. Path is a string, if an item's path equals given the item will be removed
    */
   protected void removeItem(final String itemPath)
   {
      final ItemRemoveSelector remover = new ItemRemoveSelector(itemPath);
      try
      {
         cache.select(remover);
      }
      catch (Exception e)
      {
         log.error(name + ", removeSuccessors() " + itemPath, e);
      }
   }

   // @Deprecated
   // protected void removeSuccessors(final String parentPath) {
   // final ByPathRemoveSelector remover = new ByPathRemoveSelector(parentPath);
   // try {
   // cache.select(remover);
   // } catch (Exception e) {
   // log.error(name + ", removeSuccessors() " + parentPath, e);
   // }
   // }

   /**
    * Remove successors by parent path. Path is a string, if an item's path starts with it then the
    * item will be removed
    */
   protected void removeSuccessors(final NodeData parent)
   {
      final ByParentRemoveSelector remover = new ByParentRemoveSelector(parent);
      try
      {
         cache.select(remover);
      }
      catch (Exception e)
      {
         log.error(name + ", removeSuccessors() " + parent.getQPath().getAsString(), e);
      }
   }

   // ------ internal selectors ------

   protected class ByParentRemoveSelector
      implements CachedObjectSelector
   {

      private final NodeData parent;

      protected ByParentRemoveSelector(NodeData parent)
      {
         this.parent = parent;
      }

      public void onSelect(ExoCache exoCache, Serializable key, ObjectCacheInfo value) throws Exception
      {
         try
         {
            ItemData removed = (ItemData) exoCache.remove(key);
            if (removed != null && key instanceof CacheQPath)
               exoCache.remove(removed.getIdentifier());
         }
         catch (Exception e)
         {
            log.error(name + ", ByParentRemoveSelector.onSelect() " + parent.getIdentifier() + ": "
                     + parent.getQPath().getAsString() + " key: " + key, e);
         }
      }

      public boolean select(Serializable key, ObjectCacheInfo value)
      {
         if (key instanceof CacheQPath)
         {
            // path
            CacheQPath path = (CacheQPath) key;
            if (path.getQPath().isDescendantOf(parent.getQPath()))
               return true;
         }
         else
         {
            // id
            String id = (String) key;
            if (id.equals(parent.getIdentifier()))
               return true;
         }

         return false;
      }
   }

   @Deprecated
   protected class ByPathRemoveSelector
      implements CachedObjectSelector
   {

      private final String parentPath;

      protected ByPathRemoveSelector(String parentPath)
      {
         this.parentPath = parentPath;
      }

      public void onSelect(ExoCache exoCache, Serializable key, ObjectCacheInfo value) throws Exception
      {
         try
         {
            ItemData removed = (ItemData) exoCache.remove(key);
            if (removed != null)
            {
               exoCache.remove(removed.getIdentifier());
            }
         }
         catch (Exception e)
         {
            log.error(name + ", ByPathRemoveSelector.onSelect() " + parentPath + " key: " + key, e);
         }
      }

      public boolean select(Serializable key, ObjectCacheInfo value)
      {
         return ((String) key).startsWith(parentPath);
      }
   }

   protected class ItemRemoveSelector
      implements CachedObjectSelector
   {

      private final String itemPath;

      protected ItemRemoveSelector(String itemPath)
      {
         this.itemPath = itemPath;
      }

      public void onSelect(ExoCache exoCache, Serializable key, ObjectCacheInfo value) throws Exception
      {
         try
         {
            ItemData removed = (ItemData) exoCache.remove(key);
            if (removed != null)
            {
               exoCache.remove(removed.getIdentifier());
            }
         }
         catch (Exception e)
         {
            log.error(name + ", ItemRemoveSelector.onSelect() " + itemPath + " key: " + key, e);
         }
      }

      public boolean select(Serializable key, ObjectCacheInfo value)
      {
         return ((String) key).equals(itemPath);
      }
   }

   /**
    * Remove relations in CN and CP for item expired in C
    */
   protected class ExpiredListener
      implements CacheListener
   {

      public void onExpire(ExoCache cache, Serializable key, Object obj) throws Exception
      {
         if (obj == null)
            return;

         ItemData item = (ItemData) obj;

         // log.info("expired " + item.getQPath().getAsString());

         if (item.isNode())
         {
            // removing childs of the node
            if (removeChildNodes(item.getIdentifier(), false) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", onExpire() removeChildNodes() " + item.getIdentifier());
            }
            if (removeChildProperties(item.getIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", onExpire() removeChildProperties() " + item.getIdentifier());
            }
         }
         else
         {
            // removing child properties of the item parent
            if (removeChildProperties(item.getParentIdentifier()) != null)
            {
               if (log.isDebugEnabled())
                  log.debug(name + ", onExpire() parent.removeChildProperties() " + item.getParentIdentifier());
            }
         }
      }

      public void onClearCache(ExoCache cache) throws Exception
      {
      }

      public void onGet(ExoCache cache, Serializable key, Object obj) throws Exception
      {
      }

      public void onPut(ExoCache cache, Serializable key, Object obj) throws Exception
      {
      }

      public void onRemove(ExoCache cache, Serializable key, Object obj) throws Exception
      {
      }

      public void onClearCache(CacheListenerContext context) throws Exception
      {
         // TODO Auto-generated method stub

      }

      public void onExpire(CacheListenerContext context, Serializable key, Object obj) throws Exception
      {
         // TODO Auto-generated method stub

      }

      public void onGet(CacheListenerContext context, Serializable key, Object obj) throws Exception
      {
         // TODO Auto-generated method stub

      }

      public void onPut(CacheListenerContext context, Serializable key, Object obj) throws Exception
      {
         // TODO Auto-generated method stub

      }

      public void onRemove(CacheListenerContext context, Serializable key, Object obj) throws Exception
      {
         // TODO Auto-generated method stub

      }
   }
}
