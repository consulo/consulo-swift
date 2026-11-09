enum Failure: Error {
    case bad
}

func die() -> Never {
    fatalError()
}

func use(_ x: Int) {
}

func value() -> Int {
    return 1
}

struct Item {
    func touch() {
    }
}

var flag = true

func afterReturn() -> Int {
    return 1
    print("a") // expected-warning@:5 {{code after 'return' will never be executed}}
}

func afterThrow() throws {
    throw Failure.bad
    print("b") // expected-warning@:5 {{code after 'throw' will never be executed}}
}

func afterJumps(_ items: [Int]) {
    for item in items {
        if item > 1 {
            continue
            print(item) // expected-warning@:13 {{code after 'continue' will never be executed}}
        }
        break
        print("c") // expected-warning@:9 {{code after 'break' will never be executed}}
    }
}

func afterBranches(_ b: Bool, _ x: Int) -> Int {
    if b {
        return 1
    } else {
        return 2
    }
    let z = 3 // expected-warning@:5 {{will never be executed}}
    return z
}

func afterSwitch(_ x: Int) -> Int {
    switch x {
    case 1:
        return 1
    default:
        return 2
    }
    print("d") // expected-warning@:5 {{will never be executed}}
}

func afterDo() -> Int {
    do {
        return 1
    }
    print("e") // expected-warning@:5 {{will never be executed}}
}

func afterInfiniteLoop() {
    while true {
    }
    print("not reported")
}

func constantIf() {
    if false {
        print("not reported")
    }
}

func afterNever1() {
    fatalError() // expected-note {{a call to a never-returning function}}
    print("f") // expected-warning@:11 {{will never be executed}}
}

func afterNever2() {
    die() // expected-note {{a call to a never-returning function}}
    use(1) // expected-warning@:9 {{will never be executed}}
}

func afterNever3() -> Int {
    fatalError("x") // expected-note {{a call to a never-returning function}}
    return 1 // expected-warning@:12 {{will never be executed}}
}

func afterNever4() {
    fatalError() // expected-note {{a call to a never-returning function}}
    _ = value() // expected-warning@:9 {{will never be executed}}
}

func afterNever5(t: Item) {
    fatalError() // expected-note {{a call to a never-returning function}}
    t.touch() // expected-warning@:7 {{will never be executed}}
}

func afterNever6() throws {
    fatalError() // expected-note {{a call to a never-returning function}}
    throw Failure.bad // expected-warning@:19 {{will never be executed}}
}

func afterNever7() {
    fatalError() // expected-note {{a call to a never-returning function}}
    return // expected-warning@:5 {{will never be executed}}
}

func afterNever8(_ x: Int) -> Int {
    fatalError() // expected-note {{a call to a never-returning function}}
    return x // expected-warning@:5 {{will never be executed}}
}

func afterNever9() {
    fatalError() // expected-note {{a call to a never-returning function}}
    if flag { // expected-warning@:8 {{will never be executed}}
        print("g")
    }
}

func neverEnd(_ b: Bool) -> Int {
    if b {
        return 1
    }
    fatalError()
}

func missing(_ b: Bool) -> Int {
    if b {
        return 1
    }
} // expected-error@:1 {{missing return in global function expected to return 'Int'}}

func missingDictionary(_ b: Bool) -> [Int: String] {
    if b {
        return [:]
    }
} // expected-error@:1 {{missing return in global function expected to return '[Int : String]'}}

func missingTuple(_ b: Bool) -> (Int, Int)? {
    if b {
        return (1, 2)
    }
} // expected-error@:1 {{missing return in global function expected to return '(Int, Int)?'}}

func infinite() -> Int {
    while true {
    }
}

func infiniteBreak(_ b: Bool) -> Int {
    while true {
        if b {
            break
        }
    }
} // expected-error@:1 {{missing return in global function expected to return 'Int'}}

func single() -> Int {
    value()
}

func ifExpression(_ b: Bool) -> Int {
    if b { 1 } else { 2 }
}

func never() -> Never {
} // expected-error@:1 {{global function with uninhabited return type 'Never' is missing call to another never-returning function on all paths}}

struct Holder {
    var flag: Bool

    func method() -> Int {
        if flag {
            return 1
        }
    } // expected-error@:5 {{missing return in instance method expected to return 'Int'}}

    static func create(_ b: Bool) -> Holder {
        if b {
            return Holder(flag: b)
        }
    } // expected-error@:5 {{missing return in static method expected to return 'Holder'}}

    func copy() -> Self {
        if flag {
            return self
        }
    } // expected-error@:5 {{missing return in instance method expected to return 'Holder'}}

    var computed: Int {
        if flag {
            return 1
        }
    } // expected-error@:5 {{missing return in getter expected to return 'Int'}}

    var folded: Int {
        if true {
            return 1
        }
    }

    var accessor: Int {
        get {
            if flag {
                return 1
            }
        } // expected-error@:9 {{missing return in getter expected to return 'Int'}}
        set {
        }
    }

    subscript(i: Int) -> Int {
        if flag {
            return i
        }
    } // expected-error@:5 {{missing return in getter expected to return 'Int'}}

    func outer() -> Int {
        func local() -> Int {
            if flag {
                return 1
            }
        } // expected-error@:9 {{missing return in local function expected to return 'Int'}}
        let closure = { () -> Int in
            if flag {
                return 1
            }
        } // expected-error@:9 {{missing return in closure expected to return 'Int'}}
        return local() + closure()
    }
}

class Base {
    class func make(_ b: Bool) -> Int {
        if b {
            return 1
        }
    } // expected-error@:5 {{missing return in class method expected to return 'Int'}}
}

func guards(_ b: Bool, _ o: Int?) -> Int {
    guard b else {
        return 0
    }
    guard let v = o else {
        die()
    }
    guard b else {
        fatalError()
    }
    guard b else {
        while true {
        }
    }
    guard b else {
        if v > 1 {
            return 1
        }
    } // expected-error@:5 {{'guard' body must not fall through, consider using a 'return' or 'throw' to exit the scope}}
    return v
}
