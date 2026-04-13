import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    // 👇 THE MAGIC LINE: This forces SwiftUI to redraw and pass the theme to Kotlin!
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.container, edges: .all)    }
}



