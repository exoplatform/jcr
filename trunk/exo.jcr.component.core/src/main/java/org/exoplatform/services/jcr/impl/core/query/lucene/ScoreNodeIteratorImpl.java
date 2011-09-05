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

import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * <code>ScoreNodeIteratorImpl</code> implements a {@link ScoreNodeIterator}
 * over an array of {@link ScoreNode ScoreNode[]}.
 */
public class ScoreNodeIteratorImpl implements ScoreNodeIterator
{

   //private final ScoreNode[][] scoreNodes;

   private ListIterator<ScoreNode[]> iterator;

   public ScoreNodeIteratorImpl(ScoreNode[][] scoreNodes)
   {

      iterator = Arrays.asList(scoreNodes).listIterator();
      //this.scoreNodes = scoreNodes;
      ///this(Arrays.asList(scoreNodes));
      this.size = scoreNodes.length;
      //this.position = -1;
   }

   /**
    * {@inheritDoc}
    */
   public ScoreNode[] nextScoreNodes()
   {
      return (ScoreNode[])next();
   }

   /**
    * Number of elements in the adapted iterator, or -1 if unknown.
    */
   private int size;

   //-------------------------------------------------------< RangeIterator >

   /**
    * Returns the current position of the iterator.
    *
    * @return iterator position
    */
   public long getPosition()
   {
      return iterator.nextIndex();
   }

   /**
    * Returns the size of the iterator.
    *
    * @return iterator size, or -1 if unknown
    */
   public long getSize()
   {
      return size;
   }

   /**
    * Skips the given number of elements.
    *
    * @param n number of elements to skip
    * @throws IllegalArgumentException if n is negative
    * @throws NoSuchElementException if skipped past the last element
    */
   public void skip(long n) throws IllegalArgumentException, NoSuchElementException
   {
      if (n < 0)
      {
         throw new IllegalArgumentException("skip(" + n + ")");
      }

      for (long i = 0; i < n; i++)
      {
         next();
      }
   }

   //------------------------------------------------------------< Iterator >

   /**
    * Checks if this iterator has more elements. If there are no more
    * elements and the size of the iterator is unknown, then the size is
    * set to the current position.
    *
    * @return <code>true</code> if this iterator has more elements,
    *         <code>false</code> otherwise
    */
   public boolean hasNext()
   {
      return iterator.hasNext();
   }

   /**
    * Returns the next element in this iterator and advances the iterator
    * position. If there are no more elements and the size of the iterator
    * is unknown, then the size is set to the current position.
    *
    * @return next element
    * @throws NoSuchElementException if there are no more elements
    */
   public Object next() throws NoSuchElementException
   {
      return iterator.next();
   }

   /**
    * Removes the previously retrieved element. Decreases the current
    * position and size of this iterator.
    *
    * @throws UnsupportedOperationException if removes are not permitted
    * @throws IllegalStateException if there is no previous element to remove
    */
   public void remove() throws UnsupportedOperationException, IllegalStateException
   {
      iterator.remove();
      size--;
   }

   public void skipBack(long skipNum)
   {
      if (skipNum < 0)
      {
         throw new IllegalArgumentException("skip(" + skipNum + ")");
      }
      if ((iterator.nextIndex() - skipNum) < 0)
      {
         throw new NoSuchElementException();
      }
      for (long i = 0; i < skipNum; i++)
      {
         iterator.previous();
      }

   }
}
