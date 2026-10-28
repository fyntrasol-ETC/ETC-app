import SwiftUI

struct MainTabView: View {
    @State private var selectedTab: ETCTab = .home
    @State private var showTabBar: Bool = true
    @EnvironmentObject private var authVM: AuthViewModel

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch selectedTab {
                case .home:
                    HomeScreen(selectedTab: $selectedTab, showTabBar: $showTabBar)
                        .environmentObject(authVM)
                case .courses:
                    CoursesScreen(selectedTab: $selectedTab, showTabBar: $showTabBar)
                        .environmentObject(authVM)
                case .speak:
                    PracticeScreen(selectedTab: $selectedTab, showTabBar: $showTabBar)
                        .environmentObject(authVM)
                case .community:
                    CommunityScreen(selectedTab: $selectedTab, showTabBar: $showTabBar)
                        .environmentObject(authVM)
                case .profile:
                    ProfileScreen(selectedTab: $selectedTab, showTabBar: $showTabBar)
                        .environmentObject(authVM)
                }
            }
            .appSizeClass()

            if showTabBar {
                ETCTabBar(selectedTab: $selectedTab)
                    .ignoresSafeArea(edges: .bottom)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .ignoresSafeArea(edges: .bottom)
        .animation(.easeInOut(duration: 0.2), value: showTabBar)
    }
}

// Also fix the private struct at the bottom
private struct HomeWithTabBar: View {
    @Binding var selectedTab: ETCTab
    @EnvironmentObject private var authVM: AuthViewModel
    @Binding var showTabBar: Bool          // ← add this


    var body: some View {
        ZStack(alignment: .bottom) {
            HomeScreen(selectedTab: $selectedTab, showTabBar: $showTabBar)  // ← add this
                .environmentObject(authVM)
            ETCTabBar(selectedTab: $selectedTab)
                .ignoresSafeArea(edges: .bottom)
        }
    }
}
