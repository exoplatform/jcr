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
import org.exoplatform.services.security.ConversationState;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ContentHandlerImporter.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */
public class ContentHandlerImporter implements ContentHandler, ErrorHandler, RawDataImporter
{

   private final ContentImporter importer;

   private final ItemDataKeeper dataKeeper;

   public ContentHandlerImporter(NodeData parent, int uuidBehavior, ItemDataKeeper dataKeeper,
      ItemDataConsumer dataConsumer, NodeTypeDataManager ntManager, LocationFactory locationFactory,
      ValueFactoryImpl valueFactory, NamespaceRegistry namespaceRegistry, AccessManager accessManager,
      ConversationState userState, Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      this.dataKeeper = dataKeeper;
      this.importer =
         createContentImporter(parent, uuidBehavior, dataConsumer, ntManager, locationFactory, valueFactory,
            namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);

   }

   /*
    * (non-Javadoc)
    * @see org.xml.sax.ContentHandler#characters(char[], int, int)
    */
   public void characters(char ch[], int start, int length) throws SAXException
   {
      try
      {
         importer.characters(ch, start, length);
      }
      catch (RepositoryException e)
      {
         throw new SAXException(e);
      }

   }

   /**
    * Create ContentImporter.
    * 
    * @param parent
    * @param uuidBehavior
    * @param dataConsumer
    * @param ntManager
    * @param locationFactory
    * @param valueFactory
    * @param namespaceRegistry
    * @param accessManager
    * @param userState
    * @param context
    * @param repository
    * @param currentWorkspaceName
    * @return
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
    * {@inheritDoc}
    */
   public void endDocument() throws SAXException
   {
      try
      {
         dataKeeper.save(importer.getChanges());
      }
      catch (RepositoryException e)
      {
         // e.printStackTrace();
         throw new SAXException(e);
      }
      catch (IllegalStateException e)
      {
         throw new SAXException(e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      try
      {
         importer.endElement(uri, localName, qName);
      }
      catch (RepositoryException e)
      {
         throw new SAXException(e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public void endPrefixMapping(String arg0) throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void error(SAXParseException exception) throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void fatalError(SAXParseException exception) throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void processingInstruction(String arg0, String arg1) throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void setDocumentLocator(Locator arg0)
   {
   }

   /**
    * {@inheritDoc}
    */
   public void skippedEntity(String arg0) throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void startDocument() throws SAXException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
   {
      try
      {
         // /!!!!
         Map<String, String> attribute = new HashMap<String, String>();
         for (int i = 0; i < atts.getLength(); i++)
         {
            attribute.put(atts.getQName(i), atts.getValue(i));
         }

         importer.startElement(uri, localName, qName, attribute);
      }
      catch (RepositoryException e)
      {
         // e.printStackTrace();
         throw new SAXException(e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public void startPrefixMapping(String prefix, String uri) throws SAXException
   {
      importer.registerNamespace(prefix, uri);

   }

   /**
    * {@inheritDoc}
    */
   public void warning(SAXParseException exception) throws SAXException
   {
   }

}
