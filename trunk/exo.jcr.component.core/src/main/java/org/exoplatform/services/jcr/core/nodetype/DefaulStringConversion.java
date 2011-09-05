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
package org.exoplatform.services.jcr.core.nodetype;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Serialization/Deserialization for simple java types and Strings. For JiBX binding process only.
 * 
 * @author <a href="mailto:peterit@rambler.ru">Petro Nedonosko</a>
 */
public class DefaulStringConversion
{

   private static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DefaulStringConversion");

   public static String serializeString(String stringValue)
   {

      String r = "";
      try
      {
         r = stringValue != null ? stringValue : "";
      }
      catch (Exception e)
      {
         LOG.error("serializeString.Exception: " + e, e);
      }
      return r;
   }

   public static String deserializeString(String stringString)
   {

      String r = "";
      try
      {
         r = stringString != null ? stringString : "";
      }
      catch (Exception e)
      {
         LOG.error("deserializeString.Exception: " + e, e);
      }
      return r;
   }

   public static String serializeInt(int intValue)
   {
      String r = "";
      try
      {
         r = String.valueOf(intValue);
      }
      catch (Exception e)
      {
         LOG.error("serializeInt.Exception: " + e, e);
      }
      return r;
   }

   public static int deserializeInt(String intString)
   {

      int r = 0;
      try
      {
         r = Integer.parseInt(intString);
      }
      catch (Exception e)
      {
         LOG.error("deserializeInt.Exception: " + e, e);
      }
      return r;
   }

   public static String serializeLong(long longValue)
   {

      String r = "";
      try
      {
         r = String.valueOf(longValue);
      }
      catch (Exception e)
      {
         LOG.error("serializeLong.Exception: " + e, e);
      }
      return r;
   }

   public static long deserializeLong(String longString)
   {

      long r = 0;
      try
      {
         r = Long.parseLong(longString);
      }
      catch (Exception e)
      {
         LOG.error("deserializeLong.Exception: " + e, e);
      }
      return r;
   }

   public static String serializeLong(int longValue)
   {

      String r = "";
      try
      {
         r = String.valueOf(longValue);
      }
      catch (Exception e)
      {
         LOG.error("serializeLong.Exception: " + e, e);
      }
      return r;
   }

   public static boolean deserializeBoolean(String boolString)
   {

      boolean r = false;
      try
      {
         r = Boolean.parseBoolean(boolString);
      }
      catch (Exception e)
      {
         LOG.error("deserializeBoolean.Exception: " + e, e);
      }
      return r;
   }
}
