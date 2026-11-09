enum Failure: Error {
    case bad
    case worse
}

func risky() throws -> Int {
    return 1
}

func handle() -> Int {
    do {
        let x = try risky()
        return x
    } catch Failure.bad {
        return -1
    } catch {
        return -2
    }
}

func rethrowing() throws -> Int {
    defer { print("done") }
    do {
        return try risky()
    } catch Failure.bad {
        return 0
    }
}

func throwing(_ b: Bool) throws {
    if b {
        throw Failure.worse
    }
    let y = try? risky()
    print(y ?? 0)
}
