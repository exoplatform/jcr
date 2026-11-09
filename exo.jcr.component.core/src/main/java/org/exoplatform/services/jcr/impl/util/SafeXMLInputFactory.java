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

import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * StAX input factories for XML the repository receives from outside: content
 * imports, workspace restores and backup sets. A document that refers to an
 * external DTD or an external entity is refused with an
 * {@link XMLStreamException}; an internal DTD subset and its internal entities
 * are still parsed.
 * <p>
 * Every external resource goes through a resolver that throws: the JDK's StAX
 * and Woodstox both consult it for the external DTD subset and for external
 * general and parameter entities. On an implementation that supports it (the
 * JDK's), {@link XMLConstants#ACCESS_EXTERNAL_DTD} is also emptied as a second
 * lock; Woodstox rejects that property, so it is set only when supported.
 * {@link XMLInputFactory#IS_SUPPORTING_EXTERNAL_ENTITIES} is deliberately left
 * alone: on the JDK, disabling it drops an external entity silently instead of
 * refusing the document.
 */
public final class SafeXMLInputFactory
{

   /**
    * Resolver that refuses every external DTD and external entity.
    */
   public static final XMLResolver REFUSING_RESOLVER = (publicId, systemId, baseUri, namespace) -> {
      throw new XMLStreamException("External DTD or entity not allowed: " + systemId);
   };

   private SafeXMLInputFactory()
   {
   }

   /**
    * @return a new {@link XMLInputFactory} that refuses external DTDs and
    *         external entities
    */
   public static XMLInputFactory newInstance()
   {
      return restrict(XMLInputFactory.newInstance());
   }

   /**
    * Configures the given factory to refuse external DTDs and external
    * entities.
    *
    * @param factory the factory to configure
    * @return the same factory
    */
   public static XMLInputFactory restrict(XMLInputFactory factory)
   {
      factory.setXMLResolver(REFUSING_RESOLVER);
      if (factory.isPropertySupported(XMLConstants.ACCESS_EXTERNAL_DTD))
      {
         factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      }
      return factory;
   }

   /**
    * Unmarshals a whole document with JiBX, parsed by a factory of
    * {@link #newInstance()} instead of JiBX's default one.
    *
    * @param uctx the unmarshalling context, as created by a JiBX binding
    *          factory
    * @param stream the document
    * @param documentName the document name, used in error messages
    * @return the unmarshalled root object
    * @throws JiBXException if the document cannot be parsed or unmarshalled,
    *           an external DTD or entity included
    */
   public static Object unmarshalDocument(IUnmarshallingContext uctx, InputStream stream, String documentName)
      throws JiBXException
   {
      XMLStreamReader reader;
      try
      {
         reader = newInstance().createXMLStreamReader(stream);
      }
      catch (XMLStreamException e)
      {
         throw new JiBXException("Error creating parser", e);
      }
      UnmarshallingContext context = (UnmarshallingContext)uctx;
      context.setDocument(new StAXReaderWrapper(reader, documentName, true));
      context.reset();
      return context.unmarshalElement();
   }
}
