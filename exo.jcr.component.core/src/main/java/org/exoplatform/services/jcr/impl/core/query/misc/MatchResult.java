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
package org.exoplatform.services.jcr.impl.core.query.misc;

import org.exoplatform.services.jcr.datamodel.QPath;

import javax.jcr.RepositoryException;

/**
 * A MatchResult instance represents the result of matching a {@link Pattern} against
 * a {@link QPath}.
 */
public class MatchResult
{
   private final int pathLength;

   private int matchPos;

   private final int matchLength;

   MatchResult(QPath path, int length)
   {
      this(path, 0, length);
   }

   MatchResult(QPath path, int pos, int length)
   {
      super();
      this.matchPos = pos;
      this.matchLength = length;
      this.pathLength = path.getEntries().length;
   }

   /**
    * Returns the remaining path after the matching part.
    * @return  The remaining path after the matching part such that the path constructed from
    *   {@link #getMatch()} followed by {@link #getRemainder()} is the original path or
    *   <code>null</code> if {@link #isFullMatch()} is <code>true</code>.
    */
   public QPath getRemainder()
   {
      if (matchPos + matchLength >= pathLength)
      {
         return null;
      }
      else
      {
         try
         {
            throw new RepositoryException("Not implemented");
            //return path.subPath(matchPos + matchLength, pathLength);
         }
         catch (RepositoryException e)
         {
            throw (IllegalStateException)new IllegalStateException("Path not normalized").initCause(e);
         }
      }
   }

   /**
    * Returns the path which was matched by the {@link Pattern}.
    * @return The path which was matched such that the path constructed from
    *   {@link #getMatch()} followed by {@link #getRemainder()} is the original path or
    *   <code>null</code> if {@link #getMatchLength()} is <code>0</code>.
    */
   public QPath getMatch()
   {
      if (matchLength == 0)
      {
         return null;
      }
      else
      {
         try
         {
            //return path.subPath(matchPos, matchPos + matchLength);
            throw new RepositoryException("Not implemented");
         }
         catch (RepositoryException e)
         {
            throw (IllegalStateException)new IllegalStateException("Path not normalized").initCause(e);
         }
      }

   }

   /**
    * Returns the position of the match
    * @return
    */
   public int getMatchPos()
   {
      return matchPos;
   }

   /**
    * Returns the number of elements which where matched by the {@link Pattern}.
    * @return
    */
   public int getMatchLength()
   {
      return matchLength;
   }

   /**
    * Returns true if the {@link Pattern} matched anything or false otherwise.
    * @return
    */
   public boolean isMatch()
   {
      return matchLength > 0;
   }

   /**
    * Returns true if the {@link Pattern} matched the whole {@link QPath}.
    * @return
    */
   public boolean isFullMatch()
   {
      return pathLength == matchLength;
   }

   MatchResult setPos(int matchPos)
   {
      this.matchPos = matchPos;
      return this;
   }

}
