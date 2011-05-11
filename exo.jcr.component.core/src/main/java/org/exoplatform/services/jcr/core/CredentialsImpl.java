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
package org.exoplatform.services.jcr.core;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: CredentialsImpl.java 12841 2007-02-16 08:58:38Z peterit $
 */

public class CredentialsImpl implements Credentials
{

   private SimpleCredentials simpleCredentials;

   public CredentialsImpl(String userID, char[] password)
   {
      this.simpleCredentials = new SimpleCredentials(userID, password);
   }

   /**
    * @param name
    * @return
    */
   public Object getAttribute(String name)
   {
      return simpleCredentials.getAttribute(name);
   }

   /**
    * @return
    */
   public String[] getAttributeNames()
   {
      return simpleCredentials.getAttributeNames();
   }

   /**
    * @return
    */
   public char[] getPassword()
   {
      return simpleCredentials.getPassword();
   }

   /**
    * @return
    */
   public String getUserID()
   {
      return simpleCredentials.getUserID();
   }

   /**
    * @param name
    */
   public void removeAttribute(String name)
   {
      simpleCredentials.removeAttribute(name);
   }

   /**
    * @param name
    * @param value
    */
   public void setAttribute(String name, Object value)
   {
      simpleCredentials.setAttribute(name, value);
   }

   public String toString()
   {
      return simpleCredentials.toString();
   }

   public int hashCode()
   {
      return simpleCredentials.hashCode();
   }

   public boolean equals(Object arg0)
   {
      return simpleCredentials.equals(arg0);
   }

}
