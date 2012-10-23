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

package org.exoplatform.services.jcr.impl.ext.action;

import org.apache.commons.chain.Context;
import org.exoplatform.services.command.action.Action;

/**
 * @author <a href="mailto:dvishinskiy@exoplatform.com">Dmitriy Vyshinskiy</a>
 * @version $Id: $
 */
public interface AdvancedAction extends Action
{

   /**
    * This method will be called in case an exception occurs while executing the AdvancedAction.
    * This method will be called instead of exception logging (default behavior).
    * @param parentException - is the Exception that occurred while executing the AdvancedAction
    * @param context The {@link Context} that has been processed by the {@link AdvancedAction}
    * @throws AdvancedActionException if error occurs.
    */
   void onError(Exception parentException, Context context) throws AdvancedActionException;
}
