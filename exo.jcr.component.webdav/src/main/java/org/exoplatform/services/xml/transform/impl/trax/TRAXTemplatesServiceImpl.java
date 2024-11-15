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

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.xml.transform.NotSupportedIOTypeException;
import org.exoplatform.services.xml.transform.trax.TRAXTemplates;
import org.exoplatform.services.xml.transform.trax.TRAXTemplatesService;
import org.exoplatform.services.xml.transform.trax.TRAXTransformerService;
import org.picocontainer.Startable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class TRAXTemplatesServiceImpl implements TRAXTemplatesService, Startable
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.xml-processing.TRAXTemplatesServiceImpl");

   private Map<String, TRAXTemplates> templates_;

   private TRAXTransformerService traxTransformerService_;

   public TRAXTemplatesServiceImpl(TRAXTransformerService traxTransformerService)
   {
      traxTransformerService_ = traxTransformerService;
      templates_ = new HashMap<String, TRAXTemplates>();
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.xml.transform.trax.TRAXTemplatesService#getTemplates
    * ( java.lang.String)
    */
   public TRAXTemplates getTemplates(String key)
   {
      return templates_.get(key);
   }

   /*
    * (non-Javadoc)
    * @seeorg.exoplatform.services.xml.transform.trax.TRAXTemplatesService#
    * addTRAXTemplates( java.lang.String,
    * org.exoplatform.services.xml.transform.trax.TRAXTemplates)
    */
   public void addTRAXTemplates(String key, TRAXTemplates templates) throws IllegalArgumentException
   {
      if (templates_.get(key) != null)
      {
         throw new IllegalArgumentException("Templates with key '" + key + "' already exists!");
      }
      templates_.put(key, templates);
   }

   /*
    * (non-Javadoc)
    * @seeorg.exoplatform.services.xml.transform.trax.TRAXTemplatesService#
    * addTRAXTemplates( java.lang.String, javax.xml.transform.Source)
    */
   public void addTRAXTemplates(String key, Source source) throws IllegalArgumentException
   {
      if (templates_.get(key) != null)
      {
         throw new IllegalArgumentException("Templates with key '" + key + "' already exists!");
      }
      try
      {
         templates_.put(key, traxTransformerService_.getTemplates(source));
      }
      catch (NotSupportedIOTypeException e)
      {
         throw new IllegalArgumentException("Source has unsupported context." + e, e);
      }
      catch (TransformerException e)
      {
         throw new IllegalArgumentException("Can't get templates from source." + e, e);
      }
   }

   public void addPlugin(ComponentPlugin plugin)
   {
      if (plugin instanceof TRAXTemplatesLoaderPlugin)
      {
         Map<String, String> m = ((TRAXTemplatesLoaderPlugin)plugin).getTRAXTemplates();
         Set<String> keys = m.keySet();
         for (String key : keys)
         {
            String xsltSchema = m.get(key);
            try
            {
               if (Thread.currentThread().getContextClassLoader().getResource(xsltSchema) != null)
               {
                  LOG.info("XSLT schema found by relative path: " + xsltSchema);
                  addTRAXTemplates(key, traxTransformerService_.getTemplates(new StreamSource(Thread.currentThread().getContextClassLoader()
                     .getResourceAsStream(xsltSchema))));
               }
               else
                  LOG.error("XSLT schema not found: " + xsltSchema);
            }
            catch (IllegalArgumentException e)
            {
               LOG.error("Add new TRAXTemplates failed : " + e.getMessage());
            }
            catch (TransformerException e)
            {
               LOG.error("Add new TRAXTemplates failed : " + e.getMessage());
            }
            catch (NotSupportedIOTypeException e)
            {
               LOG.error("Add new TRAXTemplates failed : " + e.getMessage());
            }
         }
      }
   }

   // ------ Startable -------

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#start()
    */
   public void start()
   {
   }

   /*
    * (non-Javadoc)
    * @see org.picocontainer.Startable#stop()
    */
   public void stop()
   {
   }

}
