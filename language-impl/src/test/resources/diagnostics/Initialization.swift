enum Failure: Error {
    case bad
}

func use(_ x: Int) {
}

func make() throws -> Int {
    return 1
}

func update(_ x: inout Int) {
    x += 1
}

func someBranch(_ b: Bool) {
    var x: Int // expected-note {{variable defined here}}
    if b {
        x = 1
    }
    use(x) // expected-error@:9 {{variable 'x' used before being initialized}}
}

func constantBranch(_ b: Bool) {
    let y: Int // expected-note 2 {{constant defined here}}
    if b {
        y = 1
    }
    use(y) // expected-error@:9 {{constant 'y' used before being initialized}}
    use(y) // expected-error@:9 {{constant 'y' used before being initialized}}
}

func bothBranches(_ b: Bool) {
    let y: Int
    if b {
        y = 1
    } else {
        y = 2
    }
    use(y)
}

func twice() {
    let z: Int // expected-note {{change 'let' to 'var' to make it mutable}}
    z = 1
    z = 2 // expected-error@:7 {{immutable value 'z' may only be initialized once}}
    use(z)
}

func maybeTwice(_ b: Bool) {
    let p: Int // expected-note {{change 'let' to 'var' to make it mutable}} // expected-note {{constant defined here}}
    if b {
        p = 1
    }
    if !b {
        p = 2 // expected-error@:11 {{immutable value 'p' may only be initialized once}}
    }
    use(p) // expected-error@:9 {{constant 'p' used before being initialized}}
}

func neverReturns(_ b: Bool) {
    let w: Int
    if b {
        w = 1
    } else {
        fatalError()
    }
    use(w)
}

func folded() {
    let u: Int
    if true {
        u = 1
    }
    use(u)
}

func infinite() {
    let v: Int
    while true {
    }
    use(v)
}

func loop() {
    let t: Int // expected-note {{change 'let' to 'var' to make it mutable}} // expected-note {{constant defined here}}
    for _ in 0..<2 {
        t = 1 // expected-error@:11 {{immutable value 't' may only be initialized once}}
    }
    use(t) // expected-error@:9 {{constant 't' used before being initialized}}
}

func repeatLoop(_ b: Bool) {
    let k: Int // expected-note {{change 'let' to 'var' to make it mutable}}
    repeat {
        k = 1 // expected-error@:11 {{immutable value 'k' may only be initialized once}}
    } while b
    use(k)
}

func freshInLoop() {
    for i in 0..<2 {
        let f: Int
        f = i
        use(f)
    }
}

func guarded(_ b: Bool) {
    let q: Int
    guard b else {
        return
    }
    q = 1
    use(q)
}

func caught() {
    let o: Int
    do {
        o = try make()
    } catch {
        o = 2
    }
    use(o)
}

func switched(_ b: Bool) {
    let l: Int // expected-note {{constant defined here}}
    switch b {
    case true:
        l = 1
    case false:
        break
    }
    use(l) // expected-error@:9 {{constant 'l' used before being initialized}}
}

func inout1() {
    var n: Int // expected-note {{variable defined here}}
    n += 1 // expected-error@:7 {{variable 'n' passed by reference before being initialized}}
    use(n)
}

func inout2() {
    var m: Int // expected-note {{variable defined here}}
    update(&m) // expected-error@:5 {{variable 'm' passed by reference before being initialized}}
    use(m)
}

func optional() {
    var o: Int?
    use(o ?? 0)
    o = 1
    use(o ?? 0)
}

func capturedConstant() {
    let s: Int // expected-note {{constant defined here}}
    let c = { use(s) } // expected-error@:13 {{constant 's' captured by a closure before being initialized}}
    s = 1
    c()
}

func capturedVariable() {
    var r: Int // expected-note {{variable defined here}}
    let c = { use(r) } // expected-error@:13 {{variable 'r' captured by a closure before being initialized}}
    r = 1
    c()
}

func capturedAfter() {
    let s: Int
    s = 1
    let c = { use(s) }
    c()
}

func interpolated(_ b: Bool) {
    let i: Int // expected-note {{constant defined here}}
    if b {
        i = 1
    }
    print("\(i)") // expected-error@:14 {{constant 'i' used before being initialized}}
}

struct Point {
    let a: Int // expected-note {{change 'let' to 'var' to make it mutable}}
    var b: Int // expected-note 5 {{'self.b' not initialized}}
    var c: Int = 0
    var d: Int?
    var computed: Int {
        return a
    }
    static var shared: Int = 0
    lazy var lazy: Int = 1

    init(f: Bool) {
        a = 1
        if f {
            b = 1
        }
    } // expected-error@:5 {{return from initializer without initializing all stored properties}}

    init(x: Bool) {
        a = 1
        if x {
            return // expected-error@:13 {{return from initializer without initializing all stored properties}}
        }
        b = 2
    }

    init(y: Bool) {
        a = 1
        a = 2 // expected-error@:11 {{immutable value 'self.a' may only be initialized once}}
        b = 3
    }

    init(z: Bool) {
        self.a = 1
        print(self.b) // expected-error@:20 {{'self' used before all stored properties are initialized}}
        self.b = 1
    }

    init(w: Bool) {
        a = 1
        touch() // expected-error@:9 {{'self' used before all stored properties are initialized}}
        b = 1
    }

    init(v: Bool) {
        a = 1
        use(computed) // expected-error@:13 {{'self' used before all stored properties are initialized}}
        b = 1
    }

    init(u: Bool) {
        self.init(x: u)
    }

    init(all: Bool) {
        a = 1
        b = 2
        touch()
        use(computed)
    }

    func touch() {
    }
}

class Shape {
    let p: Int
    var q: String // expected-note 3 {{'self.q' not initialized}}
    var area: Int {
        return 1
    }

    init(_ b: Bool) {
        p = 1
        if b {
            q = ""
        }
    } // expected-error@:5 {{return from initializer without initializing all stored properties}}

    init(read: Bool) {
        p = 1
        print(q) // expected-error@:15 {{constant 'self.q' used before being initialized}}
        q = ""
    }

    init(method: Bool) {
        p = 1
        draw() // expected-error@:9 {{'self' used in method call 'draw' before all stored properties are initialized}}
        q = ""
    }

    init(property: Bool) {
        p = 1
        use(area) // expected-error@:13 {{'self' used in property access 'area' before all stored properties are initialized}}
        q = ""
    }

    init(itself: Bool) {
        p = 1
        print(self) // expected-error@:15 {{variable 'self.q' used before being initialized}}
        q = ""
    }

    init?(failable: Bool) {
        if failable {
            return nil
        }
        p = 1
        q = ""
    }

    init(throwing: Bool) throws {
        p = 1
        throw Failure.bad
    }

    func draw() {
    }
}

class Square: Shape {
    var side: Int

    override init(_ b: Bool) {
        super.init(b) // expected-error@:15 {{property 'self.side' not initialized at super.init call}}
        side = 1
    }
}
