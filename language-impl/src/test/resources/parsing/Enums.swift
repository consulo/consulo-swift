enum Direction {
    case north
    case south
    case east
    case west
}

enum Result {
    case success(String)
    case failure(Int, String)
}

enum Status: Int {
    case ok = 200
    case notFound = 404
    case error = 500
}

indirect enum Tree {
    case leaf
    case node(Tree, Int, Tree)
}
