import Testing
import Url

@Suite("Url Swift Export Suite")
struct UrlExportTests {
    @Test("Swift module loads cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true), "Url swift module imported cleanly")
    }
}
