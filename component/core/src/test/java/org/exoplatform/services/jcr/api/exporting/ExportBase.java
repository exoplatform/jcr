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

import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ExportBase.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ExportBase
   extends JcrAPIBaseTest
{
   protected DocumentBuilder builder;

   protected XPath xpath;

   protected List<String[]> valList;

   public ExportBase() throws ParserConfigurationException
   {
      super();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true); // never forget this!
      factory.setValidating(false);
      factory.setIgnoringElementContentWhitespace(false);
      builder = factory.newDocumentBuilder();

      XPathFactory xPathFactory = XPathFactory.newInstance();
      xpath = xPathFactory.newXPath();

      valList = new LinkedList<String[]>();
      valList.add(new String[]
      {""});
      valList.add(new String[]
      {"1"});
      valList.add(new String[]
      {"1", "2"});
      valList.add(new String[]
      {"\">", "\"<"});
      valList.add(new String[]
      {"</sv:value>"});
      valList.add(new String[]
      {"</sv:value>", "</sv:value>"});
      valList.add(new String[]
      {"<sv:value>nt:unstructured</sv:value>"});
      valList.add(new String[]
      {"<sv:value>nt:unstructured</sv:value>", "<sv:value>nt:unstructured</sv:value>"});
      // !!!! /r http://www.w3.org/TR/2000/REC-xml-20001006#sec-line-ends
      valList.add(new String[]
      {"anvwiuehovi", "akf\"123\401/.m4gjsdlfg", "qp_f i\tsdfh\npihqebpf"});
      valList.add(new String[]
      {"bejhryi&oph<nb >3  'o[..=123-"});
      valList.add(new String[]
      {
               "\u043c\u0430\u043c\u0430 \u043c\u044b\u043b\u0430 \u0440\u0430\u043c\u0443.",
               "xin ch\u0413\u00a0o b\u0431\u0454\u040en ch\u0413\u0454ng "
                        + "t\u0413\u0491i \u0414\u2018\u0431\u0454\u0457n t\u0431\u00bb\u00ab "
                        + "ecm vi\u0431\u00bb\u2021t nam, ch\u0413\u0454ng t\u0413\u0491i "
                        + "c\u0413\u0456 th\u0431\u00bb\u0453 gi\u0413\u0454p g\u0413\u00ac cho "
                        + "b\u0431\u0454\u040en \u0414\u2018\u0416\u00b0\u0431\u00bb\u0408c kh\u0413\u0491ng "
                        + "v\u0431\u0454\u00ady ?"});

   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      xpath.setNamespaceContext(new JcrNamespaceContext(session));
   }
}
