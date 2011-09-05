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
package org.exoplatform.services.jcr.impl.xml.importing;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: StreamImporter.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */
public class StreamImporter implements RawDataImporter
{

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.StreamImporter");

   private final ContentImporter importer;

   private boolean namespacesRegistered = false;

   private final ItemDataKeeper dataKeeper;

   /**
    * @param saveType
    * @param parent
    * @param uuidBehavior
    * @param respectPropertyDefinitionsConstraints
    */
   public StreamImporter(NodeData parent, int uuidBehavior, ItemDataKeeper dataKeeper, ItemDataConsumer dataConsumer,
      NodeTypeDataManager ntManager, LocationFactory locationFactory, ValueFactoryImpl valueFactory,
      NamespaceRegistry namespaceRegistry, AccessManager accessManager, ConversationState userState,
      Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      super();
      this.dataKeeper = dataKeeper;
      this.importer =
         createContentImporter(parent, uuidBehavior, dataConsumer, ntManager, locationFactory, valueFactory,
            namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public ContentImporter createContentImporter(NodeData parent, int uuidBehavior, ItemDataConsumer dataConsumer,
      NodeTypeDataManager ntManager, LocationFactory locationFactory, ValueFactoryImpl valueFactory,
      NamespaceRegistry namespaceRegistry, AccessManager accessManager, ConversationState userState,
      Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      return new NeutralImporter(parent, parent.getQPath(), uuidBehavior, dataConsumer, ntManager, locationFactory,
         valueFactory, namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   /**
    * @param stream
    * @throws RepositoryException
    */
   public void importStream(InputStream stream) throws RepositoryException
   {

      XMLInputFactory factory = XMLInputFactory.newInstance();
      if (log.isDebugEnabled())
         log.debug("FACTORY: " + factory);

      try
      {

         XMLEventReader reader = factory.createXMLEventReader(stream);

         if (log.isDebugEnabled())
            log.debug("Start event handling");
         while (reader.hasNext())
         {
            XMLEvent event = reader.nextEvent();
            // log.info(event.toString());
            switch (event.getEventType())
            {
               case XMLStreamConstants.START_ELEMENT :
                  StartElement element = event.asStartElement();

                  if (!namespacesRegistered)
                  {
                     namespacesRegistered = true;
                     registerNamespaces(element);
                  }
                  Iterator attributes = element.getAttributes();
                  Map<String, String> attr = new HashMap<String, String>();
                  while (attributes.hasNext())
                  {
                     Attribute attribute = (Attribute)attributes.next();
                     attr.put(attribute.getName().getPrefix() + ":" + attribute.getName().getLocalPart(), attribute
                        .getValue());
                  }
                  QName name = element.getName();
                  importer.startElement(name.getNamespaceURI(), name.getLocalPart(), name.getPrefix() + ":"
                     + name.getLocalPart(), attr);
                  break;
               case XMLStreamConstants.END_ELEMENT :
                  EndElement endElement = event.asEndElement();
                  importer.endElement(endElement.getName().getNamespaceURI(), endElement.getName().getLocalPart(),
                     endElement.getName().getPrefix() + ":" + endElement.getName().getLocalPart());
                  break;
               case XMLStreamConstants.PROCESSING_INSTRUCTION :
                  break;
               case XMLStreamConstants.CHARACTERS :
                  String chars = event.asCharacters().getData();
                  importer.characters(chars.toCharArray(), 0, chars.length());
                  break;
               case XMLStreamConstants.COMMENT :
                  break;
               case XMLStreamConstants.START_DOCUMENT :
                  break;
               case XMLStreamConstants.END_DOCUMENT :
                  dataKeeper.save(importer.getChanges());
                  break;
               case XMLStreamConstants.ENTITY_REFERENCE :
                  break;
               case XMLStreamConstants.ATTRIBUTE :
                  break;
               case XMLStreamConstants.DTD :
                  break;
               case XMLStreamConstants.CDATA :
                  break;
               case XMLStreamConstants.SPACE :
                  break;
               default :
                  break;
            }
         }
         if (log.isDebugEnabled())
            log.debug("Event handling finished");

      }
      catch (XMLStreamException e)
      {
         throw new InvalidSerializedDataException("ImportXML failed", e);
      }
   }

   /**
    * @param event
    */
   private void registerNamespaces(StartElement event)
   {
      Iterator<Namespace> iter = event.getNamespaces();
      while (iter.hasNext())
      {
         Namespace namespace = iter.next();
         importer.registerNamespace(namespace.getPrefix(), namespace.getNamespaceURI());
      }
   }
}
