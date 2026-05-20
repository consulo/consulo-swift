extension Int {
    var squared: Int { return self * self }
    func times(_ block: () -> Void) {
        for _ in 0 ..< self {
            block()
        }
    }
}

extension Array where Element: Equatable {
    func contains2(_ x: Element) -> Bool {
        for e in self {
            if e == x { return true }
        }
        return false
    }
}

extension String: Named {
    var name: String { return self }
}
