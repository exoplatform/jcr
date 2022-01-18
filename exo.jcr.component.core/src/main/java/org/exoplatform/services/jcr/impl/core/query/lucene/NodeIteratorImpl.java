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

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * Implements a {@link javax.jcr.NodeIterator} returned by
 * {@link javax.jcr.query.QueryResult#getNodes()}.
 */
class NodeIteratorImpl implements TwoWayRangeIterator, NodeIterator
{

   /** Logger instance for this class */
   private static final Log          log     = ExoLogger.getLogger("exo.jcr.component.core.NodeIteratorImpl");

   /** The node ids of the nodes in the result set with their score value */
   protected final ScoreNodeIterator scoreNodes;

   /** The index for the default selector withing {@link #scoreNodes} */
   private final int selectorIndex;

   /** ItemManager to turn UUIDs into Node instances */
   protected final SessionDataManager itemMgr;

   /** Number of invalid nodes */
   protected int invalid = 0;

   /** Reference to the next node instance */
   private NodeImpl next;

   /**
    * Whether this iterator had been initialized.
    */
   private boolean initialized;

   /**
    * Creates a new <code>NodeIteratorImpl</code> instance.
    *
    * @param itemMgr       the <code>ItemManager</code> to turn UUIDs into
    *                      <code>Node</code> instances.
    * @param scoreNodes    iterator over score nodes.
    * @param selectorIndex the index for the default selector within
    *                      <code>scoreNodes</code>.
    */
   NodeIteratorImpl(SessionDataManager itemMgr, ScoreNodeIterator scoreNodes, int selectorIndex)
   {
      this.itemMgr = itemMgr;
      this.scoreNodes = scoreNodes;
      this.selectorIndex = selectorIndex;
   }

   /**
    * Returns the current position in this <code>NodeIterator</code>.
    * @return the current position in this <code>NodeIterator</code>.
    */
   public long getPosition()
   {
      initialize();
      long position = scoreNodes.getPosition() - invalid;
      // scoreNode.getPosition() is one ahead
      // if there is a prefetched node
      if (next != null)
      {
         position--;
      }
      return position;
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
   public long getSize()
   {
      long size = scoreNodes.getSize();
      if (size == -1)
      {
         return size;
      }
      else
      {
         return size - invalid;
      }
   }

   /**
    * Returns <code>true</code> if there is another <code>Node</code>
    * available; <code>false</code> otherwise.
    * @return <code>true</code> if there is another <code>Node</code>
    *  available; <code>false</code> otherwise.
    */
   public boolean hasNext()
   {
      initialize();
      return next != null;
   }

   /**
    * Returns the next <code>Node</code> in the result set.
    * @return the next <code>Node</code> in the result set.
    * @throws NoSuchElementException if iteration has no more
    *   <code>Node</code>s.
    */
   public Object next() throws NoSuchElementException
   {
      initialize();
      return nextNode();
   }

   /**
    * Returns the next <code>Node</code> in the result set.
    * @return the next <code>Node</code> in the result set.
    * @throws NoSuchElementException if iteration has no more
    *   <code>Node</code>s.
    */
   public Node nextNode() throws NoSuchElementException
   {
      initialize();
      if (next == null)
      {
         throw new NoSuchElementException();
      }
      NodeImpl n = next;
      fetchNext();
      return n;
   }

   /**
    * @throws UnsupportedOperationException always.
    */
   public void remove()
   {
      throw new UnsupportedOperationException("remove");
   }

   /**
    * Skip a number of <code>Node</code>s in this iterator.
    * @param skipNum the non-negative number of <code>Node</code>s to skip
    * @throws NoSuchElementException
    *          if skipped past the last <code>Node</code> in this iterator.
    */
   public void skip(long skipNum) throws NoSuchElementException
   {
      initialize();
      if (skipNum > 0)
      {
         scoreNodes.skip(skipNum - 1);
         fetchNext();
      }
   }

   public void skipBack(long skipNum)
   {
      initialize();
      if (skipNum < 0)
      {
         throw new IllegalArgumentException("skipNum must not be negative");
      }
      scoreNodes.skipBack(skipNum + 1);
      fetchNext();

   }

   /**
    * Clears {@link #next} and tries to fetch the next Node instance.
    * When this method returns {@link #next} refers to the next available
    * node instance in this iterator. If {@link #next} is null when this
    * method returns, then there are no more valid element in this iterator.
    */
   protected void fetchNext()
   {
      // reset
      next = null;
      while (next == null && scoreNodes.hasNext())
      {
         ScoreNode[] sn = scoreNodes.nextScoreNodes();
         try
         {
            next = (NodeImpl)itemMgr.getItemByIdentifier(sn[selectorIndex].getNodeId(), true);
            if (next == null)
            {
               invalid++;
            }
         }
         catch (RepositoryException e)
         {
            log.warn("Exception retrieving Node with UUID: " + sn[selectorIndex].getNodeId() + ": " + e.toString());
            // try next
            invalid++;
         }
      }
   }

   protected void initialize()
   {
      if (!initialized)
      {
         fetchNext();
         initialized = true;
      }
   }
}
