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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a ScoreNodeIterator that returns the score nodes in document order.
 */
class DocOrderScoreNodeIterator implements ScoreNodeIterator {

    /** Logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(DocOrderScoreNodeIterator.class);

    /** A node iterator with ordered nodes */
    private ScoreNodeIterator orderedNodes;

    /** Unordered list of {@link ScoreNode}[]s. */
    private final List scoreNodes;

    /** ItemManager to turn UUIDs into Node instances */
    protected final ItemDataConsumer itemMgr;

    /**
     * Apply document order on the score nodes with this selectorIndex.
     */
    private final int selectorIndex;

    /**
     * Creates a <code>DocOrderScoreNodeIterator</code> that orders the nodes in
     * <code>scoreNodes</code> in document order.
     *
     * @param itemMgr       the item manager of the session executing the
     *                      query.
     * @param scoreNodes    the ids of the matching nodes with their score
     *                      value. <code>List&lt;ScoreNode[]></code>
     * @param selectorIndex apply document order on the score nodes with this
     *                      selectorIndex.
     */
    DocOrderScoreNodeIterator(ItemDataConsumer itemMgr,
                             List scoreNodes,
                             int selectorIndex) {
        this.itemMgr = itemMgr;
        this.scoreNodes = scoreNodes;
        this.selectorIndex = selectorIndex;
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextScoreNodes();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[] nextScoreNodes() {
        initOrderedIterator();
        return orderedNodes.nextScoreNodes();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        initOrderedIterator();
        orderedNodes.skip(skipNum);
    }

    /**
     * Returns the number of nodes in this iterator.
     * </p>
     * Note: The number returned by this method may differ from the number
     * of nodes actually returned by calls to hasNext() / getNextNode()! This
     * is because this iterator works on a lazy instantiation basis and while
     * iterating over the nodes some of them might have been deleted in the
     * meantime. Those will not be returned by getNextNode(). As soon as an
     * invalid node is detected, the size of this iterator is adjusted.
     *
     * @return the number of node in this iterator.
     */
    public long getSize() {
        if (orderedNodes != null) {
            return orderedNodes.getSize();
        } else {
            return scoreNodes.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        initOrderedIterator();
        return orderedNodes.getPosition();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        initOrderedIterator();
        return orderedNodes.hasNext();
    }

    //------------------------< internal >--------------------------------------

    /**
     * Initializes the NodeIterator in document order
     */
    private void initOrderedIterator() {
        if (orderedNodes != null) {
            return;
        }
        long time = System.currentTimeMillis();
        ScoreNode[][] nodes = (ScoreNode[][]) scoreNodes.toArray(new ScoreNode[scoreNodes.size()][]);

        final Set<String> invalidIDs = new HashSet<String>(2);

        /** Cache for Nodes obtainer during the order (comparator work) */
        final Map<String, NodeData> lcache = new HashMap<String, NodeData>();
        do {
            if (invalidIDs.size() > 0) {
                // previous sort run was not successful -> remove failed uuids
                List tmp = new ArrayList();
                for (int i = 0; i < nodes.length; i++) {
                    if (!invalidIDs.contains(nodes[i][selectorIndex].getNodeId())) {
                        tmp.add(nodes[i]);
                    }
                }
                nodes = (ScoreNode[][]) tmp.toArray(new ScoreNode[tmp.size()][]);
                invalidIDs.clear();
            }

            try {
                // sort the uuids
               Arrays.sort(nodes, new ScoreNodeComparator(lcache,invalidIDs));
            } catch (SortFailedException e) {
                // retry
            }

        } while (invalidIDs.size() > 0);

        if (log.isDebugEnabled()) {
            log.debug("" + nodes.length + " node(s) ordered in " + (System.currentTimeMillis() - time) + " ms");
        }
        
        
        orderedNodes = new ScoreNodeIteratorImpl(nodes);
    }
    private class ScoreNodeComparator implements Comparator<ScoreNode[]>
    {
       private final Map<String, NodeData> lcache;

       private final Set<String> invalidIDs;

       public ScoreNodeComparator(Map<String, NodeData> lcache, Set<String> invalidIDs)
       {
          super();
          this.lcache = lcache;
          this.invalidIDs = invalidIDs;
       }

       private NodeData getNode(String id) throws RepositoryException
       {
          NodeData node = lcache.get(id);
          if (node == null)
          {
             node = (NodeData)itemMgr.getItemData(id);
             if (node != null)
                lcache.put(id, node);

          }
          return node;
       }

       public int compare(final ScoreNode[] nodes1, final ScoreNode[] nodes2)
       {
          ScoreNode n1 = nodes1[selectorIndex];
          ScoreNode n2 = nodes2[selectorIndex];
          try
          {
             NodeData ndata1;
             try
             {
                ndata1 = getNode(n1.getNodeId());
                if (ndata1 == null)
                   throw new RepositoryException("Node not found for " + n1.getNodeId());
             }
             catch (RepositoryException e)
             {
                // log.warn("Node " + n1.identifier + " does not exist anymore:
                // " + e);
                // node does not exist anymore
                invalidIDs.add(n1.getNodeId());
                throw new SortFailedException();
             }

             NodeData ndata2;
             try
             {
                ndata2 = getNode(n2.getNodeId());
                if (ndata2 == null)
                   throw new RepositoryException("Node not found for " + n2.getNodeId());
             }
             catch (RepositoryException e)
             {
                // log.warn("Node " + n2.identifier + " does not exist anymore:
                // " + e);
                // node does not exist anymore
                invalidIDs.add(n2.getNodeId());
                throw new SortFailedException();
             }

             QPath path1 = ndata1.getQPath();
             QPath path2 = ndata2.getQPath();

             QPathEntry[] pentries1 = path1.getEntries();
             QPathEntry[] pentries2 = path2.getEntries();

             // find nearest common ancestor
             int commonDepth = 0; // root
             while (pentries1.length > commonDepth && pentries2.length > commonDepth)
             {
                if (pentries1[commonDepth].equals(pentries2[commonDepth]))
                {
                   commonDepth++;
                }
                else
                {
                   break;
                }
             }

             // path elements at last depth were equal
             commonDepth--;

             // check if either path is an ancestor of the other
             if (pentries1.length - 1 == commonDepth)
             {
                // path1 itself is ancestor of path2
                return -1;
             }

             if (pentries2.length - 1 == commonDepth)
             {
                // path2 itself is ancestor of path1
                return 1;
             }

             return ndata1.getOrderNumber() - ndata2.getOrderNumber();
          }
          catch (SortFailedException e)
          {
             throw e;
          }
          catch (Exception e)
          {
             log.error("Exception while sorting nodes in document order: " + e.toString(), e);
          }

          // if we get here something went wrong
          // remove both identifiers from array
          if (n1 != null)
             invalidIDs.add(n1.getNodeId());
          else
             log.warn("Null ScoreNode n1 will not be added into invalid identifiers set");
          if (n2 != null)
             invalidIDs.add(n2.getNodeId());
          else
             log.warn("Null ScoreNode n2 will not be added into invalid identifiers set");

          // terminate sorting
          throw new SortFailedException();
       }
    }
    /**
     * Indicates that sorting failed.
     */
    private static final class SortFailedException extends RuntimeException {
    }
}
