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
package org.exoplatform.services.jcr.ext.registry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by The eXo Platform SAS .<br/>
 * Encapsulates registry entry (i.e services', applications' etc settings)
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public final class RegistryEntry
{

   private Document document;

   /**
    * creates a RegistryEntry after XML DOM Document root element node name it is the name of the
    * Entry
    * 
    * @param dom
    */
   public RegistryEntry(Document dom)
   {
      this.document = dom;
   }

   /**
    * creates an empty RegistryEntry
    * 
    * @param rootName
    * @throws IOException
    * @throws SAXException
    * @throws ParserConfigurationException
    */
   public RegistryEntry(String rootName) throws IOException, SAXException, ParserConfigurationException
   {
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      this.document = db.newDocument();
      Element nodeElement = document.createElement(rootName);
      document.appendChild(nodeElement);
   }

   /**
    * Factory method to create RegistryEntry from serialized XML
    * 
    * @param bytes
    * @return RegistryEntry
    * @throws IOException
    * @throws SAXException
    * @throws ParserConfigurationException
    */
   public static RegistryEntry parse(byte[] bytes) throws IOException, SAXException, ParserConfigurationException
   {
      return new RegistryEntry(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
         new ByteArrayInputStream(bytes)));
   }

   /**
    * Factory method to create RegistryEntry from stream XML
    * 
    * @return RegistryEntry
    * @throws IOException
    * @throws SAXException
    * @throws ParserConfigurationException
    */
   public static RegistryEntry parse(InputStream in) throws IOException, SAXException, ParserConfigurationException
   {
      return new RegistryEntry(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in));
   }

   /**
    * @return the entry as InputStream
    * @throws IOException
    * @throws TransformerException
    */
   public InputStream getAsInputStream() throws TransformerException
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(out));

      return new ByteArrayInputStream(out.toByteArray());
   }

   /**
    * @return the name of entry (which is the same as underlying Document's root name)
    */
   public String getName()
   {
      return document.getDocumentElement().getNodeName();
   }

   /**
    * @return the underlying Document
    */
   public Document getDocument()
   {
      return document;
   }
}
