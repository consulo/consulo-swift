let nothing: Int? = nil
var maybe: String? = nil

if maybe == nil {
    maybe = "set"
}

let coalesced = maybe ?? "default"
