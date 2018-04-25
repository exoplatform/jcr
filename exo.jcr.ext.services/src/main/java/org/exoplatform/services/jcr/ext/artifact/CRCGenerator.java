/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by The eXo Platform SAS. <br>
 * Date: 21 Nov 2008
 * 
 * @author <a href="dkatayev@gmail.com">Dmytro Katayev</a>
 * @version $Id: CheckSumGenerator.java
 */
public class CRCGenerator {

  private static final String HEX = "0123456789abcdef";

  /**
   * Generates checksum for the InputStream.
   * 
   * @param in
   *          stream to generate CheckSum
   * @param algo
   *          algorithm name according to the
   *          {@literal
   *          <a href= "http://java.sun.com/j2se/1.4.2/docs/guide/security/CryptoSpec.html#AppA">
   *            Java Cryptography Architecture API Specification & Reference
   *          </a>
   *          }
   * @return hexadecimal string checksun representation
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */
  public static String getChecksum(InputStream in, String algo) throws NoSuchAlgorithmException,
                                                               IOException {

    MessageDigest md = MessageDigest.getInstance(algo);

    DigestInputStream digestInputStream = new DigestInputStream(in, md);
    digestInputStream.on(true);

    while (digestInputStream.read() > -1) {
      digestInputStream.read();
    }

    byte[] bytes = digestInputStream.getMessageDigest().digest();
    return generateString(bytes);
  }

  /**
   * Converts the array of bytes into a HEX string.
   * 
   * @param bytes
   *          byte array
   * @return HEX string
   */
  private static String generateString(byte[] bytes) {

    StringBuffer sb = new StringBuffer();

    for (byte b : bytes) {

      int v = b & 0xFF;

      sb.append((char) HEX.charAt(v >> 4));
      sb.append((char) HEX.charAt(v & 0x0f));
    }

    return sb.toString();
  }
}
