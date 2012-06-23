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
package org.exoplatform.services.jcr.impl.core.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is used to manage the livecycle of the corresponding {@link IndexerIoMode}  
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class IndexerIoModeHandler
{

   /**
    * The logger instance for this class
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.IndexerIoModeHandler");

   /**
    * The current mode
    */
   private volatile IndexerIoMode mode;

   /**
    * The list of all the listeners
    */
   private final List<IndexerIoModeListener> listeners;

   /**
    * Initialize the mode
    * @param mode the initial value of the mode
    */
   public IndexerIoModeHandler(IndexerIoMode mode)
   {
      log.info("Indexer io mode=" + mode);
      this.mode = mode;
      this.listeners = new CopyOnWriteArrayList<IndexerIoModeListener>();
   }

   /**
    * @return the current mode of the indexer
    */
   public IndexerIoMode getMode()
   {
      return mode;
   }

   /**
    * Changes the current mode of the indexer. If the value has changes all the listeners
    * will be notified
    */
   public synchronized void setMode(IndexerIoMode mode)
   {
      if (this.mode != mode)
      {
         log.info("Indexer io mode=" + mode);
         this.mode = mode;
         for (IndexerIoModeListener listener : listeners)
         {
            listener.onChangeMode(mode);
         }
      }
   }

   /**
    * Add a new IndexerIoModeListener to the list of listeners
    * @param listener the listener to add
    */
   public void addIndexerIoModeListener(IndexerIoModeListener listener)
   {
      listeners.add(listener);
   }
   
   /**
    * Removes IndexerIoModeListener from the list of listeners
    * @param listener
    */
   public void removeIndexerIoModeListener(IndexerIoModeListener listener)
   {
      listeners.remove(listener);
   }
}
