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
package org.exoplatform.services.xml.transform.trax;

import org.exoplatform.services.xml.transform.PipeTransformer;

import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @author <a href="mailto:alex.kravchuk@gmail.com">Alexander Kravchuk</a>
 * @version $Id: TRAXTransformer.java 5799 2006-05-28 17:55:42Z geaz $
 */
public interface TRAXTransformer extends PipeTransformer
{
   /**
    * @see javax.xml.transform.Transformer#getParameter(java.lang.String)
    */
   Object getParameter(String param);

   /**
    * @see javax.xml.transform.Transformer#setParameter(java.lang.String,
    *      java.lang.Object)
    */
   void setParameter(String name, Object value);

   /**
    * @see javax.xml.transform.Transformer#clearParameters()
    */
   void clearParameters();

   /**
    * @see javax.xml.transform.Transformer#getOutputProperty(java.lang.String)
    */
   String getOutputProperty(String prop);

   /**
    * @see javax.xml.transform.Transformer#setOutputProperty(java.lang.String,
    *      java.lang.String)
    */
   void setOutputProperty(String name, String value);

   /**
    * @see javax.xml.transform.Transformer#setOutputProperties(java.util.Properties)
    */
   void setOutputProperties(Properties props);

   /**
    * @see javax.xml.transform.Transformer#getOutputProperties()
    */
   Properties getOutputProperties();

   /**
    * @see javax.xml.transform.Transformer#getURIResolver()
    */
   URIResolver getURIResolver();

   /**
    * @see javax.xml.transform.Transformer#setURIResolver(javax.xml.transform.URIResolver)
    */
   void setURIResolver(URIResolver resolver);

   /**
    * @see javax.xml.transform.Transformer#getErrorListener()
    */
   ErrorListener getErrorListener();

   /**
    * @see javax.xml.transform.Transformer#setErrorListener(javax.xml.transform.ErrorListener)
    */
   void setErrorListener(ErrorListener listener);

}
