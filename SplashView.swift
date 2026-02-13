//
//  SplashView.swift
//  Squatch Sports Basketball Training WatchOS Companion
//
//  Created by Melvin Sanare on 2/13/26.
//

import SwiftUI

struct SplashView: View {
    var body: some View {
        VStack(spacing: 8) {
            ProgressView()
            Text("Loading...")
        }
    }
}

#Preview {
    SplashView()
}
