/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.itemfilters;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 24 трав. 2011 skarpenko $
 *
 */
public class TestPatternQPathEntryFilter extends JcrAPIBaseTest
{
   public void testPatternMap()
   {
      PatternQPathEntryFilter filter = new PatternQPathEntryFilter(new PatternQPathEntry("", "local*name", -1));

      Map<PatternQPathEntryFilter, String> map = new HashMap<PatternQPathEntryFilter, String>();

      map.put(new PatternQPathEntryFilter(new PatternQPathEntry("", "local*name", 3)), "secondvalue");
      map.put(filter, "value");
      map.put(new PatternQPathEntryFilter(new PatternQPathEntry("", "local*name", 31)), "thirdvalue");

      PatternQPathEntryFilter newfilter = new PatternQPathEntryFilter(new PatternQPathEntry("", "local*name", -1));

      String val = map.get(newfilter);

      assertEquals("value", val);
   }

   public void testPatternQPathEntryFilterExtrnalization() throws Exception
   {
      PatternQPathEntryFilter filter = new PatternQPathEntryFilter(new PatternQPathEntry("", "local*name", -1));

      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bout);

      filter.writeExternal(out);
      out.close();
      bout.close();

      ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
      ObjectInputStream in = new ObjectInputStream(bin);

      PatternQPathEntryFilter newFilter = new PatternQPathEntryFilter();
      newFilter.readExternal(in);
      in.close();
      bin.close();

      assertEquals(filter, newFilter);
   }
}
