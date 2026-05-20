for i in 0 ..< 10 {
    print(i)
}

for case let .some(x) in items {
    print(x)
}

for (k, v) in dict {
    print(k, v)
}

while running {
    tick()
}

repeat {
    step()
} while !done

outer: for i in 1 ... 5 {
    for j in 1 ... 5 {
        if i == j { continue outer }
        if i + j > 6 { break outer }
        print(i, j)
    }
}
