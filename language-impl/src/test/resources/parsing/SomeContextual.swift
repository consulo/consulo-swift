// === Contextual keywords in their KEYWORD positions ===

// type-marker
func view() -> some View {
    return Text("hi")
}

protocol P {
    associatedtype Item
    var item: some P { get }
}

// declaration modifiers
final class C {}
public lazy var counter: Int = 0
weak var owner: Owner?
unowned let parent: Parent

class Animal {
    convenience init() { self.init(name: "") }
    final func describe() {}
    override func hashed() {}
    required init(value: Int) {}
}

struct Point {
    mutating func translate(dx: Int) {}
    nonmutating func area() -> Int { return 0 }
}

dynamic var name: String = ""
optional func optionalMethod()

// custom operators
prefix operator +++
postfix operator ---
infix operator **

// property accessors
class Box {
    var stored: Int {
        get { return 42 }
        set(v) { print(v) }
    }
    var observed: Int = 0 {
        willSet { print("before") }
        didSet { print("after") }
    }
}

// precedencegroup body
precedencegroup MyGroup {
    higherThan: MultiplicationPrecedence
    lowerThan: AdditionPrecedence
    associativity: left
    assignment: false
}

precedencegroup OtherGroup {
    associativity: right
}

precedencegroup NoneGroup {
    associativity: none
}

// indirect enum
indirect enum Tree {
    case leaf
    case node(Tree, Int, Tree)
}


// === Same words used as plain IDENTIFIERS in non-keyword positions ===

// As variable names
let some = 5
let final = 7
let lazy = 8
let weak = 9
let unowned = 10
let mutating = 11
let nonmutating = 12
let override = 13
let required = 14
let optional = 15
let dynamic = 16
let convenience = 17
let prefix = 18
let infix = 19
let postfix = 20
let indirect = 21
let get = 22
let set = 23
let willSet = 24
let didSet = 25
let left = 26
let right = 27
let none = 28
let associativity = 29
let higherThan = 30
let lowerThan = 31
let assignment = 32

// As function names
func some() -> Int { return 1 }
func lazy() -> Int { return 2 }
func get() -> Int { return 3 }
func left() -> Int { return 4 }

// As parameter labels
func draw(left x: Int, right y: Int, none z: Int) {}

// As struct members
struct Settings {
    var lazy: Bool = false
    var final: Bool = false
    var mutating: Int = 0
}

// Optional.some/.none patterns still work
let x: Int? = 5
if case .some(let value) = x {
    print(value)
}
if case .none = x {
    print("nil")
}

let r = x.some
let n = x.none
