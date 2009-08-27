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

import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.util.NodeTypeRecognizer;
import org.exoplatform.services.security.ConversationState;

/**
 * The main purpose of class is determinate of import document type
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: NeutralImporter.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */
public class NeutralImporter
   extends BaseXmlImporter
{

   private ContentImporter contentImporter = null;

   public NeutralImporter(NodeData parent, QPath ancestorToSave, int uuidBehavior, ItemDataConsumer dataConsumer,
            NodeTypeDataManager ntManager, LocationFactory locationFactory, ValueFactoryImpl valueFactory,
            NamespaceRegistry namespaceRegistry, AccessManager accessManager, ConversationState userState,
            Map<String, Object> context, RepositoryImpl repository, String currentWorkspaceName)
   {
      super(parent, ancestorToSave, uuidBehavior, dataConsumer, ntManager, locationFactory, valueFactory,
               namespaceRegistry, accessManager, userState, context, repository, currentWorkspaceName);
   }

   /**
    * {@inheritDoc}
    */
   public void characters(char[] ch, int start, int length) throws RepositoryException
   {
      if (contentImporter == null)
      {
         throw new IllegalStateException("StartElement must be  call first");
      }
      contentImporter.characters(ch, start, length);
   }

   /**
    * {@inheritDoc}
    */
   public void endElement(String uri, String localName, String qName) throws RepositoryException
   {
      if (contentImporter == null)
      {
         throw new IllegalStateException("StartElement must be call first");
      }
      contentImporter.endElement(uri, localName, qName);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PlainChangesLog getChanges()
   {
      if (contentImporter != null)
         return contentImporter.getChanges();
      return super.getChanges();
   }

   /**
    * {@inheritDoc}
    */
   public void startElement(String namespaceURI, String localName, String name, Map<String, String> atts)
            throws RepositoryException
   {
      if (contentImporter == null)
      {
         switch (NodeTypeRecognizer.recognize(namespaceURI, name))
         {
            case DOCVIEW :
               contentImporter =
                        new DocumentViewImporter(getParent(), ancestorToSave, uuidBehavior, dataConsumer,
                                 nodeTypeDataManager, locationFactory, valueFactory, namespaceRegistry, accessManager,
                                 userState, context, repository, currentWorkspaceName);
               break;
            case SYSVIEW :
               contentImporter =
                        new SystemViewImporter(getParent(), ancestorToSave, uuidBehavior, dataConsumer,
                                 nodeTypeDataManager, locationFactory, valueFactory, namespaceRegistry, accessManager,
                                 userState, context, repository, currentWorkspaceName);
               break;
            default :
               throw new IllegalStateException("There was an error during ascertaining the "
                        + "type of document. First element " + namespaceURI + ":" + name);
         }
      }
      contentImporter.startElement(namespaceURI, localName, name, atts);
   }

}
