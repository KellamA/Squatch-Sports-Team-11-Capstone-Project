import Foundation
import MultipeerConnectivity
import Combine

final class NearbyMatchService: NSObject, ObservableObject {
    static let shared = NearbyMatchService()

    private let serviceType = "squatch-1v1"

    @Published private(set) var discoveredPeers: [MCPeerID] = []
    @Published private(set) var connectedPeers: [MCPeerID] = []
    @Published var statusText = "Not connected"

    private var peerID: MCPeerID!
    private var session: MCSession!
    private var advertiser: MCNearbyServiceAdvertiser!
    private var browser: MCNearbyServiceBrowser!

    var onMessage: ((NearbyMatchMessage) -> Void)?

    private override init() {
        super.init()
    }

    func start(displayName: String) {
        stop()

        let safeName = displayName.isEmpty ? "Squatch Player" : displayName

        peerID = MCPeerID(displayName: safeName)

        session = MCSession(
            peer: peerID,
            securityIdentity: nil,
            encryptionPreference: .required
        )
        session.delegate = self

        advertiser = MCNearbyServiceAdvertiser(
            peer: peerID,
            discoveryInfo: nil,
            serviceType: serviceType
        )
        advertiser.delegate = self

        browser = MCNearbyServiceBrowser(
            peer: peerID,
            serviceType: serviceType
        )
        browser.delegate = self

        advertiser.startAdvertisingPeer()
        browser.startBrowsingForPeers()

        statusText = "Looking for nearby players"
    }

    func stop() {
        advertiser?.stopAdvertisingPeer()
        browser?.stopBrowsingForPeers()
        session?.disconnect()

        discoveredPeers = []
        connectedPeers = []
        statusText = "Not connected"
    }

    func connect(to peer: MCPeerID) {
        guard session != nil else { return }

        browser.invitePeer(
            peer,
            to: session,
            withContext: nil,
            timeout: 15
        )

        statusText = "Connecting to \(peer.displayName)"
    }

    func send(_ message: NearbyMatchMessage) {
        guard let session,
              !session.connectedPeers.isEmpty,
              let data = try? JSONEncoder().encode(message)
        else {
            return
        }

        do {
            try session.send(
                data,
                toPeers: session.connectedPeers,
                with: .reliable
            )
        } catch {
            print("Nearby match send failed:", error)
        }
    }
}

enum NearbyMatchMessage: Codable, Equatable {
    case startMatch(NearbyMatchSetup)
    case event(matchID: UUID, event: MatchEvent)
    case undo(matchID: UUID)
    case finish(matchID: UUID)

    case cancel(matchID: UUID)
}

struct NearbyMatchSetup: Codable, Equatable {
    let matchID: UUID

    let hostName: String
    let guestName: String

    let hostSide: PlayerSide
    let settings: MatchSettings
}

extension NearbyMatchService: MCSessionDelegate {
    func session(
        _ session: MCSession,
        peer peerID: MCPeerID,
        didChange state: MCSessionState
    ) {
        DispatchQueue.main.async {
            self.connectedPeers = session.connectedPeers

            switch state {
            case .notConnected:
                self.statusText = "Disconnected"

            case .connecting:
                self.statusText = "Connecting to \(peerID.displayName)"

            case .connected:
                self.statusText = "Connected to \(peerID.displayName)"

            @unknown default:
                self.statusText = "Unknown connection state"
            }
        }
    }

    func session(
        _ session: MCSession,
        didReceive data: Data,
        fromPeer peerID: MCPeerID
    ) {
        guard let message = try? JSONDecoder().decode(
            NearbyMatchMessage.self,
            from: data
        ) else {
            return
        }

        DispatchQueue.main.async {
            self.onMessage?(message)
        }
    }

    func session(
        _ session: MCSession,
        didReceive stream: InputStream,
        withName streamName: String,
        fromPeer peerID: MCPeerID
    ) {}

    func session(
        _ session: MCSession,
        didStartReceivingResourceWithName resourceName: String,
        fromPeer peerID: MCPeerID,
        with progress: Progress
    ) {}

    func session(
        _ session: MCSession,
        didFinishReceivingResourceWithName resourceName: String,
        fromPeer peerID: MCPeerID,
        at localURL: URL?,
        withError error: Error?
    ) {}

#if os(iOS)
    func session(
        _ session: MCSession,
        didReceiveCertificate certificate: [Any]?,
        fromPeer peerID: MCPeerID,
        certificateHandler: @escaping (Bool) -> Void
    ) {
        certificateHandler(true)
    }
#endif
}

extension NearbyMatchService: MCNearbyServiceAdvertiserDelegate {
    func advertiser(
        _ advertiser: MCNearbyServiceAdvertiser,
        didReceiveInvitationFromPeer peerID: MCPeerID,
        withContext context: Data?,
        invitationHandler: @escaping (Bool, MCSession?) -> Void
    ) {
        invitationHandler(true, session)
    }

    func advertiser(
        _ advertiser: MCNearbyServiceAdvertiser,
        didNotStartAdvertisingPeer error: Error
    ) {
        DispatchQueue.main.async {
            self.statusText = "Advertising failed"
        }

        print("Nearby advertising failed:", error)
    }
}

extension NearbyMatchService: MCNearbyServiceBrowserDelegate {
    func browser(
        _ browser: MCNearbyServiceBrowser,
        foundPeer peerID: MCPeerID,
        withDiscoveryInfo info: [String: String]?
    ) {
        DispatchQueue.main.async {
            guard !self.discoveredPeers.contains(peerID) else {
                return
            }

            self.discoveredPeers.append(peerID)
        }
    }

    func browser(
        _ browser: MCNearbyServiceBrowser,
        lostPeer peerID: MCPeerID
    ) {
        DispatchQueue.main.async {
            self.discoveredPeers.removeAll {
                $0 == peerID
            }
        }
    }

    func browser(
        _ browser: MCNearbyServiceBrowser,
        didNotStartBrowsingForPeers error: Error
    ) {
        DispatchQueue.main.async {
            self.statusText = "Browsing failed"
        }

        print("Nearby browsing failed:", error)
    }
}
