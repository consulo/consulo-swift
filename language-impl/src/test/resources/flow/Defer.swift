func cleanup(_ b: Bool) -> Int {
    defer { print("first") }
    defer { print("second") }
    if b {
        return 1
    }
    for i in 0..<3 {
        defer { print(i) }
        if i == 1 {
            break
        }
    }
    return 2
}
