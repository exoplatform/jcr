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
package org.exoplatform.services.xml.transform.impl.trax;

import org.exoplatform.services.xml.resolving.XMLResolvingService;
import org.exoplatform.services.xml.transform.NotSupportedIOTypeException;
import org.exoplatform.services.xml.transform.trax.TRAXTemplates;
import org.exoplatform.services.xml.transform.trax.TRAXTransformer;
import org.exoplatform.services.xml.transform.trax.TRAXTransformerService;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @author <a href="mailto:alex.kravchuk@gmail.com">Alexander Kravchuk</a>
 * @version $Id: TRAXTransformerServiceImpl.java 5799 2006-05-28 17:55:42Z geaz
 *          $
 */

public class TRAXTransformerServiceImpl implements TRAXTransformerService
{

   private XMLResolvingService resolvingService;

   public TRAXTransformerServiceImpl(XMLResolvingService resolvingService)
   {
      this.resolvingService = resolvingService;
   }

   public TRAXTransformer getTransformer() throws TransformerConfigurationException
   {
      TRAXTransformerImpl transf = new TRAXTransformerImpl();
      transf.setResolvingService(resolvingService);
      return transf;
   }

   public TRAXTransformer getTransformer(Source source) throws TransformerConfigurationException
   {
      TRAXTransformerImpl transf = new TRAXTransformerImpl(source);
      transf.setResolvingService(resolvingService);
      return transf;
   }

   public TRAXTemplates getTemplates(Source source) throws TransformerException, NotSupportedIOTypeException
   {
      TRAXTemplatesImpl templates = new TRAXTemplatesImpl(getXSLTemplates(source));
      templates.setResolvingService(resolvingService);
      return templates;
   }

   private Templates getXSLTemplates(Source source) throws TransformerException, NotSupportedIOTypeException
   {
      SAXTransformerFactory saxTFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

      TemplatesHandler templateHandler = saxTFactory.newTemplatesHandler();
      XMLReader xmlReader;
      try
      {
         // xmlReader = XMLReaderFactory.
         // createXMLReader("org.apache.xerces.parsers.SAXParser");
         xmlReader = TRAXTransformerImpl.getXMLReader();
         // set default resolver
         if (resolvingService != null)
         {
            xmlReader.setEntityResolver(resolvingService.getEntityResolver());
         }

      }
      catch (SAXException ex)
      {
         throw new TransformerException(ex);
      }

      xmlReader.setContentHandler(templateHandler);
      InputSource inputSource = SAXSource.sourceToInputSource(source);
      if (inputSource == null)
      {
         throw new NotSupportedIOTypeException(source);
      }

      try
      {
         xmlReader.parse(inputSource);
      }
      catch (SAXException ex)
      {
         throw new TransformerException(ex);
      }
      catch (IOException ex)
      {
         throw new TransformerException(ex);
      }

      return templateHandler.getTemplates();

   }

}
