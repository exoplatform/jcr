/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to serialize and deserialize the changes to apply to the lucene index.
 * 
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 */
public class ChangesHolder implements Externalizable
{

   private static final int STORED_FLAG = 1;

   private static final int COMPRESSED_FLAG = 1 << 1;

   private static final int INDEXED_FLAG = 1 << 2;

   private static final int TOKENIZED_FLAG = 1 << 3;

   private static final int OMIT_NORMS_FLAG = 1 << 4;

   private static final int BINARY_FLAG = 1 << 5;

   private static final int STORE_TERM_VECTOR_FLAG = 1 << 6;

   private static final int STORE_POSITION_WITH_TERM_VECTOR_FLAG = 1 << 7;

   private static final int STORE_OFFSET_WITH_TERM_VECTOR_FLAG = 1 << 8;

   private static final int LAZY_FLAG = 1 << 9;

   private static final int OMIT_TF_FLAG = 1 << 10;

   private static final int BOOST_FLAG = 1 << 11;

   /**
    * List of doc ids to remove from the index
    */
   private List<String> remove;

   /**
    * Collection of Lucene Documents to add to the index
    */
   private Collection<Document> add;

   /**
    * Default constructor used during the deserializing phase
    */
   public ChangesHolder()
   {
   }

   /**
    * @param remove Collection of doc ids to remove from the index
    * @param add Lucene Documents to add to the index
    */
   public ChangesHolder(Collection<String> remove, Collection<Document> add)
   {
      this.remove = new ArrayList<String>(remove);
      this.add = add;
   }

   /**
    * @return the collection of doc id to remove
    */
   public Collection<String> getRemove()
   {
      return remove;
   }

   /**
    * @return the collection of lucene document to add
    */
   public Collection<Document> getAdd()
   {
      return add;
   }

   /**
    * @return the collection of id of lucene document to add
    */
   public Collection<String> getAddIds()
   {
      Collection<String> ids = new LinkedList<String>();
      for (Document doc : add)
      {
         ids.add(getDocId(doc));
      }
      return ids;
   }
   
   /**
    * @return the id of the given lucene doc
    */
   public String getDocId(Document doc)
   {
      return doc.get(FieldNames.UUID);
   }
   
   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      int length = in.readInt();
      this.remove = new ArrayList<String>(length);
      for (int i = 0; i < length; i++)
      {
         remove.add(in.readUTF());
      }
      this.add = new LinkedList<Document>();
      while (in.readBoolean())
      {
         Document doc = new Document();
         doc.setBoost(in.readFloat());
         int l = in.readInt();
         for (int i = 0; i < l; i++)
         {
            doc.add(readField(in, doc));
         }
         add.add(doc);
      }
   }

   /**
    * Deserialize the field from the given {@link ObjectInput}
    * @param in the stream from which we deserialize the Field
    * @return the deserialized field
    * @throws IOException 
    * @throws ClassNotFoundException 
    */
   private static Field readField(ObjectInput in, Document doc) throws IOException, ClassNotFoundException
   {
      String name = in.readUTF();
      int flags = in.readInt();
      float boost = (flags & BOOST_FLAG) > 0 ? in.readFloat() : 1.0f;
      Object value = in.readObject();
      Field field;
      if (value instanceof TokenStream)
      {
         field = new Field(name, (TokenStream)value, getTermVectorParameter(flags));
      }
      else
      {
         // The value is a String
         field = new Field(name, (String)value, getStoreParameter(flags), getIndexParameter(flags), getTermVectorParameter(flags));
      }
      field.setBoost(boost);
      field.setOmitNorms((flags & OMIT_NORMS_FLAG) > 0);
      field.setOmitTf((flags & OMIT_TF_FLAG) > 0);
      return field;
   }

   /**
    * Returns the index parameter extracted from the flags.
    *
    * @param flags the flags of the Lucene field.
    * @return the index parameter corresponding to the given flags.
    */
   private static Field.Index getIndexParameter(int flags)
   {
      if ((flags & INDEXED_FLAG) == 0)
      {
         return Field.Index.NO;
      }
      else if ((flags & TOKENIZED_FLAG) > 0)
      {
         return Field.Index.ANALYZED;
      }
      else
      {
         return Field.Index.NOT_ANALYZED;
      }
   }

   /**
    * Returns the store parameter extracted from the flags.
    *
    * @param flags the flags of the Lucene field.
    * @return the store parameter corresponding to the given flags.
    */
   private static Field.Store getStoreParameter(int flags)
   {
      if ((flags & COMPRESSED_FLAG) > 0)
      {
         return Field.Store.COMPRESS;
      }
      else if ((flags & STORED_FLAG) > 0)
      {
         return Field.Store.YES;
      }
      else
      {
         return Field.Store.NO;
      }
   }

   /**
    * Returns the term vector parameter extracted from the flags.
    *
    * @param flags the flags of the Lucene field.
    * @return the term vector parameter corresponding to the given flags.
    */
   private static Field.TermVector getTermVectorParameter(int flags)
   {
      if (((flags & STORE_POSITION_WITH_TERM_VECTOR_FLAG) > 0) 
          && ((flags & STORE_OFFSET_WITH_TERM_VECTOR_FLAG) > 0))
      {
         return Field.TermVector.WITH_POSITIONS_OFFSETS;
      }
      else if ((flags & STORE_POSITION_WITH_TERM_VECTOR_FLAG) > 0)
      {
         return Field.TermVector.WITH_POSITIONS;
      }
      else if ((flags & STORE_OFFSET_WITH_TERM_VECTOR_FLAG) > 0)
      {
         return Field.TermVector.WITH_OFFSETS;
      }
      else if ((flags & STORE_TERM_VECTOR_FLAG) > 0)
      {
         return Field.TermVector.YES;
      }
      else
      {
         return Field.TermVector.NO;
      }
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public void writeExternal(ObjectOutput out) throws IOException
   {
      int length = remove.size();
      out.writeInt(length);
      for (int i = 0; i < length; i++)
      {
         out.writeUTF(remove.get(i));
      }
      out.flush();
      for (Document doc : add)
      {
         // Indicate that there is a doc to come
         out.writeBoolean(true);
         // boost
         out.writeFloat(doc.getBoost());
         List<Fieldable> fields = doc.getFields();
         int l = fields.size();
         out.writeInt(l);
         for (int i = 0; i < l; i++)
         {
            writeField(out, fields.get(i));
         }
         out.flush();
      }
      // There is no doc anymore
      out.writeBoolean(false);
   }

   /**
    * Serialize the Field into the given {@link ObjectOutput}
    * @param out the stream in which we serialize the Field
    * @param field the Field instance to serialize
    * @throws IOException if the Field could not be serialized
    */
   private static void writeField(ObjectOutput out, Fieldable field) throws IOException
   {
      // Name
      out.writeUTF(field.name());
      // Flags
      writeFlags(out, field);
      if (field.getBoost() != 1.0f)
      {
         // Boost
         out.writeFloat(field.getBoost());         
      }
      // Value
      writeValue(out, field);
   }

   /**
    * Serialize the value into the given {@link ObjectOutput}
    * @param out the stream in which we serialize the value
    * @param field the field from which we extract the value
    * @throws IOException if the value could not be serialized
    */
   private static void writeValue(ObjectOutput out, Fieldable field) throws IOException
   {
      Object o = field.stringValue();
      if (o != null)
      {
         // Use writeObject instead of writeUTF because the value could contain unsupported
         // characters
         out.writeObject(o);
         return;
      }
      o = field.tokenStreamValue();
      if (o != null)
      {
         out.writeObject(o);
         return;
      }
      o = field.readerValue();
      throw new RuntimeException("Unsupported value " + o);
   }

   /**
    * Serialize the flags into the given {@link ObjectOutput}
    * @param out the stream in which we serialize the flags
    * @param field the field from which we extract the flags
    * @throws IOException if the flags could not be serialized
    */
   private static void writeFlags(ObjectOutput out, Fieldable field) throws IOException
   {
      int flags = 0;
      if (field.isStored())
      {
         flags |= STORED_FLAG;
      }
      if (field.isCompressed())
      {
         flags |= COMPRESSED_FLAG;
      }
      if (field.isIndexed())
      {
         flags |= INDEXED_FLAG;
      }
      if (field.isTokenized())
      {
         flags |= TOKENIZED_FLAG;
      }
      if (field.getOmitNorms())
      {
         flags |= OMIT_NORMS_FLAG;
      }
      if (field.isBinary())
      {
         flags |= BINARY_FLAG;
      }
      if (field.isTermVectorStored())
      {
         flags |= STORE_TERM_VECTOR_FLAG;
      }
      if (field.isStorePositionWithTermVector())
      {
         flags |= STORE_POSITION_WITH_TERM_VECTOR_FLAG;
      }
      if (field.isStoreOffsetWithTermVector())
      {
         flags |= STORE_OFFSET_WITH_TERM_VECTOR_FLAG;
      }
      if (field.isLazy())
      {
         flags |= LAZY_FLAG;
      }
      if (field.getOmitTf())
      {
         flags |= OMIT_TF_FLAG;
      }
      if (field.getBoost() != 1.0f)
      {
         flags |= BOOST_FLAG;
      }
      out.writeInt(flags);
   }
}
