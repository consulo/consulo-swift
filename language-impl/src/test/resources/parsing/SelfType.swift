class Node {
    func clone() -> Self {
        return self
    }

    static func factory() -> Self {
        return Self()
    }
}

protocol Copyable {
    func copy() -> Self
}
