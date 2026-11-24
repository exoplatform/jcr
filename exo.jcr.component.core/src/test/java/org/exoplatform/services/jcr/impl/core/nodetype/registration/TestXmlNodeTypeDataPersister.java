/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.nodetype.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Node type XML read by {@link XmlNodeTypeDataPersister}: external DTDs and
 * entities are refused, an ordinary document is unmarshalled. The JiBX parsing
 * uses the provider of the test classpath, as production does.
 */
public class TestXmlNodeTypeDataPersister
{
   private static final String MARKER = "MARKER-EXO-93953";

   private static final String REFUSAL = "External DTD or entity not allowed";

   private static final String NODE_TYPES_OPEN =
      "<nodeTypes xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\">";

   private Path dir;

   private String dtdUri;

   private String entityUri;

   private NodeTypeConverter converter;

   @Before
   public void setUp() throws Exception
   {
      dir = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "node-type-xml");
      Path dtd = dir.resolve("external.dtd");
      Files.write(dtd, ("<!ENTITY m \"" + MARKER + "\">").getBytes(StandardCharsets.UTF_8));
      dtdUri = dtd.toUri().toString();
      Path entity = dir.resolve("external.txt");
      Files.write(entity, MARKER.getBytes(StandardCharsets.UTF_8));
      entityUri = entity.toUri().toString();
      converter = mock(NodeTypeConverter.class);
      when(converter.convertFromValueToData(anyList())).thenReturn(new ArrayList<>());
   }

   @After
   public void tearDown() throws Exception
   {
      Files.deleteIfExists(dir.resolve("external.dtd"));
      Files.deleteIfExists(dir.resolve("external.txt"));
      Files.deleteIfExists(dir);
   }

   @Test
   public void testOrdinaryDocumentIsRead() throws Exception
   {
      read("<?xml version='1.0'?>" + NODE_TYPES_OPEN
         + "<nodeType name='nt:myType' isMixin='false' hasOrderableChildNodes='false' primaryItemName=''>"
         + "<supertypes><supertype>nt:base</supertype></supertypes></nodeType></nodeTypes>");

      assertEquals("nt:myType", capturedValues().get(0).getName());
   }

   @Test
   public void testInternalEntityIsStillRead() throws Exception
   {
      read("<?xml version='1.0'?><!DOCTYPE nodeTypes [<!ENTITY n 'nt:internal'>]>" + NODE_TYPES_OPEN
         + "<nodeType name='&n;' isMixin='false' hasOrderableChildNodes='false' primaryItemName=''/></nodeTypes>");

      assertEquals("nt:internal", capturedValues().get(0).getName());
   }

   @Test
   public void testExternalEntityIsRefused() throws Exception
   {
      assertRefused("<?xml version='1.0'?><!DOCTYPE nodeTypes [<!ENTITY e SYSTEM '" + entityUri + "'>]>"
         + NODE_TYPES_OPEN + "<nodeType name='nt:myType' isMixin='false' hasOrderableChildNodes='false' primaryItemName=''>"
         + "<supertypes><supertype>&e;</supertype></supertypes></nodeType></nodeTypes>");
   }

   @Test
   public void testExternalDtdIsRefused() throws Exception
   {
      assertRefused("<?xml version='1.0'?><!DOCTYPE nodeTypes SYSTEM '" + dtdUri + "'>" + NODE_TYPES_OPEN
         + "<nodeType name='&m;' isMixin='false' hasOrderableChildNodes='false' primaryItemName=''/></nodeTypes>");
   }

   private void assertRefused(String xml) throws Exception
   {
      try
      {
         read(xml);
         fail("Document read, values: " + capturedValues());
      }
      catch (RepositoryException e)
      {
         String messages = causeMessages(e);
         assertTrue(messages, messages.contains(REFUSAL));
         assertFalse(messages.contains(MARKER));
      }
      verify(converter, never()).convertFromValueToData(anyList());
   }

   private void read(String xml) throws RepositoryException
   {
      new XmlNodeTypeDataPersister(converter,
         new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getAllNodeTypes();
   }

   @SuppressWarnings("unchecked")
   private List<NodeTypeValue> capturedValues() throws RepositoryException
   {
      ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
      verify(converter).convertFromValueToData(captor.capture());
      return captor.getValue();
   }

   private static String causeMessages(Throwable e)
   {
      StringBuilder messages = new StringBuilder();
      for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause())
      {
         messages.append(t.getMessage()).append('\n');
         if (t instanceof JiBXException jibx && jibx.getRootCause() != null
            && jibx.getRootCause() != t.getCause())
         {
            messages.append(causeMessages(jibx.getRootCause()));
         }
      }
      return messages.toString();
   }
}
