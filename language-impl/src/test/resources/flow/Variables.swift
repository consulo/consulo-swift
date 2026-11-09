func variables(_ b: Bool) -> Int {
    let x: Int
    var y = 1
    if b {
        x = 1
    } else {
        x = 2
    }
    y += x;
    (y, _) = (y + 1, 0)
    let s = "value \(x) and \(y)"
    print(s)
    return y
}

func shadow(_ o: Int?) -> Int {
    if let o = o {
        return o
    }
    return 0
}
