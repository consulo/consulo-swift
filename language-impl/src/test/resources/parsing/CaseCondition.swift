if case .success(let value) = result {
    print(value)
}

if case let .pair(a, b) = tuple, a > 0 {
    print(a, b)
}

while case let .next(item) = iterator {
    process(item)
}

guard case .ready = state else {
    return
}
