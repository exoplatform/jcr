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
package org.exoplatform.services.jcr.util;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.jcr.datamodel.Identifier;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov
 *         </a>
 * @version $Id: IdGenerator.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class IdGenerator {
  public static final int                    IDENTIFIER_LENGTH = IDGeneratorService.ID_LENGTH;

  private static volatile IDGeneratorService idGenerator;

  public IdGenerator(IDGeneratorService idGenerator) {
    if (IdGenerator.idGenerator == null) {
      synchronized (IdGenerator.class) {
        if (IdGenerator.idGenerator == null) {
          IdGenerator.idGenerator = idGenerator;
        }
      }
    }
  }

  public Identifier generateId(String path) {
    return new Identifier(getIdGenerator().generateStringID(path));
  }

  public String generateStringId(String path) {
    return getIdGenerator().generateStringID(path);
  }

  public static String generate() {
    return getIdGenerator().generateStringID(Long.toString(System.currentTimeMillis()));
  }

  public static IDGeneratorService getIdGenerator() {
    if (idGenerator == null) {
      idGenerator = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IDGeneratorService.class);
    }
    return idGenerator;
  }
}
