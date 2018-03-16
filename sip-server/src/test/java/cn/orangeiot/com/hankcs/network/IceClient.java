package cn.orangeiot.com.hankcs.network;

import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.parser.SDPAnnounceParser;
import io.vertx.core.AbstractVerticle;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;

import javax.sdp.MediaDescription;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.List;

public class IceClient extends AbstractVerticle {

    private int port;

    private String streamName;

    private Agent agent;

    private String localSdp;

    private String remoteSdp;

    private String[] turnServers = new String[]{"114.67.58.242:3479"};

    private String[] stunServers = new String[]{"114.67.58.242:3479"};

//    private String username = "700";
//
//    private String password = "700pass";

    private String username = null;

    private String password = null;

    static Logger log = Logger.getLogger(IceClient.class);

    public IceClient(int port, String streamName) {
        this.port = port;
        this.streamName = streamName;
    }

    public void init() throws Throwable {

        agent = createAgent(port, streamName);

        agent.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);


        agent.setControlling(false);

        agent.setTa(10000);

        localSdp = SdpUtils.createSDPDescription(agent);

        log.info("=================== feed the following"
                + " to the remote agent ===================");

        System.out.println(localSdp);

        SDPAnnounceParser parser = new SDPAnnounceParser(localSdp);
        SessionDescriptionImpl parsedDescription = null;
        try {
            parsedDescription = parser.parse();
            MediaDescription md = (MediaDescription) parsedDescription.getMediaDescriptions(false).get(0);

//            socket.listen(8888, "192.168.42.8", asyncResult -> {
//                if (asyncResult.succeeded()) {
//                    socket.handler(rs -> {
//                        System.out.println("rece data -> " + new String(rs.data().getBytes()));
//                    });//数据包处理
//
//                    try {
//                        String msg = new JsonObject().put("host", md.getConnection().getAddressType())
//                                .put("port", md.getMedia().getMediaPort()).toString();
//                        socket.send(msg, 14500, "114.67.58.243", as -> {
//                            System.out.println("send data -> " + as.succeeded());
//                            if (!as.succeeded()) {
//                                reSend(vertx, msg, 14500, "114.67.58.243", socket);
//                            }
//                        });
//                    } catch (SdpParseException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    System.out.println("Listen failed" + asyncResult.cause());
//                    System.out.println("Failed to bind!");
//                    System.exit(0);
//                }
//            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("======================================"
                + "========================================\n");


    }


    public DatagramSocket getDatagramSocket() throws Throwable {

        LocalCandidate localCandidate = agent
                .getSelectedLocalCandidate(streamName);

        IceMediaStream stream = agent.getStream(streamName);
        List<Component> components = stream.getComponents();
        for (Component c : components) {
            log.info(c);
        }
        log.info(localCandidate.toString());
        LocalCandidate candidate = (LocalCandidate) localCandidate;
        return candidate.getDatagramSocket();

    }

    public SocketAddress getRemotePeerSocketAddress() {
        RemoteCandidate remoteCandidate = agent
                .getSelectedRemoteCandidate(streamName);
        log.info("Remote candinate transport address:"
                + remoteCandidate.getTransportAddress());
        log.info("Remote candinate host address:"
                + remoteCandidate.getHostAddress());
        log.info("Remote candinate mapped address:"
                + remoteCandidate.getMappedAddress());
        log.info("Remote candinate relayed address:"
                + remoteCandidate.getRelayedAddress());
        log.info("Remote candinate reflexive address:"
                + remoteCandidate.getReflexiveAddress());
        return remoteCandidate.getTransportAddress();
    }

    /**
     * Reads an SDP description from the standard input.In production
     * environment that we can exchange SDP with peer through signaling
     * server(SIP server)
     */
    public void exchangeSdpWithPeer() throws Throwable {
        log.info("Paste remote SDP here. Enter an empty line to proceed:");

        remoteSdp = "v=0\r\n" +
                "o=ice4j.org 0 0 IN IP4 114.67.85.243\r\n" +
                "s=-\r\n" +
                "t=0 0\r\n" +
                "a=ice-options:trickle\r\n" +
                "a=ice-ufrag:24g801c8jnumts\r\n" +
                "a=ice-pwd:7hlr33oiseo1aub5vdjhssa993\r\n" +
                "m=text 14500 RTP/AVP 0\r\n" +
                "c=IN 114.67.85.243 IP4v\r\n" +
                "a=mid:text\r\n" +
                "a=candidate:1 1 udp 2130706431 fe80:0:0:0:1e6d:4818:c0a:a273 8888 typ host\r\n" +
                "a=candidate:2 1 udp 2130706431 192.168.42.8 8888 typ host\r\n" +
                "a=candidate:3 1 udp 1677724415 117.136.40.222 38388 typ srflx raddr 192.168.42.8 rport 8888";

        SdpUtils.parseSDP(agent, remoteSdp);
    }



    private Agent createAgent(int rtpPort, String streamName) throws Throwable {
        return createAgent(rtpPort, streamName, false);
    }

    private Agent createAgent(int rtpPort, String streamName,
                              boolean isTrickling) throws Throwable {

        long startTime = System.currentTimeMillis();

        Agent agent = new Agent();

        agent.setTrickling(isTrickling);

        // STUN
        for (String server : stunServers) {
            String[] pair = server.split(":");
            agent.addCandidateHarvester(new StunCandidateHarvester(
                    new TransportAddress(pair[0], Integer.parseInt(pair[1]),
                            Transport.UDP)));
        }

        // TURN
        LongTermCredential longTermCredential = new LongTermCredential(username,
                password);

        for (String server : turnServers) {
            String[] pair = server.split(":");
            agent.addCandidateHarvester(new TurnCandidateHarvester(
                    new TransportAddress(pair[0], Integer.parseInt(pair[1]), Transport.UDP),
                    longTermCredential));
        }
        // STREAMS
        createStream(rtpPort, streamName, agent);

        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;

        log.info("Total harvesting time: " + total + "ms.");

        return agent;
    }

    private IceMediaStream createStream(int rtpPort, String streamName,
                                        Agent agent) throws Throwable {
        long startTime = System.currentTimeMillis();
        IceMediaStream stream = agent.createMediaStream(streamName);
        // rtp
        Component component = agent.createComponent(stream, Transport.UDP,
                rtpPort, rtpPort, rtpPort + 100);

        long endTime = System.currentTimeMillis();
        log.info("Component Name:" + component.getName());
        log.info("RTP Component created in " + (endTime - startTime) + " ms");

        return stream;
    }
}