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

package org.exoplatform.frameworks.jcr.cli;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public abstract class AbstractCliCommand implements Command
{

   public final boolean execute(Context ctx) throws Exception
   {
      CliAppContext cliCtx = (CliAppContext)ctx;
      cliCtx.clearOutput();
      boolean result = perform(cliCtx);
      return result;
   }

   public abstract boolean perform(CliAppContext ctx) throws Exception;

}
