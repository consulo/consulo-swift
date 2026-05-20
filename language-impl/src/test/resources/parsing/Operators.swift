prefix operator +++
postfix operator ---
infix operator ** : MultiplicationPrecedence

precedencegroup ExponentPrecedence {
    higherThan: MultiplicationPrecedence
    associativity: right
    assignment: false
}

prefix func +++ (x: inout Int) -> Int {
    x = x + 1
    return x
}

postfix func --- (x: inout Int) -> Int {
    let old = x
    x = x - 1
    return old
}

func ** (base: Int, power: Int) -> Int {
    var result = 1
    for _ in 0 ..< power {
        result = result * base
    }
    return result
}
