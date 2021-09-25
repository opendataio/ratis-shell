package opendataio.ratisshell.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Common utilities shared by all components.
 */
@ThreadSafe
public final class CommonUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CommonUtils.class);

  /**
   * Creates new instance of a class by calling a constructor that receives ctorClassArgs arguments.
   *
   * @param <T> type of the object
   * @param cls the class to create
   * @param ctorClassArgs parameters type list of the constructor to initiate, if null default
   *        constructor will be called
   * @param ctorArgs the arguments to pass the constructor
   * @return new class object
   * @throws RuntimeException if the class cannot be instantiated
   */
  public static <T> T createNewClassInstance(Class<T> cls, Class<?>[] ctorClassArgs,
      Object[] ctorArgs) {
    try {
      if (ctorClassArgs == null) {
        return cls.newInstance();
      }
      Constructor<T> ctor = cls.getConstructor(ctorClassArgs);
      return ctor.newInstance(ctorArgs);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private CommonUtils() {} // prevent instantiation
}
