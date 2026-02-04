//
//  Item.swift
//  Squatch Sports Basketball Training WatchOS Companion
//
//  Created by Kellam Adams on 2/4/26.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
