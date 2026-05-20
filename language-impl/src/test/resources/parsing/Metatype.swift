let intType: Int.Type = Int.self
let stringType = String.self
let protoType: Comparable.Protocol = Comparable.self

func make<T>(_ kind: T.Type) -> T? { return nil }
