let kind = type(of: self)
let nameKind = type(of: "hello")

func describe<T>(_ value: T) -> String {
    return "\(type(of: value))"
}
