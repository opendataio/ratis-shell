package opendataio.ratisshell.cli;

import org.apache.ratis.client.RaftClient;
import org.apache.ratis.client.RaftClientConfigKeys;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.ClientId;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.retry.ExponentialBackoffRetry;
import org.apache.ratis.util.TimeDuration;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for raft journal operations.
 */
public class RaftUtils {
  public static final String RAFT_DIR = "raft";

  private RaftUtils() {
    // prevent instantiation
  }

  /**
   * Gets the raft peer id.
   *
   * @param address the address of the server
   * @return the raft peer id
   */
  public static RaftPeerId getPeerId(InetSocketAddress address) {
    return getPeerId(address.getHostString(), address.getPort());
  }

  /**
   * Gets the raft peer id.
   *
   * @param host the hostname of the server
   * @param port the port of the server
   * @return the raft peer id
   */
  public static RaftPeerId getPeerId(String host, int port) {
    return RaftPeerId.getRaftPeerId(host + "_" + port);
  }

  /**
   * Gets the raft journal dir.
   *
   * @param baseDir the journal base dir
   * @return the raft peer id
   */
  public static File getRaftJournalDir(File baseDir) {
    return new File(baseDir, RAFT_DIR);
  }

  /**
   * Creates a future that is completed exceptionally.
   *
   * @param e the exception to be returned by the future
   * @param <T> the type of the future
   * @return the completed future
   */
  public static <T> CompletableFuture<T> completeExceptionally(Exception e) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(e);
    return future;
  }

  /**
   * Create a raft client to communicate to ratis server.
   * @param raftGroup the raft group
   * @return return a raft client
   */
  public static RaftClient createClient(
      RaftGroup raftGroup) {
    RaftProperties properties = new RaftProperties();
    Parameters parameters = new Parameters();
    RaftClientConfigKeys.Rpc.setRequestTimeout(properties,
        TimeDuration.valueOf(15, TimeUnit.SECONDS));
    ExponentialBackoffRetry retryPolicy = ExponentialBackoffRetry.newBuilder()
        .setBaseSleepTime(TimeDuration.valueOf(1000, TimeUnit.MILLISECONDS))
        .setMaxAttempts(10)
        .setMaxSleepTime(
            TimeDuration.valueOf(100_000, TimeUnit.MILLISECONDS))
        .build();
    return RaftClient.newBuilder()
        .setRaftGroup(raftGroup)
        .setClientId(ClientId.randomId())
        .setLeaderId(null)
        .setProperties(properties)
        .setParameters(parameters)
        .setRetryPolicy(retryPolicy)
        .build();
  }

  /**
   * @param reply from the ratis operation
   * @param msgToUser message to user
   * @param printStream the print stream
   * @throws IOException
   */
  public static void processReply(RaftClientReply reply, String msgToUser,
      PrintStream printStream) throws IOException {
    if (!reply.isSuccess()) {
      IOException ioe = reply.getException() != null
          ? reply.getException()
          : new IOException(String.format("reply <%s> failed", reply));
      printStream.printf("%s. Error: %s%n", msgToUser, ioe);
      throw new IOException(msgToUser);
    }
  }

  /**
   * @param serverAddress the string containing the hostname and port separated by a ':
   * @return a NetAddress object composed of a hostname and a port
   */
  public static InetSocketAddress stringToAddress(String serverAddress) {
    String hostName;
    int port;
    try {
      hostName = serverAddress.substring(0, serverAddress.indexOf(":"));
      port = Integer.parseInt(serverAddress.substring(serverAddress.indexOf(":") + 1));
    } catch (Exception e) {
      throw new IllegalArgumentException("illegal argument");
    }
    return InetSocketAddress.createUnresolved(hostName, port);
  }
}
