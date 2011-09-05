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
package org.exoplatform.frameworks.jcr.command.web.fckeditor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: FCKConnectorXMLOutput.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class FCKConnectorXMLOutput extends AbstractFCKConnector
{

   protected Element rootElement;

   protected void initRootElement(String commandStr, String typeStr, String currentPath, String currentUrl)
      throws ParserConfigurationException
   {

      Document doc = null;
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.newDocument();

      rootElement = doc.createElement("Connector");
      doc.appendChild(rootElement);
      rootElement.setAttribute("command", commandStr);
      rootElement.setAttribute("resourceType", typeStr);

      Element myEl = doc.createElement("CurrentFolder");
      myEl.setAttribute("path", currentPath);
      myEl.setAttribute("url", currentUrl);
      rootElement.appendChild(myEl);
   }

   protected void outRootElement(HttpServletResponse response) throws Exception
   {
      response.setContentType("text/xml; charset=UTF-8");
      response.setHeader("Cache-Control", "no-cache");
      PrintWriter out = response.getWriter();

      rootElement.normalize();
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();
      DOMSource source = new DOMSource(rootElement.getOwnerDocument());

      StreamResult result = new StreamResult(out);
      transformer.transform(source, result);
      out.flush();
      out.close();
   }
}
