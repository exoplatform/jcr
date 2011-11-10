/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.impl.core.query.lucene;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Payload;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * <code>SingletonTokenStream</code> implements a token stream that wraps a
 * single value with a given property type. The property type is stored as a
 * payload on the single returned token.
 */
public final class SingletonTokenStream extends TokenStream implements Externalizable
{

   /**
    * The string value of the token.
    */
   private String value;

   /**
    * The payload of the token.
    */
   private Payload payload;

   private boolean hasNext = true;

   /**
    * for serialization 
    */
   public SingletonTokenStream()
   {
   }

   /**
    * Creates a new SingleTokenStream with the given value and a property
    * <code>type</code>.
    *
    * @param value the string value that will be returned with the token.
    * @param type the JCR property type.
    */
   public SingletonTokenStream(String value, int type)
   {
      this.value = value;
      this.payload = new Payload(new PropertyMetaData(type).toByteArray());
   }

   /**
    * Creates a new SingleTokenStream with the given token.
    *
    * @param t the token.
    */
   public SingletonTokenStream(Token t)
   {
      this.value = t.term();
      this.payload = t.getPayload();
   }

   /**
    * {@inheritDoc}
    */
   public Token next(Token reusableToken) throws IOException
   {
      if (hasNext)
      {
         reusableToken.clear();
         reusableToken.setTermBuffer(value);
         reusableToken.setPayload(payload);
         reusableToken.setStartOffset(0);
         reusableToken.setEndOffset(value.length());
         hasNext = false;
         return reusableToken;
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      payload = (Payload)in.readObject();
      int length = in.readInt();
      byte[] binValue = new byte[length];
      in.read(binValue);
      value = new String(binValue, Constants.DEFAULT_ENCODING);
   }

   /**
    * {@inheritDoc}
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // skip writing hasNext field
      out.writeObject(payload);
      byte[] binValue = value.getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(binValue.length);
      out.write(binValue);
   }
}
