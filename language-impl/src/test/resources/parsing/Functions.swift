func greet() {
    print("hi")
}

func add(_ a: Int, _ b: Int) -> Int {
    return a + b
}

func named(from start: Int, to end: Int) -> Int {
    return end - start
}

func variadic(_ numbers: Int...) -> Int {
    return 0
}

func defaulted(x: Int = 10) -> Int { return x }

func throwingFn() throws -> String {
    return ""
}

func rethrowingFn(_ body: () throws -> Void) rethrows {
}

func generic<T: Equatable>(_ a: T, _ b: T) -> Bool where T: Hashable {
    return a == b
}
