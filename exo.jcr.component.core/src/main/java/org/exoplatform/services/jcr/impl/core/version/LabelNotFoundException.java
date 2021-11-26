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

package org.exoplatform.services.jcr.impl.core.version;

import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS 11.12.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko </a>
 * @version $Id: LabelNotFoundException.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class LabelNotFoundException extends VersionException
{

   LabelNotFoundException(String message)
   {
      super(message);
   }

   LabelNotFoundException(String message, Throwable e)
   {
      super(message, e);
   }
}
