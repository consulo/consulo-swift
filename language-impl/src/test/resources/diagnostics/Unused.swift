func use(_ x: Int) {
}

func bump(_ x: inout Int) {
    x += 1
}

let global = 1

func overwritten() {
    var a = 1 // expected-warning@:9 {{variable 'a' was written to, but never read}}
    a = 2
}

func deadStore() {
    var b = 1
    b = 2
    use(b)
}

func onlyInitialized() {
    var c = 1 // expected-warning@:9 {{initialization of variable 'c' was never used; consider replacing with assignment to '_' or removing it}}
    let d = 1 // expected-warning@:9 {{initialization of immutable value 'd' was never used; consider replacing with assignment to '_' or removing it}}
}

func assignedLater() {
    var e: Int // expected-warning@:9 {{variable 'e' was written to, but never read}}
    e = 1
    let k: Int // expected-warning@:9 {{immutable value 'k' was never used; consider removing it}}
    k = 1
}

func neverUsed() {
    var j: Int // expected-warning@:9 {{variable 'j' was never used; consider replacing with '_' or removing it}}
}

func compound() {
    var g = 1
    g += 1
    var u = 1
    bump(&u)
}

func captured() {
    var h = 1 // expected-warning@:9 {{variable 'h' was written to, but never read}}
    let write = { h = 2 }
    write()
    let i = 1
    let read = { use(i) }
    read()
    let n = 1
    let list = { [n] in print(n) }
    list()
}

func nested() {
    var counter = 0
    func increment() {
        counter += 1
    }
    increment()
    let unused = { // expected-warning@:9 {{initialization of immutable value 'unused' was never used; consider replacing with assignment to '_' or removing it}}
        let v = 1 // expected-warning@:13 {{initialization of immutable value 'v' was never used; consider replacing with assignment to '_' or removing it}}
        var w = 2 // expected-warning@:13 {{variable 'w' was written to, but never read}}
        w = 3
    }
}

func bindings(_ o: Int?) {
    if let m = o { // expected-warning@:12 {{value 'm' was defined but never used; consider replacing with boolean test}}
    }
    guard let t = o else { // expected-warning@:15 {{value 't' was defined but never used; consider replacing with boolean test}}
        return
    }
    while let s = o { // expected-warning@:15 {{value 's' was defined but never used; consider replacing with boolean test}}
        break
    }
    if var r = o { // expected-warning@:12 {{variable 'r' was written to, but never read}}
        r = 2
    }
    if let used = o {
        use(used)
    }
}

func patterns() {
    let (p, q) = (1, 2) // expected-warning@:13 {{immutable value 'q' was never used; consider replacing with '_' or removing it}}
    use(p)
    var (x, y) = (1, 2) // expected-warning@:13 {{variable 'y' was never used; consider replacing with '_' or removing it}}
    use(x)
    x = 3
    for o in 0..<1 { // expected-warning@:9 {{immutable value 'o' was never used; consider replacing with '_' or removing it}}
    }
    for (i, z) in [(1, 2)] { // expected-warning@:13 {{immutable value 'z' was never used; consider replacing with '_' or removing it}}
        use(i)
    }
}

func interpolation() {
    let name = "world"
    print("hello \(name)")
}

func unreachableRead() {
    let late = 1
    return;
    use(late) // expected-warning@:5 {{code after 'return' will never be executed}}
}
