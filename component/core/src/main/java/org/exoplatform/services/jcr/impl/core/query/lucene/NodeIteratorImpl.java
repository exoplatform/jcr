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

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;

import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionDataManager;
import org.exoplatform.services.log.ExoLogger;

/**
 * Implements a {@link javax.jcr.NodeIterator} returned by
 * {@link javax.jcr.query.QueryResult#getNodes()}.
 */
class NodeIteratorImpl implements TwoWayRangeIterator, ScoreNodeIterator
{

   /** Logger instance for this class */
   private static final Log log = ExoLogger.getLogger(NodeIteratorImpl.class);

   /** The node ids of the nodes in the result set with their score value */
   protected final ScoreNode[] scoreNodes;

   /** ItemManager to turn UUIDs into Node instances */
   protected final SessionDataManager itemMgr;

   /** Current position in the UUID array */
   protected int pos = -1;

   /** Number of invalid nodes */
   protected int invalid = 0;

   /** Reference to the next node instance */
   private NodeImpl next;

   /**
    * Creates a new <code>NodeIteratorImpl</code> instance.
    * 
    * @param itemMgr
    *          the <code>ItemManager</code> to turn UUIDs into <code>Node</code> instances.
    * @param scoreNodes
    *          the node ids of the matching nodes.
    */
   NodeIteratorImpl(SessionDataManager itemMgr, ScoreNode[] scoreNodes)
   {
      this.itemMgr = itemMgr;
      this.scoreNodes = scoreNodes;
      fetchNext();
   }

   /**
    * Returns the next <code>Node</code> in the result set.
    * 
    * @return the next <code>Node</code> in the result set.
    * @throws NoSuchElementException
    *           if iteration has no more <code>Node</code>s.
    */
   public Node nextNode() throws NoSuchElementException
   {
      return nextNodeImpl();
   }

   /**
    * Returns the next <code>Node</code> in the result set.
    * 
    * @return the next <code>Node</code> in the result set.
    * @throws NoSuchElementException
    *           if iteration has no more <code>Node</code>s.
    */
   public Object next() throws NoSuchElementException
   {
      return nextNode();
   }

   /**
    * Returns the next <code>Node</code> in the result set.
    * 
    * @return the next <code>Node</code> in the result set.
    * @throws NoSuchElementException
    *           if iteration has no more <code>Node</code>s.
    */
   public NodeImpl nextNodeImpl() throws NoSuchElementException
   {
      if (next == null)
      {
         throw new NoSuchElementException();
      }
      NodeImpl n = next;
      fetchNext();
      return n;
   }

   /**
    * Skip a number of <code>Node</code>s in this iterator.
    * 
    * @param skipNum
    *          the non-negative number of <code>Node</code>s to skip
    * @throws NoSuchElementException
    *           if skipped past the last <code>Node</code> in this iterator.
    */
   public void skip(long skipNum) throws NoSuchElementException
   {
      if (skipNum < 0)
      {
         throw new IllegalArgumentException("skipNum must not be negative");
      }
      if ((pos + skipNum) > scoreNodes.length)
      {
         throw new NoSuchElementException();
      }
      if (skipNum == 0)
      {
         // do nothing
      }
      else
      {
         pos += skipNum - 1;
         fetchNext();
      }
   }

   public void skipBack(long skipNum)
   {

      if (skipNum < 0)
      {
         throw new IllegalArgumentException("skipNum must not be negative");
      }
      if ((pos - skipNum) < 0)
      {
         throw new NoSuchElementException();
      }
      if (skipNum > 0)
      {
         pos -= skipNum + 1;
         fetchNext();
      }
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
      return scoreNodes.length - invalid;
   }

   /**
    * Returns the current position in this <code>NodeIterator</code>.
    * 
    * @return the current position in this <code>NodeIterator</code>.
    */
   public long getPosition()
   {
      return pos - invalid;
   }

   /**
    * Returns <code>true</code> if there is another <code>Node</code> available; <code>false</code>
    * otherwise.
    * 
    * @return <code>true</code> if there is another <code>Node</code> available; <code>false</code>
    *         otherwise.
    */
   public boolean hasNext()
   {
      return next != null;
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
    * Returns the score of the node returned by {@link #nextNode()}. In other words, this method
    * returns the score value of the next <code>Node</code>.
    * 
    * @return the score of the node returned by {@link #nextNode()}.
    * @throws NoSuchElementException
    *           if there is no next node.
    */
   public float getScore() throws NoSuchElementException
   {
      if (!hasNext())
      {
         throw new NoSuchElementException();
      }
      return scoreNodes[pos].getScore();
   }

   /**
    * Clears {@link #next} and tries to fetch the next Node instance. When this method returns
    * {@link #next} refers to the next available node instance in this iterator. If {@link #next} is
    * null when this method returns, then there are no more valid element in this iterator.
    */
   protected void fetchNext()
   {
      // reset
      next = null;
      while (next == null && (pos + 1) < scoreNodes.length)
      {
         try
         {
            next = (NodeImpl)itemMgr.getItemByIdentifier(scoreNodes[pos + 1].getNodeId(), true);
            if (next == null)
            {
               invalid++;
               pos++;
            }
         }
         catch (RepositoryException e)
         {
            log.warn("Exception retrieving Node with UUID: " + scoreNodes[pos + 1].getNodeId() + ": " + e.toString());
            // try next
            invalid++;
            pos++;
         }
      }
      pos++;
   }
}
