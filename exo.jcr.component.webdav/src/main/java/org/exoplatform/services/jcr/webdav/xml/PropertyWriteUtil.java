/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.webdav.xml;

import org.exoplatform.common.util.HierarchicalProperty;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public class PropertyWriteUtil
{
   private final static Pattern ESCAPE_PATTERN = Pattern.compile("%[0-9a-fA-F]{2}");

   /**
    * Writes the statuses of properties into XML.
    * 
    * @param xmlStreamWriter XML writer
    * @param propStatuses properties statuses
    * @throws XMLStreamException {@link XMLStreamException}
    */
   public static void writePropStats(XMLStreamWriter xmlStreamWriter,
      Map<String, Set<HierarchicalProperty>> propStatuses) throws XMLStreamException
   {
      for (Map.Entry<String, Set<HierarchicalProperty>> stat : propStatuses.entrySet())
      {
         xmlStreamWriter.writeStartElement("DAV:", "propstat");

         xmlStreamWriter.writeStartElement("DAV:", "prop");
         for (HierarchicalProperty prop : propStatuses.get(stat.getKey()))
         {
            writeProperty(xmlStreamWriter, prop);
         }
         xmlStreamWriter.writeEndElement();

         xmlStreamWriter.writeStartElement("DAV:", "status");
         xmlStreamWriter.writeCharacters(stat.getKey());
         xmlStreamWriter.writeEndElement();

         // D:propstat
         xmlStreamWriter.writeEndElement();
      }
   }

   /**
    * Writes the statuses of property into XML.
    * 
    * @param xmlStreamWriter XML writer
    * @param prop property
    * @throws XMLStreamException {@link XMLStreamException}
    */
   public static void writeProperty(XMLStreamWriter xmlStreamWriter, HierarchicalProperty prop)
      throws XMLStreamException
   {
      String uri = prop.getName().getNamespaceURI();

      String prefix = xmlStreamWriter.getNamespaceContext().getPrefix(uri);

      if (prefix == null)
      {
         prefix = "";
      }

      String local = prop.getName().getLocalPart();

      if (prop.getValue() == null)
      {

         if (prop.getChildren().size() != 0)
         {
            xmlStreamWriter.writeStartElement(prefix, local, uri);
            if (!uri.equalsIgnoreCase("DAV:"))
            {
               xmlStreamWriter.writeNamespace(prefix, uri);
            }

            writeAttributes(xmlStreamWriter, prop);

            for (int i = 0; i < prop.getChildren().size(); i++)
            {
               HierarchicalProperty property = prop.getChildren().get(i);
               writeProperty(xmlStreamWriter, property);
            }
            xmlStreamWriter.writeEndElement();
         }
         else
         {
            xmlStreamWriter.writeEmptyElement(prefix, local, uri);
            if (!uri.equalsIgnoreCase("DAV:"))
            {
               xmlStreamWriter.writeNamespace(prefix, uri);
            }

            writeAttributes(xmlStreamWriter, prop);
         }

      }
      else
      {
         xmlStreamWriter.writeStartElement(prefix, local, uri);

         if (!uri.equalsIgnoreCase("DAV:"))
         {
            xmlStreamWriter.writeNamespace(prefix, uri);
         }

         writeAttributes(xmlStreamWriter, prop);
         xmlStreamWriter.writeCharacters(prop.getValue());
         
         xmlStreamWriter.writeEndElement();
      }
   }

   /**
    * Writes property attributes into XML.
    * 
    * @param xmlStreamWriter XML writer
    * @param property property
    * @throws XMLStreamException {@link XMLStreamException}
    */
   public static void writeAttributes(XMLStreamWriter xmlStreamWriter, HierarchicalProperty property)
      throws XMLStreamException
   {
      Map<String, String> attributes = property.getAttributes();
      Iterator<String> keyIter = attributes.keySet().iterator();
      while (keyIter.hasNext())
      {
         String attrName = keyIter.next();
         String attrValue = attributes.get(attrName);
         xmlStreamWriter.writeAttribute(attrName, attrValue);
      }
   }
}