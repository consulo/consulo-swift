let pi = 3.14
let name: String = "Swift"
var counter = 0
var items: [Int] = [1, 2, 3]

var computed: Int {
    return counter * 2
}

var observed: Int = 0 {
    willSet { print("about to set", newValue) }
    didSet { print("did set", oldValue) }
}

class Box {
    var stored: Int {
        get { return 42 }
        set(v) { print(v) }
    }
}
