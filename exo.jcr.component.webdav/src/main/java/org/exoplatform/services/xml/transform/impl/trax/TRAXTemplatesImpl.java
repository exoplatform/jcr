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
import org.exoplatform.services.xml.transform.trax.TRAXTemplates;
import org.exoplatform.services.xml.transform.trax.TRAXTransformer;

import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;

/**
 * Created by The eXo Platform SAS . Wrapper for Trax Transformer.
 * 
 * @author <a href="mailto:alex.kravchuk@gmail.com">Alexander Kravchuk</a>
 * @version $Id: TRAXTemplatesImpl.java 5799 2006-05-28 17:55:42Z geaz $
 */

public class TRAXTemplatesImpl implements TRAXTemplates
{
   private Templates templates;

   private XMLResolvingService resolvingService;

   public TRAXTemplatesImpl(Templates templates)
   {
      this.templates = templates;
   }

   public Properties getOutputProperties()
   {
      return templates.getOutputProperties();
   }

   public TRAXTransformer newTransformer() throws TransformerConfigurationException
   {
      TRAXTransformerImpl transf = new TRAXTransformerImpl(this.templates);
      transf.setResolvingService(resolvingService);
      return transf;
   }

   public void setResolvingService(XMLResolvingService r)
   {
      resolvingService = r;
   }
}
