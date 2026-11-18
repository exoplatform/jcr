/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.services.jcr.ext.index.persistent.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;

public class PersistentIndexChangesFilterTest extends TestCase {

  public void testAnIndexCopiedWithRsyncKeepsTheQueueMarks() {
    assertTrue(PersistentIndexChangesFilter.isCopiedWithoutAFlush(handler(SearchIndex.INDEX_RECOVERY_RSYNC_STRATEGY)));
    assertTrue(PersistentIndexChangesFilter.isCopiedWithoutAFlush(handler(SearchIndex.INDEX_RECOVERY_RSYNC_WITH_DELETE_STRATEGY)));
  }

  public void testAnIndexCopiedAfterAFlushTakesTheQueueMarksOfItsMarker() {
    assertFalse(PersistentIndexChangesFilter.isCopiedWithoutAFlush(handler(SearchIndex.INDEX_RECOVERY_DEFAULT_STRATEGY)));
    assertFalse(PersistentIndexChangesFilter.isCopiedWithoutAFlush(mock(QueryHandler.class)));
  }

  private static SearchIndex handler(String strategy) {
    SearchIndex handler = mock(SearchIndex.class);
    when(handler.getIndexRecoveryStrategy()).thenReturn(strategy);
    return handler;
  }
}
