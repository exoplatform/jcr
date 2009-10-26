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
package org.exoplatform.services.jcr.impl.util;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.xml.XmlMapping;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: NodeTypeRecognizer.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class NodeTypeRecognizer
{
   public static XmlMapping recognize(InputStream is) throws IOException, SAXException, ParserConfigurationException
   {

      DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
      dfactory.setNamespaceAware(true);
      DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
      Document doc = docBuilder.parse(is);

      String namespaceURI = doc.getNamespaceURI();
      String name = doc.getDocumentElement().getNodeName();
      return recognize(namespaceURI, name);
   }

   public static XmlMapping recognize(String namespaceURI, String qName)
   {

      boolean hasSysName = qName != null && qName.toUpperCase().toLowerCase().startsWith(Constants.NS_SV_PREFIX + ":");
      if (Constants.NS_SV_URI.equals(namespaceURI) && hasSysName)
      {
         return XmlMapping.SYSVIEW;
      }
      return XmlMapping.DOCVIEW;
   }

}
