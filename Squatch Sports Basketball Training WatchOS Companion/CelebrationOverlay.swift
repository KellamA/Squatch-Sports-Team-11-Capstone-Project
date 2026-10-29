import SwiftUI

/// Full-screen popup for XP, streak, and goal celebrations. Tap anywhere or wait to dismiss.
struct CelebrationOverlay: View {
    let message: CelebrationMessage?
    let onDismiss: () -> Void

    private let displayDuration: Duration = .milliseconds(3500)

    var body: some View {
        ZStack {
            if let message {
                Color.black.opacity(0.45)
                    .ignoresSafeArea()
                    .transition(.opacity)

                CelebrationCard(message: message)
                    // A new identity per message so each card bounces in, including back-to-back ones.
                    .id(message.id)
                    .transition(.scale(scale: 0.6).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.45, dampingFraction: 0.6), value: message?.id)
        .contentShape(Rectangle())
        .allowsHitTesting(message != nil)
        .onTapGesture(perform: onDismiss)
        .task(id: message?.id) {
            guard message != nil else { return }
            try? await Task.sleep(for: displayDuration)
            if !Task.isCancelled {
                onDismiss()
            }
        }
    }
}

private struct CelebrationCard: View {
    let message: CelebrationMessage

    var body: some View {
        VStack(spacing: 6) {
            Text(message.emoji)
                .font(.system(size: 64))
            Text(message.headline)
                .font(.system(size: 44, weight: .black))
                .foregroundStyle(Color.streakOrange)
            Text(message.caption)
                .font(.headline)
            Text(message.title)
                .font(.title2)
                .bold()
                .multilineTextAlignment(.center)
                .padding(.top, 6)
            Text(message.subtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Text("Tap to continue")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.top, 4)
        }
        .padding(.horizontal, 28)
        .padding(.vertical, 24)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24))
        .shadow(radius: 12)
        .padding(32)
    }
}

#Preview {
    CelebrationOverlay(
        message: CelebrationMessages.forStreak(StreakCelebration(streak: 7, isMilestone: true, isNewBest: true)),
        onDismiss: {}
    )
}
