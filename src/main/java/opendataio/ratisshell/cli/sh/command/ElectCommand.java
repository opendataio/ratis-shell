package opendataio.ratisshell.cli.sh.command;

import com.google.common.annotations.VisibleForTesting;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ElectCommand extends AbstractRatisCommand {
  public static final String ADDRESS_OPTION_NAME = "address";
  public static final String PEER_OPTION_NAME = "peers";
  public static final String DOMAIN_OPTION_NAME = "domain";
  public static final UUID RAFT_GROUP_UUID =
      UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1");
  public static final RaftGroupId RAFT_GROUP_ID = RaftGroupId.valueOf(RAFT_GROUP_UUID);
  private static final AtomicLong CALL_ID_COUNTER = new AtomicLong();
  private RaftGroup mRaftGroup;

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
    List<InetSocketAddress> addresses = new ArrayList<>();
    if (cl.hasOption(PEER_OPTION_NAME)) {
      String peersStr = cl.getOptionValue(PEER_OPTION_NAME);
      String[] peersArray = peersStr.split(",");
      for (int i = 0; i< peersArray.length; i++) {
        String[] hostPortPair = peersArray[i].split(":");
        InetSocketAddress addr = new InetSocketAddress(hostPortPair[0], Integer.parseInt(hostPortPair[1]));
        addresses.add(addr);
      }
    } else {
      // TODO(maobaolong) fill addresses from config
    }

    Set<RaftPeer> peers = addresses.stream()
        .map(addr -> RaftPeer.newBuilder()
            .setId(RaftUtils.getPeerId(addr))
            .setAddress(addr)
            .build()
        ).collect(Collectors.toSet());
    mRaftGroup = RaftGroup.valueOf(RAFT_GROUP_ID, peers);

    String strAddr = cl.getOptionValue(ADDRESS_OPTION_NAME);
    InetSocketAddress serverAddress = RaftUtils.stringToAddress(strAddr);
    // if you cannot find the address in the quorum, throw exception.
    if (peers.stream().map(RaftPeer::getAddress).noneMatch(addr -> addr.equals(strAddr))) {
      throw new IOException(String.format("<%s> is not part of the quorum <%s>.",
          strAddr, peers.stream().map(RaftPeer::getAddress).collect(Collectors.toList())));
    }

    RaftPeerId newLeaderPeerId =
        RaftUtils.getPeerId(serverAddress.getHostString(), serverAddress.getPort());
    // update priorities to enable transfer
    List<RaftPeer> peersWithNewPriorities = new ArrayList<>();
    for (RaftPeer peer : peers) {
      peersWithNewPriorities.add(
          RaftPeer.newBuilder(peer)
              .setPriority(peer.getId().equals(newLeaderPeerId) ? 2 : 1)
              .build()
      );
    }
    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      String stringPeers = "[" + peersWithNewPriorities.stream().map(RaftPeer::toString)
          .collect(Collectors.joining(", ")) + "]";
      mPrintStream.printf(
          "Applying new peer state before transferring leadership: %s%n", stringPeers);
      RaftClientReply setConfigurationReply =
          client.admin().setConfiguration(peersWithNewPriorities);
      processReply(setConfigurationReply,
          "failed to set priorities before initiating election");
      // transfer leadership
      mPrintStream.printf(
          "Transferring leadership to server with address <%s> and with RaftPeerId <%s>%n",
          serverAddress, newLeaderPeerId);
      try {
        Thread.sleep(3_000);
        RaftClientReply transferLeadershipReply =
            client.admin().transferLeadership(newLeaderPeerId, 60_000);
        processReply(transferLeadershipReply, "election failed");
      } catch (Throwable t) {
        mPrintStream.printf("caught an error when executing transfer: %s%n", t.getMessage());
        return -1;
      }
      mPrintStream.println("Transferring leadership initiated");
    }
    return 0;
  }

  private void processReply(RaftClientReply reply, String msg)
      throws IOException {
    RaftUtils.processReply(reply, msg, mPrintStream);
  }

  @Override
  public void validateArgs(CommandLine cl) throws IllegalArgumentException {
    if (!cl.hasOption(ADDRESS_OPTION_NAME)) {
      throw new IllegalArgumentException(String.format("[%s]", ADDRESS_OPTION_NAME));
    }
  }

  @Override
  public String getUsage() {
    return String.format("%s -%s <HOSTNAME:PORT>", getCommandName(), ADDRESS_OPTION_NAME);
  }

  @Override
  public String getDescription() {
    return description();
  }

  @Override
  public Options getOptions() {
    return new Options()
        .addOption(ADDRESS_OPTION_NAME, true,
            "Server address that will take over as leader")
        .addOption(PEER_OPTION_NAME, true, "Peer addresses seperated by comma");
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Transfers leadership to the <hostname>:<port>";
  }
}
