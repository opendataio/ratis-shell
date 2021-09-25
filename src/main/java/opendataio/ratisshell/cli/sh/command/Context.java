package opendataio.ratisshell.cli.sh.command;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;

public final class Context implements Closeable {
  private final PrintStream mPrintStream;
  private final Closer mCloser;
  public Context(PrintStream printStream) {
    mCloser = Closer.create();
    mCloser.register(
        mPrintStream = Preconditions.checkNotNull(printStream, "printStream"));
  }

  /**
   * @return the print stream to write to
   */
  public PrintStream getPrintStream() {
    return mPrintStream;
  }

  @Override
  public void close() throws IOException {
    mCloser.close();
  }
}
