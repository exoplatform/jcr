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
package org.exoplatform.services.jcr.statistics;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.management.ManagementContext;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class manages all the statistics of eXo JCR. This will print all the metrics value into a file in csv format 
 * for all the registered statistics. It will provide metrics of type minimum, maximum, total, times and average 
 * for each method of {@link WorkspaceStorageConnection} and the global values. It will add data into the file 
 * every 5 seconds and add the last line at JVM exit. This class will also expose all the statistics through JMX.
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 30 mars 2010  
 */
@Managed
@ManagedDescription("JCR statistics manager")
@NameTemplate({@Property(key = "view", value = "jcr"), @Property(key = "service", value = "statistic")})
@RESTEndpoint(path = "jcrstatistics")
public class JCRStatisticsManager
{

   /**
    * The logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JCRStatisticsManager");

   /**
    * The name of the global statistics
    */
   private static final String GLOBAL_STATISTICS_NAME = "global";

   /**
    * The flag that indicates if the manager is launched or not.
    */
   private static volatile boolean STARTED;

   /**
    * The list of all the contexts of statistics managed
    */
   private static Map<String, StatisticsContext> CONTEXTS =
      Collections.unmodifiableMap(new HashMap<String, StatisticsContext>());

   /**
    * Indicates if the persistence of the statistics has to be enabled.
    */
   public static final boolean PERSISTENCE_ENABLED =
      Boolean.valueOf(PrivilegedSystemHelper.getProperty("JCRStatisticsManager.persistence.enabled", "true"));

   /**
    * The length of time in milliseconds after which the snapshot of the statistics is persisted.
    */
   public static final long PERSISTENCE_TIMEOUT =
      Long.valueOf(PrivilegedSystemHelper.getProperty("JCRStatisticsManager.persistence.timeout", "5000"));

   /**
    * Default constructor.
    */
   private JCRStatisticsManager()
   {
   }

   /**
    * Register a new category of statistics to manage.
    * @param category the name of the category of statistics to register.
    * @param global the global statistics.
    * @param allStatistics the list of all statistics corresponding to the category.
    */
   public static void registerStatistics(String category, Statistics global, Map<String, Statistics> allStatistics)
   {
      if (category == null || category.length() == 0)
      {
         throw new IllegalArgumentException("The category of the statistics cannot be empty");
      }
      if (allStatistics == null || allStatistics.isEmpty())
      {
         throw new IllegalArgumentException("The list of statistics " + category + " cannot be empty");
      }
      PrintWriter pw = null;
      if (PERSISTENCE_ENABLED)
      {
         pw = initWriter(category);
         if (pw == null)
         {
            LOG.warn("Cannot create the print writer for the statistics " + category);
         }
      }
      startIfNeeded();
      synchronized (JCRStatisticsManager.class)
      {
         Map<String, StatisticsContext> tmpContexts = new HashMap<String, StatisticsContext>(CONTEXTS);
         StatisticsContext ctx = new StatisticsContext(pw, global, allStatistics);
         tmpContexts.put(category, ctx);
         if (pw != null)
         {
            // Define the file header
            printHeader(ctx);
         }
         CONTEXTS = Collections.unmodifiableMap(tmpContexts);
      }
   }

   /**
    * Initialize the {@link PrintWriter}.
    * It will first try to create the file in the user directory, if it cannot, it will try
    * to create it in the temporary folder.
    * @return the corresponding {@link PrintWriter}
    */
   private static PrintWriter initWriter(String category)
   {
      PrintWriter pw = null;
      File file = null;
      try
      {
         file =
            new File(PrivilegedSystemHelper.getProperty("user.dir"), "Statistics" + category + "-"
               + System.currentTimeMillis() + ".csv");
         file.createNewFile();
         pw = new PrintWriter(file);
      }
      catch (IOException e)
      {
         LOG.error("Cannot create the file for the statistics " + category
            + " in the user directory, we will try to create it in the temp directory", e);
         try
         {
            file =
               PrivilegedFileHelper.createTempFile("Statistics" + category, "-" + System.currentTimeMillis() + ".csv");
            pw = new PrintWriter(file);
         }
         catch (IOException e1)
         {
            LOG.error("Cannot create the file for the statistics " + category, e1);
         }
      }
      if (file != null)
      {
         LOG.info("The file for the statistics " + category + " is " + file.getPath());
      }
      return pw;
   }

   /**
    * Starts the manager if needed
    */
   private static void startIfNeeded()
   {
      if (!STARTED)
      {
         synchronized (JCRStatisticsManager.class)
         {
            if (!STARTED)
            {
               addTriggers();
               ExoContainer container = ExoContainerContext.getTopContainer();
               ManagementContext ctx = null;
               if (container != null)
               {
                  ctx = container.getManagementContext();
               }
               if (ctx == null)
               {
                  LOG.warn("Cannot register the statistics");
               }
               else
               {
                  ctx.register(new JCRStatisticsManager());
               }
               STARTED = true;
            }
         }
      }
   }

   /**
    * Add all the triggers that will keep the file up to date only if the persistence
    * is enabled.
    */
   private static void addTriggers()
   {
      if (!PERSISTENCE_ENABLED)
      {
         return;
      }
      Runtime.getRuntime().addShutdownHook(new Thread("JCRStatisticsManager-Hook")
      {
         @Override
         public void run()
         {
            printData();
         }
      });
      Thread t = new Thread("JCRStatisticsManager-Writer")
      {
         @Override
         public void run()
         {
            while (true)
            {
               try
               {
                  sleep(PERSISTENCE_TIMEOUT);
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
    * Print the header of the csv file related to the given context.
    */
   private static void printHeader(StatisticsContext context)
   {
      if (context.writer == null)
      {
         return;
      }
      boolean first = true;
      if (context.global != null)
      {
         context.global.printHeader(context.writer);
         first = false;
      }
      for (Statistics s : context.allStatistics.values())
      {
         if (first)
         {
            first = false;
         }
         else
         {
            context.writer.print(',');
         }
         s.printHeader(context.writer);
      }
      context.writer.println();
      context.writer.flush();
   }

   /**
    * Add one line of data to all the csv files.
    */
   private static void printData()
   {
      Map<String, StatisticsContext> tmpContexts = CONTEXTS;
      for (StatisticsContext context : tmpContexts.values())
      {
         printData(context);
      }
   }

   /**
    * Add one line of data to the csv file related to the given context.
    */
   private static void printData(StatisticsContext context)
   {
      if (context.writer == null)
      {
         return;
      }
      boolean first = true;
      if (context.global != null)
      {
         context.global.printData(context.writer);
         first = false;
      }
      for (Statistics s : context.allStatistics.values())
      {
         if (first)
         {
            first = false;
         }
         else
         {
            context.writer.print(',');
         }
         s.printData(context.writer);
      }
      context.writer.println();
      context.writer.flush();
   }

   /**
    * Retrieve statistics context of the given category.
    * @return the related {@link StatisticsContext}, <code>null</code> otherwise.
    */
   private static StatisticsContext getContext(String category)
   {
      if (category == null)
      {
         return null;
      }
      return CONTEXTS.get(category);
   }

   /**
    * Format the name of the statistics in the target format
    * @param name the name of the statistics requested
    * @return the formated statistics name
    */
   static String formatName(String name)
   {
      return name == null ? null : name.replaceAll(" ", "").replaceAll("[,;]", ", ");
   }

   /**
    * Retrieve statistics of the given category and name.
    * @return the related {@link Statistics}, <code>null</code> otherwise.
    */
   private static Statistics getStatistics(String category, String name)
   {
      StatisticsContext context = getContext(category);
      if (context == null)
      {
         return null;
      }
      // Format the name
      name = formatName(name);
      if (name == null)
      {
         return null;
      }
      Statistics statistics;
      if (GLOBAL_STATISTICS_NAME.equalsIgnoreCase(name))
      {
         statistics = context.global;
      }
      else
      {
         statistics = context.allStatistics.get(name);
      }
      return statistics;
   }

   /**
    * @return the <code>min</code> value for the statistics corresponding to the given
    * category and name.
    */
   @Managed
   @ManagedDescription("The minimum value of the time spent for one call.")
   public static long getMin(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category,
      @ManagedDescription("The name of the expected method or global for the global value") 
      @ManagedName("statisticsName") String name)
   {
      Statistics statistics = getStatistics(category, name);
      return statistics == null ? 0l : statistics.getMin();
   }

   /**
    * @return the <code>max</code> value for the statistics corresponding to the given
    * category and name.
    */
   @Managed
   @ManagedDescription("The maximum value of the time spent for one call.")
   public static long getMax(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category,
      @ManagedDescription("The name of the expected method or global for the global value") 
      @ManagedName("statisticsName") String name)
   {
      Statistics statistics = getStatistics(category, name);
      return statistics == null ? 0l : statistics.getMax();
   }

   /**
    * @return the <code>total</code> value for the statistics corresponding to the given
    * category and name.
    */
   @Managed
   @ManagedDescription("The total time spent for all the calls.")
   public static long getTotal(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category,
      @ManagedDescription("The name of the expected method or global for the global value") 
      @ManagedName("statisticsName") String name)
   {
      Statistics statistics = getStatistics(category, name);
      return statistics == null ? 0l : statistics.getTotal();
   }

   /**
    * @return the <code>times</code> value for the statistics corresponding to the given
    * category and name.
    */
   @Managed
   @ManagedDescription("The total amount of calls.")
   public static long getTimes(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category,
      @ManagedDescription("The name of the expected method or global for the global value") 
      @ManagedName("statisticsName") String name)
   {
      Statistics statistics = getStatistics(category, name);
      return statistics == null ? 0l : statistics.getTimes();
   }

   /**
    * @return the <code>avg</code> value for the statistics corresponding to the given
    * category and name.
    */
   @Managed
   @ManagedDescription("The average value of the time spent for one call.")
   public static float getAvg(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category,
      @ManagedDescription("The name of the expected method or global for the global value") 
      @ManagedName("statisticsName") String name)
   {
      Statistics statistics = getStatistics(category, name);
      return statistics == null ? 0l : statistics.getAvg();
   }

   /**
    * Allows to reset the statistics corresponding to the given category and name.
    * @param category
    * @param name
    */
   @Managed
   @ManagedDescription("Reset the statistics.")
   public static void reset(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category,
      @ManagedDescription("The name of the expected method or global for the global value") 
      @ManagedName("statisticsName") String name)
   {
      Statistics statistics = getStatistics(category, name);
      if (statistics != null)
      {
         statistics.reset();
      }
   }

   /**
    * Allows to reset all the statistics corresponding to the given category.
    * @param category
    */
   @Managed
   @ManagedDescription("Reset all the statistics.")
   public static void resetAll(
      @ManagedDescription("The name of the category of the statistics") 
      @ManagedName("categoryName") String category)
   {
      StatisticsContext context = getContext(category);
      if (context != null)
      {
         context.reset();
      }
   }

   /**
    * Define the context of a given category of statistics
    */
   private static class StatisticsContext
   {

      /**
       * The printer used to print the statistics in csv format
       */
      private final PrintWriter writer;

      /**
       * The list of all the statistics
       */
      private final Map<String, Statistics> allStatistics;

      /**
       * The global statistics
       */
      private final Statistics global;

      /**
       * The default constructor.
       */
      public StatisticsContext(PrintWriter writer, Statistics global, Map<String, Statistics> allStatistics)
      {
         this.writer = writer;
         this.global = global;
         this.allStatistics = allStatistics;
      }

      /**
       * Reset all the statistics related to the given context.
       */
      public void reset()
      {
         if (global != null)
         {
            global.reset();
         }
         for (Statistics statistics : allStatistics.values())
         {
            statistics.reset();
         }
      }
   }
}
