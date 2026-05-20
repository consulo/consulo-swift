enum NetworkError: Error {
    case timeout
    case offline
    case http(Int)
}

func fetch(url: String) throws -> String {
    if url.isEmpty {
        throw NetworkError.offline
    }
    return ""
}

do {
    let s = try fetch(url: "https://x")
    print(s)
} catch NetworkError.timeout {
    print("timed out")
} catch NetworkError.http(let code) where code >= 500 {
    print("server error", code)
} catch let err {
    print("other error", err)
}

let optional1 = try? fetch(url: "")
let forced = try! fetch(url: "")

func defered() {
    defer {
        print("cleanup")
    }
    print("work")
}
