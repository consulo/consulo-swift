let square = { (n: Int) -> Int in n * n }

let add = { (a: Int, b: Int) in a + b }

let shorthand: (Int, Int) -> Int = { $0 + $1 }

func apply(_ f: (Int) -> Int, to x: Int) -> Int {
    return f(x)
}

let r = apply({ x in x * 2 }, to: 5)

let trailing = [1, 2, 3].map { $0 * 2 }

class Owner {
    var name = ""
    lazy var printer: () -> Void = { [weak self] in
        print(self?.name ?? "")
    }
}

let captured = { [unowned o] in print(o) }
