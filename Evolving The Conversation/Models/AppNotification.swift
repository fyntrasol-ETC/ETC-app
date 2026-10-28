//
//  AppNotification.swift
//  Evolving The Conversation
//
//  Created by Zainab's Mac on 16/06/2026.
//

import SwiftUI

// MARK: - Notification Model
struct AppNotification: Identifiable {
    let id: String
    let icon: String
    let iconColor: Color
    let iconBg: Color
    let title: String
    let body: String
    let time: String
    var isUnread: Bool
}
