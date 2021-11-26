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

package org.exoplatform.services.jcr.config;

import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ValueStorageEntry.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ValueStorageEntry extends ExtendedMappedParametrizedObjectEntry
{
   public static final String VALUE_STORAGE = "value-storage";
   
   private String id;

   private List<ValueStorageFilterEntry> filters;

   public ValueStorageEntry()
   {
      super(VALUE_STORAGE);
   }

   public ValueStorageEntry(String type, List<SimpleParameterEntry> params)
   {
      super(type, params, VALUE_STORAGE);
   }

   public List<ValueStorageFilterEntry> getFilters()
   {
      return filters;
   }

   public void setFilters(List<ValueStorageFilterEntry> filters)
   {
      this.filters = filters;
   }

   public String getId()
   {
      return id;
   }

   public void setId(String id)
   {
      this.id = id;
   }
}
