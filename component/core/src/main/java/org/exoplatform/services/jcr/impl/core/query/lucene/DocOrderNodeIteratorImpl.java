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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.log.ExoLogger;

/**
 * Implements a NodeIterator that returns the nodes in document order.
 */
class DocOrderNodeIteratorImpl implements TwoWayRangeIterator, ScoreNodeIterator
{

   /** Logger instance for this class */
   private static final Log log = ExoLogger.getLogger(DocOrderNodeIteratorImpl.class);

   /** A node iterator with ordered nodes */
   private NodeIteratorImpl orderedNodes;

   /** Unordered list of {@link ScoreNode}s. */
   private final List<ScoreNode> scoreNodes;

   /** ItemManager to turn UUIDs into Node instances */
   protected final SessionDataManager itemMgr;

   private final AccessManager accessManager;

   private final String userId;

   /**
    * Creates a <code>DocOrderNodeIteratorImpl</code> that orders the nodes in
    * <code>scoreNodes</code> in document order.
    * 
    * @param itemMgr
    *          the item manager of the session executing the query.
    * @param scoreNodes
    *          the ids of the matching nodes with their score value.
    */
   DocOrderNodeIteratorImpl(final SessionDataManager itemMgr, AccessManager accessManager, String userId,
      List<ScoreNode> scoreNodes)
   {
      this.itemMgr = itemMgr;
      this.accessManager = accessManager;
      this.userId = userId;
      this.scoreNodes = scoreNodes;
   }

   /**
    * {@inheritDoc}
    */
   public Object next()
   {
      return nextNodeImpl();
   }

   /**
    * {@inheritDoc}
    */
   public Node nextNode()
   {
      return nextNodeImpl();
   }

   /**
    * {@inheritDoc}
    */
   public NodeImpl nextNodeImpl()
   {
      initOrderedIterator();
      return orderedNodes.nextNodeImpl();
   }

   /**
    * @throws UnsupportedOperationException
    *           always.
    */
   public void remove()
   {
      throw new UnsupportedOperationException("remove");
   }

   /**
    * {@inheritDoc}
    */
   public void skip(long skipNum)
   {
      initOrderedIterator();
      orderedNodes.skip(skipNum);
   }

   public void skipBack(long skipNum)
   {
      initOrderedIterator();
      orderedNodes.skipBack(skipNum);
   }

   /**
    * Returns the number of nodes in this iterator. </p> Note: The number returned by this method may
    * differ from the number of nodes actually returned by calls to hasNext() / getNextNode()! This
    * is because this iterator works on a lazy instantiation basis and while iterating over the nodes
    * some of them might have been deleted in the meantime. Those will not be returned by
    * getNextNode(). As soon as an invalid node is detected, the size of this iterator is adjusted.
    * 
    * @return the number of node in this iterator.
    */
   public long getSize()
   {
      if (orderedNodes != null)
      {
         return orderedNodes.getSize();
      }
      else
      {
         return scoreNodes.size();
      }
   }

   /**
    * {@inheritDoc}
    */
   public long getPosition()
   {
      initOrderedIterator();
      return orderedNodes.getPosition();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNext()
   {
      initOrderedIterator();
      return orderedNodes.hasNext();
   }

   /**
    * {@inheritDoc}
    */
   public float getScore()
   {
      initOrderedIterator();
      return orderedNodes.getScore();
   }

   // ------------------------< internal >--------------------------------------

   /**
    * Initializes the NodeIterator in document order
    */
   private void initOrderedIterator()
   {
      if (orderedNodes != null)
      {
         return;
      }
      long time = System.currentTimeMillis();

      ScoreNode[] nodes = scoreNodes.toArray(new ScoreNode[scoreNodes.size()]);

      final Set<String> invalidIDs = new HashSet<String>(2);

      /** Cache for Nodes obtainer during the order (comparator work) */
      final Map<String, NodeData> lcache = new HashMap<String, NodeData>();

      do
      {
         if (invalidIDs.size() > 0)
         {
            // previous sort run was not successful -> remove failed uuids
            List<ScoreNode> tmp = new ArrayList<ScoreNode>();
            for (int i = 0; i < nodes.length; i++)
            {
               if (!invalidIDs.contains(nodes[i].getNodeId()))
               {
                  tmp.add(nodes[i]);
               }
               else
               {
                  lcache.remove(nodes[i].getNodeId());
               }
            }
            nodes = tmp.toArray(new ScoreNode[tmp.size()]);

            invalidIDs.clear();
         }

         try
         {
            // sort the identifiers
            Arrays.sort(nodes, new Comparator<ScoreNode>()
            {

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

               public int compare(final ScoreNode n1, final ScoreNode n2)
               {
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
            });
         }
         catch (SortFailedException e)
         {
            // retry
         }
      }
      while (invalidIDs.size() > 0);

      if (log.isDebugEnabled())
      {
         log.debug("" + nodes.length + " node(s) ordered in " + (System.currentTimeMillis() - time) + " ms");
      }
      orderedNodes = new NodeIteratorImpl(itemMgr, nodes);
   }

   /**
    * Indicates that sorting failed.
    */
   private static final class SortFailedException extends RuntimeException
   {
   }
}
