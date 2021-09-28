package opendataio.ratisshell.cli.sh.command;

import com.google.common.annotations.VisibleForTesting;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftPeer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
   * Command for setting priority of the specific ratis server.
   */
public class SetPriorityCommand extends AbstractRatisCommand {
  public static final String PEER_WITH_NEW_PRIORITY = "addressPriority";

  /**
   * @param context command context
   */
  public SetPriorityCommand(Context context) {
    super(context);
  }

  @Override
  public String getCommandName() {
    return "setPriority";
  }

  @Override
  public int run(CommandLine cl) throws IOException {
    super.run(cl);
    String[] peersNewPriority = cl.getOptionValue(PEER_WITH_NEW_PRIORITY).split(",");
    if (peersNewPriority.length != 2) {
      return -2;
    }
    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      List<RaftPeer> peers = new ArrayList<>();
      for (RaftPeer peer : mRaftGroup.getPeers()) {
        if (!peer.getAddress().equals(peersNewPriority[0])) {
          peers.add(RaftPeer.newBuilder(peer).build());
        } else {
          int priority = Integer.parseInt(peersNewPriority[1]);
          peers.add(
                  RaftPeer.newBuilder(peer)
                          .setPriority(priority)
                          .build()
          );
        }
      }
      RaftClientReply reply = client.admin().setConfiguration(peers);
      processReply(reply, "failed to set master priorities");
    }
    return 0;
  }

  @Override
  public String getUsage() {
    return String.format("%s"
                    + " [-%s PEER0_HOST:PEER0_PORT,PEER1_HOST:PEER1_PORT,PEER2_HOST:PEER2_PORT]"
                    + " [-%s RAFT_GROUP_ID]"
                    + " [-%s SERVICE_ID]"
                    + " [-%s PEER_HOST:PEER_PORT,PRIORITY]",
            getCommandName(), PEER_OPTION_NAME, GROUPID_OPTION_NAME, SERVICE_ID_OPTION_NAME);
  }

  @Override
  public String getDescription() {
    return description();
  }

  @Override
  public Options getOptions() {
    return new Options()
            .addOption(PEER_OPTION_NAME, true, "Peer addresses seperated by comma")
            .addOption(GROUPID_OPTION_NAME, true, "Raft group id")
            .addOption(SERVICE_ID_OPTION_NAME, true, "Service id")
            .addOption(PEER_WITH_NEW_PRIORITY, true, "Peer information with priority");
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Set priority of a specific raft peer";
  }
}
