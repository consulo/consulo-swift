func loops(_ items: [Int], _ b: Bool) {
    var i = 0
    while i < 10 {
        i += 1
    }
    repeat {
        i -= 1
    } while i > 0
    for item in items where item > 1 {
        print(item)
    }
}

func infinite() -> Int {
    while true {
    }
}

func infiniteBreak(_ b: Bool) -> Int {
    while true {
        if b {
            break
        }
    }
}
