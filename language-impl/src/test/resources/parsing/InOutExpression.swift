func increment(_ x: inout Int) {
    x = x + 1
}

var n = 0
increment(&n)

func swap2<T>(_ a: inout T, _ b: inout T) {
    let tmp = a
    a = b
    b = tmp
}

var p = 1
var q = 2
swap2(&p, &q)
