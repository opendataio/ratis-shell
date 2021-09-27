package opendataio.ratisshell;

import java.util.function.Supplier;

/**
 * Supplier for a configuration property default.
 */
public class DefaultSupplier implements Supplier<Object> {
  private final Supplier<Object> mSupplier;
  private final String mDescription;

  /**
   * @param supplier the value
   * @param description a description of the default value
   */
  public DefaultSupplier(Supplier<Object> supplier, String description) {
    mSupplier = supplier;
    mDescription = description;
  }

  @Override
  public Object get() {
    return mSupplier.get();
  }

  /**
   * @return a description of how the default value is determined
   */
  public String getDescription() {
    return mDescription;
  }
}
