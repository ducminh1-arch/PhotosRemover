import SwiftUI

public struct LaserScannerView: View {
    @State private var laserOffset: CGFloat = 0.08

    public init() {}

    public var body: some View {
        ZStack {
            // Device outline
            RoundedRectangle(cornerRadius: 24)
                .stroke(Color(red: 0.2, green: 0.25, blue: 0.35), lineWidth: 3)
                .background(RoundedRectangle(cornerRadius: 24).fill(Color.white.opacity(0.95)))
                .frame(width: 190, height: 320)

            // Inner screen frame
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(red: 0.8, green: 0.84, blue: 0.9), lineWidth: 1.5)
                .frame(width: 172, height: 260)

            // Notch, inner cards & circular home button
            VStack {
                // Top speaker & camera
                HStack(spacing: 8) {
                    Circle()
                        .fill(Color(red: 0.4, green: 0.45, blue: 0.55))
                        .frame(width: 5, height: 5)
                    Capsule()
                        .fill(Color(red: 0.4, green: 0.45, blue: 0.55))
                        .frame(width: 44, height: 4)
                }
                .padding(.top, 12)

                Spacer()

                // Floating sample photo cards inside phone screen
                VStack(spacing: 16) {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(red: 1.0, green: 0.7, blue: 0.0), lineWidth: 1.5)
                        .background(RoundedRectangle(cornerRadius: 8).fill(Color(red: 1.0, green: 0.7, blue: 0.0).opacity(0.15)))
                        .frame(width: 80, height: 52)
                        .offset(x: -20)

                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color(red: 1.0, green: 0.7, blue: 0.0), lineWidth: 1.5)
                        .background(RoundedRectangle(cornerRadius: 8).fill(Color(red: 1.0, green: 0.7, blue: 0.0).opacity(0.25)))
                        .frame(width: 80, height: 52)
                        .offset(x: 20)
                }

                Spacer()

                // Circular home button at bottom (matching reference)
                Circle()
                    .stroke(Color(red: 0.4, green: 0.45, blue: 0.55), lineWidth: 2)
                    .frame(width: 22, height: 22)
                    .padding(.bottom, 8)
            }
            .frame(width: 190, height: 320)

            // Animated Laser Beam
            GeometryReader { geo in
                let height = geo.size.height
                let currentY = height * laserOffset

                ZStack {
                    // Glow gradient
                    Rectangle()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.yellow.opacity(0.0),
                                    Color(red: 1.0, green: 0.75, blue: 0.0).opacity(0.4),
                                    Color.white.opacity(0.8),
                                    Color(red: 1.0, green: 0.75, blue: 0.0).opacity(0.4),
                                    Color.yellow.opacity(0.0)
                                ]),
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .frame(width: 180, height: 44)
                        .position(x: geo.size.width / 2, y: currentY)

                    // Sharp center laser line
                    Rectangle()
                        .fill(Color(red: 1.0, green: 0.7, blue: 0.0))
                        .frame(width: 176, height: 3)
                        .position(x: geo.size.width / 2, y: currentY)
                }
            }
            .frame(width: 190, height: 320)
            .clipped()
        }
        .onAppear {
            withAnimation(
                .easeInOut(duration: 1.8)
                .repeatForever(autoreverses: true)
            ) {
                laserOffset = 0.92
            }
        }
    }
}
