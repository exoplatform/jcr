/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.checker.RepositoryCheckController;

import java.io.File;

/**
 * For testing purpose. Returns last log file to have possibility to delete.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: TesterRepositoryCheckController.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class TesterRepositoryCheckController extends RepositoryCheckController
{
   public TesterRepositoryCheckController(ManageableRepository repository)
   {
      super(repository);
   }

   public File getLastLogFile()
   {
      return new File(lastReport.getReportPath());
   }
}
