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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Command for remove ratis server.
 */
public class QuorumRemoveCommand extends AbstractRatisCommand {
  public static final String REMOVE_PEER_ADDRESS = "removePeer";

  /**
   * @param context command context
   */
  public QuorumRemoveCommand(Context context) {
    super(context);
  }

  @Override
  public String getCommandName() {
    return "quorumRemove";
  }

  @Override
  public int run(CommandLine cl) throws IOException {
    super.run(cl);

    String[] addresses = cl.getOptionValues(REMOVE_PEER_ADDRESS);
    if (addresses.length < 1) {
      return -2;
    }
    List<RaftPeerId> raftPeerIds = new ArrayList<>();
    for (String address : addresses) {
      String[] str = address.split(":");
      InetSocketAddress serverAddress = InetSocketAddress
              .createUnresolved(str[0], Integer.parseInt(str[1]));
      RaftPeerId peerId = RaftUtils.getPeerId(serverAddress);
      raftPeerIds.add(peerId);
    }

    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      List<RaftPeer> peers = new ArrayList<>();
      for (RaftPeer peer : mRaftGroup.getPeers()) {
        if (!raftPeerIds.contains(peer.getId())) {
          peers.add(RaftPeer.newBuilder(peer).build());
        }
      }
      RaftClientReply reply = client.admin().setConfiguration(peers);
      processReply(reply, "failed to remove raft peer");
    }
    return 0;
  }

  @Override
  public String getUsage() {
    return String.format("%s"
                    + " [-%s PEER0_HOST:PEER0_PORT,PEER1_HOST:PEER1_PORT,PEER2_HOST:PEER2_PORT]"
                    + " [-%s RAFT_GROUP_ID]"
                    + " [-%s SERVICE_ID]"
                    + " [-%s PEER_HOST:PEER_PORT]",
            getCommandName(), PEER_OPTION_NAME, GROUPID_OPTION_NAME,
            SERVICE_ID_OPTION_NAME, REMOVE_PEER_ADDRESS);
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
            .addOption(REMOVE_PEER_ADDRESS, true, "peer address to be removed");
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Remove peers of a ratis group";
  }
}
