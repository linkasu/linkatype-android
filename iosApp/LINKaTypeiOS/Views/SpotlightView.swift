import SwiftUI

struct SpotlightView: View {
    let text: String
    @Binding var isPresented: Bool
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack {
                Spacer()
                
                Text(text)
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding()
                
                Spacer()
            }
        }
        .accessibilityIdentifier("spotlight_view")
        .statusBar(hidden: true)
        .onTapGesture {
            isPresented = false
        }
        .gesture(
            DragGesture()
                .onEnded { gesture in
                    if gesture.translation.height > 100 {
                        isPresented = false
                    }
                }
        )
    }
}
