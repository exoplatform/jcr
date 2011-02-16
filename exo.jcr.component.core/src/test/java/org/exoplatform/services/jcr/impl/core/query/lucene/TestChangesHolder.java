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

import junit.framework.TestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 */
public class TestChangesHolder extends TestCase
{

   public void testSerNDeserializeDocs() throws Exception
   {
      System.out.println("###       testSerNDeserializeDocs    ###");
      Collection<Document> add = new ArrayList<Document>(3);
      Document doc = new Document();
      doc.setBoost(2.0f);
      Field fieldFull = new Field("full", "full-value", Store.COMPRESS, Index.ANALYZED_NO_NORMS, TermVector.WITH_POSITIONS_OFFSETS);
      fieldFull.setBoost(2.0f);
      fieldFull.setOmitTf(true);
      doc.add(fieldFull);
      Field fieldEmpty = new Field("empty", "empty-value", Store.NO, Index.NOT_ANALYZED, TermVector.NO);
      doc.add(fieldEmpty);
      add.add(doc);
      doc = new Document();
      doc.add(fieldFull);
      add.add(doc);
      doc = new Document();
      doc.add(fieldEmpty);
      add.add(doc);
      
      ByteArrayOutputStream baos = null;
      
      int total = 100000;
      long start;
      Collection<String> remove = Collections.emptyList();
      Collection<Document> addResult = null;
      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(new ChangesHolder(remove, add));
         oos.close();
      }
      System.out.println("Custom serialization: total time = " + (System.currentTimeMillis() - start) + ", size = " + baos.size());

      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
         addResult = ((ChangesHolder)ois.readObject()).getAdd();
         ois.close();
      }
      System.out.println("Custom deserialization: total time = " + (System.currentTimeMillis() - start));
      checkDocs(addResult);
      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(add);
         oos.close();
      }
      System.out.println("Native serialization: total time = " + (System.currentTimeMillis() - start) + ", size = " + baos.size());
      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
         addResult = (Collection<Document>)ois.readObject();
         ois.close();
      }
      System.out.println("Native deserialization: total time = " + (System.currentTimeMillis() - start));
      checkDocs(addResult);
   }

   private void checkDocs(Collection<Document> addResult)
   {
      assertNotNull(addResult);
      assertEquals(3, addResult.size());
      Iterator<Document> it = addResult.iterator();
      Document doc = it.next();
      assertEquals(2.0f, doc.getBoost());
      List<Field> fields = doc.getFields();
      assertNotNull(fields);
      assertEquals(2, fields.size());
      checkFieldFull(fields.get(0));
      checkFieldEmpty(fields.get(1));
      doc = it.next();
      assertEquals(1.0f, doc.getBoost());
      fields = doc.getFields();
      assertNotNull(fields);
      assertEquals(1, fields.size());
      checkFieldFull(fields.get(0));
      doc = it.next();
      assertEquals(1.0f, doc.getBoost());
      fields = doc.getFields();
      assertNotNull(fields);
      assertEquals(1, fields.size());
      checkFieldEmpty(fields.get(0));
   }

   private void checkFieldFull(Field field)
   {
      assertEquals("full", field.name());
      assertEquals("full-value", field.stringValue());
      assertTrue(field.isStored());
      assertTrue(field.isCompressed());
      assertTrue(field.isIndexed());
      assertTrue(field.isTokenized());
      assertTrue(field.getOmitNorms());
      assertTrue(field.isTermVectorStored());
      assertTrue(field.isStoreOffsetWithTermVector());
      assertTrue(field.isStorePositionWithTermVector());
      assertTrue(field.getOmitTf());
      assertFalse(field.isBinary());
      assertFalse(field.isLazy());
      assertEquals(2.0f, field.getBoost());
      assertEquals(0, field.getBinaryLength());
      assertEquals(0, field.getBinaryOffset());
   }

   private void checkFieldEmpty(Field field)
   {
      assertEquals("empty", field.name());
      assertEquals("empty-value", field.stringValue());
      assertFalse(field.isStored());
      assertFalse(field.isCompressed());
      assertTrue(field.isIndexed());
      assertFalse(field.isTokenized());
      assertFalse(field.getOmitNorms());
      assertFalse(field.isTermVectorStored());
      assertFalse(field.isStoreOffsetWithTermVector());
      assertFalse(field.isStorePositionWithTermVector());
      assertFalse(field.getOmitTf());
      assertFalse(field.isBinary());
      assertFalse(field.isLazy());
      assertEquals(1.0f, field.getBoost());
      assertEquals(0, field.getBinaryLength());
      assertEquals(0, field.getBinaryOffset());
   }
   
   public void testSerNDeserializeIds() throws Exception
   {
      System.out.println("###       testSerNDeserializeIds    ###");      
      Collection<String> remove = new ArrayList<String>(3);
      remove.add(UUID.randomUUID().toString());
      remove.add(UUID.randomUUID().toString());
      remove.add(UUID.randomUUID().toString());
      ByteArrayOutputStream baos = null;
      
      int total = 100000;
      long start;
      Collection<Document> add = Collections.emptyList();
      Collection<String> addResult = null;
      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(new ChangesHolder(remove, add));
         oos.close();
      }
      System.out.println("Custom serialization: total time = " + (System.currentTimeMillis() - start) + ", size = " + baos.size());

      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
         addResult = ((ChangesHolder)ois.readObject()).getRemove();
         ois.close();
      }
      System.out.println("Custom deserialization: total time = " + (System.currentTimeMillis() - start));
      checkIds(remove, addResult);
      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(remove);
         oos.close();
      }
      System.out.println("Native serialization: total time = " + (System.currentTimeMillis() - start) + ", size = " + baos.size());
      start = System.currentTimeMillis();
      for (int i = 0; i < total; i++)
      {
         ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
         addResult = (Collection<String>)ois.readObject();
         ois.close();
      }
      System.out.println("Native deserialization: total time = " + (System.currentTimeMillis() - start));
      checkIds(remove, addResult);     
   }

   private void checkIds(Collection<String> remove, Collection<String> addResult)
   {
      assertNotNull(addResult);
      assertEquals(remove.size(), addResult.size());
      Iterator<String> it1 = remove.iterator();
      Iterator<String> it2 = addResult.iterator();
      while (it1.hasNext())
      {
         assertEquals(it1.next(), it2.next());
      }
   }
}
