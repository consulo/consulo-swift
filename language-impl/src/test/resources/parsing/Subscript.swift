struct Matrix {
    var rows: Int
    var cols: Int
    var data: [Double]

    subscript(row: Int, col: Int) -> Double {
        get { return data[row * cols + col] }
        set { data[row * cols + col] = newValue }
    }

    subscript(index: Int) -> Double {
        return data[index]
    }
}
