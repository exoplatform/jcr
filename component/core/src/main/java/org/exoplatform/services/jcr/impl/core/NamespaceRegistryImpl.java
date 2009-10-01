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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.lucene.FieldNames;
import org.exoplatform.services.jcr.impl.core.query.lucene.QueryHits;
import org.exoplatform.services.jcr.impl.core.query.lucene.ScoreNode;
import org.exoplatform.services.jcr.impl.core.value.NameValue;
import org.exoplatform.services.jcr.impl.core.value.PathValue;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.AbstractValueData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: NamespaceRegistryImpl.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class NamespaceRegistryImpl implements ExtendedNamespaceRegistry
{

   public static final Map<String, String> DEF_NAMESPACES = new HashMap<String, String>();

   public static final Map<String, String> DEF_PREFIXES = new HashMap<String, String>();

   private final static Set<String> PROTECTED_NAMESPACES = new HashSet<String>();

   protected final static Log log = ExoLogger.getLogger("jcr.NamespaceRegistryImpl");

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
      DEF_PREFIXES.put("http://www.w3.org/XML/1998/namespace", "mix");
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

   private Map<String, String> namespaces;

   private NamespaceDataPersister persister;

   private Map<String, String> prefixes;

   private HashSet<QueryHandler> queryHandlers;

   // private final NodeTypeDataManager nodeTypeDataManager;

   /**
    * for tests.
    */
   public NamespaceRegistryImpl()
   {
      this.namespaces = DEF_NAMESPACES;
      this.prefixes = DEF_PREFIXES;
      this.queryHandlers = new HashSet<QueryHandler>();
      // this.nodeTypeDataManager = null;
   }

   public NamespaceRegistryImpl(NamespaceDataPersister persister)
   {

      // this.nodeTypeDataManager = nodeTypeDataManager;
      this.namespaces = new HashMap<String, String>(DEF_NAMESPACES);
      this.prefixes = new HashMap<String, String>(DEF_PREFIXES);
      this.persister = persister;
      this.queryHandlers = new HashSet<QueryHandler>();
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

   public void loadFromStorage() throws RepositoryException
   {

      try
      {
         persister.loadNamespaces(namespaces, prefixes);
      }
      catch (PathNotFoundException e)
      {
         log.info("Namespaces storage (/jcr:system/exo:namespaces) is not accessible."
            + " Default namespaces only will be used. " + e);
         return;
      }
   }

   // //////////////////// NamespaceAccessor

   /**
    * {@inheritDoc}
    */
   public synchronized void registerNamespace(String prefix, String uri) throws NamespaceException, RepositoryException
   {

      validateNamespace(prefix, uri);

      // if (namespaces.containsKey(prefix) || prefixes.containsKey(uri)) {
      if (namespaces.containsKey(prefix))
      {
         unregisterNamespace(prefix);
      }
      else if (prefixes.containsKey(uri))
      {
         unregisterNamespace(prefixes.get(uri));
      }

      persister.addNamespace(prefix, uri);
      persister.saveChanges();

      String newPrefix = new String(prefix);
      String newUri = new String(uri);

      namespaces.put(newPrefix, newUri);
      prefixes.put(newUri, newPrefix);
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

      // throw new NamespaceException("Unregistration is not supported as"
      // + " may cause integrity problems. (todo issue #46)");

      Set<String> nodes = getNodes(prefix);
      if (nodes.size() > 0)
      {
         StringBuffer buffer = new StringBuffer();
         buffer.append("Fail to unregister namespace");
         buffer.append(prefix);
         buffer.append(" because of following nodes:  ");
         DataManager dm = persister.getDataManager();
         for (String uuid : nodes)
         {
            ItemData item = dm.getItemData(uuid);
            if (item != null && item.isNode())
               buffer.append(item.getQPath().getAsString());
         }
         buffer.append(" contains whese prefix  ");
         throw new NamespaceException(buffer.toString());
      }

      prefixes.remove(getURI(prefix));
      namespaces.remove(prefix);
      persister.removeNamespace(prefix);

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
            throw new NamespaceException("Can not remove built-in namespace");
         }
         throw new NamespaceException("Can not change built-in namespace");
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

   public void addQueryHandler(QueryHandler queryHandler)
   {
      queryHandlers.add(queryHandler);
   }

   /**
    * Return
    * 
    * @param nodeType
    * @return
    * @throws RepositoryException
    * @throws IOException
    */
   public Set<String> getNodes(String prefix) throws RepositoryException
   {

      LocationFactory locationFactory = new LocationFactory(this);
      ValueFactoryImpl valueFactory = new ValueFactoryImpl(locationFactory);

      BooleanQuery query = new BooleanQuery();
      // query.add(new MatchAllDocsQuery(), Occur.MUST);
      // name of the node
      query.add(new WildcardQuery(new Term(FieldNames.LABEL, prefix + ":*")), Occur.SHOULD);
      // name of the property
      query.add(new WildcardQuery(new Term(FieldNames.PROPERTIES_SET, prefix + ":*")), Occur.SHOULD);

      Set<String> result = getNodes(query);

      // value of the property
      Set<String> propSet = getNodes(new WildcardQuery(new Term(FieldNames.PROPERTIES, "*" + prefix + ":*")));
      // Manually check property values;
      for (String uuid : propSet)
      {
         if (isPrefixMatch(valueFactory, uuid, prefix))
            result.add(uuid);
      }

      return result;
   }

   /**
    * @param valueFactory
    * @param dm
    * @param uuid
    * @param prefix
    * @throws RepositoryException
    */
   private boolean isPrefixMatch(ValueFactoryImpl valueFactory, String uuid, String prefix) throws RepositoryException
   {
      DataManager dm = persister.getDataManager();
      ItemData node = dm.getItemData(uuid);
      if (node != null && node.isNode())
      {
         List<PropertyData> props = dm.getChildPropertiesData((NodeData)node);
         for (PropertyData propertyData : props)
         {
            if (propertyData.getType() == PropertyType.PATH || propertyData.getType() == PropertyType.NAME)
            {
               for (ValueData vdata : propertyData.getValues())
               {
                  Value val =
                     valueFactory.loadValue(((AbstractValueData)vdata).createTransientCopy(), propertyData.getType());
                  if (propertyData.getType() == PropertyType.PATH)
                  {
                     if (isPrefixMatch(((PathValue)val).getQPath(), prefix))
                        return true;
                  }
                  else if (propertyData.getType() == PropertyType.NAME)
                  {
                     if (isPrefixMatch(((NameValue)val).getQName(), prefix))
                        return true;
                  }
               }
            }
         }
      }
      return false;
   }

   private boolean isPrefixMatch(QPath value, String prefix) throws NamespaceException
   {
      for (int i = 0; i < value.getEntries().length; i++)
      {
         if (isPrefixMatch(value.getEntries()[i], prefix))
            return true;
      }
      return false;
   }

   private boolean isPrefixMatch(InternalQName value, String prefix) throws NamespaceException
   {
      return (value.getNamespace().equals(getURI(prefix)));
   }

   /**
    * @param query
    * @return
    * @throws RepositoryException
    */
   private Set<String> getNodes(Query query) throws RepositoryException
   {
      Set<String> result = new HashSet<String>();

      Iterator<QueryHandler> it = queryHandlers.iterator();
      try
      {
         while (it.hasNext())
         {
            QueryHandler queryHandler = it.next();
            QueryHits hits = queryHandler.executeQuery(query);

            ScoreNode sn;

            while ((sn = hits.nextScoreNode()) != null)
            {
               result.add(sn.getNodeId());
            }
            //            for (int i = 0; i < hits.getSize(); i++)
            //            {
            //               result.add(hits.getFieldContent(i, FieldNames.UUID));
            //            }
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e.getLocalizedMessage(), e);
      }
      return result;
   }

}
