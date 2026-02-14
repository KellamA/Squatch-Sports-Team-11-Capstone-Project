//
//  SplashView.swift
//  Squatch Sports Basketball Training WatchOS Companion
//
//  Created by Melvin Sanare on 2/13/26.
//

import SwiftUI

private enum AssetNames {
    static let logo = "Squatch Sports logo"
}

// Minimal three-dot loader
private struct DotLoader: View {
    let dotSize: CGFloat
    let color: Color
    @State private var animate = false

    var body: some View {
        HStack(spacing: dotSize * 0.6) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .fill(color)
                    .frame(width: dotSize, height: dotSize)
                    .scaleEffect(animate ? 1.0 : 0.6)
                    .opacity(animate ? 1.0 : 0.5)
                    .animation(
                        .easeInOut(duration: 0.6)
                            .repeatForever(autoreverses: true)
                            .delay(0.15 * Double(i)),
                        value: animate
                    )
            }
        }
        .onAppear { animate = true }
    }
}

struct SplashView: View {
    @State private var showLogo = false
    @State private var showLoading = false

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width

            ZStack {
                // Force light background so our black logo displays correctly
                Color.white.ignoresSafeArea()

                VStack(spacing: 8) {
                    // Logo (original colors, no template tint)
                    if showLogo {
                        Image(AssetNames.logo)
                            .resizable()
                            .scaledToFit()
                            .frame(width: width * 1.0)
                            .transition(.opacity)
                    }

                    // Loading indicator
                    if showLoading {
                        VStack(spacing: 4) {
                            DotLoader(dotSize: min(12, width * 0.08), color: .black.opacity(0.85))
                            Text("Loading…")
                                .font(.caption2)
                                .foregroundColor(.black.opacity(0.5))
                        }
                        .transition(.opacity)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .onAppear {
                // Sequence: logo → loading
                withAnimation(.easeIn(duration: 0.35)) { showLogo = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    withAnimation(.easeIn(duration: 0.35)) { showLoading = true }
                }
            }
        }
        .padding(10)
#if os(watchOS)
        .preferredColorScheme(.light)
#endif
    }
}

#Preview {
    SplashView()
}

