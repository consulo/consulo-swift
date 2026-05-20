let x = 10

if x > 0 {
    print("positive")
} else if x < 0 {
    print("negative")
} else {
    print("zero")
}

if let value = optionalThing() {
    print(value)
}

guard let v = optionalThing() else {
    return
}

switch x {
case 0:
    print("zero")
case 1, 2, 3:
    print("small")
case let n where n > 100:
    print("big", n)
case 4 ... 10:
    print("medium")
default:
    print("other")
}
