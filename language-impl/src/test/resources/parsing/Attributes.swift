@available(iOS 11, *)
class Modern {
    @discardableResult
    func work() -> Int { return 0 }

    @objc dynamic var name: String = ""
}

@inline(__always)
func fast() {}

@warn_unused_result
public func compute() -> Int { return 0 }

@available(*, deprecated, message: "use newApi instead")
func oldApi() {}
