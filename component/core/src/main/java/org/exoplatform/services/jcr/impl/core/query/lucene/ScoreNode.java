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

/**
 * <code>ScoreNode</code> implements a simple container which holds a mapping of {@link NodeId} to a
 * score value.
 */
final class ScoreNode
{

   /**
    * The id of a node.
    */
   private final String id;

   /**
    * The score of the node.
    */
   private final float score;

   /**
    * Creates a new <code>ScoreNode</code>.
    * 
    * @param id
    *          the node id.
    * @param score
    *          the score value.
    */
   ScoreNode(String id, float score)
   {
      this.id = id;
      this.score = score;
   }

   /**
    * @return the node id for this <code>ScoreNode</code>.
    */
   public String getNodeId()
   {
      return id;
   }

   /**
    * @return the score for this <code>ScoreNode</code>.
    */
   public float getScore()
   {
      return score;
   }

   @Override
   public int hashCode()
   {
      return id.hashCode();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (getClass() != obj.getClass())
      {
         return false;
      }
      ScoreNode other = (ScoreNode)obj;
      if (id == null)
      {
         if (other.id != null)
         {
            return false;
         }
      }
      else if (!id.equals(other.id))
      {
         return false;
      }
      if (Float.floatToIntBits(score) != Float.floatToIntBits(other.score))
      {
         return false;
      }
      return true;
   }
}
