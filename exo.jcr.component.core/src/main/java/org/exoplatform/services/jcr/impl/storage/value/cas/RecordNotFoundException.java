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

package org.exoplatform.services.jcr.impl.storage.value.cas;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br> Describe the state when CAS doesn't contains any record asked in context.
 * 
 * <br> Date: 15.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: RecordNotFoundException.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class RecordNotFoundException extends VCASException
{

   public RecordNotFoundException(String message, Throwable cause)
   {
      super(message, cause);
   }

   public RecordNotFoundException(String s)
   {
      super(s);
   }
}
