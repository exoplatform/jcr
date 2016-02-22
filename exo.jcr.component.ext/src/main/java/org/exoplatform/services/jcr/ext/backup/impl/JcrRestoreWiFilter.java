/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.backup.impl;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: JcrRestoreWiFilter.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class JcrRestoreWiFilter
   implements FilenameFilter
{

   public boolean accept(File dir, String name)
   {
      return name.endsWith(".tmp") && name.startsWith("jcrrestorewi");
   }

}
