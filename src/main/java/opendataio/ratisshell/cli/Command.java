package opendataio.ratisshell.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An interface for all the commands that can be run from a shell.
 */
public interface Command extends Closeable {

  /**
   * Gets the command name as input from the shell.
   *
   * @return the command name
   */
  String getCommandName();

  /**
   * @return the supported {@link Options} of the command
   */
  default Options getOptions() {
    return new Options();
  }

  /**
   * If a command has sub-commands, the first argument should be the sub-command's name,
   * all arguments and options will be parsed for the sub-command.
   *
   * @return whether this command has sub-commands
   */
  default boolean hasSubCommand() {
    return false;
  }

  /**
   * @return a map from sub-command names to sub-command instances
   */
  default Map<String, Command> getSubCommands() {
    return new HashMap<>();
  }

  /**
   * Parses and validates the arguments.
   *
   * @param args the arguments for the command, excluding the command name
   * @return the parsed command line object
   * @throws IllegalArgumentException when arguments are not valid
   */
  default CommandLine parseAndValidateArgs(String... args) throws IllegalArgumentException {
    CommandLine cmdline;
    Options opts = getOptions();
    CommandLineParser parser = new DefaultParser();
    try {
      cmdline = parser.parse(opts, args);
    } catch (ParseException e) {
      throw new IllegalArgumentException(
          String.format("Failed to parse args for %s: %s", getCommandName(), e.getMessage()), e);
    }
    validateArgs(cmdline);
    return cmdline;
  }

  /**
   * Checks if the arguments are valid or throw InvalidArgumentException.
   *
   * @param cl the parsed command line for the arguments
   * @throws IllegalArgumentException when arguments are not valid
   */
  default void validateArgs(CommandLine cl) throws IllegalArgumentException {}

  /**
   * Runs the command.
   *
   * @param cl the parsed command line for the arguments
   * @return the result of running the command
   */
  default int run(CommandLine cl) throws IOException {
    return 0;
  }

  /**
   * @return the usage information of the command
   */
  String getUsage();

  /**
   * @return the description information of the command
   */
  String getDescription();

  /**
   * Used to close resources created by commands.
   *
   * @throws IOException if closing resources fails
   */
  default void close() throws IOException {}
}
