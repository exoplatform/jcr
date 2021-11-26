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

package org.exoplatform.services.jcr.datamodel;

import org.exoplatform.commons.utils.QName;

/**
 * Created by The eXo Platform SAS<br>
 *
 * Extends a QName with provides method to parse qname
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: InternalQName.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Unsupported
 */

public class InternalQName extends QName
{

   /**
    * InternalQName constructor.
    * 
    * @param namespace
    *          - namespace URI
    * @param name
    *          - Item name
    */
   public InternalQName(String namespace, String name)
   {
      super(namespace, name);
   }

   /**
    * Parse qname in form of eXo-JCR names conversion string. E.g. [name_space]item_name,
    * [http://www.jcp.org/jcr/nt/1.0]:base.
    * 
    * @param qName
    *          - String to be parsed
    * @return InternalQName instance
    * @throws IllegalNameException
    *           if String contains invalid QName
    */
   public static InternalQName parse(String qName) throws IllegalNameException
   {

      if (!qName.startsWith("["))
         throw new IllegalNameException("Invalid Internal QName '" + qName + "' Should start of '['");
      int uriStart = 0;
      int uriFinish = qName.indexOf("]", uriStart);
      if (uriFinish == -1)
         throw new IllegalNameException("Invalid Internal QName '" + qName + "' No closed ']'");
      String uri = qName.substring(uriStart + 1, uriFinish);
      String localName = qName.substring(uriFinish + 1, qName.length());
      return new InternalQName(uri, localName);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object o)
   {
      if (o == this)
         return true;

      if (o == null)
         return false;

      if (o instanceof InternalQName)
      {
         InternalQName that = (InternalQName)o;
         if (hashCode == that.hashCode)
         {
            String s1 = getAsString();
            String s2 = that.getAsString();
            return s1.equals(s2);
         }
      }

      return false;

   }
}
