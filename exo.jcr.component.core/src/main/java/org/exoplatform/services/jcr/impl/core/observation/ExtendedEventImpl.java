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
package org.exoplatform.services.jcr.impl.core.observation;

import org.exoplatform.services.jcr.observation.ExtendedEvent;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 15.01.2014
 *
 * @author <a href="mailto:aymen.boughzela@exoplatform.com.ua">Aymen Boughzela</a>
 * @version $Id: ExtendedEventImpl.java 34360 2014-01-15 11:11:11Z aymen $
 */
public class ExtendedEventImpl extends EventImpl implements ExtendedEvent
{
    private final String itemId;

    private final String userData;

    private final long timestamp;

    private final Map<String, String> info;

    /**
     * The key <code>srcAbsPath</code> in the info map.
     */
    static final String SRC_ABS_PATH = "srcAbsPath";

    /**
     * The key <code>destAbsPath</code> in the info map.
     */
    static final String DEST_ABS_PATH = "destAbsPath";

    /**
     * The key <code>srcChildRelPath</code> in the info map.
     */
    static final String SRC_CHILD_REL_PATH = "srcChildRelPath";

    /**
     * The key <code>destChildRelPath</code> in the info map.
     */
    static final String DEST_CHILD_REL_PATH = "destChildRelPath";

    public ExtendedEventImpl(int type, String path, String itemId,
                     String userId, String userData, long timestamp,
                     Map<String, String> info)
    {
        super(type,path,userId);

        this.itemId = itemId;
        this.userData = userData;
        this.info = new HashMap<String, String>(info);
        this.timestamp = timestamp;
    }

    /**
     * {@inheritDoc}
     */
    public String getIdentifier()
    {
        return itemId;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getInfo() throws RepositoryException
    {
        return info;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserData() throws UnsupportedRepositoryOperationException
    {
        if (userData == null)
        {
            throw new UnsupportedRepositoryOperationException("Event.getUserData() not supported");
        }
        else
        {
            return userData;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getDate() throws RepositoryException
    {
        return timestamp;
    }
}
