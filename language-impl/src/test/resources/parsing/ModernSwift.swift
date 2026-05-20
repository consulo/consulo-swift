// =========================================================================
//  Swift 4.0+ — multi-line strings, key paths
// =========================================================================

let multiline = """
    line 1
    line 2 with "embedded" quotes
    line 3
    """

let keyPath = \String.count
let nestedPath = \Person.address.city

// =========================================================================
//  Swift 5.0+ — raw strings
// =========================================================================

let rawSimple = #"no \n escape here"#
let rawNested = ##"can contain "#" inside"##
let rawMulti = #"""
    raw multi
    line
    """#

// =========================================================================
//  Swift 5.1+ — opaque types `some`, property wrappers
// =========================================================================

func body() -> some View {
    return Text("hi")
}

@propertyWrapper
struct Wrapped<T> {
    var wrappedValue: T
}

class State {
    @Wrapped var counter: Int = 0
}

// =========================================================================
//  Swift 5.3+ — multi-trailing-closure, @main
// =========================================================================

@main
struct App {
    static func main() {}
}

func withAnimation(_ a: Int, body: () -> Void, completion: () -> Void) {}

withAnimation(0) {
    print("animating")
} completion: {
    print("done")
}

// =========================================================================
//  Swift 5.5+ — async / await / actor / nonisolated / isolated
// =========================================================================

func loadData() async throws -> Data {
    let bytes = await fetch()
    return try await process(bytes)
}

func parallel() async {
    async let a = compute(1)
    async let b = compute(2)
    let sum = await a + b
    _ = sum
}

actor Counter {
    private var value = 0

    func increment() {
        value = value + 1
    }

    nonisolated func description() -> String {
        return "counter"
    }
}

distributed actor RemoteCounter {
    distributed func increment() {}
}

func sleep(seconds: Double, isolated worker: isolated Counter) async {}

// =========================================================================
//  Swift 5.6+ — `any` existential type
// =========================================================================

let seq: any Sequence = [1, 2, 3]
let collection: any Collection = []

func process(_ item: any Equatable) {}

// =========================================================================
//  Swift 5.7+ — primary associated types
// =========================================================================

func numbers() -> some Collection<Int> {
    return [1, 2, 3]
}

func anyNumbers() -> any Collection<Int> {
    return [1, 2, 3]
}

// =========================================================================
//  Swift 5.9+ — if/switch as expressions, ownership, package modifier
// =========================================================================

let label = if score > 100 { "high" } else if score > 50 { "mid" } else { "low" }

let kind = switch value {
case 0: "zero"
case 1: "one"
default: "many"
}

borrowing func read(_ value: Int) {}
consuming func take(_ value: Int) {}

package class PackageWide {}
package(set) var packageWritable: Int = 0

// =========================================================================
//  Swift 6.0+ — typed throws
// =========================================================================

enum NetworkError: Error {
    case offline
}

func fetch() throws(NetworkError) -> Data {
    throw NetworkError.offline
}

func mayRethrow(_ body: () throws(NetworkError) -> Void) rethrows {
    try body()
}
