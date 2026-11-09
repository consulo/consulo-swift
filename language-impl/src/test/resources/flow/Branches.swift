func ifElse(_ b: Bool, _ c: Bool) -> Int {
    if b && c {
        return 1
    } else if b || !c {
        return 2
    } else {
        print("none")
    }
    return 3
}

func literals() -> Int {
    if true {
        return 1
    }
    if false {
        print("never")
    }
}

func guards(_ o: Int?) -> Int {
    guard let v = o, v > 0 else {
        return 0
    }
    return v
}

func ternary(_ b: Bool, _ o: Int?) -> Int {
    let x = b ? 1 : 2
    let y = o ?? x
    return y
}
