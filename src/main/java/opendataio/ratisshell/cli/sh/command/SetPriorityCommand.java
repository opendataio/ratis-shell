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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Command for setting priority of the specific ratis server.
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
    String[] peersNewPriority = cl.getOptionValues(PEER_WITH_NEW_PRIORITY);
    if (peersNewPriority.length < 1) {
      return -2;
    }
    Map<String, Integer> addressPriorityMap = new HashMap<>();
    for (String peer : peersNewPriority) {
      String[] str = peer.split(",");
      addressPriorityMap.put(str[0], Integer.parseInt(str[1]));
    }

    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      List<RaftPeer> peers = new ArrayList<>();
      for (RaftPeer peer : mRaftGroup.getPeers()) {
        if (!addressPriorityMap.containsKey(peer.getAddress())) {
          peers.add(RaftPeer.newBuilder(peer).build());
        } else {
          peers.add(
                  RaftPeer.newBuilder(peer)
                          .setPriority(addressPriorityMap.get(peer.getAddress()))
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
            getCommandName(), PEER_OPTION_NAME, GROUPID_OPTION_NAME,
            SERVICE_ID_OPTION_NAME, PEER_WITH_NEW_PRIORITY);
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
