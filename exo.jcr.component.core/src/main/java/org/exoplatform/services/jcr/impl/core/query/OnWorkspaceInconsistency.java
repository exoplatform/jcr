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

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>OnWorkspaceInconsistency</code> defines an interface to handle
 * workspace inconsistencies.
 */
public abstract class OnWorkspaceInconsistency
{

   /**
    * Logger instance for this class.
    */
   private static final Logger log = LoggerFactory.getLogger("exo.jcr.component.core.OnWorkspaceInconsistency");

   /**
    * An handler that simply logs the path of the parent node and the name
    * of the missing child node and then re-throws the exception.
    */
   public static final OnWorkspaceInconsistency FAIL = new OnWorkspaceInconsistency("fail")
   {

      public void handleMissingChildNode(ItemNotFoundException exception, QueryHandler handler, QPath path,
         NodeData node, NodeData child) throws RepositoryException
      {
         //NamePathResolver resolver = new DefaultNamePathResolver(handler.getContext().getNamespaceRegistry());
         log.error("TO DO ");
         //         log.error("Node {} ({}) has missing child '{}' ({})", new Object[]{resolver.getJCRPath(path),
         //            node.getNodeId().getUUID().toString(), resolver.getJCRName(child.getName()),
         //            child.getId().getUUID().toString()});

         throw exception;
      }
   };

   protected static final Map<String, OnWorkspaceInconsistency> INSTANCES =
      new HashMap<String, OnWorkspaceInconsistency>();

   static
   {
      INSTANCES.put(FAIL.name, FAIL);
   }

   /**
    * The name of the {@link OnWorkspaceInconsistency} handler.
    */
   private final String name;

   /**
    * Protected constructor.
    */
   protected OnWorkspaceInconsistency(String name)
   {
      this.name = name;
   }

   /**
    * @return the name of this {@link OnWorkspaceInconsistency}.
    */
   public String getName()
   {
      return name;
   }

   /**
    * Returns the {@link OnWorkspaceInconsistency} with the given
    * <code>name</code>.
    *
    * @param name the name of a {@link OnWorkspaceInconsistency}.
    * @return the {@link OnWorkspaceInconsistency} with the given
    *         <code>name</code>.
    * @throws IllegalArgumentException if <code>name</code> is not a well-known
    *                                  {@link OnWorkspaceInconsistency} name.
    */
   public static OnWorkspaceInconsistency fromString(String name) throws IllegalArgumentException
   {
      OnWorkspaceInconsistency handler = INSTANCES.get(name.toLowerCase());
      if (handler == null)
      {
         throw new IllegalArgumentException("Unknown name: " + name);
      }
      else
      {
         return handler;
      }
   }

   /**
    * Handle a missing child node state.
    *
    * @param exception the exception that was thrown when the query handler
    *                  tried to load the child node state.
    * @param handler   the query handler.
    * @param path      the path of the parent node.
    * @param node      the parent node state.
    * @param child     the child node entry, for which no node state could be
    *                  found.
    * @throws RepositoryException if another error occurs not related to item
    *                             state reading.
    */
   public abstract void handleMissingChildNode(ItemNotFoundException exception, QueryHandler handler, QPath path,
      NodeData node, NodeData child) throws RepositoryException;
}
