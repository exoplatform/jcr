/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.store.Directory;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.IndexInputStream;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.IndexOutputStream;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a redo log for changes that have not been committed to disk. While
 * nodes are added to and removed from the volatile index (held in memory) a
 * redo log is written to keep track of the changes. In case the Jackrabbit
 * process terminates unexpected the redo log is applied when Jackrabbit is
 * restarted the next time.
 * <br>
 * This class is not thread-safe.
 */
class RedoLog
{

   /**
    * Logger instance for this class
    */
   private static final Log     log           = ExoLogger.getLogger("exo.jcr.component.core.RedoLog");

   /**
    * Default name of the redo log file
    */
   private static final String REDO_LOG = "redo.log";

   /**
    * Implements a {@link ActionCollector} that counts all entries and sets
    * {@link #entryCount}.
    */
   private final ActionCollector ENTRY_COUNTER = new ActionCollector()
   {
      public void collect(MultiIndex.Action a)
      {
         entryCount++;
      }
   };

   /**
    * The directory where the log file is stored.
    */
   private final Directory dir;

   /**
    * The number of log entries in the log file
    */
   private int entryCount = 0;

   /**
    * Writer to the log file
    */
   private Writer out;

   /**
    * Creates a new <code>RedoLog</code> instance, which stores its log in the
    * given directory.
    *
    * @param dir the directory where the redo log file is located.
    * @throws IOException if an error occurs while reading the redo log.
    */
   RedoLog(Directory dir) throws IOException
   {
      this.dir = dir;
      read(ENTRY_COUNTER);
   }

   /**
    * Returns <code>true</code> if this redo log contains any entries,
    * <code>false</code> otherwise.
    * @return <code>true</code> if this redo log contains any entries,
    * <code>false</code> otherwise.
    */
   boolean hasEntries()
   {
      return entryCount > 0;
   }

   /**
    * Returns the number of entries in this redo log.
    * @return the number of entries in this redo log.
    */
   int getSize()
   {
      return entryCount;
   }

   /**
    * Returns a List with all {@link MultiIndex.Action} instances in the
    * redo log.
    *
    * @return an List with all {@link MultiIndex.Action} instances in the
    *         redo log.
    * @throws IOException if an error occurs while reading from the redo log.
    */
   List<MultiIndex.Action> getActions() throws IOException
   {
      final List<MultiIndex.Action> actions = new ArrayList<MultiIndex.Action>();
      read(new ActionCollector()
      {
         public void collect(MultiIndex.Action a)
         {
            actions.add(a);
         }
      });
      return actions;
   }

   /**
    * Appends an action to the log.
    *
    * @param action the action to append.
    * @throws IOException if the node cannot be written to the redo
    * log.
    */
   void append(final MultiIndex.Action action) throws IOException
   {
      initOut();
      out.write(action.toString() + "\n");
      entryCount++;
   }

   /**
    * Flushes all pending writes to the underlying file.
    * @throws IOException if an error occurs while writing.
    */
   void flush() throws IOException
   {
      if (out != null)
      {
         out.flush();
      }
   }

   /**
    * Clears the redo log.
    * @throws IOException if the redo log cannot be cleared.
    */
   synchronized void clear() throws IOException
   {
      if (out != null)
      {
         out.close();
         out = null;
      }
      try
      {
         if (dir.fileExists(REDO_LOG))
         {
            dir.deleteFile(REDO_LOG);
         }
      }
      catch (IOException e)
      {
         log.error(e.getLocalizedMessage(), e);
         throw e;
      }
      entryCount = 0;
   }

   /**
    * Initializes the {@link #out} stream if it is not yet set.
    * @throws IOException if an error occurs while creating the
    * output stream.
    */
   private synchronized void initOut() throws IOException
   {
      if (out == null)
      {
         OutputStream os = new IndexOutputStream(dir.createOutput(REDO_LOG));
         out = new BufferedWriter(new OutputStreamWriter(os));
      }
   }

   /**
    * Reads the log file and calls back {@link RedoLog.ActionCollector}.
    *
    * @param collector called back for each {@link MultiIndex.Action} read.
    * @throws IOException if an error occurs while reading from the
    * log file.
    */
   private void read(final ActionCollector collector) throws IOException
   {
      if (!dir.fileExists(REDO_LOG))
      {
         return;
      }
      InputStream in = new IndexInputStream(dir.openInput(REDO_LOG));
      BufferedReader reader = null;
      try
      {
         reader = new BufferedReader(new InputStreamReader(in));
         String line;
         while ((line = reader.readLine()) != null)
         {
            try
            {
               collector.collect(MultiIndex.Action.fromString(line));
            }
            catch (IllegalArgumentException e)
            {
               log.warn("Malformed redo entry: " + e.getMessage());
            }
         }
      }
      finally
      {
         if (reader != null)
         {
            try
            {
               reader.close();
            }
            catch (IOException e)
            {
               log.warn("Exception while closing redo log: " + e.toString());
            }
         }
         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               log.warn("Exception while closing redo log: " + e.toString());
            }
         }
      }
   }

   //-----------------------< internal >---------------------------------------

   /**
    * Helper interface to collect Actions read from the redo log.
    */
   interface ActionCollector
   {

      /** Called when an action is created */
      void collect(MultiIndex.Action action);
   }
}
