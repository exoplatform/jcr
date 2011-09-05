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
package org.exoplatform.services.jcr.util;

import org.exoplatform.container.xml.Deserializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 18.06.2008
 * 
 * <br/> For use with JiBX binding in eXo configuration.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ConfigurationFormat.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ConfigurationFormat
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ConfigurationFormat");

   public static boolean parseBoolean(String text)
   {
      try
      {
         text = Deserializer.resolveNClean(text);
         return Boolean.valueOf(text).booleanValue();
      }
      catch (Throwable e)
      {
         LOG.warn("Unparseable boolean '" + text + "'.", e);
         return false;
      }
   }

   public static int parseInt(String text)
   {
      try
      {
         text = Deserializer.resolveNClean(text);
         return StringNumberParser.parseInt(text);
      }
      catch (Throwable e)
      {
         LOG.warn("Unparseable int '" + text + "'. Check StringNumberParser.parseInt for details.", e);
         return 0;
      }
   }

   public static long parseLong(String text)
   {
      try
      {
         text = Deserializer.resolveNClean(text);
         return StringNumberParser.parseLong(text);
      }
      catch (Throwable e)
      {
         LOG.warn("Unparseable long '" + text + "'. Check StringNumberParser.parseLong for details.", e);
         return 0l;
      }
   }

   public static long parseTime(String text)
   {
      try
      {
         text = Deserializer.resolveNClean(text);
         return StringNumberParser.parseTime(text);
      }
      catch (Throwable e)
      {
         LOG.warn("Unparseable time (as long) '" + text + "'. Check StringNumberParser.parseTime for details.", e);
         return 0l;
      }
   }

}
