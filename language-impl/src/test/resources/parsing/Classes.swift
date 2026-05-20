class Animal {
    let name: String
    var age: Int

    init(name: String, age: Int) {
        self.name = name
        self.age = age
    }

    func describe() -> String {
        return "\(name) is \(age)"
    }

    deinit {
        print("freed", name)
    }
}

final class Dog: Animal {
    override func describe() -> String {
        return super.describe() + " (dog)"
    }

    convenience init(name: String) {
        self.init(name: name, age: 0)
    }
}

public class Cat: Animal {
    private var lives: Int = 9
    static let species = "Felis catus"
}
