struct SettingsView: View {
    @State private var showDetails = false
    @State private var name = ""

    var body: some View {
        VStack {
            Toggle("Show details", isOn: $showDetails)

            TextField("Enter name", text: $name)

            if showDetails {
                VStack {
                    Text("Detail line 1")
                    Text("Detail line 2")
                }
            }

            Button("Save") {
                save(name)
            }
        }
    }
}
