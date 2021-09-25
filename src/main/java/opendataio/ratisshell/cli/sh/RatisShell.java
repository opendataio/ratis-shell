package opendataio.ratisshell.cli.sh;

import opendataio.ratisshell.cli.AbstractShell;
import opendataio.ratisshell.cli.Command;
import opendataio.ratisshell.cli.sh.command.Context;
import opendataio.ratisshell.util.CommonUtils;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class RatisShell extends AbstractShell {

  public static void main(String[] args) throws IOException {
    RatisShell extensionShell = new RatisShell();
    System.exit(extensionShell.run(args));
  }

  @Override
  protected String getShellName() {
    return "sh";
  }

  @Override
  protected Map<String, Command> loadCommands() {
    Context adminContext = new Context(System.out);
    return loadCommands(RatisShell.class.getPackage().getName(),
        new Class[] {Context.class},
        new Object[] {mCloser.register(adminContext)});
  }

  /**
   * Get instances of all subclasses of {@link Command} in a sub-package called "command" the given
   * package.
   *
   * @param pkgName package prefix to look in
   * @param classArgs type of args to instantiate the class
   * @param objectArgs args to instantiate the class
   * @return a mapping from command name to command instance
   */
  public static Map<String, Command> loadCommands(String pkgName, Class[] classArgs,
      Object[] objectArgs) {
    Map<String, Command> commandsMap = new HashMap<>();
    Reflections reflections = new Reflections(RatisShell.class.getPackage().getName());
    for (Class<? extends Command> cls : reflections.getSubTypesOf(Command.class)) {
      // Add commands from <pkgName>.command.*
      if (cls.getPackage().getName().equals(pkgName + ".command")
          && !Modifier.isAbstract(cls.getModifiers())) {
        // Only instantiate a concrete class
        Command cmd = CommonUtils.createNewClassInstance(cls, classArgs, objectArgs);
        commandsMap.put(cmd.getCommandName(), cmd);
      }
    }
    return commandsMap;
  }
}
