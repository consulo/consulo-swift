let top = 1
print(top)

struct Point {
    var x: Int
    var y: Int

    init(x: Int) {
        self.x = x
        self.y = 0
    }

    var length: Int {
        x + y
    }

    var scaled: Int {
        get {
            return x * 2
        }
        set {
            x = newValue / 2
        }
    }

    func apply(_ f: (Int) -> Int) -> Int {
        let g = { (v: Int) -> Int in
            return f(v) + self.x
        }
        func nested() -> Int {
            return g(1)
        }
        return nested()
    }
}

class Base {
    init(_ v: Int) {
    }
}

class Derived: Base {
    var value: Int
    var twice: Int {
        return value * 2
    }

    init() {
        value = 1
        super.init(value)
        report(twice)
        print(self)
    }

    func report(_ v: Int) {
    }
}
