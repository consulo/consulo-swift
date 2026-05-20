struct Point {
    var x: Double
    var y: Double

    mutating func translate(dx: Double, dy: Double) {
        x = x + dx
        y = y + dy
    }
}

public struct Rectangle {
    let origin: Point
    let size: Size

    nonmutating func area() -> Double {
        return size.width * size.height
    }
}

struct Size {
    var width: Double = 0
    var height: Double = 0
}
