#if DEBUG
let mode = "debug"
#elseif RELEASE
let mode = "release"
#else
let mode = "unknown"
#endif

#if os(iOS)
import UIKit
#elseif os(macOS)
import AppKit
#endif

if #available(iOS 10, macOS 10.12, *) {
    print("modern")
}

#sourceLocation(file: "fake.swift", line: 1)

#sourceLocation()

let f = #file
let l = #line
let c = #column
let fn = #function
