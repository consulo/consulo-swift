protocol Named {
    var name: String { get }
}

protocol Greetable: Named {
    func greet() -> String
    static func register()
}

protocol Initializable {
    init(value: Int)
}

protocol Container {
    associatedtype Item
    mutating func append(_ item: Item)
    var count: Int { get }
    subscript(i: Int) -> Item { get set }
}
