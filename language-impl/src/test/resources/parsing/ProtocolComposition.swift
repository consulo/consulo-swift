protocol A {}
protocol B {}
protocol C {}

func combined(value: A & B) {}

typealias AB = A & B
typealias ABC = A & B & C

func generic<T: A & B>(_ x: T) {}
