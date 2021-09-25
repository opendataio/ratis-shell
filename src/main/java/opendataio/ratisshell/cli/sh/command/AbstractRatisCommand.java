package opendataio.ratisshell.cli.sh.command;

import opendataio.ratisshell.cli.Command;

import java.io.PrintStream;

public abstract class AbstractRatisCommand implements Command {
  protected final PrintStream mPrintStream;

  public AbstractRatisCommand(Context context) {
    mPrintStream = context.getPrintStream();
  }
}
