struct Stack<Element> {
    private var items: [Element] = []
    mutating func push(_ item: Element) { items.append(item) }
    mutating func pop() -> Element? { return items.popLast() }
}

func swap2<T>(_ a: inout T, _ b: inout T) {
    let tmp = a
    a = b
    b = tmp
}

func findIndex<T: Equatable>(of value: T, in array: [T]) -> Int? {
    for (i, v) in array.enumerated() {
        if v == value { return i }
    }
    return nil
}

protocol Sequenceable {
    associatedtype Iterator
    func makeIterator() -> Iterator
}

extension Stack: Sequenceable where Element: Hashable {
    func makeIterator() -> AnyIterator<Element> {
        return AnyIterator { nil }
    }
}
