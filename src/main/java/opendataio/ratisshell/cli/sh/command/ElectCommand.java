package opendataio.ratisshell.cli.sh.command;

import com.google.common.annotations.VisibleForTesting;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for transferring the leadership to another peer.
 */
public class ElectCommand extends AbstractRatisCommand {
  public static final String ADDRESS_OPTION_NAME = "address";

  /**
   * @param context command context
   */
  public ElectCommand(Context context) {
    super(context);
  }

  @Override
  public String getCommandName() {
    return "elect";
  }

  @Override
  public int run(CommandLine cl) throws IOException {
    super.run(cl);

    String strAddr = cl.getOptionValue(ADDRESS_OPTION_NAME);

    RaftPeerId newLeaderId = null;
    // update priorities to enable transfer
    List<RaftPeer> peersWithNewPriorities = new ArrayList<>();
    for (RaftPeer peer : mRaftGroup.getPeers()) {
      peersWithNewPriorities.add(
          RaftPeer.newBuilder(peer)
              .setPriority(peer.getAddress().equals(strAddr) ? 2 : 1)
              .build()
      );
      if (peer.getAddress().equals(strAddr)) {
        newLeaderId = peer.getId();
      }
    }
    if (newLeaderId == null) {
      return -2;
    }
    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      String stringPeers = "[" + peersWithNewPriorities.stream().map(RaftPeer::toString)
          .collect(Collectors.joining(", ")) + "]";
      mPrintStream.printf(
          "Applying new peer state before transferring leadership: %n%s%n", stringPeers);
      RaftClientReply setConfigurationReply =
          client.admin().setConfiguration(peersWithNewPriorities);
      processReply(setConfigurationReply,
          "failed to set priorities before initiating election");
      // transfer leadership
      mPrintStream.printf(
          "Transferring leadership to server with address <%s> %n", strAddr);
      try {
        Thread.sleep(3_000);
        RaftClientReply transferLeadershipReply =
            client.admin().transferLeadership(newLeaderId, 60_000);
        processReply(transferLeadershipReply, "election failed");
      } catch (Throwable t) {
        mPrintStream.printf("caught an error when executing transfer: %s%n", t.getMessage());
        return -1;
      }
      mPrintStream.println("Transferring leadership initiated");
    }
    return 0;
  }

  @Override
  public void validateArgs(CommandLine cl) throws IllegalArgumentException {
    super.validateArgs(cl);
    if (!cl.hasOption(ADDRESS_OPTION_NAME)) {
      throw new IllegalArgumentException(String.format("[%s]", ADDRESS_OPTION_NAME));
    }
  }

  @Override
  public String getUsage() {
    return String.format("%s -%s <HOSTNAME:PORT>"
        + " [-%s PEER0_HOST:PEER0_PORT,PEER1_HOST:PEER1_PORT,PEER2_HOST:PEER2_PORT]"
        + " [-%s RAFT_GROUP_ID]"
        + " [-%s SERVICE_ID]",
        getCommandName(), ADDRESS_OPTION_NAME, PEER_OPTION_NAME,
        GROUPID_OPTION_NAME, SERVICE_ID_OPTION_NAME);
  }

  @Override
  public String getDescription() {
    return description();
  }

  @Override
  public Options getOptions() {
    return super.getOptions()
        .addOption(ADDRESS_OPTION_NAME, true,
            "Server address that will take over as leader");
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Transfers leadership to the <hostname>:<port>";
  }
}
