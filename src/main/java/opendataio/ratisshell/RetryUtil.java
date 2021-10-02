package opendataio.ratisshell;

import java.util.List;
import java.util.function.Function;

/**
 * Retry related utilities function.
 */
public class RetryUtil {

  /**
   * Execute a given function with input parameter from member of list.
   *
   * @param list the input parameters
   * @param function the function to be executed
   * @param <T> parameter type
   * @param <K> return value type
   * @return the function return value
   */
  public static <T, K> K run(List<T> list, Function<T, K> function) {
    for (T t : list) {
      try {
        K ret = function.apply(t);
        if (ret != null) {
          return ret;
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
