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
package org.exoplatform.services.jcr.impl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * The StAX cases parse with the JDK's implementation, named explicitly
 * ({@link XMLInputFactory#newDefaultFactory()}), whatever the test classpath
 * holds; the JiBX cases parse with the provider of the classpath, as
 * production does.
 */
public class TestSafeXMLInputFactory
{
   static final String REFUSAL = "External DTD or entity not allowed";

   private static final String MARKER = "MARKER-EXO-92938";

   private Path dir;

   private String dtdUri;

   private String entityUri;

   @Before
   public void setUp() throws Exception
   {
      dir = Files.createTempDirectory("safe-xml");
      Path dtd = dir.resolve("external.dtd");
      Files.write(dtd, ("<!ENTITY m \"" + MARKER + "\">").getBytes(StandardCharsets.UTF_8));
      dtdUri = dtd.toUri().toString();
      Path entity = dir.resolve("external.txt");
      Files.write(entity, MARKER.getBytes(StandardCharsets.UTF_8));
      entityUri = entity.toUri().toString();
   }

   @After
   public void tearDown() throws Exception
   {
      Files.deleteIfExists(dir.resolve("external.dtd"));
      Files.deleteIfExists(dir.resolve("external.txt"));
      Files.deleteIfExists(dir);
   }

   @Test
   public void testExternalDtdIsRefused() throws Exception
   {
      assertRefused("<?xml version='1.0'?><!DOCTYPE r SYSTEM '" + dtdUri + "'><r>&m;</r>");
   }

   @Test
   public void testExternalEntityIsRefused() throws Exception
   {
      assertRefused("<?xml version='1.0'?><!DOCTYPE r [<!ENTITY e SYSTEM '" + entityUri + "'>]><r>&e;</r>");
   }

   @Test
   public void testExternalParameterEntityIsRefused() throws Exception
   {
      assertRefused("<?xml version='1.0'?><!DOCTYPE r [<!ENTITY % p SYSTEM '" + dtdUri + "'> %p;]><r>&m;</r>");
   }

   @Test
   public void testInternalSubsetIsParsed() throws Exception
   {
      assertEquals("internal value",
         text("<?xml version='1.0'?><!DOCTYPE r [<!ENTITY i 'internal value'>]><r>&i;</r>"));
   }

   @Test
   public void testDocumentWithoutDoctypeIsParsed() throws Exception
   {
      assertEquals("a&b", text("<?xml version='1.0'?><r xmlns:sv='http://www.jcp.org/jcr/sv/1.0'>a&amp;b</r>"));
   }

   /**
    * Pins the resolver's presence directly, whatever the provider: on the JDK,
    * the emptied access property also refuses, with its own message.
    */
   @Test
   public void testResolverRefusesEveryExternalResource() throws Exception
   {
      XMLInputFactory factory = SafeXMLInputFactory.newInstance();
      assertSame(SafeXMLInputFactory.REFUSING_RESOLVER, factory.getXMLResolver());
      try
      {
         factory.getXMLResolver().resolveEntity(null, entityUri, null, null);
         fail("The resolver must refuse an external resource");
      }
      catch (XMLStreamException e)
      {
         assertTrue(e.getMessage().contains(REFUSAL));
      }
   }

   /**
    * Pins the access property on its own: the resolver alone also refuses, so
    * the behaviour tests above pass without it.
    */
   @Test
   public void testExternalAccessIsEmptiedWhenSupported() throws Exception
   {
      XMLInputFactory factory = SafeXMLInputFactory.restrict(XMLInputFactory.newDefaultFactory());
      assertTrue(factory.isPropertySupported(XMLConstants.ACCESS_EXTERNAL_DTD));
      assertEquals("", factory.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD));
   }

   /**
    * An implementation that does not know the access property (Woodstox
    * rejects it with an IllegalArgumentException) still gets the resolver, and
    * the property is not set.
    */
   @Test
   public void testAccessPropertyIsSkippedWhenUnsupported() throws Exception
   {
      XMLInputFactory factory = mock(XMLInputFactory.class);
      when(factory.isPropertySupported(XMLConstants.ACCESS_EXTERNAL_DTD)).thenReturn(false);

      assertSame(factory, SafeXMLInputFactory.restrict(factory));

      verify(factory).setXMLResolver(SafeXMLInputFactory.REFUSING_RESOLVER);
      verify(factory, never()).setProperty(eq(XMLConstants.ACCESS_EXTERNAL_DTD), nullable(Object.class));
   }

   @Test
   public void testJibxDocumentWithExternalDtdIsRefused() throws Exception
   {
      String xml =
         "<?xml version='1.0'?><!DOCTYPE repository-service SYSTEM '" + dtdUri
            + "'><repository-service default-repository='&m;'><repositories/></repository-service>";
      try
      {
         RepositoryServiceConfiguration conf = unmarshal(xml);
         fail("Document parsed, default repository: " + conf.getDefaultRepositoryName());
      }
      catch (JiBXException e)
      {
         assertTrue(causeMessages(e).contains(REFUSAL));
      }
   }

   @Test
   public void testJibxDocumentWithExternalEntityIsRefused() throws Exception
   {
      String xml =
         "<?xml version='1.0'?><!DOCTYPE repository-service [<!ENTITY e SYSTEM '" + entityUri
            + "'>]><repository-service default-repository='x'><repositories/>&e;</repository-service>";
      try
      {
         unmarshal(xml);
         fail("Document parsed");
      }
      catch (JiBXException e)
      {
         assertTrue(causeMessages(e).contains(REFUSAL));
      }
   }

   @Test
   public void testJibxDocumentIsUnmarshalled() throws Exception
   {
      String xml =
         "<?xml version='1.0'?><!DOCTYPE repository-service [<!ENTITY i 'internal'>]>"
            + "<repository-service default-repository='&i;'><repositories/></repository-service>";
      assertEquals("internal", unmarshal(xml).getDefaultRepositoryName());

      try (InputStream config =
         getClass().getResourceAsStream("/conf/standalone/test-jcr-config-sjdbc-ispn.xml"))
      {
         RepositoryServiceConfiguration conf =
            (RepositoryServiceConfiguration)SafeXMLInputFactory.unmarshalDocument(BindingDirectory.getFactory(
               RepositoryServiceConfiguration.class).createUnmarshallingContext(), config, "config");
         assertEquals("db1", conf.getDefaultRepositoryName());
         assertNotNull(conf.getRepositoryConfiguration("db1").getWorkspaceEntries());
         assertFalse(conf.getRepositoryConfiguration("db1").getWorkspaceEntries().isEmpty());
      }
   }

   private RepositoryServiceConfiguration unmarshal(String xml) throws JiBXException
   {
      return (RepositoryServiceConfiguration)SafeXMLInputFactory.unmarshalDocument(BindingDirectory.getFactory(
         RepositoryServiceConfiguration.class).createUnmarshallingContext(),
         new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test");
   }

   private void assertRefused(String xml) throws Exception
   {
      try
      {
         String text = text(xml);
         fail("Document parsed, text: " + text);
      }
      catch (XMLStreamException e)
      {
         assertTrue(e.getMessage(), e.getMessage().contains(REFUSAL));
         assertFalse(e.getMessage().contains(MARKER));
      }
   }

   private String text(String xml) throws XMLStreamException
   {
      XMLStreamReader reader =
         SafeXMLInputFactory.restrict(XMLInputFactory.newDefaultFactory()).createXMLStreamReader(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
      StringBuilder text = new StringBuilder();
      while (reader.hasNext())
      {
         if (reader.next() == XMLStreamReader.CHARACTERS)
         {
            text.append(reader.getText());
         }
      }
      return text.toString();
   }

   static String causeMessages(Throwable e)
   {
      StringBuilder messages = new StringBuilder();
      for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause())
      {
         messages.append(t.getMessage()).append('\n');
         if (t instanceof JiBXException && ((JiBXException)t).getRootCause() != null
            && ((JiBXException)t).getRootCause() != t.getCause())
         {
            messages.append(causeMessages(((JiBXException)t).getRootCause()));
         }
      }
      return messages.toString();
   }
}
