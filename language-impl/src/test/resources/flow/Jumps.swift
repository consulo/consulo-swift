func labeled(_ rows: [[Int]]) -> Int {
    var found = 0
    outer: for row in rows {
        for value in row {
            if value < 0 {
                continue outer
            }
            if value == 0 {
                break outer
            }
            found = value
        }
    }
    return found
}

func afterReturn() -> Int {
    return 1
    print("never")
}

func noReturn(_ b: Bool) -> Int {
    if b {
        return 1
    }
    fatalError("unreachable")
}
