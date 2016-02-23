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

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;

/**
 * <code>AggregateRule</code> defines a configuration for a node index
 * aggregate. It defines rules for items that should be included in the node
 * scope index of an ancestor. Per default the values of properties are only
 * added to the node scope index of the parent node.
 */
public interface AggregateRule {

    /**
     * Returns root node state for the indexing aggregate where
     * <code>nodeState</code> belongs to.
     *
     * @param nodeState
     * @return the root node state of the indexing aggregate or
     *         <code>null</code> if <code>nodeState</code> does not belong to an
     *         indexing aggregate.
     * @throws RepositoryException if an error occurs.
     */
   NodeData getAggregateRoot(NodeData nodeState)
            throws  RepositoryException;

    /**
     * Returns the node states that are part of the indexing aggregate of the
     * <code>nodeState</code>.
     *
     * @param nodeState a node state
     * @return the node states that are part of the indexing aggregate of
     *         <code>nodeState</code>. Returns <code>null</code> if this
     *         aggregate does not apply to <code>nodeState</code>.
     * @throws RepositoryException if an error occurs.
     */
   NodeData[] getAggregatedNodeStates(NodeData nodeState)
            throws RepositoryException;

    /**
     * Returns the property states that are part of the indexing aggregate of
     * the <code>nodeState</code>.
     *
     * @param nodeState a node state
     * @return the property states that are part of the indexing aggregate of
     *         <code>nodeState</code>. Returns <code>null</code> if this
     *         aggregate does not apply to <code>nodeState</code>.
     * @throws RepositoryException if an error occurs.
     */
    public PropertyData[] getAggregatedPropertyStates(NodeData nodeState)
            throws RepositoryException;
}
