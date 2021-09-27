package opendataio.ratisshell.conf;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import opendataio.ratisshell.util.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ratis shell configuration.
 */
public class InstancedConfiguration implements RatisShellConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(InstancedConfiguration.class);

  public static final RatisShellConfiguration EMPTY_CONFIGURATION
      = new InstancedConfiguration(new RatisShellProperties());

  /** Regex string to find "${key}" for variable substitution. */
  private static final String REGEX_STRING = "(\\$\\{([^{}]*)\\})";
  /** Regex to find ${key} for variable substitution. */
  private static final Pattern CONF_REGEX = Pattern.compile(REGEX_STRING);
  /** Source of the truth of all property values (default or customized). */
  protected RatisShellProperties mProperties;

  /**
   * Users should use this API to obtain a configuration for modification before passing to a
   * FileSystem constructor. The default configuration contains all default configuration params
   * and configuration properties modified in the ratis-shell-site.properties file.
   *
   * Example usage:
   *
   * InstancedConfiguration conf = InstancedConfiguration.defaults();
   * conf.set(...);
   * FileSystem fs = FileSystem.Factory.create(conf);
   *
   * WARNING: This API is unstable and may be changed in a future minor release.
   *
   * @return an instanced configuration preset with defaults
   */
  public static InstancedConfiguration defaults() {
    return new InstancedConfiguration(ConfigurationUtils.defaults());
  }

  /**
   * Creates a new instance of {@link InstancedConfiguration}.
   *
   * Application code should use {@link InstancedConfiguration#defaults}.
   *
   * @param properties properties underlying this configuration
   */
  public InstancedConfiguration(RatisShellProperties properties) {
    mProperties = properties;
  }

  /**
   * Creates a new instance of {@link InstancedConfiguration}.
   *
   * Application code should use {@link InstancedConfiguration#defaults}.
   *
   * @param conf configuration to copy
   */
  public InstancedConfiguration(RatisShellConfiguration conf) {
    mProperties = conf.copyProperties();
  }

  /**
   * @return the properties backing this configuration
   */
  public RatisShellProperties copyProperties() {
    return mProperties.copy();
  }

  @Override
  public String get(PropertyKey key) {
    String value = mProperties.get(key);
    if (value == null) {
      // if value or default value is not set in configuration for the given key
      throw new RuntimeException("undefined " + key);
    }
    try {
      value = lookup(value);
    } catch (UnresolvablePropertyException e) {
      throw new RuntimeException("Could not resolve key \""
          + key.getName() + "\": " + e.getMessage(), e);
    }
    return value;
  }

  private boolean isResolvable(PropertyKey key) {
    String val = mProperties.get(key);
    try {
      // Lookup to resolve any key before simply returning isSet. An exception will be thrown if
      // the key can't be resolved or if a lower level value isn't set.
      lookup(val);
      return true;
    } catch (UnresolvablePropertyException e) {
      return false;
    }
  }

  @Override
  public boolean isSet(PropertyKey key) {
    return mProperties.isSet(key) && isResolvable(key);
  }

  @Override
  public boolean isSetByUser(PropertyKey key) {
    return mProperties.isSetByUser(key) && isResolvable(key);
  }

  /**
   * Sets the value for the appropriate key in the {@link Properties} by source.
   *
   * @param key the key to set
   * @param value the value for the key
   */
  public void set(@Nonnull PropertyKey key, @Nonnull Object value) {
    Preconditions.checkArgument(key != null && value != null && !value.equals(""),
        String.format("The key value pair (%s, %s) cannot be null", key, value));
    Preconditions.checkArgument(!value.equals(""),
        String.format("The key \"%s\" cannot be have an empty string as a value. Use "
            + "ServerConfiguration.unset to remove a key from the configuration.", key));
    mProperties.put(key, String.valueOf(value));
  }

  /**
   * Unsets the value for the appropriate key in the {@link Properties}. If the {@link PropertyKey}
   * has a default value, it will still be considered set after executing this method.
   *
   * @param key the key to unset
   */
  public void unset(PropertyKey key) {
    Preconditions.checkNotNull(key, "key");
    mProperties.remove(key);
  }

  /**
   * Merges map of properties into the current properties.
   *
   * @param properties map of keys to values
   */
  public void merge(Map<?, ?> properties) {
    mProperties.merge(properties);
  }

  @Override
  public Set<PropertyKey> keySet() {
    return mProperties.keySet();
  }

  @Override
  public Set<PropertyKey> userKeySet() {
    return mProperties.userKeySet();
  }

  @Override
  public int getInt(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Integer.parseInt(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException("key not integer " + rawValue);
    }
  }

  @Override
  public long getLong(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Long.parseLong(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException("key not long " + rawValue);
    }
  }

  @Override
  public double getDouble(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Double.parseDouble(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException("key not double " + rawValue);
    }
  }

  @Override
  public float getFloat(PropertyKey key) {
    String rawValue = get(key);

    try {
      return Float.parseFloat(rawValue);
    } catch (NumberFormatException e) {
      throw new RuntimeException("key not float " + rawValue);
    }
  }

  @Override
  public boolean getBoolean(PropertyKey key) {
    String rawValue = get(key);

    if (rawValue.equalsIgnoreCase("true")) {
      return true;
    } else if (rawValue.equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new RuntimeException("key not boolean " + rawValue);
    }
  }

  @Override
  public List<String> getList(PropertyKey key, String delimiter) {
    Preconditions.checkArgument(delimiter != null,
        "Illegal separator for ratis-shell properties as list");
    String rawValue = get(key);
    return ConfigurationUtils.parseAsList(rawValue, delimiter);
  }

  @Override
  public <T extends Enum<T>> T getEnum(PropertyKey key, Class<T> enumType) {
    String rawValue = get(key).toUpperCase();
    try {
      return Enum.valueOf(enumType, rawValue);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("key not enum " + rawValue);
    }
  }

  @Override
  public <T> Class<T> getClass(PropertyKey key) {
    String rawValue = get(key);

    try {
      @SuppressWarnings("unchecked")
      Class<T> clazz = (Class<T>) Class.forName(rawValue);
      return clazz;
    } catch (Exception e) {
      LOG.error("requested class could not be loaded: {}", rawValue, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, String> getNestedProperties(PropertyKey prefixKey) {
    Map<String, String> ret = Maps.newHashMap();
    for (Map.Entry<PropertyKey, String> entry: mProperties.entrySet()) {
      String key = entry.getKey().getName();
      if (prefixKey.isNested(key)) {
        String suffixKey = key.substring(prefixKey.length() + 1);
        ret.put(suffixKey, entry.getValue());
      }
    }
    return ret;
  }

  @Override
  public void validate() {
  }

  /**
   * Lookup key names to handle ${key} stuff.
   *
   * @param base the String to look for
   * @return resolved String value
   */
  private String lookup(final String base) throws UnresolvablePropertyException {
    return lookupRecursively(base, new HashSet<>());
  }

  /**
   * Actual recursive lookup replacement.
   *
   * @param base the string to resolve
   * @param seen strings already seen during this lookup, used to prevent unbound recursion
   * @return the resolved string
   */
  private String lookupRecursively(String base, Set<String> seen)
      throws UnresolvablePropertyException {
    // check argument
    if (base == null) {
      throw new UnresolvablePropertyException("Can't resolve property with null value");
    }

    String resolved = base;
    // Lets find pattern match to ${key}.
    Matcher matcher = CONF_REGEX.matcher(base);
    while (matcher.find()) {
      String match = matcher.group(2).trim();
      if (!seen.add(match)) {
        throw new RuntimeException("KEY_CIRCULAR_DEPENDENCY");
      }
      if (!PropertyKey.isValid(match)) {
        throw new RuntimeException("INVALID_CONFIGURATION_KEY");
      }
      String value = lookupRecursively(mProperties.get(PropertyKey.fromString(match)), seen);
      seen.remove(match);
      if (value == null) {
        throw new UnresolvablePropertyException("UNDEFINED_CONFIGURATION_KEY");
      }
      resolved = resolved.replaceFirst(REGEX_STRING, Matcher.quoteReplacement(value));
    }
    return resolved;
  }

  private class UnresolvablePropertyException extends Exception {

    public UnresolvablePropertyException(String msg) {
      super(msg);
    }
  }
}
