
import SwiftUI
import RoomPlan

@main
struct RoomPlanApp: App {
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}

struct ContentView: View {
    @State private var capturedRoom: CapturedRoom?
    @State private var presentingCapture = false

    var body: some View {
        VStack(spacing: 16) {
            Button("Scan Room") { presentingCapture = true }
            if let r = capturedRoom {
                Text("Captured room with \(r.walls.count) walls").padding()
                Button("Share to PWA") { shareToPWA(room: r) }
            }
        }
        .sheet(isPresented: $presentingCapture) {
            RoomCaptureViewRep(onFinished: { room in
                capturedRoom = room
                presentingCapture = false
            })
        }
        .padding()
    }

    func shareToPWA(room: CapturedRoom) {
        // Minimal JSON (replace with parsed parametric details)
        let dims = ["L": 18.0, "W": 12.0, "H": 8.0]
        let payload: [String: Any] = [
            "format": "roomplan-v1",
            "dims": dims
        ]
        if let data = try? JSONSerialization.data(withJSONObject: payload),
           let str = String(data: data, encoding: .utf8) {
            let b64 = str.data(using: .utf8)!.base64EncodedString()
            if let url = URL(string: "https://app.yourdomain.com/import-scan?scan=\(b64)") {
                UIApplication.shared.open(url)
            }
        }
    }
}

struct RoomCaptureViewRep: UIViewControllerRepresentable {
    var onFinished: (CapturedRoom)->Void
    func makeUIViewController(context: Context) -> RoomCaptureViewController {
        let vc = RoomCaptureViewController()
        vc.delegate = context.coordinator
        return vc
    }
    func updateUIViewController(_ uiViewController: RoomCaptureViewController, context: Context) {}
    func makeCoordinator() -> Coord { Coord(parent: self) }

    class Coord: NSObject, RoomCaptureViewControllerDelegate {
        let parent: RoomCaptureViewRep
        init(parent: RoomCaptureViewRep){ self.parent = parent }
        func captureViewController(_ viewController: RoomCaptureViewController, didPresent room: CapturedRoom, error: Error?) {
            guard error == nil else { return }
            parent.onFinished(room)
        }
    }
}
