/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.api.exporting;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Created y the eXo platform team User: Benjamin Mestrallet Date: 18 ao�t 2004
 */
public class MockContentHandler implements ContentHandler
{
   public boolean reached;

   public int nodes;

   public int properties;

   public int docElement;

   public void endDocument() throws SAXException
   {
   }

   public void startDocument() throws SAXException
   {
      reached = true;
   }

   public void characters(char ch[], int start, int length) throws SAXException
   {
   }

   public void ignorableWhitespace(char ch[], int start, int length) throws SAXException
   {
   }

   public void endPrefixMapping(String prefix) throws SAXException
   {
   }

   public void skippedEntity(String name) throws SAXException
   {
   }

   public void setDocumentLocator(Locator locator)
   {
   }

   public void processingInstruction(String target, String data) throws SAXException
   {
   }

   public void startPrefixMapping(String prefix, String uri) throws SAXException
   {
   }

   public void endElement(String namespaceURI, String localName, String qName) throws SAXException
   {
   }

   public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
   {
      if ("sv:node".equals(qName))
         nodes++;
      else if ("sv:property".equals(qName))
         properties++;
      else
         docElement++;

   }
}
