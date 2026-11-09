enum Shape {
    case circle(Int)
    case square(Int)
    case none
}

func area(_ s: Shape, _ k: Int) -> Int {
    switch s {
    case .circle(let r) where r > k:
        return r * r * 3
    case .circle(let r), .square(let r):
        print(r)
        fallthrough
    case .none:
        break
    }
    return 0
}

func values(_ x: Int) -> String {
    switch x {
    case 0:
        return "zero"
    case 1, 2:
        return "small"
    default:
        return "other"
    }
}
