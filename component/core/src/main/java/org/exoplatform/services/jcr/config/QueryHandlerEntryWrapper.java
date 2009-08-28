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
package org.exoplatform.services.jcr.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.exoplatform.services.log.Log;
import org.apache.lucene.search.Query;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.query.ErrorLog;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandlerContext;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.DefaultHTMLExcerpt;
import org.exoplatform.services.jcr.impl.core.query.lucene.ExcerptProvider;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexingConfiguration;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexingConfigurationEntityResolver;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexingConfigurationImpl;
import org.exoplatform.services.jcr.impl.core.query.lucene.JcrStandartAnalyzer;
import org.exoplatform.services.jcr.impl.core.query.lucene.NamespaceMappings;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.jcr.impl.core.query.lucene.SpellChecker;
import org.exoplatform.services.jcr.impl.core.query.lucene.SynonymProvider;
import org.exoplatform.services.jcr.util.StringNumberParser;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: QueryHandlerEntry.java 14931 2008-05-29 15:02:08Z ksm $
 */

public class QueryHandlerEntryWrapper implements QueryHandlerParams
{

   /**
    * The default value for property {@link #extractorBackLog}.
    */
   public static final int DEFAULT_EXTRACTOR_BACKLOG = 100;

   /**
    * The default value for property {@link #extractorPoolSize}.
    */
   public static final int DEFAULT_EXTRACTOR_POOLSIZE = 0;

   /**
    * The default timeout in milliseconds which is granted to the text extraction process until
    * fulltext indexing is deferred to a background thread.
    */
   public static final int DEFAULT_EXTRACTOR_TIMEOUT = 100;

   /**
    * the default value for property {@link #maxFieldLength}.
    */
   public static final int DEFAULT_MAX_FIELD_LENGTH = 10000;

   /**
    * The default value for property {@link #maxMergeDocs}.
    */
   public static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;

   /**
    * the default value for property {@link #mergeFactor}.
    */
   public static final int DEFAULT_MERGE_FACTOR = 10;

   /**
    * The default value for property {@link #minMergeDocs}.
    */
   public static final int DEFAULT_MIN_MERGE_DOCS = 100;

   /**
    * Name of the file to persist search internal namespace mappings.
    */
   public static final String NS_MAPPING_FILE = "ns_mappings.properties"; // TODO

   /**
    * The excerpt provider class. Implements {@link ExcerptProvider}.
    */
   private static final String DEDAULT_EXCERPTPROVIDER_CLASS = DefaultHTMLExcerpt.class.getName();

   private static final String DEDAULT_INDEXINGCONFIGURATIONCLASS = IndexingConfigurationImpl.class.getName();

   private static final boolean DEFAULT_AUTOREPAIR = true;

   private static final int DEFAULT_BUFFER_SIZE = 10;

   private static final int DEFAULT_CACHE_SIZE = 1000;

   private final static boolean DEFAULT_CONSISTENCYCHECKENABLED = false;

   private final static boolean DEFAULT_DOCUMENTORDER = true;

   private final static boolean DEFAULT_FORCECONSISTENCYCHECK = false;

   /**
    * Name of the default query implementation class.
    */
   private static final String DEFAULT_QUERY_HANDLER_CLASS = SearchIndex.class.getName();

   /**
    * Name of the default query implementation class.
    */
   private static final String DEFAULT_QUERY_IMPL_CLASS = QueryImpl.class.getName();

   /**
    * The number of documents that are pre fetched when a query is executed. <p/> Default value is:
    * {@link Integer#MAX_VALUE}.
    */
   private final static int DEFAULT_RESULTFETCHSIZE = Integer.MAX_VALUE;

   private final static boolean DEFAULT_SUPPORTHIGHLIGHTING = false;

   private final static boolean DEFAULT_USECOMPOUNDFILE = false;

   private final static int DEFAULT_VOLATILEIDLETIME = 3;

   private QueryHandlerEntry queryHandlerEntry;

   public QueryHandlerEntry getQueryHandlerEntry()
   {
      return queryHandlerEntry;
   }

   private static void initDefaults(QueryHandlerEntry entry)
   {
      entry.putBooleanParameter(PARAM_AUTO_REPAIR, DEFAULT_AUTOREPAIR);
      entry.putIntegerParameter(PARAM_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
      entry.putIntegerParameter(PARAM_CACHE_SIZE, DEFAULT_CACHE_SIZE);
      entry.putBooleanParameter(PARAM_DOCUMENT_ORDER, DEFAULT_DOCUMENTORDER);
      entry.putParameterValue(PARAM_EXCERPTPROVIDER_CLASS, DEDAULT_EXCERPTPROVIDER_CLASS);
      entry.putParameterValue(PARAM_EXCLUDED_NODE_IDENTIFERS, null);
      entry.putIntegerParameter(PARAM_EXTRACTOR_BACKLOG, DEFAULT_EXTRACTOR_BACKLOG);
      entry.putIntegerParameter(PARAM_EXTRACTOR_POOLSIZE, DEFAULT_EXTRACTOR_POOLSIZE);
      entry.putIntegerParameter(PARAM_EXTRACTOR_TIMEOUT, DEFAULT_EXTRACTOR_TIMEOUT);
   }

   public String getType()
   {
      return queryHandlerEntry.getType();
   }

   public static QueryHandlerEntry queryHandlerEntryFactory()
   {
      QueryHandlerEntry entry = new QueryHandlerEntry();
      initDefaults(entry);
      return entry;
   }

   /** The logger instance for this class */
   private static final Log log = ExoLogger.getLogger(QueryHandlerEntry.class);

   //public QueryHandlerEntry     queryHandler;

   public Integer volatileIdleTime;

   /**
    * The analyzer we use for indexing.
    */
   private JcrStandartAnalyzer analyzer;

   private String queryHandlerClass = DEFAULT_QUERY_HANDLER_CLASS;

   public QueryHandlerEntryWrapper(QueryHandlerEntry queryHandlerEntry)
   {
      this.queryHandlerEntry = queryHandlerEntry;
      this.analyzer = new JcrStandartAnalyzer();
      initDefaults(queryHandlerEntry);
   }

   public QueryHandlerEntryWrapper(String type, List params, QueryHandlerEntry queryHandlerEntry)
   {
      this.queryHandlerEntry = queryHandlerEntry;
      queryHandlerEntry.setType(type);
      queryHandlerEntry.setParameters(params);
      this.analyzer = new JcrStandartAnalyzer();
      initDefaults(queryHandlerEntry);
   }

   /**
    * Creates an excerpt provider for the given <code>query</code>.
    * 
    * @param query the query.
    * @return an excerpt provider for the given <code>query</code>.
    * @throws IOException if the provider cannot be created.
    */
   public ExcerptProvider createExcerptProvider(Query query) throws IOException
   {
      ExcerptProvider ep;
      try
      {
         Class excerptProviderClass = Class.forName(getExcerptProviderClass(), true, this.getClass().getClassLoader());
         ep = (ExcerptProvider)excerptProviderClass.newInstance();
      }
      catch (Exception e)
      {
         IOException ex = new IOException();
         ex.initCause(e);
         throw ex;
      }

      return ep;
   }

   /**
    * @param namespaceMappings The namespace mappings
    * @return the fulltext indexing configuration or <code>null</code> if there is no configuration.
    */
   public IndexingConfiguration createIndexingConfiguration(NamespaceMappings namespaceMappings,
      QueryHandlerContext context, ConfigurationManager cfm) throws IOException, RepositoryConfigurationException
   {
      Element docElement = getIndexingConfigurationDOM(cfm);
      if (docElement == null)
      {
         return null;
      }
      IndexingConfiguration idxCfg = null;
      try
      {
         Class indexingConfigurationClass =
            Class.forName(getIndexingConfigurationClass(), true, this.getClass().getClassLoader());
         idxCfg = (IndexingConfiguration)indexingConfigurationClass.newInstance();
         idxCfg.init(docElement, context, namespaceMappings);
      }
      catch (InstantiationException e)
      {
         log.warn("Exception initializing indexing configuration from: " + getIndexingConfigurationPath(), e);
      }
      catch (IllegalAccessException e)
      {
         log.warn("Exception initializing indexing configuration from: " + getIndexingConfigurationPath(), e);
      }
      catch (RepositoryException e)
      {
         log.warn("Exception initializing indexing configuration from: " + getIndexingConfigurationPath(), e);
      }
      catch (IllegalNameException e)
      {
         log.warn("Exception initializing indexing configuration from: " + getIndexingConfigurationPath(), e);
      }
      catch (ClassNotFoundException e)
      {
         log.warn("Exception initializing indexing configuration from: " + getIndexingConfigurationPath(), e);
      }
      return idxCfg;
   }

   /**
    * Creates a spell checker for this query handler.
    * 
    * @return the spell checker or <code>null</code> if none is configured or an error occurs.
    */
   public SpellChecker createSpellChecker(QueryHandler handler)
   {
      SpellChecker spCheck = null;
      if (getSpellCheckerClass() != null)
      {
         try
         {
            Class spellCheckerClass = Class.forName(getSpellCheckerClass(), true, this.getClass().getClassLoader());
            spCheck = (SpellChecker)spellCheckerClass.newInstance();
            spCheck.init(handler);
         }
         catch (Exception e)
         {
            log.warn("Exception initializing spell checker: " + getSpellCheckerClass(), e);
         }
      }
      return spCheck;
   }

   /**
    * @param cfm
    * @return the configured synonym provider or <code>null</code> if none is configured or an error
    *         occurs.
    */
   public SynonymProvider createSynonymProvider(ConfigurationManager cfm)
   {
      SynonymProvider sp = null;
      if (getSynonymProviderClass() != null)
      {
         try
         {
            Class synonymProviderClass =
               Class.forName(getSynonymProviderClass(), true, this.getClass().getClassLoader());
            sp = (SynonymProvider)synonymProviderClass.newInstance();

            sp.initialize(createSynonymProviderConfigResource(cfm));
         }
         catch (Exception e)
         {
            log.warn("Exception initializing synonym provider: " + getSynonymProviderClass(), e);
            sp = null;
         }
      }
      return sp;
   }

   public JcrStandartAnalyzer getAnalyzer()
   {
      return analyzer;
   }

   private String getParameterString(String name)
   {
      return queryHandlerEntry.getParameterValue(name, null);
   }

   private Integer getParameterIntegerInitialized(String name)
   {
      String value = queryHandlerEntry.getParameterValue(name, null);
      return StringNumberParser.parseInt(value);
   }

   private Boolean getParameterBooleanInitialized(String name)
   {
      String value = queryHandlerEntry.getParameterValue(name, "false");
      return Boolean.parseBoolean(value);
   }

   /**
    * If set <code>true</code> errors detected by the consistency check are repaired. If
    * <code>false</code> the errors are only reported in the log. <p/> Default value is:
    * <code>true</code>.
    * @throws RepositoryConfigurationException 
    */
   public boolean getAutoRepair() throws RepositoryConfigurationException
   {
      return getParameterBooleanInitialized(PARAM_AUTO_REPAIR);
   }

   /**
    * Number of documents that are buffered before they are added to the index.
    * @throws RepositoryConfigurationException 
    */
   public int getBufferSize()
   {
      return getParameterIntegerInitialized(PARAM_BUFFER_SIZE);
   }

   public int getCacheSize()
   {
      return getParameterIntegerInitialized(PARAM_CACHE_SIZE);
   }

   /**
    * Flag indicating whether document order is enable as the default ordering.
    */
   public boolean getDocumentOrder()
   {
      return getParameterBooleanInitialized(PARAM_DOCUMENT_ORDER);
   }

   /**
    * @return the class name of the excerpt provider implementation.
    */
   public String getExcerptProviderClass()
   {
      return getParameterString(PARAM_EXCERPTPROVIDER_CLASS);
   }

   public String getExcludedNodeIdentifers()
   {
      return getParameterString(PARAM_EXCLUDED_NODE_IDENTIFERS);
   }

   /**
    * @return the size of the extractor queue back log.
    */
   public int getExtractorBackLogSize()
   {
      return getParameterIntegerInitialized(PARAM_EXTRACTOR_BACKLOG);
   }

   /**
    * @return the size of the thread pool which is used to run the text extractors when binary
    *         content is indexed.
    */
   public int getExtractorPoolSize()
   {
      return getParameterIntegerInitialized(PARAM_EXTRACTOR_POOLSIZE);
   }

   /**
    * @return the extractor timeout in milliseconds.
    */
   public long getExtractorTimeout()
   {
      return getParameterIntegerInitialized(PARAM_EXTRACTOR_TIMEOUT);
   }

   /**
    * Returns the location of the search index. Returns <code>null</code> if not set.
    * 
    * @return the location of the search index.
    * @throws RepositoryConfigurationException
    */
   public String getIndexDir() throws RepositoryConfigurationException
   {

      String indexDir;
      try
      {
         indexDir = queryHandlerEntry.getParameterValue(PARAM_INDEX_DIR);
      }
      catch (RepositoryConfigurationException e)
      {
         indexDir = queryHandlerEntry.getParameterValue(OLD_PARAM_INDEX_DIR);
      }

      indexDir = indexDir.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));

      return indexDir;
   }

   /**
    * @return the class name of the indexing configuration implementation.
    */
   public String getIndexingConfigurationClass()
   {
      return queryHandlerEntry
         .getParameterValue(PARAM_INDEXING_CONFIGURATION_CLASS, DEDAULT_INDEXINGCONFIGURATIONCLASS);
   }

   /**
    * @return the path to the indexing configuration file.
    */
   public String getIndexingConfigurationPath()
   {
      return queryHandlerEntry.getParameterValue(PARAM_INDEXING_CONFIGURATION_PATH, null);
   }

   public int getMaxFieldLength()
   {
      return queryHandlerEntry.getParameterInteger(PARAM_MAX_FIELD_LENGTH, DEFAULT_MAX_FIELD_LENGTH);
   }

   /**
    * Returns the current value for maxMergeDocs.
    * 
    * @return the current value for maxMergeDocs.
    */
   public int getMaxMergeDocs()
   {
      return queryHandlerEntry.getParameterInteger(PARAM_MAX_MERGE_DOCS, DEFAULT_MAX_MERGE_DOCS);
   }

   /**
    * Returns the current value for the merge factor.
    * 
    * @return the current value for the merge factor.
    */
   public int getMergeFactor()
   {
      return queryHandlerEntry.getParameterInteger(PARAM_MERGE_FACTOR, DEFAULT_MERGE_FACTOR);
   }

   /**
    * Returns the current value for minMergeDocs.
    * 
    * @return the current value for minMergeDocs.
    */
   public int getMinMergeDocs()
   {
      return queryHandlerEntry.getParameterInteger(PARAM_MIN_MERGE_DOCS, DEFAULT_MIN_MERGE_DOCS);
   }

   public String getQueryClass()
   {
      return queryHandlerEntry.getParameterValue(PARAM_QUERY_CLASS, DEFAULT_QUERY_IMPL_CLASS);
   }

   /**
    * @return the number of results the query handler will fetch initially when a query is executed.
    */
   public int getResultFetchSize()
   {
      return queryHandlerEntry.getParameterInteger(PARAM_RESULT_FETCH_SIZE, DEFAULT_RESULTFETCHSIZE);
   }

   public String getRootNodeIdentifer()
   {
      return queryHandlerEntry.getParameterValue(PARAM_ROOT_NODE_ID, Constants.ROOT_UUID);
   }

   /**
    * Get spell checker class.
    * 
    * @return the class name of the spell checker implementation or <code>null</code> if none is set.
    */
   public String getSpellCheckerClass()
   {
      return queryHandlerEntry.getParameterValue(PARAM_SPELLCHECKER_CLASS, null);
   }

   /**
    * Get support highlighting.
    * 
    * @return <code>true</code> if highlighting support is enabled.
    */
   public boolean getSupportHighlighting()
   {
      return queryHandlerEntry.getParameterBoolean(PARAM_SUPPORT_HIGHLIGHTING, DEFAULT_SUPPORTHIGHLIGHTING);
   }

   /**
    * Get synonym provider class.
    * 
    * @return the class name of the synonym provider implementation or <code>null</code> if none is
    *         set.
    */
   public String getSynonymProviderClass()
   {
      return queryHandlerEntry.getParameterValue(PARAM_SYNONYMPROVIDER_CLASS, null);
   }

   /**
    * Get synonym provider configuration path.
    * 
    * @return the configuration path for the synonym provider. If none is set this method returns
    *         <code>null</code>.
    */
   public String getSynonymProviderConfigPath()
   {
      return queryHandlerEntry.getParameterValue(PARAM_SYNONYMPROVIDER_CONFIG_PATH, null);
   }

   /**
    * Returns the current value for useCompoundFile.
    * 
    * @return the current value for useCompoundFile.
    */
   public boolean getUseCompoundFile()
   {
      return queryHandlerEntry.getParameterBoolean(PARAM_USE_COMPOUNDFILE, DEFAULT_USECOMPOUNDFILE);
   }

   /**
    * Returns the current value for volatileIdleTime.
    * 
    * @return the current value for volatileIdleTime.
    */
   public int getVolatileIdleTime()
   {
      if (volatileIdleTime == null)
         volatileIdleTime = queryHandlerEntry.getParameterInteger(PARAM_VOLATILE_IDLE_TIME, DEFAULT_VOLATILEIDLETIME);

      return volatileIdleTime;
   }

   /**
    * If set <code>true</code> the index is checked for consistency depending on the
    * {@link #forceConsistencyCheck} parameter. If set to <code>false</code>, no consistency check is
    * performed, even if the redo log had been applied on startup. <p/> Default value is:
    * <code>false</code>.
    * 
    * @return boolean
    */
   public boolean isConsistencyCheckEnabled()
   {
      return queryHandlerEntry.getParameterBoolean(PARAM_CONSISTENCY_CHECK_ENABLED, DEFAULT_CONSISTENCYCHECKENABLED);
   }

   public boolean isForceConsistencyCheck()
   {
      return queryHandlerEntry.getParameterBoolean(PARAM_FORCE_CONSISTENCYCHECK, DEFAULT_FORCECONSISTENCYCHECK);
   }

   /**
    * Creates a file system resource to the synonym provider configuration.
    * 
    * @param cfm
    * @return a file system resource or <code>null</code> if no path was configured.
    * @throws Exception
    */
   protected InputStream createSynonymProviderConfigResource(ConfigurationManager cfm) throws Exception
   {
      if (getSynonymProviderConfigPath() != null)
      {
         return cfm.getInputStream(getSynonymProviderConfigPath());
      }
      return null;
   }

   /**
    * Returns the document element of the indexing configuration or <code>null</code> if there is no
    * indexing configuration.
    * 
    * @return the indexing configuration or <code>null</code> if there is none.
    * @throws IOException
    * @throws RepositoryConfigurationException
    */
   protected Element getIndexingConfigurationDOM(ConfigurationManager cfm) throws IOException,
      RepositoryConfigurationException
   {
      String indexingConfigPath = getIndexingConfigurationPath();
      Element indexingConfiguration = null;
      if (indexingConfigPath != null)
      {

         InputStream is;
         try
         {
            is = cfm.getInputStream(indexingConfigPath);
         }
         catch (Exception e1)
         {
            throw new IOException(e1.getLocalizedMessage());
         }

         if (is == null)
            throw new IOException("Resource does not exist: " + indexingConfigPath);

         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         try
         {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new IndexingConfigurationEntityResolver());
            indexingConfiguration = builder.parse(is).getDocumentElement();
         }
         catch (ParserConfigurationException e)
         {
            throw new RepositoryConfigurationException(e.getLocalizedMessage(), e);
         }
         catch (SAXException e)
         {
            throw new RepositoryConfigurationException(e.getLocalizedMessage(), e);
         }
      }

      return indexingConfiguration;
   }

   /**
    * Return ErrorLog file size in Kb String representation.
    * 
    * @return int size in Kb
    */
   public int getErrorLogSize()
   {
      String size = queryHandlerEntry.getParameterValue(PARAM_ERRORLOG_SIZE, null);
      if ((size == null) || (size.equals("")))
      {
         return ErrorLog.DEFAULT_FILE_SIZE;
      }
      else
      {
         return new Integer(size);
      }
   }

}
