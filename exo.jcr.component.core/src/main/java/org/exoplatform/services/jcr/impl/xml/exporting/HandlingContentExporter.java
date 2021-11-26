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

package org.exoplatform.services.jcr.impl.xml.exporting;

import org.exoplatform.services.jcr.dataflow.ItemDataConsumer;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version
 */
public abstract class HandlingContentExporter extends BaseXmlExporter
{

   protected final ContentHandler contentHandler;

   public HandlingContentExporter(ContentHandler handler, ItemDataConsumer dataManager,
      NamespaceRegistry namespaceRegistry, ValueFactoryImpl systemValueFactory, boolean skipBinary, boolean noRecurse)
      throws NamespaceException, RepositoryException
   {

      super(dataManager, namespaceRegistry, systemValueFactory, skipBinary, noRecurse, noRecurse ? 1 : -1);
      this.contentHandler = handler;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void export(NodeData node) throws RepositoryException, SAXException
   {
      if (contentHandler != null)
      {
         contentHandler.startDocument();
         startPrefixMapping();
         node.accept(this);
         endPrefixMapping();
         contentHandler.endDocument();
      }

   }

   protected void endPrefixMapping() throws RepositoryException, SAXException
   {
      String[] prefixes = getNamespaceRegistry().getPrefixes();
      for (String prefix : prefixes)
      {
         contentHandler.endPrefixMapping(prefix);
      }
   }

   protected void startPrefixMapping() throws RepositoryException, SAXException
   {
      String[] prefixes = getNamespaceRegistry().getPrefixes();
      for (String prefix : prefixes)
      {
         // skeep xml prefix
         if ((prefix == null) || (prefix.length() < 1) || prefix.equals(Constants.NS_XML_PREFIX))
         {
            continue;
         }
         contentHandler.startPrefixMapping(prefix, getNamespaceRegistry().getURI(prefix));
      }
   }
}
