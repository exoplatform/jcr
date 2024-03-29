/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.services.document.impl.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.services.document.diff.Chunk;

/**
 * Holds a information about a parrt of the text involved in a differencing or
 * patching operation.
 * 
 * @version $Id: ChunkImpl.java 5799 2006-05-28 17:55:42Z geaz $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @see DiffServiceImpl
 * @see DeltaImpl
 */
public class ChunkImpl extends ToStringImpl implements Chunk
{

   protected int anchor;

   protected int count;

   protected List chunk;

   /**
    * Creates a chunk that doesn't copy the original text.
    * 
    * @param pos the start position in the text.
    * @param count the size of the chunk.
    */
   public ChunkImpl(int pos, int count)
   {
      this.anchor = pos;
      this.count = (count >= 0 ? count : 0);
   }

   /**
    * Creates a chunk and saves a copy the original chunk's text.
    * 
    * @param iseq the original text.
    * @param pos the start position in the text.
    * @param count the size of the chunk.
    */
   public ChunkImpl(Object[] iseq, int pos, int count)
   {
      this(pos, count);
      chunk = slice(iseq, pos, count);
   }

   /**
    * Creates a chunk that will be displaced in the resulting text, and saves a
    * copy the original chunk's text.
    * 
    * @param iseq the original text.
    * @param pos the start position in the text.
    * @param count the size of the chunk.
    * @param offset the position the chunk should have in the resulting text.
    */
   public ChunkImpl(Object[] iseq, int pos, int count, int offset)
   {
      this(offset, count);
      chunk = slice(iseq, pos, count);
   }

   /**
    * Creates a chunk and saves a copy the original chunk's text.
    * 
    * @param iseq the original text.
    * @param pos the start position in the text.
    * @param count the size of the chunk.
    */
   public ChunkImpl(List iseq, int pos, int count)
   {
      this(pos, count);
      chunk = slice(iseq, pos, count);
   }

   /**
    * Creates a chunk that will be displaced in the resulting text, and saves a
    * copy the original chunk's text.
    * 
    * @param iseq the original text.
    * @param pos the start position in the text.
    * @param count the size of the chunk.
    * @param offset the position the chunk should have in the resulting text.
    */
   public ChunkImpl(List iseq, int pos, int count, int offset)
   {
      this(offset, count);
      chunk = slice(iseq, pos, count);
   }

   /**
    * Returns the anchor position of the chunk.
    * 
    * @return the anchor position.
    */
   public int anchor()
   {
      return anchor;
   }

   /**
    * Returns the size of the chunk.
    * 
    * @return the size.
    */
   public int size()
   {
      return count;
   }

   /**
    * Returns the index of the first line of the chunk.
    */
   public int first()
   {
      return anchor();
   }

   /**
    * Returns the index of the last line of the chunk.
    */
   public int last()
   {
      return anchor() + size() - 1;
   }

   /**
    * Returns the <i>from</i> index of the chunk in RCS terms.
    */
   public int rcsfrom()
   {
      return anchor + 1;
   }

   /**
    * Returns the <i>to</i> index of the chunk in RCS terms.
    */
   public int rcsto()
   {
      return anchor + count;
   }

   /**
    * Returns the text saved for this chunk.
    * 
    * @return the text.
    */
   public List chunk()
   {
      return chunk;
   }

   /**
    * Verifies that this chunk's saved text matches the corresponding text in the
    * given sequence.
    * 
    * @param target the sequence to verify against.
    * @return true if the texts match.
    */
   public boolean verify(List target)
   {
      if (chunk == null)
      {
         return true;
      }
      if (last() > target.size())
      {
         return false;
      }
      for (int i = 0; i < count; i++)
      {
         if (!target.get(anchor + i).equals(chunk.get(i)))
         {
            return false;
         }
      }
      return true;
   }

   /**
    * Delete this chunk from he given text.
    * 
    * @param target the text to delete from.
    */
   public void applyDelete(List target)
   {
      for (int i = last(); i >= first(); i--)
      {
         target.remove(i);
      }
   }

   /**
    * Add the text of this chunk to the target at the given position.
    * 
    * @param start where to add the text.
    * @param target the text to add to.
    */
   public void applyAdd(int start, List target)
   {
      Iterator i = chunk.iterator();
      while (i.hasNext())
      {
         target.add(start++, i.next());
      }
   }

   /**
    * Provide a string image of the chunk using the an empty prefix and postfix.
    */
   public void toString(StringBuffer s)
   {
      toString(s, "", "");
   }

   /**
    * Provide a string image of the chunk using the given prefix and postfix.
    * 
    * @param s where the string image should be appended.
    * @param prefix the text thatshould prefix each line.
    * @param postfix the text that should end each line.
    */
   public StringBuffer toString(StringBuffer s, String prefix, String postfix)
   {
      if (chunk != null)
      {
         Iterator i = chunk.iterator();
         while (i.hasNext())
         {
            s.append(prefix);
            s.append(i.next());
            s.append(postfix);
         }
      }
      return s;
   }

   /**
    * Retreives the specified part from a {@link List List}.
    * 
    * @param seq the list to retreive a slice from.
    * @param pos the start position.
    * @param count the number of items in the slice.
    * @return a {@link List List} containing the specified items.
    */
   public List slice(List seq, int pos, int count)
   {
      if (count <= 0)
      {
         return new ArrayList(seq.subList(pos, pos));
      }
      else
      {
         return new ArrayList(seq.subList(pos, pos + count));
      }
   }

   /**
    * Retrieves a slice from an {@link Object Object} array.
    * 
    * @param seq the list to retreive a slice from.
    * @param pos the start position.
    * @param count the number of items in the slice.
    * @return a {@link List List} containing the specified items.
    */
   public List slice(Object[] seq, int pos, int count)
   {
      return slice(Arrays.asList(seq), pos, count);
   }

   /**
    * Provide a string representation of the numeric range of this chunk.
    */
   public String rangeString()
   {
      StringBuffer result = new StringBuffer();
      rangeString(result);
      return result.toString();
   }

   /**
    * Provide a string representation of the numeric range of this chunk.
    * 
    * @param s where the string representation should be appended.
    */
   public void rangeString(StringBuffer s)
   {
      rangeString(s, ",");
   }

   /**
    * Provide a string representation of the numeric range of this chunk.
    * 
    * @param s where the string representation should be appended.
    * @param separ what to use as line separator.
    */
   public void rangeString(StringBuffer s, String separ)
   {
      if (size() <= 1)
      {
         s.append(Integer.toString(rcsfrom()));
      }
      else
      {
         s.append(Integer.toString(rcsfrom()));
         s.append(separ);
         s.append(Integer.toString(rcsto()));
      }
   }
}
