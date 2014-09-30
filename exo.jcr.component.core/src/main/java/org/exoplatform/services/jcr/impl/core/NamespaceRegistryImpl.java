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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.AddNamespacesPlugin;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.query.RepositoryIndexSearcherHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.picocontainer.Startable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: NamespaceRegistryImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class NamespaceRegistryImpl implements ExtendedNamespaceRegistry, Startable
{

   public static final Map<String, String> DEF_NAMESPACES = new HashMap<String, String>();

   public static final Map<String, String> DEF_PREFIXES = new HashMap<String, String>();

   private final static Set<String> PROTECTED_NAMESPACES = new HashSet<String>();

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.NamespaceRegistryImpl");

   private boolean started = false;
   static
   {

      DEF_NAMESPACES.put("", "");
      DEF_NAMESPACES.put("jcr", "http://www.jcp.org/jcr/1.0");
      DEF_NAMESPACES.put("nt", "http://www.jcp.org/jcr/nt/1.0");
      DEF_NAMESPACES.put("mix", "http://www.jcp.org/jcr/mix/1.0");
      DEF_NAMESPACES.put("xml", "http://www.w3.org/XML/1998/namespace");
      DEF_NAMESPACES.put("sv", "http://www.jcp.org/jcr/sv/1.0");
      DEF_NAMESPACES.put("exo", "http://www.exoplatform.com/jcr/exo/1.0");
      DEF_NAMESPACES.put("xs", "http://www.w3.org/2001/XMLSchema");
      DEF_NAMESPACES.put(Constants.NS_XSI_PREFIX, Constants.NS_XSI_URI);
      DEF_NAMESPACES.put("fn", "http://www.w3.org/2005/xpath-functions");
      DEF_NAMESPACES.put("fn_old", "http://www.w3.org/2004/10/xpath-functions");
      DEF_NAMESPACES.put("rep", "internal");

      DEF_PREFIXES.put("", "");
      DEF_PREFIXES.put("http://www.jcp.org/jcr/1.0", "jcr");
      DEF_PREFIXES.put("http://www.jcp.org/jcr/nt/1.0", "nt");
      DEF_PREFIXES.put("http://www.jcp.org/jcr/mix/1.0", "mix");
      DEF_PREFIXES.put("http://www.w3.org/XML/1998/namespace", "xml");
      DEF_PREFIXES.put("http://www.jcp.org/jcr/sv/1.0", "sv");
      DEF_PREFIXES.put("http://www.exoplatform.com/jcr/exo/1.0", "exo");
      DEF_PREFIXES.put("http://www.w3.org/2001/XMLSchema", "xs");
      DEF_PREFIXES.put(Constants.NS_XSI_URI, Constants.NS_XSI_PREFIX);
      DEF_PREFIXES.put("http://www.w3.org/2005/xpath-functions", "fn");
      DEF_PREFIXES.put("http://www.w3.org/2004/10/xpath-functions", "fn_old");
      DEF_PREFIXES.put("internal", "rep");

      PROTECTED_NAMESPACES.add("jcr");
      PROTECTED_NAMESPACES.add("nt");
      PROTECTED_NAMESPACES.add("mix");
      PROTECTED_NAMESPACES.add("xml");
      PROTECTED_NAMESPACES.add("sv");
      PROTECTED_NAMESPACES.add("exo");

   }

   private final DataManager dataManager;

   private final RepositoryIndexSearcherHolder indexSearcherHolder;

   private final Map<String, String> namespaces;

   private final Map<String, String> prefixes;

   private NamespaceDataPersister persister;

   private AddNamespacePluginHolder addNamespacePluginHolder;

   /**
    * Component used to execute commands over the cluster.
    */
   private final RPCService rpcService;

   /**
    * The command that registers the namespace over the cluster
    */
   private RemoteCommand registerNamespace;

   /**
    * The command that unregisters the namespace over the cluster
    */
   private RemoteCommand unregisterNamespace;

   /**
    * Id used to avoid launching twice the same command on the same node
    */
   private String id;

   /**
    * for tests.
    */
   public NamespaceRegistryImpl()
   {
      this(null, null, null, null);
   }

   public NamespaceRegistryImpl(RepositoryEntry config, NamespaceDataPersister persister, DataManager dataManager,
      RepositoryIndexSearcherHolder indexSearcherHolder)
   {
      this(config, persister, dataManager, indexSearcherHolder, (AddNamespacePluginHolder)null);
   }

   public NamespaceRegistryImpl(RepositoryEntry config, NamespaceDataPersister persister, DataManager dataManager,
      RepositoryIndexSearcherHolder indexSearcherHolder, RPCService rpcService)
   {
      this(config, persister, dataManager, indexSearcherHolder, null, rpcService);
   }

   public NamespaceRegistryImpl(RepositoryEntry config, NamespaceDataPersister persister, DataManager dataManager,
      RepositoryIndexSearcherHolder indexSearcherHolder, AddNamespacePluginHolder addNamespacePluginHolder)
   {
      this(config, persister, dataManager, indexSearcherHolder, addNamespacePluginHolder, null);
   }

   public NamespaceRegistryImpl(final RepositoryEntry config, NamespaceDataPersister persister, DataManager dataManager,
      RepositoryIndexSearcherHolder indexSearcherHolder, AddNamespacePluginHolder addNamespacePluginHolder,
      RPCService rpcService)
   {
      this.namespaces = new HashMap<String, String>(DEF_NAMESPACES);
      this.prefixes = new HashMap<String, String>(DEF_PREFIXES);

      this.dataManager = dataManager;
      this.indexSearcherHolder = indexSearcherHolder;
      this.persister = persister;
      this.addNamespacePluginHolder = addNamespacePluginHolder;
      this.rpcService = rpcService;
      if (rpcService != null)
      {
         initRemoteCommands(config);
      }
   }

   /**
    * Registers all the remote commands
    */
   private void initRemoteCommands(final RepositoryEntry config)
   {
      this.id = UUID.randomUUID().toString();
      registerNamespace = rpcService.registerCommand(new RemoteCommand()
      {

         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl-registerNamespace-"
               + config.getName();
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            if (!id.equals(args[0]))
            {
               try
               {
                  registerNamespace((String)args[1], (String)args[2], false);
               }
               catch (Exception e)
               {
                  LOG.warn("Could not register the namespace on other cluster nodes", e);
               }
            }
            return true;
         }
      });
      unregisterNamespace = rpcService.registerCommand(new RemoteCommand()
      {

         public String getId()
         {
            return "org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl-unregisterNamespace-"
               + config.getName();
         }

         public Serializable execute(Serializable[] args) throws Throwable
         {
            if (!id.equals(args[0]))
            {
               try
               {
                  unregisterNamespace((String)args[1], false);
               }
               catch (Exception e)
               {
                  LOG.warn("Could not unregister the namespace on other cluster nodes", e);
               }
            }
            return true;
         }
      });
   }

   /**
    * Unregisters the remote commands.
    */
   private void unregisterRemoteCommands()
   {
      if (rpcService != null)
      {
         rpcService.unregisterCommand(registerNamespace);
         rpcService.unregisterCommand(unregisterNamespace);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String[] getAllNamespacePrefixes()
   {
      return getPrefixes();
   }

   /**
    * {@inheritDoc}
    */
   public String getNamespacePrefixByURI(String uri) throws NamespaceException, RepositoryException
   {
      return getPrefix(uri);
   }

   /**
    * {@inheritDoc}
    */
   public String getNamespaceURIByPrefix(String prefix) throws NamespaceException
   {
      return getURI(prefix);
   }

   /**
    * {@inheritDoc}
    */
   public String getPrefix(String uri) throws NamespaceException
   {
      String prefix = prefixes.get(uri);
      if (prefix != null)
      {
         return prefix;
      }
      throw new NamespaceException("Prefix for " + uri + " not found");
   }

   /**
    * {@inheritDoc}
    */
   public String[] getPrefixes()
   {
      return namespaces.keySet().toArray(new String[namespaces.keySet().size()]);
   }

   /**
    * {@inheritDoc}
    */
   public String getURI(String prefix) throws NamespaceException
   {
      String uri = namespaces.get(prefix);
      if (uri == null)
      {
         throw new NamespaceException("Unknown Prefix " + prefix);
      }
      return uri;
   }

   /**
    * {@inheritDoc}
    */
   public String[] getURIs()
   {
      return namespaces.values().toArray(new String[namespaces.size()]);
   }

   public boolean isDefaultNamespace(String uri)
   {
      return DEF_PREFIXES.containsKey(uri);
   }

   public boolean isDefaultPrefix(String prefix)
   {
      return DEF_NAMESPACES.containsKey(prefix);
   }

   public boolean isPrefixMaped(String prefix)
   {
      return namespaces.containsKey(prefix);
   }

   public boolean isUriRegistered(String uri)
   {
      return prefixes.containsKey(uri);
   }

   // //////////////////// NamespaceAccessor

   /**
    * {@inheritDoc}
    */
   public void registerNamespace(String prefix, String uri) throws NamespaceException, RepositoryException
   {

      validateNamespace(prefix, uri);

      registerNamespace(prefix, uri, true);
      if (started && rpcService != null)
      {
         try
         {
            rpcService.executeCommandOnAllNodes(registerNamespace, false, id, prefix, uri);
         }
         catch (Exception e)
         {
            LOG.warn("Could not register the namespace '" + uri + "' on other cluster nodes", e);
         }
      }
   }

   /**
    * Registers the namespace and persists it if <code>persist</code> has been set to <code>true</code> and a persister has been configured
    */
   private synchronized void registerNamespace(String prefix, String uri, boolean persist) throws NamespaceException, RepositoryException
   {
      if (namespaces.containsKey(prefix))
      {
         unregisterNamespace(prefix);
      }
      else if (prefixes.containsKey(uri))
      {
         unregisterNamespace(prefixes.get(uri));
      }
      if (persister != null && persist)
      {
         persister.addNamespace(prefix, uri);
      }
      final String newPrefix = new String(prefix);
      final String newUri = new String(uri);

      namespaces.put(newPrefix, newUri);
      prefixes.put(newUri, newPrefix);
   }

   public void start()
   {
      if (!started)
      {
         // save default
         if (persister != null)
         {
            // no save default
            try
            {
               if (!persister.isStorageFilled())
               {
                  persister.addNamespaces(DEF_NAMESPACES);
               }
               else
               {
                  persister.loadNamespaces(namespaces, prefixes);
               }
            }
            catch (final RepositoryException e)
            {
               throw new RuntimeException(e.getLocalizedMessage(), e);
            }
         }

         if (addNamespacePluginHolder != null)
         {
            addPendingNamespaces();
         }

         started = true;
      }
   }

   public void stop()
   {
      unregisterRemoteCommands();
   }

   /**
    * {@inheritDoc}
    */
   public void unregisterNamespace(String prefix) throws NamespaceException, RepositoryException
   {

      unregisterNamespace(prefix, true);

      if (started && rpcService != null)
      {
         try
         {
            rpcService.executeCommandOnAllNodes(unregisterNamespace, false, id, prefix);
         }
         catch (Exception e)
         {
            LOG.warn("Could not unregister the prefix '" + prefix + "' on other cluster nodes", e);
         }
      }
   }

   /**
    * unregisters the namespace and persists it if <code>persist</code> has been set to <code>true</code> and a persister has been configured
    */
   private void unregisterNamespace(String prefix, boolean persist) throws NamespaceException, RepositoryException
   {
      if (namespaces.get(prefix) == null)
      {
         throw new NamespaceException("Prefix " + prefix + " is not registered");
      }

      if (PROTECTED_NAMESPACES.contains(prefix))
      {
         throw new NamespaceException("Prefix " + prefix + " is protected");
      }
      String uri = getURI(prefix);
      if (indexSearcherHolder != null)
      {
         final Set<String> nodes = indexSearcherHolder.getNodesByUri(uri);
         if (nodes.size() > 0)
         {
            StringBuilder builder = new StringBuilder();
            builder.append("Fail to unregister namespace '");
            builder.append(prefix);
            builder.append("' because of following nodes:  ");

            for (String uuid : nodes)
            {
               ItemData item = dataManager.getItemData(uuid);
               if (item != null && item.isNode())
               {
                  builder.append(" - ");
                  builder.append(item.getQPath().getAsString());
                  builder.append("\r\n");
               }
            }
            builder.append(" uses this prefix.");
            throw new NamespaceException(builder.toString());
         }
      }
      prefixes.remove(uri);
      namespaces.remove(prefix);
      if (persister != null && persist)
      {
         persister.removeNamespace(prefix);
      }
   }

   public void validateNamespace(String prefix, String uri) throws NamespaceException, RepositoryException
   {
      if (prefix.indexOf(":") > 0)
      {
         throw new RepositoryException("Namespace prefix should not contain ':' " + prefix);
      }

      if (PROTECTED_NAMESPACES.contains(prefix))
      {
         if (uri == null)
         {
            throw new NamespaceException("Can not remove built-in namespace " + prefix);
         }
         throw new NamespaceException("Can not change built-in namespace " + prefix);
      }
      if (prefix.toLowerCase().startsWith("xml"))
      {
         throw new NamespaceException("Can not re-assign prefix that start with 'xml'");
      }
      if (uri == null)
      {
         throw new NamespaceException("Can not register NULL URI!");
      }
   }

   private void addPendingNamespaces()
   {
      for (ComponentPlugin plugin : addNamespacePluginHolder.getAddNamespacesPlugins())
      {
         Map<String, String> namespaces = ((AddNamespacesPlugin)plugin).getNamespaces();
         try
         {
            for (Map.Entry<String, String> namespace : namespaces.entrySet())
            {

               String prefix = namespace.getKey();
               String uri = namespace.getValue();

               // register namespace if not found
               try
               {
                  getURI(prefix);
               }
               catch (NamespaceException e)
               {
                  registerNamespace(prefix, uri);
               }
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Namespace is registered " + prefix + " = " + uri);
               }
            }
         }
         catch (Exception e)
         {
            LOG.error("Error load namespaces ", e);
         }
      }
   }

}
