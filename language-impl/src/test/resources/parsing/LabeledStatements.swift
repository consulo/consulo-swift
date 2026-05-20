outer: for i in 0 ..< 10 {
    for j in 0 ..< 10 {
        if i + j > 12 { break outer }
        if i == j { continue outer }
    }
}

check: if condition {
    if other { break check }
    work()
}

main: switch value {
case 0:
    break main
default:
    continue
}

retry: do {
    try work()
} catch {
    break retry
}
