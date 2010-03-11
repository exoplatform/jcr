package org.exoplatform.services.jcr.impl.storage.jdbc.statistics;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */

/**
 * This class is used to give statistics about the time spent in the database access layer. It will
 * print all the metrics value into a file in csv format. It will provide metrics of type 
 * minimum, maximum, total, times and average for each method of {@link WorkspaceStorageConnection}
 * and the global values. It will add data into the file every 5 seconds and add the last line at
 * JVM exit. To activate the statistics, set the JVM parameter called
 * "JDBCWorkspaceDataContainer.statistics.enabled" to <code>true</code>.
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 26 févr. 2010  
 */
public class StatisticsJDBCStorageConnection implements WorkspaceStorageConnection
{

   /**
    * The logger
    */
   private static final Log LOG = ExoLogger.getLogger(StatisticsJDBCStorageConnection.class);

   /**
    * The description of the statistics corresponding to the method 
    * <code>update(PropertyData data)</code>
    */
   private static final String UPDATE_PROPERTY_DATA_DESCR = "updatePropertyData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>update(NodeData data)</code>
    */
   private static final String UPDATE_NODE_DATA_DESCR = "updateNodeData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>rollback()</code>
    */
   private static final String ROLLBACK_DESCR = "rollback";

   /**
    * The description of the statistics corresponding to the method 
    * <code>rename(NodeData data)</code>
    */
   private static final String RENAME_NODE_DATA_DESCR = "renameNodeData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>listChildPropertiesData(NodeData parent)</code>
    */
   private static final String LIST_CHILD_PROPERTIES_DATA_DESCR = "listChildPropertiesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>isOpened()</code>
    */
   private static final String IS_OPENED_DESCR = "isOpened";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getReferencesData(String nodeIdentifier)</code>
    */
   private static final String GET_REFERENCES_DATA_DESCR = "getReferencesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getItemData(String identifier)</code>
    */
   private static final String GET_ITEM_DATA_BY_ID_DESCR = "getItemDataById";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getItemData(NodeData parentData, QPathEntry name)</code>
    */
   private static final String GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR = "getItemDataByNodeDataNQPathEntry";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getChildPropertiesData(NodeData parent)</code>
    */
   private static final String GET_CHILD_PROPERTIES_DATA_DESCR = "getChildPropertiesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getChildNodesData(NodeData parent)</code>
    */
   private static final String GET_CHILD_NODES_DATA_DESCR = "getChildNodesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getChildNodesCount(NodeData parent)</code>
    */
   private static final String GET_CHILD_NODES_COUNT_DESCR = "getChildNodesCount";

   /**
    * The description of the statistics corresponding to the method 
    * <code>delete(PropertyData data)</code>
    */
   private static final String DELETE_PROPERTY_DATA_DESCR = "deletePropertyData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>delete(NodeData data)</code>
    */
   private static final String DELETE_NODE_DATA_DESCR = "deleteNodeData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>commit()</code>
    */
   private static final String COMMIT_DESCR = "commit";

   /**
    * The description of the statistics corresponding to the method 
    * <code>close()</code>
    */
   private static final String CLOSE_DESCR = "close";

   /**
    * The description of the statistics corresponding to the method 
    * <code>add(PropertyData data)</code>
    */
   private static final String ADD_PROPERTY_DATA_DESCR = "addPropertyData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>add(NodeData data)</code>
    */
   private static final String ADD_NODE_DATA_DESCR = "addNodeData";

   /**
    * The global statistics for all the database accesses
    */
   private final static Statistics GLOBAL_STATISTICS = new Statistics("global");

   /**
    * The list of all the statistics, one per method
    */
   private final static Map<String, Statistics> ALL_STATISTICS = new LinkedHashMap<String, Statistics>();
   static
   {
      // Read Methods
      ALL_STATISTICS.put(GET_ITEM_DATA_BY_ID_DESCR, new Statistics(GET_ITEM_DATA_BY_ID_DESCR));
      ALL_STATISTICS.put(GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR, new Statistics(
         GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR));
      ALL_STATISTICS.put(GET_CHILD_NODES_DATA_DESCR, new Statistics(GET_CHILD_NODES_DATA_DESCR));
      ALL_STATISTICS.put(GET_CHILD_NODES_COUNT_DESCR, new Statistics(GET_CHILD_NODES_COUNT_DESCR));
      ALL_STATISTICS.put(GET_CHILD_PROPERTIES_DATA_DESCR, new Statistics(GET_CHILD_PROPERTIES_DATA_DESCR));
      ALL_STATISTICS.put(LIST_CHILD_PROPERTIES_DATA_DESCR, new Statistics(LIST_CHILD_PROPERTIES_DATA_DESCR));
      ALL_STATISTICS.put(GET_REFERENCES_DATA_DESCR, new Statistics(GET_REFERENCES_DATA_DESCR));
      // Write Methods
      // Commit
      ALL_STATISTICS.put(COMMIT_DESCR, new Statistics(COMMIT_DESCR));
      // Add methods
      ALL_STATISTICS.put(ADD_NODE_DATA_DESCR, new Statistics(ADD_NODE_DATA_DESCR));
      ALL_STATISTICS.put(ADD_PROPERTY_DATA_DESCR, new Statistics(ADD_PROPERTY_DATA_DESCR));
      // Update methods
      ALL_STATISTICS.put(UPDATE_NODE_DATA_DESCR, new Statistics(UPDATE_NODE_DATA_DESCR));
      ALL_STATISTICS.put(UPDATE_PROPERTY_DATA_DESCR, new Statistics(UPDATE_PROPERTY_DATA_DESCR));
      // Delete methods
      ALL_STATISTICS.put(DELETE_NODE_DATA_DESCR, new Statistics(DELETE_NODE_DATA_DESCR));
      ALL_STATISTICS.put(DELETE_PROPERTY_DATA_DESCR, new Statistics(DELETE_PROPERTY_DATA_DESCR));
      // Rename
      ALL_STATISTICS.put(RENAME_NODE_DATA_DESCR, new Statistics(RENAME_NODE_DATA_DESCR));
      // Rollback
      ALL_STATISTICS.put(ROLLBACK_DESCR, new Statistics(ROLLBACK_DESCR));
      // Others
      ALL_STATISTICS.put(IS_OPENED_DESCR, new Statistics(IS_OPENED_DESCR));
      ALL_STATISTICS.put(CLOSE_DESCR, new Statistics(CLOSE_DESCR));
   }

   /**
    * The printer used to print the statistics in csv format
    */
   private static PrintWriter STATISTICS_WRITER;
   static
   {
      if (JDBCWorkspaceDataContainer.STATISTICS_ENABLED)
      {
         initWriter();
         if (STATISTICS_WRITER != null)
         {
            addTriggers();
         }
      }
   }

   /**
    * Add all the triggers that will keep the file up to date.
    */
   private static void addTriggers()
   {
      Runtime.getRuntime().addShutdownHook(new Thread("StatisticsJDBCStorageConnection-Hook")
      {
         public void run()
         {
            printData();
         }
      });
      // Define the file header
      printHeader();
      Thread t = new Thread("StatisticsJDBCStorageConnection-Writer")
      {
         public void run()
         {
            while (true)
            {
               try
               {
                  sleep(5000);
               }
               catch (InterruptedException e)
               {
                  LOG.debug("InterruptedException", e);
               }
               printData();
            }
         }
      };
      t.setDaemon(true);
      t.start();
   }

   /**
    * Initialize the {@link PrintWriter}.
    * It will first try to create the file in the user directory, if it cannot, it will try
    * to create it in the temporary folder.
    */
   private static void initWriter()
   {
      File file = null;
      try
      {
         file =
            new File(System.getProperty("user.dir"), "StatisticsJDBCStorageConnection-" + System.currentTimeMillis()
               + ".csv");
         file.createNewFile();
         STATISTICS_WRITER = new PrintWriter(file);
      }
      catch (IOException e)
      {
         LOG
            .error(
               "Cannot create the file for the statistics in the user directory, we will try to create it in the temp directory",
               e);
         try
         {
            file = File.createTempFile("StatisticsJDBCStorageConnection", "-" + System.currentTimeMillis() + ".csv");
            STATISTICS_WRITER = new PrintWriter(file);
         }
         catch (IOException e1)
         {
            LOG.error("Cannot create the file for the statistics", e1);
         }
      }
      if (file != null)
      {
         LOG.info("The file for the statistics is " + file.getPath());
      }
   }

   /**
    * The nested {@link WorkspaceStorageConnection}
    */
   private final WorkspaceStorageConnection wcs;

   /**
    * The default constructor
    */
   public StatisticsJDBCStorageConnection(WorkspaceStorageConnection wcs)
   {
      this.wcs = wcs;
   }

   /**
    * @return the nested {@link WorkspaceStorageConnection}
    */
   public WorkspaceStorageConnection getNestedWorkspaceStorageConnection()
   {
      return wcs;
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#add(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void add(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(ADD_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.add(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#add(org.exoplatform.services.jcr.datamodel.PropertyData)
    */
   public void add(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(ADD_PROPERTY_DATA_DESCR);
      try
      {
         s.begin();
         wcs.add(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#close()
    */
   public void close() throws IllegalStateException, RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(CLOSE_DESCR);
      try
      {
         s.begin();
         wcs.close();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#commit()
    */
   public void commit() throws IllegalStateException, RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(COMMIT_DESCR);
      try
      {
         s.begin();
         wcs.commit();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#delete(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(DELETE_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.delete(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#delete(org.exoplatform.services.jcr.datamodel.PropertyData)
    */
   public void delete(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(DELETE_PROPERTY_DATA_DESCR);
      try
      {
         s.begin();
         wcs.delete(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getChildNodesCount(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public int getChildNodesCount(NodeData parent) throws RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(GET_CHILD_NODES_COUNT_DESCR);
      try
      {
         s.begin();
         return wcs.getChildNodesCount(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getChildNodesData(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_CHILD_NODES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.getChildNodesData(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getChildPropertiesData(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_CHILD_PROPERTIES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.getChildPropertiesData(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getItemData(org.exoplatform.services.jcr.datamodel.NodeData, org.exoplatform.services.jcr.datamodel.QPathEntry)
    */
   public ItemData getItemData(NodeData parentData, QPathEntry name) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR);
      try
      {
         s.begin();
         return wcs.getItemData(parentData, name);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getItemData(java.lang.String)
    */
   public ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_ITEM_DATA_BY_ID_DESCR);
      try
      {
         s.begin();
         return wcs.getItemData(identifier);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getReferencesData(java.lang.String)
    */
   public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException,
      IllegalStateException, UnsupportedOperationException
   {
      Statistics s = ALL_STATISTICS.get(GET_REFERENCES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.getReferencesData(nodeIdentifier);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#isOpened()
    */
   public boolean isOpened()
   {
      Statistics s = ALL_STATISTICS.get(IS_OPENED_DESCR);
      try
      {
         s.begin();
         return wcs.isOpened();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#listChildPropertiesData(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(LIST_CHILD_PROPERTIES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.listChildPropertiesData(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#rename(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void rename(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(RENAME_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.rename(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#rollback()
    */
   public void rollback() throws IllegalStateException, RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(ROLLBACK_DESCR);
      try
      {
         s.begin();
         wcs.rollback();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#update(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void update(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(UPDATE_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.update(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#update(org.exoplatform.services.jcr.datamodel.PropertyData)
    */
   public void update(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(UPDATE_PROPERTY_DATA_DESCR);
      try
      {
         s.begin();
         wcs.update(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * Print the header of the csv file
    */
   private static void printHeader()
   {
      GLOBAL_STATISTICS.printHeader(STATISTICS_WRITER);
      for (Statistics s : ALL_STATISTICS.values())
      {
         STATISTICS_WRITER.print(',');
         s.printHeader(STATISTICS_WRITER);
      }
      STATISTICS_WRITER.println();
      STATISTICS_WRITER.flush();
   }

   /**
    * Add one line of data
    */
   private static void printData()
   {
      GLOBAL_STATISTICS.printData(STATISTICS_WRITER);
      for (Statistics s : ALL_STATISTICS.values())
      {
         STATISTICS_WRITER.print(',');
         s.printData(STATISTICS_WRITER);
      }
      STATISTICS_WRITER.println();
      STATISTICS_WRITER.flush();
   }

   /**
    * The class used to manage all the metrics such as minimum, maximum, total, times and average.
    */
   private static class Statistics
   {

      /**
       * The description of the statistics
       */
      private final String description;

      /**
       * The min value of the time spent for one call
       */
      private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

      /**
       * The max value of the time spent for one call
       */
      private final AtomicLong max = new AtomicLong(-1);

      /**
       * The total time spent for all the calls
       */
      private final AtomicLong total = new AtomicLong();

      /**
       * The total amount of calls
       */
      private final AtomicLong times = new AtomicLong();

      /**
       * The {@link ThreadLocal} used to keep the initial timestamp
       */
      private final ThreadLocal<Queue<Long>> currentTime = new ThreadLocal<Queue<Long>>()
      {
         protected Queue<Long> initialValue()
         {
            return new LinkedList<Long>();
         }
      };

      /**
       * The default constructor
       * @param description the description of the statistics
       */
      public Statistics(String description)
      {
         this.description = description;
      }

      /**
       * Start recording
       */
      public void begin()
      {
         GLOBAL_STATISTICS.onBegin();
         onBegin();
      }

      /**
       * Store the current timestamp in the {@link ThreadLocal}
       */
      private void onBegin()
      {
         Queue<Long> q = currentTime.get();
         q.add(System.currentTimeMillis());
      }

      /**
       * Stop recording
       */
      public void end()
      {
         onEnd();
         GLOBAL_STATISTICS.onEnd();
      }

      /**
       * Refresh the values of the metrics (min, max, total and times)
       */
      private void onEnd()
      {
         long result = System.currentTimeMillis() - currentTime.get().poll();
         times.incrementAndGet();
         if (result < min.get())
         {
            min.set(result);
         }
         if (max.get() < result)
         {
            max.set(result);
         }
         total.addAndGet(result);
      }

      /**
       * Print the description of all the metrics into the given {@link PrintWriter}
       */
      public void printHeader(PrintWriter pw)
      {
         pw.print(description);
         pw.print("-Min,");
         pw.print(description);
         pw.print("-Max,");
         pw.print(description);
         pw.print("-Total,");
         pw.print(description);
         pw.print("-Avg,");
         pw.print(description);
         pw.print("-Times");
      }

      /**
       * Print the current snapshot of the metrics and evaluate the average value
       */
      public void printData(PrintWriter pw)
      {
         long lmin = min.get();
         if (lmin == Long.MAX_VALUE)
         {
            lmin = -1;
         }
         long lmax = max.get();
         long ltotal = total.get();
         long ltimes = times.get();
         float favg = ltimes == 0 ? 0f : (float)ltotal / ltimes;
         pw.print(lmin);
         pw.print(',');
         pw.print(lmax);
         pw.print(',');
         pw.print(ltotal);
         pw.print(',');
         pw.print(favg);
         pw.print(',');
         pw.print(ltimes);
      }
   }
}
