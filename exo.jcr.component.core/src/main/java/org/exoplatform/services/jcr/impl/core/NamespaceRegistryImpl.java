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
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.impl.AddNamespacesPlugin;
import org.exoplatform.services.jcr.impl.core.query.RepositoryIndexSearcherHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

   protected final static Log log = ExoLogger.getLogger("exo.jcr.component.core.NamespaceRegistryImpl");

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
    * for tests.
    */
   public NamespaceRegistryImpl()
   {
      this.namespaces = new HashMap<String, String>(DEF_NAMESPACES);
      this.prefixes = new HashMap<String, String>(DEF_PREFIXES);

      this.dataManager = null;
      this.indexSearcherHolder = null;
      this.persister = null;
      this.addNamespacePluginHolder = null;
   }

   public NamespaceRegistryImpl(NamespaceDataPersister persister, DataManager dataManager,
      RepositoryIndexSearcherHolder indexSearcherHolder)
   {
      this.namespaces = new HashMap<String, String>(DEF_NAMESPACES);
      this.prefixes = new HashMap<String, String>(DEF_PREFIXES);

      this.dataManager = dataManager;
      this.indexSearcherHolder = indexSearcherHolder;
      this.persister = persister;
      this.addNamespacePluginHolder = null;
   }

   public NamespaceRegistryImpl(NamespaceDataPersister persister, DataManager dataManager,
      RepositoryIndexSearcherHolder indexSearcherHolder, AddNamespacePluginHolder addNamespacePluginHolder)
   {
      this.namespaces = new HashMap<String, String>(DEF_NAMESPACES);
      this.prefixes = new HashMap<String, String>(DEF_PREFIXES);

      this.dataManager = dataManager;
      this.indexSearcherHolder = indexSearcherHolder;
      this.persister = persister;
      this.addNamespacePluginHolder = addNamespacePluginHolder;
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
   public synchronized void registerNamespace(String prefix, String uri) throws NamespaceException, RepositoryException
   {

      validateNamespace(prefix, uri);

      if (namespaces.containsKey(prefix))
      {
         unregisterNamespace(prefix);
      }
      else if (prefixes.containsKey(uri))
      {
         unregisterNamespace(prefixes.get(uri));
      }
      if (persister != null)
      {
         persister.addNamespace(prefix, uri);
         // persister.saveChanges();
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
   }

   /**
    * {@inheritDoc}
    */
   public void unregisterNamespace(String prefix) throws NamespaceException, RepositoryException
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
      if (persister != null)
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

   private void validate() throws RepositoryException
   {
      if (dataManager == null)
      {
         throw new RepositoryException("Datamanager not initialized");
      }
      if (indexSearcherHolder == null)
      {
         throw new RepositoryException("RepositoryIndexSearcherHolder not initialized");
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
               if (log.isDebugEnabled())
               {
                  log.debug("Namespace is registered " + prefix + " = " + uri);
               }
            }
         }
         catch (Exception e)
         {
            log.error("Error load namespaces ", e);
         }
      }
   }

}
