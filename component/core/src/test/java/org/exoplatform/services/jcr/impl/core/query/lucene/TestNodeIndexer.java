/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.query.lucene;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.PropertyType;

import org.exoplatform.services.log.Log;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: TestNodeIndexer.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class TestNodeIndexer
   extends JcrImplBaseTest
{
   public static final Log logger = ExoLogger.getLogger(TestNodeIndexer.class);

   /**
    * Test node with string valued property.
    * <p>
    * In this case there are:
    * <p>
    * - node "test"
    * <p>
    * - property "jcr:primaryType" val "nt:ustructured" string
    * <p>
    * - property "jcr:prop" val "prop value" string
    * <p>
    * <p>
    * Expected lucene document structure. Fields:
    * <p>
    * _:UUID uuid of node;
    * <p>
    * _:PARENT uuid of parent node _:LABEL name of the node "test"
    * <p>
    * _:PROPERTIES "jcr:primaryType"+'\uFFFF'+"nt:unstructured" _:PROPERTIES
    * "jcr:prop"+'\uFFFF'+"prop value"
    * <p>
    * _:FULLTEXT "prop value"
    * <p>
    * jcr:FULL:prop "prop value"
    * <p>
    * 
    * 
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   public void testCreateDocument_String() throws Exception
   {

      Node node = this.root.addNode("test");
      node.setProperty("jcr:prop", "prop value");
      root.save();

      ItemDataConsumer manager = this.session.getTransientNodesManager();

      node = (Node) this.session.getItem("/test");

      NodeData data = (NodeData) ((NodeImpl) node).getData();
      assertNotNull(data);

      DocumentReaderService extractor =
               (DocumentReaderService) container.getComponentInstanceOfType(DocumentReaderServiceImpl.class);
      NodeIndexer indexer =
               new NodeIndexer(data, manager, new NSRegistryBasedNamespaceMappings(
                        (NamespaceRegistryImpl) this.repository.getNamespaceRegistry()), extractor);

      Document doc = indexer.createDoc();

      List<Field> list = doc.getFields();

      // _:UUID
      List<Field> uuid = this.findField(list, FieldNames.UUID);
      assertNotNull(uuid);
      assertEquals(1, uuid.size());
      assertEquals(data.getIdentifier(), uuid.get(0).stringValue());

      // _:PARENT
      List<Field> parent = this.findField(list, FieldNames.PARENT);
      assertNotNull(parent);
      assertEquals(1, parent.size());
      assertEquals(data.getParentIdentifier(), parent.get(0).stringValue());

      // _:LABEL
      List<Field> label = this.findField(list, FieldNames.LABEL);
      assertNotNull(label);
      assertEquals(1, label.size());
      assertEquals(node.getName(), label.get(0).stringValue());

      // _:PROPERTIES jcr:primaryType
      List<Field> props = this.findField(list, FieldNames.PROPERTIES);
      assertNotNull(props);
      assertEquals(2, props.size());

      // :PROPERTIES jcr:primaryType "jcr:prop" + '\uFFFF' + "prop value"
      List<Field> prop1 = this.findField(list, FieldNames.PROPERTIES, "jcr:prop" + '\uFFFF' + "prop value");
      assertNotNull(prop1);
      assertEquals(1, prop1.size());

      // :PROPERTIES jcr:primaryType "jcr:prop" + '\uFFFF' + "prop value"
      List<Field> prop2 = this.findField(list, FieldNames.PROPERTIES, "jcr:primaryType" + '\uFFFF' + "nt:unstructured");
      assertNotNull(prop2);
      assertEquals(1, prop2.size());

      // _:FULLTEXT
      List<Field> full = this.findField(list, FieldNames.FULLTEXT);
      assertNotNull(full);
      assertEquals(1, full.size());
      assertEquals(node.getProperty("jcr:prop").getString(), full.get(0).stringValue());

      // jcr:FULL:prop "prop","value"
      List<Field> prefixed = this.findField(list, "jcr:FULL:prop");
      assertNotNull(prefixed);
      assertEquals(1, prefixed.size());
      assertEquals(node.getProperty("jcr:prop").getString(), prefixed.get(0).stringValue());

      // Check sum - there must be 6 fields
      assertEquals(7, list.size());
   }

   /**
    * Test root node indexing;
    * <p>
    * In this case there are:
    * <p>
    * - root node
    * <p>
    * Expected lucene document structure. Fields:
    * <p>
    * _:UUID uuid of root node;
    * <p>
    * _:PARENT "" _:LABEL name ""
    * 
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   public void testCreateDocumentRootNode() throws Exception
   {

      ItemDataConsumer manager = this.session.getTransientNodesManager();

      // ((NodeImpl)node).getData();

      NodeData data = (NodeData) ((NodeImpl) root).getData();
      assertNotNull(data);

      DocumentReaderService extractor =
               (DocumentReaderService) container.getComponentInstanceOfType(DocumentReaderServiceImpl.class);
      NodeIndexer indexer =
               new NodeIndexer(data, manager, new NSRegistryBasedNamespaceMappings(
                        (NamespaceRegistryImpl) this.repository.getNamespaceRegistry()), extractor);

      Document doc = indexer.createDoc();

      List<Field> list = doc.getFields();

      // _:UUID
      List<Field> uuid = this.findField(list, FieldNames.UUID);
      assertNotNull(uuid);
      assertEquals(1, uuid.size());
      assertEquals(data.getIdentifier(), uuid.get(0).stringValue());

      // _:PARENT
      List<Field> parent = this.findField(list, FieldNames.PARENT);
      assertNotNull(parent);
      assertEquals(1, parent.size());
      assertEquals("", parent.get(0).stringValue());

      // _:LABEL
      List<Field> label = this.findField(list, FieldNames.LABEL);
      assertNotNull(label);
      assertEquals(1, label.size());
      assertEquals("", label.get(0).stringValue());
   }

   /**
    * Test of indexing Binary value
    * <p>
    * In this case there are:
    * <p>
    * - node "jcr:content"
    * <p>
    * - property "jcr:primaryType" val "nt:resource" name
    * <p>
    * - property "jcr:lastModified" calendar
    * <p>
    * - property "jcr:uuid" string
    * <p>
    * - property "jcr:mimetype" "text/plain" string
    * <p>
    * - property "jcr:data" "binary value" binary
    * <p>
    * Expected lucene document structure. Fields:
    * <p>
    * _:UUID uuid of node;
    * <p>
    * _:PARENT uuid of parent node _:LABEL name of the node "test"
    * <p>
    * _:PROPERTIES "jcr:primaryType"+'\uFFFF'+"nt:resource"
    * <p>
    * _:PROPERTIES "jcr:lastModified"+'\uFFFF'+currentTtimeString
    * <p>
    * _:PROPERTIES "jcr:uuid"+'\uFFFF'+ uuid
    * <p>
    * jcr:FULL:uuid uuid
    * <p>
    * _:PROPERTIES "jcr:mimetype"+'\uFFFF'+"text/plain"
    * <p>
    * _:FULLTEXT "text/plain"
    * <p>
    * _:FULLTEXT "binary value"
    * <p>
    * 
    * 
    * 
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   public void testCreateDocument_Binary() throws Exception
   {
      Node node = root.addNode("jcr:content", "nt:resource");

      MimeTypeResolver mimetypeResolver = new MimeTypeResolver();
      mimetypeResolver.setDefaultMimeType("text/plain");// "application/zip");
      String mimeType = mimetypeResolver.getMimeType("text/plain");

      node.setProperty("jcr:mimeType", mimeType);
      node.setProperty("jcr:lastModified", Calendar.getInstance());
      node.setProperty("jcr:data", new ByteArrayInputStream("binary value".getBytes()));

      root.save();

      ItemDataConsumer manager = this.session.getTransientNodesManager();

      NodeData data = (NodeData) ((NodeImpl) node).getData();
      assertNotNull(data);

      DocumentReaderService extractor =
               (DocumentReaderService) container.getComponentInstanceOfType(DocumentReaderServiceImpl.class);
      NodeIndexer indexer =
               new NodeIndexer(data, manager, new NSRegistryBasedNamespaceMappings(
                        (NamespaceRegistryImpl) this.repository.getNamespaceRegistry()), extractor);

      Document doc = indexer.createDoc();

      List<Field> list = doc.getFields();

      // Check sum - there must be 6 fields
      assertEquals(10, list.size());

      // _:UUID
      List<Field> uuid = this.findField(list, FieldNames.UUID);
      assertNotNull(uuid);
      assertEquals(1, uuid.size());
      assertEquals(data.getIdentifier(), uuid.get(0).stringValue());

      // _:PARENT
      List<Field> parent = this.findField(list, FieldNames.PARENT);
      assertNotNull(parent);
      assertEquals(1, parent.size());
      assertEquals(data.getParentIdentifier(), parent.get(0).stringValue());

      // _:LABEL
      List<Field> label = this.findField(list, FieldNames.LABEL);
      assertNotNull(label);
      assertEquals(1, label.size());
      assertEquals(node.getName(), label.get(0).stringValue());

   }

   /**
    * Test of indexing Binary value
    * <p>
    * In this case there are:
    * <p>
    * - node "jcr:content"
    * <p>
    * - property "jcr:primaryType" val "nt:unstructured" name
    * <p>
    * - property "pathprop" "/wooo" path
    * <p>
    * Expected lucene document structure. Fields:
    * <p>
    * _:UUID uuid of node;
    * <p>
    * _:PARENT uuid of parent node _:LABEL name of the node "test"
    * <p>
    * _:PROPERTIES "jcr:primaryType"+'\uFFFF'+"nt:unstructured"
    * <p>
    * _:PROPERTIES "pathprop"+'\uFFFF'+"/wooo"
    * <p>
    * 
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   public void testCreateDocument_Path() throws Exception
   {
      Node node = root.addNode("test_path");

      node.setProperty("pathprop", "/wooo", PropertyType.PATH);

      root.save();

      ItemDataConsumer manager = this.session.getTransientNodesManager();

      NodeData data = (NodeData) ((NodeImpl) node).getData();
      assertNotNull(data);

      DocumentReaderService extractor =
               (DocumentReaderService) container.getComponentInstanceOfType(DocumentReaderServiceImpl.class);
      NodeIndexer indexer =
               new NodeIndexer(data, manager, new NSRegistryBasedNamespaceMappings(
                        (NamespaceRegistryImpl) this.repository.getNamespaceRegistry()), extractor);

      Document doc = indexer.createDoc();

      List<Field> list = doc.getFields();

      // Check sum - there must be 6 fields
      assertEquals(5, list.size());

      // _:UUID
      List<Field> uuid = this.findField(list, FieldNames.UUID);
      assertNotNull(uuid);
      assertEquals(1, uuid.size());
      assertEquals(data.getIdentifier(), uuid.get(0).stringValue());

      // _:PARENT
      List<Field> parent = this.findField(list, FieldNames.PARENT);
      assertNotNull(parent);
      assertEquals(1, parent.size());
      assertEquals(data.getParentIdentifier(), parent.get(0).stringValue());

      // _:LABEL
      List<Field> label = this.findField(list, FieldNames.LABEL);
      assertNotNull(label);
      assertEquals(1, label.size());
      assertEquals(node.getName(), label.get(0).stringValue());

      // _:PROPERTIES
      List<Field> props = this.findField(list, FieldNames.PROPERTIES);
      assertNotNull(props);
      assertEquals(2, props.size());

      // :PROPERTIES jcr:primaryType "pathprop" + '\uFFFF' + "/wooo"
      List<Field> prop1 = this.findField(list, FieldNames.PROPERTIES, "pathprop" + '\uFFFF' + "/wooo");
      assertNotNull(prop1);
      assertEquals(1, prop1.size());

      // :PROPERTIES jcr:primaryType "jcr:primaryType" + '\uFFFF' + "jcr:primaryType" + '\uFFFF' +
      // "nt:unstructured"
      List<Field> prop2 = this.findField(list, FieldNames.PROPERTIES, "jcr:primaryType" + '\uFFFF' + "nt:unstructured");
      assertNotNull(prop2);
      assertEquals(1, prop2.size());

   }

   /**
    * @return Field or null if is not finded
    */
   public List<Field> findField(List<Field> list, String fieldName)
   {
      List<Field> out = new Vector<Field>();
      for (int i = 0; i < list.size(); i++)
      {
         if (list.get(i).name().equalsIgnoreCase(fieldName))
            out.add(list.get(i));
      }
      return (out.isEmpty()) ? null : out;
   }

   /**
    * Find Field from list, named as fieldName, and valued as fieldStringValue
    * 
    * @return Field or null if is not finded
    */
   public List<Field> findField(List<Field> list, String fieldName, String fieldStringValue)
   {
      List<Field> out = new Vector<Field>();
      for (int i = 0; i < list.size(); i++)
      {
         if ((list.get(i).name().equalsIgnoreCase(fieldName)) && (list.get(i).stringValue() != null)
                  && (list.get(i).stringValue().equals(fieldStringValue)))
         {
            out.add(list.get(i));
         }
      }
      return (out.isEmpty()) ? null : out;
   }
}
