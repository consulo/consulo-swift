let tuple = (1, "hello", true)
let (a, b, c) = tuple

switch tuple {
case (0, _, _):
    print("first zero")
case (_, "hello", let flag):
    print("hi", flag)
case (let x, _, _) where x > 0:
    print("pos", x)
default:
    print("other")
}

let any: Any = 42

switch any {
case is Int:
    print("int")
case let s as String:
    print("string", s)
case .none:
    print("nothing")
default:
    print("unknown")
}

if case let .success(value) = someResult {
    print(value)
}
