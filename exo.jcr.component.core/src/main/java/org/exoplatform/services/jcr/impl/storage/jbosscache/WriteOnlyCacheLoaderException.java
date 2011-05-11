package org.exoplatform.services.jcr.impl.storage.jbosscache;

public class WriteOnlyCacheLoaderException
   extends Exception
{
   /**
    * Constructs an Exception without a message.
    */
   public WriteOnlyCacheLoaderException()
   {
      super();
   }

   /**
    * Constructs an Exception with a detailed message.
    * 
    * @param Message
    *          The message associated with the exception.
    */
   public WriteOnlyCacheLoaderException(String message)
   {
      super(message);
   }

   /**
   * Constructs an Exception with a detailed message and base exception.
   * 
   * @param Message
   *          The message associated with the exception.
   * @param cause
   *          Throwable, the base exception.
   */
   public WriteOnlyCacheLoaderException(String message, Throwable cause)
   {
      super(message, cause);
   }

   /**
    * WriteOnlyCacheLoaderException constructor.
    *
    * @param cause
    *         Throwable, the base exception.
    */
   public WriteOnlyCacheLoaderException(Throwable cause)
   {
      super(cause);
   }
}
