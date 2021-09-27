package opendataio.ratisshell.util.io;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;

/**
 * Utilities related to local file paths.
 */
@ThreadSafe
public final class PathUtils {
  private static final CharMatcher SEPARATOR_MATCHER =
      CharMatcher.is(File.separator.charAt(0));

  /**
   * Joins two path elements, separated by {@link File#separator}.
   * <p>
   * Note that empty element in base or paths is ignored.
   *
   * @param base base path
   * @param path path element to concatenate
   * @return joined path
   */
  public static String concatPath(Object base, Object path) {
    Preconditions.checkNotNull(base, "base");
    Preconditions.checkNotNull(path, "path");
    String trimmedBase = SEPARATOR_MATCHER.trimTrailingFrom(base.toString());
    String trimmedPath = SEPARATOR_MATCHER.trimFrom(path.toString());

    StringBuilder output = new StringBuilder(trimmedBase.length() + trimmedPath.length() + 1);
    output.append(trimmedBase);
    if (!trimmedPath.isEmpty()) {
      output.append(File.separator);
      output.append(trimmedPath);
    }

    if (output.length() == 0) {
      // base must be "[/]+"
      return File.separator;
    }
    return output.toString();
  }

  private PathUtils() {} // prevent instantiation
}
