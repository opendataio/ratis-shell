package opendataio.ratisshell.cli;

import com.google.common.io.Closer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Abstract class for handling command line inputs.
 */
@NotThreadSafe
public abstract class AbstractShell implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractShell.class);

  private Map<String, Command> mCommands;
  protected Closer mCloser;

  /**
   * Creates a new instance of {@link AbstractShell}.
   */
  public AbstractShell() {
    mCloser = Closer.create();
    mCommands = loadCommands();
    // Register all loaded commands under closer.
    mCommands.values().stream().forEach((cmd) -> mCloser.register(cmd));
  }

  /**
   * Handles the specified shell command request, displaying usage if the command format is invalid.
   *
   * @param argv [] Array of arguments given by the user's input from the terminal
   * @return 0 if command is successful, -1 if an error occurred
   */
  public int run(String... argv) {
    if (argv.length == 0) {
      printUsage();
      return -1;
    }

    // Sanity check on the number of arguments
    String cmd = argv[0];
    Command command = mCommands.get(cmd);

    if (command == null) {
      // Unknown command (we didn't find the cmd in our dict)
      System.err.println(String.format("%s is an unknown command.", cmd));
      printUsage();
      return -1;
    }

    // Find the inner-most command and its argument line.
    CommandLine cmdline;
    try {
      String[] currArgs = Arrays.copyOf(argv, argv.length);
      while (command.hasSubCommand()) {
        if (currArgs.length < 2) {
          throw new IllegalArgumentException("No sub-command is specified");
        }
        if (!command.getSubCommands().containsKey(currArgs[1])) {
          throw new IllegalArgumentException("Unknown sub-command: " + currArgs[1]);
        }
        command = command.getSubCommands().get(currArgs[1]);
        if (currArgs.length >= 2) {
          currArgs = Arrays.copyOfRange(currArgs, 1, currArgs.length);
        }
      }
      currArgs = Arrays.copyOfRange(currArgs, 1, currArgs.length);

      cmdline = command.parseAndValidateArgs(currArgs);
    } catch (IllegalArgumentException e) {
      // It outputs a prompt message when passing wrong args to CLI
      System.out.println(e.getMessage());
      System.out.println("Usage: " + command.getUsage());
      System.out.println(command.getDescription());
      LOG.error("Invalid arguments for command {}:", command.getCommandName(), e);
      return -1;
    }

    // Handle the command
    try {
      return command.run(cmdline);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      LOG.error("Error running " + StringUtils.join(argv, " "), e);
      return -1;
    }
  }

  /**
   * @return all commands provided by this shell
   */
  public Collection<Command> getCommands() {
    return mCommands.values();
  }

  @Override
  public void close() throws IOException {
    mCloser.close();
  }

  /**
   * @return name of the shell
   */
  protected abstract String getShellName();

  /**
   * Map structure: Command name => {@link Command} instance.
   *
   * @return a set of commands which can be executed under this shell
   */
  protected abstract Map<String, Command> loadCommands();

  /**
   * Prints usage for all commands.
   */
  protected void printUsage() {
    System.out.println("Usage: ratis " + getShellName() + " [generic options]");
    SortedSet<String> sortedCmds = new TreeSet<>(mCommands.keySet());
    for (String cmd : sortedCmds) {
      System.out.format("%-60s%n", "\t [" + mCommands.get(cmd).getUsage() + "]");
    }
  }
}
